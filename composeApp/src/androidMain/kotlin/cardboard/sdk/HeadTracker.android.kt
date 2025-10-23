package cardboard.sdk

import kotlin.Long

actual fun HeadTracker(): HeadTracker = AndroidHeadTracker()

class AndroidHeadTracker : NativeWrapper(create(), ::destroy), HeadTracker {
    companion object {
        @JvmStatic
        private external fun create(): Long

        @JvmStatic
        private external fun destroy(ptr: Long)

        @JvmStatic
        private external fun pause(ptr: Long)

        @JvmStatic
        private external fun resume(ptr: Long)

        @JvmStatic
        private external fun getPose(
            ptr: Long,
            timeStampNs: Long,
            viewportOrientation: Int,
            position: FloatArray,
            orientation: FloatArray
        )

        @JvmStatic
        private external fun recenter(ptr: Long)

        @JvmStatic
        private external fun setLowPassFilter(ptr: Long, cutoffFrequency: Int)

    }

    override fun pause() { pause(pointer) }
    override fun resume() { resume(pointer) }
    override fun getPose(timeStampNs: Long, viewportOrientation: ViewportOrientation, position: FloatArray, orientation: FloatArray) {
        require(position.size >= 3) { "Position array must have at least 3 elements." }
        require(orientation.size >= 4) { "Orientation array must have at least 4 elements." }
        getPose(pointer, timeStampNs, viewportOrientation.value, position, orientation)
    }
    override fun recenter() { recenter(pointer) }
    override fun setLowPassFilter(cutoffFrequency: Int) { setLowPassFilter(pointer, cutoffFrequency) }
}
