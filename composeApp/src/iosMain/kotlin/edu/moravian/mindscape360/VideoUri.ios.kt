package edu.moravian.mindscape360

actual fun filePathToUri(filePath: String): String {
    // iOS: Convert file path to file:// URI
    return "file://$filePath"
    // Returns: "file:///var/mobile/.../Documents/video_12345.mp4"
}

actual fun shouldStreamEarly() = false
