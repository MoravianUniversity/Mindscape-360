package cardboard.util

import android.content.Context
import android.content.res.AssetManager
import okio.source
import java.io.InputStream
import java.net.Proxy
import java.net.URL
import java.net.URLConnection
import java.net.URLStreamHandler
import kotlin.String

actual fun getAssetURL(path: String) = "asset:///$path"

actual fun getAsset(path: String, context: Any?) =
    (context as? Context ?: throw IllegalArgumentException("Context must be of type android.content.Context"))
        .assets.open(path).source()



// A custom URL stream handler for the "asset:///" scheme.
// That scheme is already supported by a few things in Android like the
// ExoPlayer, but not the standard URL class and BitmapFactory.decodeStream().

internal class AssetURLStreamHandler(val assets: AssetManager): URLStreamHandler() {
    constructor(context: Context) : this(context.assets)

    override fun openConnection(u: URL) = openConnection(u, null)
    override fun openConnection(u: URL, p: Proxy?): URLConnection? {
        val host = u.host
        return if (u.protocol == "asset" && (host == null || host == ""))
            AssetUrlConnection(u) else null
    }

    private inner class AssetUrlConnection(url: URL) : URLConnection(url) {
        private val filename = url.path.removePrefix("/")  // must be relative to the assets root
        private var stream: InputStream? = null
        private var length = 0
        override fun connect() {
            if (connected) { return }
            stream = assets.open(filename).also { length = it.available().toInt() }
            connected = true
        }
        private inline fun <T> ensureConnected(crossinline block: () -> T): T {
            if (!connected) connect()
            return block()
        }
        override fun getInputStream() = ensureConnected { stream!! }
        override fun getContentLength() = ensureConnected { length }
        override fun getContentLengthLong() = ensureConnected { length.toLong() }
    }
}