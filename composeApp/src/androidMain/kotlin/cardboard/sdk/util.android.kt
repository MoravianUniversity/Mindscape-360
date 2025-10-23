package cardboard.sdk

import android.content.Context
import android.os.SystemClock
import cardboard.util.AssetURLStreamHandler
import java.net.URL


object Cardboard {
    private var initialized: Boolean = false
    init { System.loadLibrary("cardboard") }
    @JvmStatic
    private external fun jniInit(context: Context)
    fun ensureInitialized(context: Context) {
        if (!initialized) {
            jniInit(context)
            initialized = true
            // TODO: don't override the factory if it was already set?
            URL.setURLStreamHandlerFactory {
                if (it == "asset") { AssetURLStreamHandler(context) }
                else { null }
            }
        }
    }
}

actual fun getBootTimeNano() = SystemClock.elapsedRealtimeNanos()

sealed class NativeWrapper(
    private var _ptr: Long,
    private val destroy: (Long) -> Unit,
) {
    init { check(_ptr != 0L) { "Failed to create native ${this::class.simpleName}." } }
    internal val pointer: Long get() {
        check(_ptr != 0L) { "${this::class.simpleName} is destroyed." }
        return _ptr
    }
    fun destroy() {
        if (_ptr != 0L) {
            destroy(_ptr)
            _ptr = 0L
        }
    }
    protected fun finalize() { destroy() }
}
