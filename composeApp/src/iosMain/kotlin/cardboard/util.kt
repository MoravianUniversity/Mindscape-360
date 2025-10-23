/**
 * Utils for working with CoreFoundation and CoreVideo types in Kotlin/Native.
 *
 * Due to special handling of these types in Kotlin/Native, the IDE gives lots of errors in this
 * file, but they are all false positives. The code compiles and works correctly.
 */
package cardboard

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreVideo.*
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Metal.MTLDeviceProtocol
import platform.Metal.MTLPixelFormatBGRA8Unorm
import platform.Metal.MTLTextureProtocol
import platform.UIKit.UIInterfaceOrientationMaskAllButUpsideDown
import platform.UIKit.UIViewAutoresizingFlexibleHeight
import platform.UIKit.UIViewAutoresizingFlexibleWidth
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.childViewControllers
import platform.UIKit.didMoveToParentViewController
import platform.UIKit.prefersHomeIndicatorAutoHidden
import platform.UIKit.supportedInterfaceOrientations
import platform.UIKit.willMoveToParentViewController
import platform.UIKitCompat.UIViewControllerImmersionProtocol
import kotlin.collections.any


@Suppress("MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE")
@OptIn(ExperimentalForeignApi::class)
class MetalTextureCache(device: MTLDeviceProtocol) {
    private var _native: CVMetalTextureCacheRef? = create(device)
    val native: CVMetalTextureCacheRef
        get() = _native ?: throw IllegalStateException("TextureCache is not initialized")
    private var _textureRef: CVMetalTextureRefVar? = nativeHeap.alloc<CVMetalTextureRefVar>()
    private val cleanTexCoords = FloatArray(8)

    fun destroy() {
        _native?.let { nativeHeap.free(it); _native = null }
        _textureRef?.let { nativeHeap.free(it); _textureRef = null }
    }

    fun createTextureFromImage(buffer: CVPixelBufferRef, plane: Int = 0, matrix: FloatArray? = null): MTLTextureProtocol {
        val width = CVPixelBufferGetWidth(buffer)
        val height = CVPixelBufferGetHeight(buffer)
        val retval = CVMetalTextureCacheCreateTextureFromImage(
            kCFAllocatorDefault, native, buffer, null,
            MTLPixelFormatBGRA8Unorm,
            width, height, plane.toULong(), _textureRef!!.ptr
        )
        if (retval != kCVReturnSuccess) { error("Failed to create texture from cache: $retval") }
        val textureRef = _textureRef?.value ?: error("Texture reference is not initialized")
        try {
            if (matrix != null) {
                cleanTexCoords.usePinned { CVMetalTextureGetCleanTexCoords(textureRef,
                    it.addressOf(0), it.addressOf(2), it.addressOf(4), it.addressOf(6))
                }
                setSTMatrix(matrix, cleanTexCoords)
            }
            return (CVMetalTextureGetTexture(textureRef) ?: error("Failed to get texture from CVMetalTexture")) as MTLTextureProtocol
        } finally {
            CVBufferRelease(textureRef)
            _textureRef?.value = null  // reset the texture reference for next use
         }
    }

    private companion object {
        fun create(device: MTLDeviceProtocol): CVMetalTextureCacheRef {
            val dev = device as objcnames.protocols.MTLDeviceProtocol  // cast to the special forward declaration type
            val textureCacheVar = nativeHeap.alloc<CVMetalTextureCacheRefVar>()
            try {
                val retval = CVMetalTextureCacheCreate(kCFAllocatorDefault, null, dev, null, textureCacheVar.ptr)
                if (retval != kCVReturnSuccess) { error("Failed to create texture cache: $retval") }
                return textureCacheVar.value ?: error("Failed to create texture cache")
            } catch (e: Throwable) { nativeHeap.free(textureCacheVar); throw e }
        }

        fun setSTMatrix(m: FloatArray, cleanTexCoords: FloatArray) {
            require(m.size == 16) { "Matrix must have exactly 16 elements" }
            // typically [0.0, 1.0, (lower-left)
            //            1.0, 1.0, (lower-right)
            //            1.0, 0.0, (upper-right)
            //            0.0, 0.0] (upper-left)
            // this assumes that the coordinates form an axis-aligned rectangle
            val lower = cleanTexCoords[1]  // == cleanTexCoords[3] == 1.0
            val upper = cleanTexCoords[5]  // == cleanTexCoords[7] == 0.0
            val left  = cleanTexCoords[0]  // == cleanTexCoords[6] == 0.0
            val right = cleanTexCoords[2]  // == cleanTexCoords[4] == 1.0
            val width = right - left
            val height = upper - lower
            m[0*4 + 0] = width; m[0*4 + 1] = 0f;     m[0*4 + 2] = 0f; m[0*4 + 3] = 0f
            m[1*4 + 0] = 0f;    m[1*4 + 1] = height; m[1*4 + 2] = 0f; m[1*4 + 3] = 0f
            m[2*4 + 0] = 0f;    m[2*4 + 1] = 0f;     m[2*4 + 2] = 1f; m[2*4 + 3] = 0f
            m[3*4 + 0] = left;  m[3*4 + 1] = lower;  m[3*4 + 2] = 0f; m[3*4 + 3] = 1f // TODO: this may not be correct when left != 0.0 or lower != 1.0
        }
    }
}


/**
 * Converts a [CFStringRef] to a Kotlin [String]. Does not transfer ownership of the underlying
 * CFStringRef (so it is safe to use with global constants but also means that you need to
 * release the reference yourself if not a global constant).
 */
@OptIn(ExperimentalForeignApi::class)
fun CFStringRef.toKString(): String {
    val objCStr = CFBridgingRelease(this)
    CFBridgingRetain(objCStr) // transfer ownership back to CF
    return objCStr as String
}


/**
 * A UIViewController wrapper that allows customizing the behavior of the main view controller
 * and supports immersion protocols:
 * - `prefersStatusBarHidden`
 * - `prefersHomeIndicatorAutoHidden`
 * - `supportedInterfaceOrientations`
 * Each of those properties is computed by combining the values from all child view controllers
 * (found recursively).
 *
 * Use like this in the MainViewController.kt file:
 *    fun MainViewController() = MainViewControllerWrapper(ComposeUIViewController { App() })
 */
@OptIn(ExperimentalForeignApi::class)
class MainViewControllerWrapper(val vc: UIViewController): UIViewController(null, null), UIViewControllerImmersionProtocol {
    @OptIn(ExperimentalForeignApi::class)
    override fun loadView() {
        super.loadView()
        vc.willMoveToParentViewController(this)
        vc.view.setFrame(view.frame)
        vc.view.autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        view.addSubview(vc.view)
        view.autoresizingMask = UIViewAutoresizingFlexibleWidth or UIViewAutoresizingFlexibleHeight
        addChildViewController(vc)
        vc.didMoveToParentViewController(this)
    }

    override fun prefersStatusBarHidden() = anyPrefersStatusBarHidden()
    override fun prefersHomeIndicatorAutoHidden() = anyPrefersHomeIndicatorAutoHidden()
    override fun supportedInterfaceOrientations() = combinedSupportedInterfaceOrientations()
    companion object {
        fun UIViewController.anyPrefersStatusBarHidden() = any { prefersStatusBarHidden }
        fun UIViewController.anyPrefersHomeIndicatorAutoHidden() = any { prefersHomeIndicatorAutoHidden }
        fun UIViewController.combinedSupportedInterfaceOrientations() =
            combine(UIInterfaceOrientationMaskAllButUpsideDown, { supportedInterfaceOrientations }) { a, b -> a and b }

        private fun UIViewController.any(
            get: UIViewController.() -> Boolean,
        ): Boolean = childViewControllers.any { (it as? UIViewController)?.let { it.get() || it.any(get) } == true }

        private fun <T> UIViewController.combine(
            start: T,
            get: UIViewController.() -> T,
            combine: (T, T) -> T,
        ): T = childViewControllers.fold(start) { result, child ->
            (child as? UIViewController)?.let {
                it.combine(combine(result, it.get()), get, combine)
            } ?: result
        }
    }
}
