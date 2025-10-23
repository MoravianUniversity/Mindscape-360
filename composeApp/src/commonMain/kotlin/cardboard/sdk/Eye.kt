package cardboard.sdk

/**
 * Enum to distinguish between the left and right eyes in VR rendering.
 */
enum class Eye(val value: Int) {
    LEFT(0),
    RIGHT(1);
    companion object {
        fun from(value: Int) = ViewportOrientation.entries.find { it.value == value }
            ?: throw IllegalArgumentException("Invalid Eye value: $value")
    }
}
