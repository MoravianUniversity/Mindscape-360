package cardboard.sdk

interface DistortionRendererTarget

interface DistortionRenderer {
    /**
     * Destroys and releases memory used by this distortion renderer.
     * Must be called from the render thread.
     */
    fun destroy()

    /**
     * Sets the mesh for the specified eye.
     *
     * @param mesh The mesh to set.
     * @param eye The eye index (0 for left, 1 for right).
     */
    fun setMesh(mesh: Mesh, eye: Eye)

    /**
     * Direct transfer of meshes from a LensDistortion object.
     */
    fun setMeshes(lensDistortion: LensDistortion)
//    {
//        // there should be a better process in native code, this is the fallback
//        setMesh(lensDistortion.getDistortionMesh(Eye.LEFT), Eye.LEFT)
//        setMesh(lensDistortion.getDistortionMesh(Eye.RIGHT), Eye.RIGHT)
//    }

    /**
     * Renders the eye textures to the display.
     *
     * @param target The target surface to render to.
     * @param x The x coordinate of the target surface.
     * @param y The y coordinate of the target surface.
     * @param width The width of the target surface.
     * @param height The height of the target surface.
     * @param leftEye The texture description for the left eye.
     * @param rightEye The texture description for the right eye.
     */
    fun renderEyeToDisplay(
        target: DistortionRendererTarget,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        leftEye: EyeTextureDescription,
        rightEye: EyeTextureDescription
    )
}
