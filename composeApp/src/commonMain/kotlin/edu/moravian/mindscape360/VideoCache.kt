package edu.moravian.mindscape360

import io.ktor.client.call.body
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.asByteWriteChannel
import io.ktor.utils.io.copyAndClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.io.files.Path
import kotlinx.io.files.FileSystem
import kotlinx.io.files.SystemFileSystem
import kotlin.math.max
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

// TODO: move all okio code to kotlinx-io?

private val videoUriCache = mutableMapOf<String, String>()
private val VIDEO_TTL = 30.days // Images expire after 30 days


/**
 * Try to get cached video URI, but return original URL if caching fails.
 */
suspend fun getVideoPath(url: String, refresh: Boolean = false, videoReadyToStream: (String) -> Unit) {
//    videoReadyToStream(url)
//    return

    //val url = url.replace(".mp4", ".mov")

    if (!refresh && url in videoUriCache) {
        // Cached and accessed this application session
        println("Video URI cache hit: $url")
        videoReadyToStream(videoUriCache[url]!!)
        return
    }

    val localPath = filePathTo("video_${url.hashCode().toString().replace("-", "")}.mp4")
    val uri = filePathToUri(localPath)
    val path = Path(localPath)
    val metadata = SystemFileSystem.metadataOrNull(path)

    if (!refresh && metadata != null && !isCacheExpired(localPath, VIDEO_TTL)) {
        // Cached on disk
        println("Video cache hit: $url")
        videoUriCache[url] = uri
        videoReadyToStream(uri)
        return
    }

    println("Downloading video: $url")
    val tempPath = Path("$localPath.temp")
    if (refresh) { SystemFileSystem.tryDelete(tempPath) }
    var calledReadyToStream = false
    withContext(Dispatchers.IO) {
        val completed = downloadVideoWithRetry(url, tempPath) { offset, received, total, elapsed ->
            if (!calledReadyToStream) {
                // Compute number of ms remaining (or null if unknown)
                // TODO: fix on iOS (crashes - uses shouldStreamEarly() which returns false on iOS to suppress for the moment
//                val eta = if (total > 0) {
//                    val rate = received.toDouble() / elapsed.inWholeMilliseconds.toDouble() // bytes per ms
//                    val remaining = total - received
//                    if (rate > 0 && remaining >= 0) { (remaining / rate).toLong() } else { null }
//                } else { null }
//
                if (shouldStreamEarly() &&
                    (/*eta == null &&*/ (offset + received) > 64*(1024*1024)) //||
                // (eta != null && eta < 15_000L) // TODO: use eta and how much has been downloaded
                ) {
                    calledReadyToStream = true
                    videoReadyToStream("$uri.temp")
                }
            }
        }
        if (completed) {
            // Rename temp file to final file
            println("Video cached successfully")
            SystemFileSystem.tryDelete(path)
            SystemFileSystem.atomicMove(tempPath, path)
            videoUriCache[url] = uri
            videoReadyToStream(uri) // TODO: ensure called only once?
        } else {
            println("Video caching failed")
            // Return original URL on failure or cached file if exists (but is too old)
            videoReadyToStream(if (metadata != null) uri else url)
        }
    }
}

expect fun shouldStreamEarly(): Boolean

/**
 * Download video file with retries on failure.
 */
@OptIn(ExperimentalTime::class)
private suspend fun downloadVideoWithRetry(
    url: String, dest: Path, maxRetries: Int = 5,
    progress: (start: Long, received: Long, total: Long, time: Duration) -> Unit
): Boolean {
    var completed = false
    var hasSomeProgress = true
    var tries = 0
    var origOffset = 0L
    val startTime = Clock.System.now()
    while (!completed && hasSomeProgress && tries < maxRetries) {
        hasSomeProgress = false
        tries++
        try {
            downloadVideo(url, dest) { offset, received, total, elapsed ->
                if (tries == 1) { origOffset = offset }
                hasSomeProgress = received > 0
                completed = total > 0 && received >= total
                val diff = offset - origOffset
                progress(origOffset, received + diff,
                    if (total > 0) total + diff else -1L,
                    Clock.System.now() - startTime)
            }
        } catch (e: Throwable) {
            println("Video caching request failed: ${e::class.simpleName}")
            e.printStackTrace()
            if (!completed && hasSomeProgress && tries < maxRetries) {
                println("Retrying download... ($tries/$maxRetries)")
            }
        }
    }
    return completed
}

/**
 * Download video file with minimal memory usage by streaming directly to disk.
 * If the file already exists, resume the download from where it left off.
 * @param url The URL of the video to download.
 * @param path The local file path to save the video.
 * @param progress A callback function to report download progress.
 */
@OptIn(ExperimentalTime::class)
private suspend fun downloadVideo(
    url: String, path: Path,
    progress: (start: Long, received: Long, total: Long, time: Duration) -> Unit
) {
    val offset = SystemFileSystem.metadataOrNull(path)?.size ?: 0L
    SystemFileSystem.sink(path, true).use { sink ->
        client.prepareGet(url) {
            if (offset > 0L) {
                println("Resuming download from byte offset $offset")
                headers.append("Range", "bytes=$offset-")
            }
            val startTime = Clock.System.now()
//            var nextReport = 10L * 1024 * 1024
            onDownload { received, total ->
                progress(offset, received, total ?: -1L, Clock.System.now() - startTime)
                // TODO: remove the next 2 if checks when not debugging
//                if (received > nextReport) {
//                    println("Downloaded ${received/(1024*1024)}MB...")
//                    nextReport = max(nextReport + 10L*1024*1024, received + 1)
//                }
//                if (Random.nextInt(20000) == 0) {
//                    println("Simulating network interruption...")
//                    throw IOException("Simulated network interruption")
//                }
            }
        }.execute { httpResponse ->
            // val total = totalSize(httpResponse, offset)
            httpResponse.body<ByteReadChannel>().copyAndClose(sink.asByteWriteChannel())
        }
    }
}

private fun FileSystem.tryDelete(path: Path) {
    try { delete(path) } catch (_: IOException) { }
}

private fun totalSize(httpResponse: HttpResponse, offset: Long = 0L): Long? {
    if (httpResponse.status == HttpStatusCode.PartialContent) {
        val contentRange = httpResponse.headers["Content-Range"]
        if (contentRange != null) {
            val parts = contentRange.split("/")
            if (parts.size == 2) { return parts[1].toLong() }
        }
    }
    return httpResponse.headers["Content-Length"]?.toLongOrNull()?.let { it + offset }
}

expect fun filePathToUri(filePath: String): String