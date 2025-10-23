package cardboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitViewController
import cardboard.util.createNotificationObserver
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.cValue
import kotlinx.cinterop.useContents
import platform.darwin.NSObject
import platform.CoreGraphics.CGSize
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLDeviceProtocol
import platform.Metal.MTLPixelFormatDepth16Unorm
import platform.Metal.MTLPixelFormatRGBA8Unorm
import platform.Metal.MTLClearColorMake
import platform.MetalKit.MTKView
import platform.MetalKit.MTKViewDelegateProtocol
import platform.UIKit.*
import platform.UIKitCompat.UIViewControllerImmersionProtocol
import platform.Foundation.*

actual abstract class NativeContext

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberNativeRenderView(): NativeRenderView = remember { IosMetalNativeRenderView() }

@OptIn(ExperimentalForeignApi::class)
class IosMetalNativeRenderView(
    val device: MTLDeviceProtocol = MTLCreateSystemDefaultDevice() ?: error("Metal is not supported on this device")
): NativeRenderView() {
    override val context: NativeContext? = null
    var view: MTKView? = null

    private var _window: UIWindow? = null
    private val window get() = view?.window?.also { _window = it } ?: _window

    private inner class MTKViewController(nibName: String? = null, bundle: NSBundle? = null): UIViewController(nibName, bundle), MTKViewDelegateProtocol, UIViewControllerImmersionProtocol {
        override fun loadView() {
            setView(MTKView().apply {
                device = this@IosMetalNativeRenderView.device
                colorPixelFormat = MTLPixelFormatRGBA8Unorm
                depthStencilPixelFormat = MTLPixelFormatDepth16Unorm
                sampleCount = 1uL // 4uL
                preferredFramesPerSecond = 60
                paused = true
                enableSetNeedsDisplay = true
                clearColor = MTLClearColorMake(0.0, 0.0, 0.0, 1.0)
                delegate = this@MTKViewController
                addGestureRecognizer(UITapGestureRecognizer(this@MTKViewController, NSSelectorFromString("trigger")))
                this@IosMetalNativeRenderView.view = this
            })
        }
        override fun mtkView(view: MTKView, drawableSizeWillChange: CValue<CGSize>) {
            drawableSizeWillChange.useContents { resize(width.toInt(), height.toInt()) }
        }
        override fun drawInMTKView(view: MTKView) { draw() }

        @Suppress("unused")
        @OptIn(BetaInteropApi::class)
        @ObjCAction // the selector must be in a class that inherits from NSObject
        fun trigger() { this@IosMetalNativeRenderView.trigger() }

        override fun viewWillAppear(animated: Boolean) {
            super.viewWillAppear(animated)
            this@IosMetalNativeRenderView.init()
        }
        override fun viewDidAppear(animated: Boolean) {
            super.viewDidAppear(animated)
            immersive()
        }
        override fun viewWillDisappear(animated: Boolean) {
            super.viewWillDisappear(animated)
            resetImmersive()
        }
        override fun viewDidDisappear(animated: Boolean) {
            super.viewDidDisappear(animated)
            destroy()
        }

        override fun prefersStatusBarHidden() = true
        override fun prefersHomeIndicatorAutoHidden() = true
        override fun supportedInterfaceOrientations() = UIInterfaceOrientationMaskLandscapeRight
    }

    override fun onInit() {
        super.onInit()
        didBecomeActive.enable()
        willResignActive.enable()
        resume()
    }
    override fun onDestroy() {
        didBecomeActive.disable()
        willResignActive.disable()
        super.onDestroy()
    }
    override val isPaused get() = view?.paused != false
    override fun onResume() {
        view?.paused = false
        super.onResume()
        // the view doesn't necessarily have a window yet, so these may not all do anything...
        immersive()
    }
    override fun onPause() {
        super.onPause()
        view?.paused = true
        resetImmersive()  // we need to reset brightness and status bar when in background
    }

    private val didBecomeActive = createNotificationObserver("UIApplicationDidBecomeActiveNotification") { resume() }
    private val willResignActive = createNotificationObserver("UIApplicationWillResignActiveNotification") { pause() }

    @OptIn(ExperimentalForeignApi::class)
    @Composable
    override fun RenderView(modifier: Modifier) {
        UIKitViewController(
            modifier = modifier,
            factory = { MTKViewController() },
        )
    }

    // CardboardRenderer calls these methods right before displaying the view
    // and right after the view is removed from the hierarchy. However, on iOS,
    // they almost all need to be called AFTER the view is added to the window
    // and BEFORE the view is removed from the window. So, we make them
    // reentrant and tolerant and call them in several places:
    //  - onResume/onPause [which also happens at the wrong time sometimes]
    //  - viewDidAppear/viewDidDisappear [at the right time]
    override fun setFullscreen() { updateFullscreen(true) }
    override fun resetFullscreen() { updateFullscreen(false) }
    private fun updateFullscreen(on: Boolean) {
        // Ways to do this:
        //   - use UIApplication.sharedApplication.setStatusBarHidden(...) - but this is deprecated in iOS 10+ and can cause problems if the app is backgrounded while active
        //   - use the Compose UIViewController's config delegate's prefersStatusBarHidden property - but this is deprecated in Compose
        //   - override prefersStatusBarHidden in our UIViewController, override childViewControllerForStatusBarHidden or similar in the root view controller, then call setNeedsStatusBarAppearanceUpdate() on the root view controller
        //UIApplication.sharedApplication.setStatusBarHidden(on, UIStatusBarAnimation.UIStatusBarAnimationNone)
        window?.rootViewController?.apply {
            setNeedsStatusBarAppearanceUpdate()
            setNeedsUpdateOfHomeIndicatorAutoHidden()
        }
    }
    private var origBrightness: Double? = null
    override fun setScreenBrightnessFull() {
        updateBrightness(true)
        if (origBrightness === null) {
            window?.windowScene?.screen?.let { scrn ->
                origBrightness = scrn.brightness
                scrn.brightness = 1.0
            }
        }
    }
    override fun resetBrightness() {
        updateBrightness(false)
        origBrightness?.let {
            window?.windowScene?.screen?.let { scrn ->
                scrn.brightness = it
                origBrightness = null
            }
        }
    }
    private fun updateBrightness(on: Boolean) {
        UIApplication.sharedApplication.idleTimerDisabled = on
    }
    override fun forceLandscape() {
        updateOrientation(appDelegate, UIInterfaceOrientationMaskLandscapeRight, UIDeviceOrientation.UIDeviceOrientationLandscapeLeft)
    }
    override fun resetOrientation() {
        updateOrientation(null, UIInterfaceOrientationMaskAllButUpsideDown, UIDeviceOrientation.UIDeviceOrientationUnknown)
    }
    private fun updateOrientation(
        delegate: AppDelegate?,
        mask: UIInterfaceOrientationMask, orientation: UIDeviceOrientation
    ) {
        UIApplication.sharedApplication.delegate = delegate
        window?.rootViewController?.apply {
            setNeedsUpdateOfSupportedInterfaceOrientations()
            // not supported yet: setNeedsUpdateOfPrefersInterfaceOrientationLocked()
        }
        window?.windowScene?.setOrientation(mask, orientation)
    }

    private val appDelegate = AppDelegate()
    private class AppDelegate: NSObject(), UIApplicationDelegateProtocol {
        override fun application(application: UIApplication, supportedInterfaceOrientationsForWindow: UIWindow?) =
            UIInterfaceOrientationMaskLandscapeRight
    }

    private companion object {
        private val pi = NSProcessInfo()
        fun iosAtLeast(major: Int, minor: Int = 0, patch: Int = 0) =
            pi.isOperatingSystemAtLeastVersion(cValue<NSOperatingSystemVersion> {
                majorVersion = major.toLong()
                minorVersion = minor.toLong()
                patchVersion = patch.toLong()
            })

        // Note: interface orientation is opposite to device orientation (i.e. left == right)
        fun UIWindowScene.setOrientation(mask: UIInterfaceOrientationMask, orientation: UIDeviceOrientation) {
            if (iosAtLeast(16)) {
                requestGeometryUpdateWithPreferences(UIWindowSceneGeometryPreferencesIOS(mask)) {
                    println("Request Geometry Update Error: $it")
                }
            }

            // this is deprecated in iOS 16, but still works for now
            UIDevice.currentDevice.setValue(orientation.value, forKey = "orientation")
            UIView.setAnimationsEnabled(true)
            UIViewController.attemptRotationToDeviceOrientation()
        }

        val UIViewController.rootViewController: UIViewController get() {
            view.window?.rootViewController?.also { return it }
            var parent = this
            while (parent.parentViewController != null) {
                parent = parent.parentViewController!!
            }
            return parent
        }

        val UIResponder.parentViewController: UIViewController? get() =
            nextResponder as? UIViewController ?: nextResponder?.parentViewController
    }
}
