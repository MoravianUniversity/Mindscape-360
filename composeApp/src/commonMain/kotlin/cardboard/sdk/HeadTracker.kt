package cardboard.sdk

/////////////////////////////////////////////////////////////////////////////
// Head Tracker
/////////////////////////////////////////////////////////////////////////////
/// @defgroup head-tracker Head Tracker
/// @brief This module calculates the predicted head's pose for a given
///     timestamp. It takes data from accelerometer and gyroscope sensors and
///     uses a Kalman filter to generate the output value. The head's pose is
///     returned as a quaternion. To have control of the usage of the sensors,
///     this module also includes pause and resume functions.
///
/// @details Let the World frame be an arbitrary 3D Cartesian right handed frame
///          whose basis is defined by a triplet of unit vectors
///          (x, y, z) which point in the same
///          direction as OpenGL. That is: x points to the right,
///          y points up and z points backwards.
///
///          The head pose is always returned in the World frame. It is the
///          average of the left and right eye position. By default, the head
///          pose is near the origin, looking roughly forwards (down the
///          -z axis).
///
///          Implementation and application code could refer to another three
///          poses:
///          - Raw sensor pose: no position, only orientation of device, derived
///            directly from sensors.
///          - Recentered sensor pose: like "Raw sensor pose", but with
///            recentering applied.
///          - Head pose: Recentered sensor pose, with neck model applied. The
///            neck model only adjusts position, it does not adjust orientation.
///            This is usually used directly as the camera pose, though it may
///            be further adjusted via a scene graph. This is the only pose
///            exposed through the API.
expect fun HeadTracker(): HeadTracker

interface HeadTracker {
    /**
     * Destroys and releases memory used by this head tracker object.
     */
    fun destroy()

    /**
     * Gets the predicted head pose at a specific time.
     *
     * @param timeStampNs The timestamp in nanoseconds.
     * @param viewportOrientation The orientation of the viewport.
     * @return A Pair containing position (FloatArray of size 3) and orientation (FloatArray of size 4).
     */
    fun getPose(timeStampNs: Long, viewportOrientation: ViewportOrientation): Pair<FloatArray, FloatArray> {
        val position = FloatArray(3)
        val orientation = FloatArray(4)
        getPose(timeStampNs, viewportOrientation, position, orientation)
        return Pair(position, orientation)
    }

    /**
     * Same as [getPose] but fills the provided arrays with position and orientation.
     */
    fun getPose(timeStampNs: Long, viewportOrientation: ViewportOrientation, position: FloatArray, orientation: FloatArray)

    /**
     * Pauses the head tracker.
     */
    fun pause()

    /**
     * Resumes the head tracker.
     */
    fun resume()

    /**
     * Re-centers the head tracker.
     */
    fun recenter()

    /**
     * Sets the low-pass filter cutoff frequency for the head tracker.
     */
    fun setLowPassFilter(cutoffFrequency: Int)
}
