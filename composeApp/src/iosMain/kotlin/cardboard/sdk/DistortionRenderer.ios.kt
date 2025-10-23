@file:OptIn(ExperimentalForeignApi::class)
@file:Suppress("MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE")

package cardboard.sdk

import cardboard.native.*
import kotlinx.cinterop.*
import platform.Foundation.CFBridgingRetain
import platform.Foundation.CFBridgingRelease
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLDeviceProtocol
import platform.Metal.MTLPixelFormat
import platform.Metal.MTLPixelFormatDepth16Unorm
import platform.Metal.MTLPixelFormatRGBA8Unorm
import platform.Metal.MTLPixelFormatStencil8
import platform.Metal.MTLRenderCommandEncoderProtocol

private typealias DistortionRendererPtr = CPointer<cnames.structs.CardboardDistortionRenderer>?

data class MetalDistortionRendererTarget(
    var renderEncoder: MTLRenderCommandEncoderProtocol? = null,
    val screenWidth: Int = 0,
    val screenHeight: Int = 0,
) : DistortionRendererTarget

@OptIn(ExperimentalForeignApi::class)
class IosMetalDistortionRenderer(
    private var nativePointer: DistortionRendererPtr?
) : DistortionRenderer {
    constructor(
        colorAttachmentPixelFormat: MTLPixelFormat = MTLPixelFormatRGBA8Unorm,
        depthAttachmentPixelFormat: MTLPixelFormat = MTLPixelFormatDepth16Unorm,
        stencilAttachmentPixelFormat: MTLPixelFormat = MTLPixelFormatStencil8,
        device: MTLDeviceProtocol = MTLCreateSystemDefaultDevice()!!
    ) : this(create(
        colorAttachmentPixelFormat,
        depthAttachmentPixelFormat,
        stencilAttachmentPixelFormat,
        device
    ))

    init {
        require(nativePointer != null) { "Failed to create CardboardDistortionRenderer" }
    }

    companion object {
        fun create(
            colorAttachmentPixelFormat: MTLPixelFormat = MTLPixelFormatRGBA8Unorm,
            depthAttachmentPixelFormat: MTLPixelFormat = MTLPixelFormatDepth16Unorm,
            stencilAttachmentPixelFormat: MTLPixelFormat = MTLPixelFormatStencil8,
            device: MTLDeviceProtocol = MTLCreateSystemDefaultDevice()!!
        ): DistortionRendererPtr? {
            memScoped {
                val nativeConfig = alloc<CardboardMetalDistortionRendererConfig>()
                val mtlDevice = CFBridgingRetain(device)
                try {
                    nativeConfig.mtl_device = mtlDevice?.rawValue?.toLong()?.toULong()
                        ?: throw IllegalArgumentException("Unable to get MTLDevice")
                    //(device ?: MTLCreateSystemDefaultDevice())?.objcPtr()?.toLong()?.toULong() ?: 0uL
                    nativeConfig.color_attachment_pixel_format = colorAttachmentPixelFormat
                    nativeConfig.depth_attachment_pixel_format = depthAttachmentPixelFormat
                    nativeConfig.stencil_attachment_pixel_format = stencilAttachmentPixelFormat
                    return CardboardMetalDistortionRenderer_create(nativeConfig.ptr)
                } finally {
                    CFBridgingRelease(mtlDevice)
                }
            }
        }
    }

    override fun destroy() {
        if (nativePointer != null) {
            CardboardDistortionRenderer_destroy(nativePointer)
            nativePointer = null
        }
    }

    override fun setMesh(mesh: Mesh, eye: Eye) {
        check(nativePointer != null) { "CardboardDistortionRenderer is disposed." }
        memScoped {
            val nativeMesh = alloc<CardboardMesh>()
            nativeMesh.n_indices = mesh.indices.size
            nativeMesh.n_vertices = mesh.vertices.size / 3

            // Convert Kotlin arrays to native pointers
            mesh.indices.usePinned { indicesPtr ->
                mesh.vertices.usePinned { verticesPtr ->
                    mesh.uvs.usePinned { uvsPtr ->
                        nativeMesh.indices = indicesPtr.addressOf(0).reinterpret()
                        nativeMesh.vertices = verticesPtr.addressOf(0).reinterpret()
                        nativeMesh.uvs = uvsPtr.addressOf(0).reinterpret()

                        CardboardDistortionRenderer_setMesh(
                            nativePointer,
                            nativeMesh.ptr,
                            eye.value.toUInt()
                        )
                    }
                }
            }
        }
    }

    override fun setMeshes(lensDistortion: LensDistortion) {
        check(nativePointer != null) { "CardboardDistortionRenderer is disposed." }
        require(lensDistortion is IosLensDistortion && lensDistortion.nativePointer != null) { "Invalid LensDistortion." }
        val ld = lensDistortion.nativePointer
        memScoped {
            val nativeMesh = alloc<CardboardMesh>()
            CardboardLensDistortion_getDistortionMesh(ld, Eye.LEFT.value.toUInt(), nativeMesh.ptr)
            CardboardDistortionRenderer_setMesh(nativePointer, nativeMesh.ptr, Eye.LEFT.value.toUInt())
            CardboardLensDistortion_getDistortionMesh(ld, Eye.RIGHT.value.toUInt(), nativeMesh.ptr)
            CardboardDistortionRenderer_setMesh(nativePointer, nativeMesh.ptr, Eye.RIGHT.value.toUInt())
        }
    }

    override fun renderEyeToDisplay(
        target: DistortionRendererTarget,
        x: Int, y: Int, width: Int, height: Int,
        leftEye: EyeTextureDescription,
        rightEye: EyeTextureDescription
    ) {
        check(nativePointer != null) { "CardboardDistortionRenderer is disposed." }
        check(target is MetalDistortionRendererTarget) { "Invalid target type." }
        memScoped {
            val renderCommandEncoder = CFBridgingRetain(target.renderEncoder) ?: throw IllegalArgumentException("Unable to get MTLRenderCommandEncoder")
            try {
                val config = alloc<CardboardMetalDistortionRendererTargetConfig>()
                config.render_command_encoder = renderCommandEncoder.toLong().toULong()
                config.screen_width = target.screenWidth
                config.screen_height = target.screenHeight
                val left = toNative(leftEye)
                val right = toNative(rightEye)
                CardboardDistortionRenderer_renderEyeToDisplay(
                    nativePointer, config.ptr.toLong().toULong(),
                    x, y, width, height, left.ptr, right.ptr
                )
            } finally {
                CFBridgingRelease(renderCommandEncoder)
            }
        }
    }
}
