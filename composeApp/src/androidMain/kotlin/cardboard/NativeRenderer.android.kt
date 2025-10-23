package cardboard

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.opengl.GLES20.*
import android.opengl.GLES30.glBindVertexArray
import android.opengl.GLES30.glDeleteVertexArrays
import android.opengl.GLES30.glGenVertexArrays
import android.opengl.GLUtils
import android.view.Surface
import cardboard.sdk.AndroidGLDistortionRenderer
import cardboard.sdk.DistortionRenderer
import cardboard.sdk.DistortionRendererTarget
import cardboard.sdk.Eye
import cardboard.sdk.GLDistortionRendererTarget
import cardboard.util.Matrix
import cardboard.util.NativeMediaPlayer
import okio.Source
import okio.buffer
import java.net.URL
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer


actual fun createDefaultNativeRenderer(): NativeRenderer = AndroidGLRenderer()

class AndroidGLRenderer: NativeRenderer {
    private var program = -1
    private var depthBuffer = -1    // depth buffer for rendering
    private var frameBuffer = -1    // frame buffer for rendering
    private var texture = -1        // distortion texture

    private var attribs = mapOf<String, Int>()
    private var uniforms = mapOf<String, Int>()

    private var width = -1
    private var height = -1

    private var _version = 0.0f
    val glVersion get(): Float {
        if (_version == 0.0f) {
            val versionString = glGetString(GL_VERSION) ?: "0.0"
            val parts = versionString.split(" ")
            // some devices don't put the version number in the first part...
            for (part in parts) {
                if (part[0].isDigit()) { _version = part.toFloatOrNull() ?: 0.0f; break }
            }
        }
        return _version
    }
    override val engine; get() = if (glVersion < 3.0f) Engine.GLES2 else Engine.GLES2

    override fun init(cr: CardboardRenderer, shader: Shader) {
        require(shader is GLSLShader) { "Shader must be GLSL" }
        glCheckError { "pre-init" }
        if (program >= 0) { glDeleteProgram(program) }
        program = glCreateProgram()
        glAttachShader(program, loadShader(GL_VERTEX_SHADER, shader.vertexShader))
        glAttachShader(program, loadShader(GL_FRAGMENT_SHADER, shader.fragmentShader))
        glLinkProgram(program)
        glUseProgram(program)
        glCheckError { "init" }

        attribs = shader.attributes.mapValues { (_, v) -> glGetAttribLocation(program, v) }
        uniforms = shader.uniforms.mapValues { (_, v) -> glGetUniformLocation(program, v) }
        glCheckError { "get shader parameters" }
    }

    override fun destroy(cr: CardboardRenderer) {
        if (frameBuffer >= 0) { glTeardown() }
        if (program >= 0) { glDeleteProgram(program); program = -1 }
    }

    override fun setupGraphics(cr: CardboardRenderer): DistortionRenderer {
        width = cr.width
        height = cr.height

        if (frameBuffer >= 0) { glTeardown() }
        val buffer = IntArray(1)

        // Create render texture
        texture = genTexture(minFilter = GL_LINEAR)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB, width, height, 0, GL_RGB, GL_UNSIGNED_BYTE, null)
        glCheckError { "create render texture" }
        cr.setTexture(texture.toLong())

        // Generate depth buffer to perform depth test
        glGenRenderbuffers(1, buffer, 0)
        depthBuffer = buffer[0]
        glBindRenderbuffer(GL_RENDERBUFFER, depthBuffer)
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT16, width, height)
        glCheckError { "create depth buffer" }

        // Save the initial/default framebuffer
        glGetIntegerv(GL_FRAMEBUFFER_BINDING, buffer, 0)
        distortionRendererTarget = GLDistortionRendererTarget(buffer[0].toLong())
        glCheckError { "get initial framebuffer" }

        // Create render target
        glGenFramebuffers(1, buffer, 0)
        frameBuffer = buffer[0]
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer)
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, texture, 0)
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthBuffer)
        glCheckError { "create frame buffer" }

        return AndroidGLDistortionRenderer(glVersion.toInt())
    }

    private fun glTeardown() {
        if (frameBuffer == -1) { return }
        val buffer = IntArray(1)
        buffer[0] = depthBuffer; glDeleteRenderbuffers(1, buffer, 0); depthBuffer = -1
        buffer[0] = frameBuffer; glDeleteFramebuffers(1, buffer, 0); frameBuffer = -1
        buffer[0] = texture; glDeleteTextures(1, buffer, 0); texture = -1
        glCheckError { "teardown" }
    }

    override fun loadMesh(mesh: Mesh): NativeMesh = when {
        glVersion < 3.0f -> GL2Mesh(mesh)
        else -> GL3Mesh(mesh) // uses VAO
    }

    abstract inner class GLMesh(mesh: Mesh): NativeMesh {
        val size = mesh.size
        val positionAttrib = attribs[ATTR_POSITION]!!
        val uvAttrib = if (mesh.uvs != null) attribs[ATTR_UV] else null
        val normalAttrib = if (mesh.normals != null) attribs[ATTR_NORMAL] else null
        val buffers = IntArray(2 + if (uvAttrib != null) 1 else 0 + if (normalAttrib != null) 1 else 0)
        @OptIn(ExperimentalUnsignedTypes::class)
        fun loadBuffers(mesh: Mesh) {
            glGenBuffers(buffers.size, buffers, 0)
            loadIndexBuffer(buffers[0], mesh.indices)
            loadBuffer(buffers[1], positionAttrib, mesh.vertices, 3)
            var i = 2
            if (uvAttrib != null) { loadBuffer(buffers[i++], uvAttrib, mesh.uvs!!, 2) }
            if (normalAttrib != null) { loadBuffer(buffers[i++], normalAttrib, mesh.normals!!, 3) }
        }
        fun unbindBuffers() {
            glBindBuffer(GL_ARRAY_BUFFER, 0)
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        }
        //protected fun finalize() { destroy() }  // unlikely to be on GL thread and could cause other issues (like what if there is a different GL context active?)
        override fun destroy() {
            if (buffers[0] != 0) {
                glDeleteBuffers(buffers.size, buffers, 0)
                for (i in buffers.indices) { buffers[i] = 0 }
            }
        }
    }

    inner class GL2Mesh(mesh: Mesh): GLMesh(mesh) {
        init {
            loadBuffers(mesh)
            unbindBuffers()
            glCheckError { "load textured mesh" }
        }
        override fun draw() {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffers[0])
            bindBuffer(buffers[1], positionAttrib, 3)
            var i = 2
            if (uvAttrib != null) { bindBuffer(buffers[i++], uvAttrib, 2) }
            if (normalAttrib != null) { bindBuffer(buffers[i++], normalAttrib, 3) }
            glDrawElements(GL_TRIANGLES, size, GL_UNSIGNED_SHORT, 0)
            unbindBuffers()
            glCheckError { "draw textured mesh" }
        }
    }

    inner class GL3Mesh(mesh: Mesh): GLMesh(mesh) {
        val vao: Int
        init {
            glGenVertexArrays(1, buffers, 0)
            vao = buffers[0]
            glBindVertexArray(vao)
            loadBuffers(mesh)
            glBindVertexArray(0)
            unbindBuffers()
            glCheckError { "load textured mesh" }
        }
        override fun draw() {
            glBindVertexArray(vao)
            glDrawElements(GL_TRIANGLES, size, GL_UNSIGNED_SHORT, 0)
            glBindVertexArray(0)
            glCheckError { "draw textured mesh $vao" }
        }
        override fun destroy() {
            if (buffers[0] != 0) {
                super.destroy()
                buffers[0] = vao
                glDeleteVertexArrays(1, buffers, 0)
                buffers[0] = 0
            }
        }
    }

    override fun loadTexture(url: String, flip: Boolean) =
        loadTexture(BitmapFactory.decodeStream(URL(url).openStream()), flip)

    override fun loadTexture(image: Source, flip: Boolean) =
        loadTexture(BitmapFactory.decodeStream(image.buffer().inputStream()), flip)

    private fun loadTexture(image: Bitmap?, flip: Boolean): NativeTexture {
        require(image != null) { "Failed to load texture from image" }
        val image = if (flip) { image.flipVertically() } else { image }
        return GLTexture { GLUtils.texImage2D(GL_TEXTURE_2D, 0, image, 0) }
    }

    override fun loadTexture(image: ByteArray, width: Int, height: Int, alpha: Boolean): NativeTexture {
        val format = if (alpha) GL_RGBA else GL_RGB
        return GLTexture {
            glTexImage2D(GL_TEXTURE_2D, 0, format, width, height, 0,
                format, GL_UNSIGNED_BYTE, ByteBuffer.wrap(image))
        }
    }

    inner class GLTexture(
        val id: Int = GL_TEXTURE0,
        val loc: Int = uniforms.getValue(UNIFORM_TEXTURE),
        texImage2D: () -> Unit,
    ): NativeTexture {
        val texture = genTexture().also {
            texImage2D()
            glGenerateMipmap(GL_TEXTURE_2D)
            glCheckError { "create texture $it" }
        }
        override fun bind() {
            if (texture != 0) {
                glActiveTexture(id)
                glBindTexture(GL_TEXTURE_2D, texture)
                glUniform1i(loc, id - GL_TEXTURE0)
                glCheckError { "bind texture $texture" }
            }
        }
        //protected fun finalize() { destroy() }  // unlikely to be on GL thread and could cause other issues (like what if there is a different GL context active?)
        override fun destroy() { if (texture != 0) { glDeleteTextures(1, intArrayOf(texture), 0) } }
    }

    override fun loadVideoTexture(player: NativeMediaPlayer): NativeTexture {
        val texture = genTexture(GL_TEXTURE_EXTERNAL_OES, minFilter = GL_LINEAR)
        glCheckError { "create texture video $texture" }
        return GLTextureVideo(GL_TEXTURE0, uniforms.getValue(UNIFORM_TEXTURE), texture, player)
    }

    inner class GLTextureVideo(
        val id: Int = GL_TEXTURE0,
        val loc: Int = uniforms.getValue(UNIFORM_TEXTURE),
        val texture: Int,
        val player: NativeMediaPlayer,
        val uniformSTMatrix: Int? = uniforms[UNIFORM_ST],
    ): NativeTexture, SurfaceTexture.OnFrameAvailableListener {
        val surfaceTexture = SurfaceTexture(texture).apply { setOnFrameAvailableListener(this@GLTextureVideo) }
        val surface = Surface(surfaceTexture)
        val stMatrix = Matrix().setIdentity()
        init { player.setSurface(surface) }

        private var frameId = 0
        private var frameIdUpdated = -1
        override fun bind() {
            if (texture != 0) {
                glActiveTexture(id)
                glBindTexture(GL_TEXTURE_EXTERNAL_OES, texture)
                while (frameId != frameIdUpdated) { // the loop is needed in case we missed a frame
                    surfaceTexture.updateTexImage()
                    surfaceTexture.getTransformMatrix(stMatrix.m)
                    frameIdUpdated++
                }
                if (uniformSTMatrix != null) {
                    glUniformMatrix4fv(uniformSTMatrix, 1, false, stMatrix.m, 0)
                    glCheckError { "set ST matrix for video texture $texture" }
                }
                glUniform1i(loc, id - GL_TEXTURE0)
                glCheckError { "bind texture video $texture" }
            }
        }
        protected fun finalize() { println("GLTextureVideo:finalize"); /*destroy()*/ }
        override fun destroy() {
            println("GLTextureVideo:destroy $texture")
            player.setSurface(null)
            surface.release()
            surfaceTexture.release()
            if (texture != 0) { glDeleteTextures(1, intArrayOf(texture), 0) }
        }
        override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) { frameId++ }
    }

    override fun prepareToDraw() {
        glCheckError { "pre-prepare to draw" }
        glUseProgram(program)
        glBindFramebuffer(GL_FRAMEBUFFER, frameBuffer)
        glEnable(GL_DEPTH_TEST)
        glEnable(GL_CULL_FACE)
        glEnable(GL_SCISSOR_TEST)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glScissor(0, 0, width, height)
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
        glCheckError { "prepare to draw" }
    }

    override fun prepareToDraw(eye: Eye) {
        val halfWidth = width / 2
        val x = if (eye == Eye.LEFT) 0 else halfWidth
        glViewport(x, 0, halfWidth, height)
        glScissor(x, 0, halfWidth, height)
        glCheckError { "prepare to draw eye $eye" }
    }

    private var distortionRendererTarget = GLDistortionRendererTarget(0)
    override fun prepareToDrawFinal(): DistortionRendererTarget {
        glScissor(0, 0, width, height)
        return distortionRendererTarget
    }

    override fun setModelViewMatrix(matrix: Matrix) {
        glUniformMatrix4fv(uniforms.getValue(UNIFORM_MV), 1, false, matrix.m, 0)
        glCheckError { "set model view matrix" }
    }

    companion object {
        inline fun glCheckError(crossinline label: () -> String) {
            val error = glGetError()
            if (error != GL_NO_ERROR) { throw RuntimeException("OpenGL ES error during ${label()}: $error") }
        }
        fun loadShader(type: Int, shaderSource: String): Int {
            val shader = glCreateShader(type)
            glShaderSource(shader, shaderSource)
            glCompileShader(shader)

            // Get the compilation status.
            val compileStatus = IntArray(1)
            glGetShaderiv(shader, GL_COMPILE_STATUS, compileStatus, 0)
            if (compileStatus[0] != GL_FALSE) { return shader }

            // If the compilation failed, delete the shader and show an error.
            val infoString = glGetShaderInfoLog(shader)
            glDeleteShader(shader)
            throw RuntimeException("Could not compile shader of type $type: $infoString")
        }
        fun loadBuffer(buffer: Int, attrib: Int, data: FloatArray, components: Int) {
            bindBuffer(buffer, attrib, components)
            glBufferData(GL_ARRAY_BUFFER, data.size*Float.SIZE_BYTES, FloatBuffer.wrap(data), GL_STATIC_DRAW)
        }
        fun bindBuffer(buffer: Int, attrib: Int, components: Int) {
            glBindBuffer(GL_ARRAY_BUFFER, buffer)
            glVertexAttribPointer(attrib, components, GL_FLOAT, false, 0, 0)
            glEnableVertexAttribArray(attrib)
        }
        @OptIn(ExperimentalUnsignedTypes::class)
        fun loadIndexBuffer(buffer: Int, indices: UShortArray) {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, buffer)
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices.size*Short.SIZE_BYTES, ShortBuffer.wrap(indices.asShortArray()), GL_STATIC_DRAW)
        }
        fun FloatArray.toBuffer(): Buffer =
            ByteBuffer.allocateDirect(this.size * Float.SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer().also {
                it.put(this)
                it.position(0)
            }
        @OptIn(ExperimentalUnsignedTypes::class)
        fun UShortArray.toBuffer(): Buffer =
            ByteBuffer.allocateDirect(this.size * Float.SIZE_BYTES).order(ByteOrder.nativeOrder()).asShortBuffer().also {
                it.put(this.asShortArray())
                it.position(0)
            }
        fun genTexture(
            target: Int = GL_TEXTURE_2D,
            wrapS: Int = GL_CLAMP_TO_EDGE, wrapT: Int = GL_CLAMP_TO_EDGE,
            minFilter: Int = GL_LINEAR_MIPMAP_NEAREST, magFilter: Int = GL_LINEAR,
        ): Int {
            val buffer = IntArray(1)
            glGenTextures(1, buffer, 0)
            val texture = buffer[0]
            glBindTexture(target, texture)
            glTexParameteri(target, GL_TEXTURE_WRAP_S, wrapS)
            glTexParameteri(target, GL_TEXTURE_WRAP_T, wrapT)
            glTexParameteri(target, GL_TEXTURE_MIN_FILTER, minFilter)
            glTexParameteri(target, GL_TEXTURE_MAG_FILTER, magFilter)
            return texture
        }
        fun Bitmap.flipVertically(): Bitmap {
            val matrix = android.graphics.Matrix().apply { postScale(1f, -1f, width / 2f, height / 2f) }
            return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
        }
    }
}
