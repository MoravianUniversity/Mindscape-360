package edu.moravian.cardboard

import edu.moravian.cardboard.sdk.DistortionRenderer
import edu.moravian.cardboard.sdk.DistortionRendererTarget
import edu.moravian.cardboard.sdk.Eye
import edu.moravian.cardboard.sdk.IosMetalDistortionRenderer
import edu.moravian.cardboard.sdk.MetalDistortionRendererTarget
import edu.moravian.cardboard.util.Matrix
import edu.moravian.cardboard.util.NativeMediaPlayer
import kotlinx.cinterop.*
import okio.Source
import okio.buffer
import platform.AVFoundation.*
import platform.CoreFoundation.CFTypeRef
import platform.CoreGraphics.*
import platform.CoreVideo.CVBufferRelease
import platform.CoreVideo.kCVPixelBufferMetalCompatibilityKey
import platform.CoreVideo.kCVPixelBufferPixelFormatTypeKey
import platform.CoreVideo.kCVPixelFormatType_32BGRA
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfURL
import platform.Metal.*
import platform.UIKit.UIImage
import platform.QuartzCore.CACurrentMediaTime

actual fun createDefaultNativeRenderer(): NativeRenderer = IosMetalNativeRenderer()

class IosMetalNativeRenderer: NativeRenderer {
    override val engine: Engine get() = Engine.METAL

    private var width = -1
    private var height = -1

    private var attribs = mapOf<String, ULong>()
    private var uniforms = mapOf<String, ULong>()

    private var view: IosMetalNativeRenderView? = null
    private var device: MTLDeviceProtocol? = null
    private var commandQueue: MTLCommandQueueProtocol? = null
    private var offscreenPipeline: MTLRenderPipelineStateProtocol? = null
    private var offscreenRenderPassDesc: MTLRenderPassDescriptor? = null

    private var texture: MTLTextureProtocol? = null
    @OptIn(ExperimentalForeignApi::class)
    private var cfTexture: CFTypeRef? = null

    private var commandBuffer: MTLCommandBufferProtocol? = null
    private var renderEncoder: MTLRenderCommandEncoderProtocol? = null
    private var dr = MetalDistortionRendererTarget()

    @OptIn(ExperimentalForeignApi::class)
    override fun init(cr: CardboardRenderer, shader: Shader) {
        require(shader is MSLShader) { "Shader must be MSL" }
        require(cr.view is IosMetalNativeRenderView) { "CardboardRenderer must use an instance of IosMetalNativeRenderView" }
        val view = cr.view.also { view = it }
        val device = view.device.also { device = it }
        commandQueue = device.newCommandQueue() ?: error("Failed to create command queue")

        attribs = shader.attributes.mapValues { (_, v) -> v.toULong() }
        uniforms = shader.uniforms.mapValues { (_, v) -> v.toULong() }

        val library = device.makeLibrary(shader.source, options = null)

        offscreenPipeline = device.makeRenderPipelineState {
            colorAttachments[0].pixelFormat = MTLPixelFormatRGBA8Unorm
            depthAttachmentPixelFormat = MTLPixelFormatDepth16Unorm
            stencilAttachmentPixelFormat = MTLPixelFormatInvalid
            sampleCount = 1uL // TODO: use view.sampleCount (4uL) but that fails...

            vertexFunction = library.newFunctionWithName(shader.vertexFunc) ?: error("Failed to create vertex shader function")
            vertexDescriptor = MTLVertexDescriptor().apply {
                var i = 0
                set(i++, attribs.getValue(ATTR_POSITION), 3)
                attribs[ATTR_NORMAL]?.let { set(i++, it, 3) }
                attribs[ATTR_UV]?.let { set(i++, it, 2) }
            }
            fragmentFunction = library.newFunctionWithName(shader.fragmentFunc) ?: error("Failed to create fragment shader function")
            vertexBuffers[0].mutability = MTLMutabilityImmutable
            vertexBuffers[1].mutability = MTLMutabilityImmutable
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun setupGraphics(cr: CardboardRenderer): DistortionRenderer {
        width = cr.width
        height = cr.height

        if (cfTexture !== null) { metalTeardown() }
        val device = device ?: error("Metal device is not initialized")

        // Create render texture
        val tex = device.newTexture {
            textureType = MTLTextureType2D
            width = cr.width.toULong()
            height = cr.height.toULong()
            pixelFormat = MTLPixelFormatRGBA8Unorm
            usage = MTLTextureUsageRenderTarget or MTLTextureUsageShaderRead
        }
        texture = tex
        CFBridgingRetain(tex).also {
            cfTexture = it
            cr.setTexture(it.toLong())
        }

        // Create offscreen render pass descriptor
        offscreenRenderPassDesc = MTLRenderPassDescriptor().apply {
            colorAttachments[0].apply {
                texture = tex
                loadAction = MTLLoadActionClear
                storeAction = MTLStoreActionStore
                clearColor = MTLClearColorMake(0.0, 0.0, 0.0, 1.0)
            }
            depthAttachment = MTLRenderPassDepthAttachmentDescriptor().apply {
                texture = device.newTexture {
                    textureType = MTLTextureType2D
                    width = cr.width.toULong()
                    height = cr.height.toULong()
                    pixelFormat = MTLPixelFormatDepth16Unorm
                    usage = MTLTextureUsageRenderTarget or MTLTextureUsageShaderRead
                    storageMode = MTLStorageModePrivate
                }
                loadAction = MTLLoadActionClear
                storeAction = MTLStoreActionStore
            }
        }

        // Setup the distortion renderer
        dr = MetalDistortionRendererTarget(screenWidth = width, screenHeight = height)

        return IosMetalDistortionRenderer(
            colorAttachmentPixelFormat = MTLPixelFormatRGBA8Unorm,
            depthAttachmentPixelFormat = MTLPixelFormatDepth16Unorm,
            stencilAttachmentPixelFormat = MTLPixelFormatInvalid,
            device = device
        )
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun destroy(cr: CardboardRenderer) {
        // from endDraw()
        dr.renderEncoder = null
        renderEncoder = null
        commandBuffer = null

        metalTeardown()

        textureCache?.destroy()
        textureCache = null
        commandQueue = null
        device = null
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun metalTeardown() {
        if (cfTexture !== null) { CFBridgingRelease(cfTexture) }
        cfTexture = null
        //texture?.setPurgeableState(MTLPurgeableStateEmpty)
        texture = null
    }

    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override fun loadMesh(mesh: Mesh): NativeMesh {
        val device = device ?: error("Metal device is not initialized")
        return MetalMesh(
            device.loadBuffer(mesh.vertices),
            mesh.normals?.let { device.loadBuffer(it) },
            mesh.uvs?.let { device.loadBuffer(it) },
            device.loadBuffer(mesh.indices),
        )
    }

    private inner class MetalMesh(
        val vertices: MTLBufferProtocol,
        val normals: MTLBufferProtocol? = null,
        val uvs: MTLBufferProtocol? = null,
        val indices: MTLBufferProtocol,
        val verticesIndex: ULong = attribs[ATTR_POSITION]!!,
        val normalsIndex: ULong? = if (normals !== null) attribs[ATTR_NORMAL] else null,
        val uvsIndex: ULong? = if (uvs !== null) attribs[ATTR_UV] else null,
    ): NativeMesh {
        val size = indices.length / UShort.SIZE_BYTES.toULong()
        override fun draw() {
            renderEncoder?.apply {
                setVertexBuffer(vertices, 0uL, verticesIndex)
                if (normalsIndex !== null) { setVertexBuffer(normals, 0uL, normalsIndex) }
                if (uvsIndex !== null) { setVertexBuffer(uvs, 0uL, uvsIndex) }
                drawIndexedPrimitives(
                    primitiveType = MTLPrimitiveTypeTriangle,
                    indexCount = size,
                    indexType = MTLIndexTypeUInt16,
                    indexBuffer = indices,
                    indexBufferOffset = 0uL
                )
            }
        }
        override fun destroy() {
            //vertices.setPurgeableState(MTLPurgeableStateEmpty)
            //normals?.setPurgeableState(MTLPurgeableStateEmpty)
            //uvs?.setPurgeableState(MTLPurgeableStateEmpty)
            //indices.setPurgeableState(MTLPurgeableStateEmpty)
        }
    }

    override fun loadTexture(url: String, flip: Boolean): NativeTexture {
        val url = NSURL(string=url)
        var image = if (url.scheme == "file") { url.path?.let { UIImage.imageWithContentsOfFile(it) } }
        else { NSData.dataWithContentsOfURL(url)?.let { UIImage.imageWithData(it) } }
        return loadTexture(image ?: error("Failed to load texture from URL: $url"), flip)
    }

    @OptIn(ExperimentalForeignApi::class)
    @Suppress("MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE")
    override fun loadTexture(image: Source, flip: Boolean) =
        loadTexture(UIImage.imageWithData(image.readNSData()) ?: error("Failed to load texture from source"), flip)

    @OptIn(ExperimentalForeignApi::class)
    @Suppress("MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE")
    private fun loadTexture(image: UIImage, flip: Boolean): NativeTexture {
        val image = image.CGImage ?: error("UIImage does not have a CGImage")
        val width = CGImageGetWidth(image)
        val height = CGImageGetHeight(image)

        memScoped {
            val rawData = allocArray<ByteVar>((height * width).toLong() * 4)
            val colorSpace = CGColorSpaceCreateDeviceRGB()
            try {
                val context = CGBitmapContextCreate(rawData,
                    width, height, 8uL, width * 4uL,
                    colorSpace, kCGImageAlphaPremultipliedLast or kCGBitmapByteOrder32Big)
                try {
                    if (flip) {
                        CGContextTranslateCTM(context, 0.0, height.toDouble())
                        CGContextScaleCTM(context, 1.0, -1.0)
                    }
                    CGContextDrawImage(context, CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()), image)
                } finally { CGContextRelease(context) }
            } finally { CGColorSpaceRelease(colorSpace) }
            return loadTexture(rawData, width, height)
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun loadTexture(
        image: ByteArray,
        width: Int,
        height: Int,
        alpha: Boolean,
    ): NativeTexture {
        require(alpha) { "Alpha channel required in this implementation" }
        return image.usePinned { loadTexture(it.addressOf(0), width.toULong(), height.toULong()) }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun loadTexture(data: CPointer<ByteVar>, width: ULong, height: ULong) =
        MetalTexture(device!!.loadTexture(data, width, height))
    // TODO: generate mipmaps for the texture
//        MetalTexture(device!!.loadTexture(data, width, height, true).also { texture ->
//            commandQueue?.commandBuffer()?.blitCommandEncoder()?.apply {
//                generateMipmapsForTexture(texture)
//                endEncoding()
//            } ?: error("Failed to create command buffer for texture mipmaps")
//        })

    private inner class MetalTexture(
        val texture: MTLTextureProtocol,
        val index: ULong = uniforms.getValue(UNIFORM_TEXTURE),
    ): NativeTexture {
        override fun bind() { renderEncoder?.apply { setFragmentTexture(texture, index) } }
        override fun destroy() { /*texture.setPurgeableState(MTLPurgeableStateEmpty)*/ }
    }

    @OptIn(ExperimentalForeignApi::class)
    private var textureCache: MetalTextureCache? = null

    @Suppress("MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE")
    @OptIn(ExperimentalForeignApi::class)
    override fun loadVideoTexture(player: NativeMediaPlayer): NativeTexture {
        // Create shared texture cache
        val device = device ?: error("Metal device is not initialized")
        if (textureCache === null) { textureCache = MetalTextureCache(device) }
        return MetalTextureVideo(player, textureCache!!)
    }

    @Suppress("MISSING_DEPENDENCY_CLASS_IN_EXPRESSION_TYPE")
    @OptIn(ExperimentalForeignApi::class)
    private inner class MetalTextureVideo(
        val player: AVPlayer,
        val textureCache: MetalTextureCache,
        val index: ULong = uniforms.getValue(UNIFORM_TEXTURE),
        val uniformSTMatrix: ULong? = uniforms[UNIFORM_ST],
    ): NativeTexture {
        var output: AVPlayerItemVideoOutput? = null
        var texture: MTLTextureProtocol? = null
        val stMatrix = Matrix().setIdentity()

        override fun bind() {
            renderEncoder?.apply {
                val item = player.currentItem ?: error("Player current item is null")
                val time = item.currentTime()
                if (output === null) {
                    if (item.status == AVPlayerItemStatusFailed) { error("Player item failed to load") }
                    val curTime = time.useContents { value.toDouble() / timescale }
                    if (item.status != AVPlayerItemStatusReadyToPlay || curTime < 0.1) {
                        if (uniformSTMatrix !== null) { setVertexUniform(stMatrix.m, uniformSTMatrix) }
                        return
                    }
                    val settings = mapOf<Any?, Any>(
                        kCVPixelBufferPixelFormatTypeKey?.toKString() to kCVPixelFormatType_32BGRA,
                        kCVPixelBufferMetalCompatibilityKey?.toKString() to true,
                    )
                    output = AVPlayerItemVideoOutput(pixelBufferAttributes=settings).also { item.addOutput(it) }
                }
                val output = output!!
                val outputTime = output.itemTimeForHostTime(CACurrentMediaTime())
                val buffer = output.copyPixelBufferForItemTime(outputTime, null)
                if (buffer != null) {
                    texture = try {
                        textureCache.createTextureFromImage(buffer, matrix=stMatrix.m)
                    } finally {
                        CVBufferRelease(buffer)
                    }
                }
                if (texture !== null) { setFragmentTexture(texture, index) }
                if (uniformSTMatrix !== null) { setVertexUniform(stMatrix.m, uniformSTMatrix) }
            }
        }
        override fun destroy() {
            output?.let { player.currentItem?.removeOutput(it) }
            output = null
            //texture?.let { it.setPurgeableState(MTLPurgeableStateEmpty) }
            texture = null
        }
    }

    override fun prepareToDraw() {
        val commandBuffer = (commandQueue?.commandBuffer() ?: error("Failed to make command buffer")).also { commandBuffer = it }
        renderEncoder = commandBuffer.renderCommandEncoder(offscreenRenderPassDesc!!, offscreenPipeline!!)
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun prepareToDraw(eye: Eye) {
        val halfWidth = width / 2
        val xx = if (eye == Eye.LEFT) 0 else halfWidth
        val h = height
        renderEncoder?.apply {
            setViewport(cValue<MTLViewport> {
                originX = xx.toDouble(); originY = 0.0
                width = halfWidth.toDouble(); height = h.toDouble()
                znear = -1.0; zfar = 1.0
            })
            setScissorRect(cValue<MTLScissorRect> {
                x = xx.toULong(); y = 0uL
                width = halfWidth.toULong(); height = h.toULong()
            })
        }
    }

    override fun prepareToDrawFinal(): DistortionRendererTarget {
        renderEncoder?.endEncoding() // end the offscreen render pass
        renderEncoder = null

        val view = view?.view ?: error("Renderer view is not set")
        val renderPassDesc = view.currentRenderPassDescriptor ?: error("No current render pass descriptor")
        dr.renderEncoder = commandBuffer!!.renderCommandEncoder(renderPassDesc)
        return dr
    }

    override fun endDraw() {
        dr.renderEncoder?.endEncoding()
        dr.renderEncoder = null
        commandBuffer?.apply {
            presentDrawable(view?.view?.currentDrawable ?: error("No current drawable"))
            commit()
        }
        commandBuffer = null
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun setModelViewMatrix(matrix: Matrix) {
        renderEncoder?.setVertexUniform(matrix.m, uniforms.getValue(UNIFORM_MV))
    }

    companion object {
        @Suppress("ConstPropertyName")
        private const val kCGImageAlphaPremultipliedLast = 1u

        @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
        private fun Source.readNSData() = buffer().readByteArray().toNSData()

        @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
        private fun ByteArray.toNSData() : NSData = usePinned { pinned ->
            memScoped {
                NSData.create(
                    bytes = pinned.addressOf(0),
                    length = this@toNSData.size.toULong()
                )
            }
        }

        @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
        fun ObjCObjectVar<NSError?>.check() { value?.let { throw RuntimeException("NSError: ${it.localizedDescription}") } }

        @OptIn(ExperimentalForeignApi::class)
        inline fun <T> withNSError(crossinline block: NativePlacement.(CPointer<ObjCObjectVar<NSError?>>) -> T): T {
            memScoped {
                val error = alloc<ObjCObjectVar<NSError?>>()
                val result = block(error.ptr)
                error.check()
                return result
            }
        }

        @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
        fun MTLDeviceProtocol.makeLibrary(source: String, options: MTLCompileOptions? = null) =
            withNSError { err -> newLibraryWithSource(source, options, err) } ?: error("Failed to create default library")

        @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
        fun MTLDeviceProtocol.makeRenderPipelineState(descriptor: MTLRenderPipelineDescriptor) =
            withNSError { err -> newRenderPipelineStateWithDescriptor(descriptor, err) } ?: error("Failed to create render pipeline state")

        fun MTLDeviceProtocol.makeRenderPipelineState(builder: MTLRenderPipelineDescriptor.() -> Unit) =
            makeRenderPipelineState(MTLRenderPipelineDescriptor().apply(builder))

        operator fun MTLRenderPipelineColorAttachmentDescriptorArray.get(index: Int) = objectAtIndexedSubscript(index.toULong())
        operator fun MTLRenderPassColorAttachmentDescriptorArray.get(index: Int) = objectAtIndexedSubscript(index.toULong())
        operator fun MTLVertexAttributeDescriptorArray.get(index: Int) = objectAtIndexedSubscript(index.toULong())
        operator fun MTLVertexBufferLayoutDescriptorArray.get(index: Int) = objectAtIndexedSubscript(index.toULong())
        operator fun MTLVertexBufferLayoutDescriptorArray.get(index: ULong) = objectAtIndexedSubscript(index)
        operator fun MTLPipelineBufferDescriptorArray.get(index: Int) = objectAtIndexedSubscript(index.toULong())

        fun MTLVertexDescriptor.set(i: Int, index: ULong, n: Int) {
            attributes[i].apply {
                format = if (n == 3) MTLVertexFormatFloat3 else MTLVertexFormatFloat2
                offset = 0uL
                bufferIndex = index
            }
            layouts[index].apply {
                stepFunction = MTLVertexStepFunctionPerVertex
                stride = (n * Float.SIZE_BYTES).toULong()
                stepRate = 1uL
            }
        }

        @OptIn(ExperimentalForeignApi::class)
        fun MTLDeviceProtocol.loadBuffer(array: FloatArray) = array.usePinned {
            newBufferWithBytes(it.addressOf(0), (array.size * Float.SIZE_BYTES).toULong(), 0uL) ?: error("Failed to create buffer")
        }

        @OptIn(ExperimentalForeignApi::class)
        fun MTLDeviceProtocol.loadBuffer(array: UShortArray) = array.usePinned {
            newBufferWithBytes(it.addressOf(0), (array.size * UShort.SIZE_BYTES).toULong(), 0uL) ?: error("Failed to create buffer")
        }

        fun MTLDeviceProtocol.newTexture(desc: MTLTextureDescriptor) =
            newTextureWithDescriptor(desc) ?: error("Failed to create texture")

        fun MTLDeviceProtocol.newTexture(builder: MTLTextureDescriptor.() -> Unit) =
            newTexture(MTLTextureDescriptor().apply(builder))

        @OptIn(ExperimentalForeignApi::class)
        fun MTLDeviceProtocol.newTexture(width: ULong, height: ULong, mipmap: Boolean = false) =
            newTexture(MTLTextureDescriptor.texture2DDescriptorWithPixelFormat(
                MTLPixelFormatRGBA8Unorm, width, height, mipmapped = mipmap,
            ))

        @OptIn(ExperimentalForeignApi::class)
        fun MTLDeviceProtocol.loadTexture(rawData: CPointer<ByteVar>, width: ULong, height: ULong, mipmap: Boolean = false) =
            newTexture(width, height, mipmap).apply {
                replaceRegion(
                    region = MTLRegionMake2D(0uL, 0uL, width, height),
                    mipmapLevel = 0uL,
                    withBytes = rawData,
                    bytesPerRow = width * 4uL,
                )
            }

        fun MTLCommandBufferProtocol.renderCommandEncoder(
            desc: MTLRenderPassDescriptor,
        ) = renderCommandEncoderWithDescriptor(desc) ?: error("Failed to create render command encoder")

        fun MTLCommandBufferProtocol.renderCommandEncoder(
            desc: MTLRenderPassDescriptor,
            pipeline: MTLRenderPipelineStateProtocol,
        ) = renderCommandEncoder(desc).apply { setRenderPipelineState(pipeline) }

        @OptIn(ExperimentalForeignApi::class)
        fun MTLRenderCommandEncoderProtocol.setVertexUniform(array: FloatArray, index: ULong) {
            array.usePinned {
                setVertexBytes(
                    bytes = it.addressOf(0),
                    length = (array.size * Float.SIZE_BYTES).toULong(),
                    atIndex = index.toULong()
                )
            }
        }
    }
}
