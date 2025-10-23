package cardboard.sdk

enum class ViewportOrientation(val value: Int) {
    /**
     * Landscape right orientation, which maps to:
     * - Android: Landscape
     * - iOS: UIDeviceOrientationLandscapeLeft
     * - Unity: ScreenOrientation.LandscapeLeft
     */
    LANDSCAPE_LEFT(0),

    /**
     * Landscape right orientation, which maps to:
     * - Android: reverseLandscape
     * - iOS: UIDeviceOrientationLandscapeRight
     * - Unity: ScreenOrientation.LandscapeRight
     */
    LANDSCAPE_RIGHT(1),

    /**
     * Portrait up orientation, which maps to:
     * - Android: Portrait
     * - iOS: UIDeviceOrientationPortrait
     * - Unity: ScreenOrientation.Portrait
     */
    PORTRAIT_UP(2),

    /**
     * Portrait down orientation, which maps to:
     * - Android: reversePortrait
     * - iOS: UIDeviceOrientationPortraitUpsideDown
     * - Unity: ScreenOrientation.PortraitUpsideDown
     */
    PORTRAIT_DOWN(3);

    /**
     * Creates ViewportOrientation from an integer value.
     *
     * @param value 0-3 for the orientation values
     * @return corresponding ViewportOrientation
     * @throws IllegalArgumentException if the value is not valid
     */
    companion object {
        fun from(value: Int) = entries.find { it.value == value }
            ?: throw IllegalArgumentException("Invalid ViewportOrientation value: $value")
    }
}