package edu.moravian.mindscape360

import java.io.File

actual fun filePathToUri(filePath: String): String {
    // Android: Convert file path to file:// URI
    return File(filePath).toURI().toString()
    // Returns: "file:///data/data/vr.app/files/video_12345.mp4"
}

actual fun shouldStreamEarly() = true
