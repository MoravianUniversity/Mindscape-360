@file:OptIn(ExperimentalForeignApi::class)

package cardboard.sdk

import cardboard.native.*
import kotlinx.cinterop.*

private typealias HeadTrackerPtr = CPointer<cnames.structs.CardboardHeadTracker>?

@OptIn(ExperimentalForeignApi::class)
actual fun HeadTracker(): HeadTracker = IosHeadTracker()

@OptIn(ExperimentalForeignApi::class)
class IosHeadTracker(
    private var nativePointer: HeadTrackerPtr?
) : HeadTracker {
    constructor(): this(CardboardHeadTracker_create())

    init {
        require(this.nativePointer != null) { "Failed to create CardboardHeadTracker" }
    }

    override fun destroy() {
        if (nativePointer != null) {
            CardboardHeadTracker_destroy(nativePointer)
            nativePointer = null
        }
    }

    override fun pause() {
        check(nativePointer != null) { "CardboardHeadTracker is disposed." }
        CardboardHeadTracker_pause(nativePointer)
    }

    override fun resume() {
        check(nativePointer != null) { "CardboardHeadTracker is disposed." }
        CardboardHeadTracker_resume(nativePointer)
    }

    override fun getPose(
        timeStampNs: Long,
        viewportOrientation: ViewportOrientation,
        position: FloatArray, orientation: FloatArray
    ) {
        check(nativePointer != null) { "CardboardHeadTracker is disposed." }
        require(position.size >= 3) { "Position array must have at least 3 elements." }
        require(orientation.size >= 4) { "Orientation array must have at least 4 elements." }
        position.usePinned { posPtr ->
            orientation.usePinned { oriPtr ->
                CardboardHeadTracker_getPose(
                    nativePointer,
                    timeStampNs,
                    viewportOrientation.value.toUInt(),
                    posPtr.addressOf(0),
                    oriPtr.addressOf(0)
                )
            }
        }
    }

    override fun recenter() {
        check(nativePointer != null) { "CardboardHeadTracker is disposed." }
        CardboardHeadTracker_recenter(nativePointer)
    }

    override fun setLowPassFilter(cutoffFrequency: Int) {
        check(nativePointer != null) { "CardboardHeadTracker is disposed." }
        CardboardHeadTracker_setLowPassFilter(nativePointer, cutoffFrequency)
    }
}