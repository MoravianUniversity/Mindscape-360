package cardboard.sdk

expect fun LensDistortion(
    encodedDeviceParams: ByteArray,
    displayWidth: Int,
    displayHeight: Int
): LensDistortion

/**
 * CardboardLensDistortion is a class that provides functionality for lens distortion
 * using the Cardboard SDK. It allows you to create a lens distortion object,
 * get eye matrices, projection matrices, field of view, distortion meshes,
 * and apply distortion and undistortion functions to UV coordinates.
 */
interface LensDistortion {
    /**
     * Destroys and releases memory used by this lens distortion object.
     */
    fun destroy()

    /**
     * Gets the eye_from_head matrix for a particular eye.
     *
     * @param eye Desired eye (left or right)
     * @return 4x4 float eye_from_head matrix.
     */
    fun getEyeFromHeadMatrix(eye: Eye): FloatArray {
        val array = FloatArray(16)
        getEyeFromHeadMatrix(eye, array)
        return array
    }

    /**
     * Same as above but without allocating a new FloatArray.
     */
    fun getEyeFromHeadMatrix(eye: Eye, matrix: FloatArray)

    /**
     * Gets the ideal projection matrix for a particular eye.
     *
     * @param eye Desired eye (left or right).
     * @param zNear Near clip plane z-axis coordinate.
     * @param zFar Far clip plane z-axis coordinate.
     * @return 4x4 float projection matrix.
     */
    fun getProjectionMatrix(eye: Eye, zNear: Float, zFar: Float): FloatArray {
        val array = FloatArray(16)
        getProjectionMatrix(eye, zNear, zFar, array)
        return array
    }

    /**
     * Same as above but without allocating a new FloatArray.
     */
    fun getProjectionMatrix(eye: Eye, zNear: Float, zFar: Float, matrix: FloatArray)

    /**
     * Gets the field of view half angles for a particular eye.
     *
     * @param eye Desired eye.
     * @return Field of view half angles in radians, in the order: left, right, bottom, top.
     */
    fun getFieldOfView(eye: Eye): FloatArray {
        val array = FloatArray(4)
        getFieldOfView(eye, array)
        return array
    }

    /**
     * Same as above but without allocating a new FloatArray.
     */
    fun getFieldOfView(eye: Eye, fov: FloatArray)

    /**
     * Gets the distortion mesh for a particular eye.
     *
     * @param eye Desired eye (left or right).
     * @return Distortion mesh for the specified eye.
     */
    fun getDistortionMesh(eye: Eye): Mesh

    /**
     * Applies lens inverse distortion function to a point normalized [0,1] in
     * pre-distortion (eye texture) space.
     * @param distortedUv Distorted UV point.
     * @param eye Desired eye.
     * @return Point normalized [0,1] in post-distortion space (screen space).
     */
    fun undistortedUvForDistortedUv(distortedUv: UV, eye: Eye): UV

    /**
     * Applies lens distortion function to a point normalized [0,1] in the screen
     * post-distortion space.
     * @param undistortedUv Undistorted UV point.
     * @param eye Desired eye.
     * @return Point normalized [0,1] in pre-distortion (eye texture) space.
     */
    fun distortedUvForUndistortedUv(undistortedUv: UV, eye: Eye): UV
}
