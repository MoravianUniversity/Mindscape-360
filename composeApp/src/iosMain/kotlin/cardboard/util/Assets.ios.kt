package cardboard.util

import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL

fun checkPath(path: String): String? =
    if (NSFileManager.defaultManager.fileExistsAtPath(path)) { NSURL.fileURLWithPath(path).absoluteString } else { null }

actual fun getAssetURL(path: String): String {
    val res = NSBundle.mainBundle.resourcePath ?: throw IllegalStateException("Resource path is null")
    return checkPath("$res/compose-resources/assets/$path") ?: checkPath("$res/$path")
        ?: throw IllegalArgumentException("Asset not found: $path")
}

actual fun getAsset(path: String, context: Any?) = FileSystem.SYSTEM.source(getAssetURL(path).toPath())
