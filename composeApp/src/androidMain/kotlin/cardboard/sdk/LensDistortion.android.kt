package cardboard.sdk

actual fun LensDistortion(
    encodedDeviceParams: ByteArray,
    displayWidth: Int, displayHeight: Int
): LensDistortion = AndroidLensDistortion(
    encodedDeviceParams,
    displayWidth, displayHeight
)

class AndroidLensDistortion(
    encodedDeviceParams: ByteArray,
    displayWidth: Int, displayHeight: Int
): NativeWrapper(
    create(encodedDeviceParams, displayWidth, displayHeight),
    ::destroy
), LensDistortion {
    companion object {
        @JvmStatic
        private external fun create(
            encodedDeviceParams: ByteArray,
            displayWidth: Int, displayHeight: Int
        ): Long

        @JvmStatic
        private external fun destroy(ptr: Long)

        @JvmStatic
        private external fun getEyeFromHeadMatrix(ptr: Long, eye: Int, matrix: FloatArray)

        @JvmStatic
        private external fun getProjectionMatrix(
            ptr: Long,
            eye: Int,
            zNear: Float, zFar: Float,
            matrix: FloatArray
        )

        @JvmStatic
        private external fun getFieldOfView(ptr: Long, eye: Int, fov: FloatArray)

        @JvmStatic
        private external fun getDistortionMesh(ptr: Long, eye: Int): Mesh

        @JvmStatic
        private external fun undistortedUvForDistortedUv(ptr: Long, distortedUv: UV, eye: Int): UV

        @JvmStatic
        private external fun distortedUvForUndistortedUv(ptr: Long, undistortedUv: UV, eye: Int): UV
    }

    override fun getEyeFromHeadMatrix(eye: Eye, matrix: FloatArray) {
        require(matrix.size >= 16) { "Matrix array must have at least 16 elements." }
        getEyeFromHeadMatrix(pointer, eye.value, matrix)
    }
    override fun getProjectionMatrix(eye: Eye, zNear: Float, zFar: Float, matrix: FloatArray) {
        require(matrix.size >= 16) { "Matrix array must have at least 16 elements." }
        getProjectionMatrix(pointer, eye.value, zNear, zFar, matrix)
    }
    override fun getFieldOfView(eye: Eye, fov: FloatArray) {
        require(fov.size >= 4) { "FOV array must have at least 4 elements." }
        getFieldOfView(pointer, eye.value, fov)
    }
    override fun getDistortionMesh(eye: Eye) = getDistortionMesh(pointer, eye.value)
    override fun undistortedUvForDistortedUv(distortedUv: UV, eye: Eye) = undistortedUvForDistortedUv(pointer, distortedUv, eye.value)
    override fun distortedUvForUndistortedUv(undistortedUv: UV, eye: Eye) = distortedUvForUndistortedUv(pointer, undistortedUv, eye.value)
}
