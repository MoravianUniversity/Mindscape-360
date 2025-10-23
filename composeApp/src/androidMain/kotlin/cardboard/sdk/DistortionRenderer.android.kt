package cardboard.sdk

class AndroidGLDistortionRenderer(
    glVersion: Int = 2, // 2 for OpenGL ES 2.0, 3 for OpenGL ES 3.0+
    textureType: Int = 0 // kGlTexture2D = 0, kGlTextureExternalOes = 1
): NativeWrapper(
    if (glVersion <= 2) createOpenGLES2(textureType)
    else createOpenGLES3(textureType),
    ::destroy
), DistortionRenderer {
    companion object {
        @JvmStatic
        private external fun createOpenGLES2(textureType: Int): Long

        @JvmStatic
        private external fun createOpenGLES3(textureType: Int): Long

        @JvmStatic
        private external fun destroy(ptr: Long)

        @JvmStatic
        private external fun setMesh(ptr: Long, mesh: Mesh, eye: Int)

        @JvmStatic
        private external fun setMeshes(dr: Long, ld: Long)

        @JvmStatic
        private external fun renderEyeToDisplay(
            ptr: Long,
            target: Long,
            x: Int, y: Int, width: Int, height: Int,
            leftEye: EyeTextureDescription, rightEye: EyeTextureDescription
        )
    }

    override fun setMesh(mesh: Mesh, eye: Eye) { setMesh(pointer, mesh, eye.value) }
    override fun setMeshes(lensDistortion: LensDistortion) {
        require(lensDistortion is AndroidLensDistortion) { "Invalid LensDistortion." }
        setMeshes(pointer, lensDistortion.pointer)
    }
    override fun renderEyeToDisplay(
        target: DistortionRendererTarget,
        x: Int, y: Int, width: Int, height: Int,
        leftEye: EyeTextureDescription, rightEye: EyeTextureDescription
    ) {
        check(target is GLDistortionRendererTarget) { "Invalid target type." }
        renderEyeToDisplay(pointer, target.id, x, y, width, height, leftEye, rightEye)
    }
}

class GLDistortionRendererTarget(var id: Long) : DistortionRendererTarget
