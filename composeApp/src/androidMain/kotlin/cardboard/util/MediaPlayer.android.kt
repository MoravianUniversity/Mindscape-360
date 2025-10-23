package cardboard.util

import android.content.ContentResolver
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import android.media.MediaPlayer.OnErrorListener
import android.media.MediaPlayer.OnPreparedListener
import android.media.MediaPlayer.OnCompletionListener


actual typealias NativeMediaPlayer = android.media.MediaPlayer

// the Android MediaPlayer has a very complex set of states and many functions
// can only be called in certain states but it also doesn't provide a way to
// query the current state so we track it the best we can
enum class AMPS {
    IDLE,        // first constructed or after reset()
    INITIALIZED, // after setDataSource()
    PREPARING,   // after prepareAsync()
    PREPARED,    // after onPrepared() or prepare()
    STARTED,     // after start()
    PAUSED,      // after pause()
    STOPPED,     // after stop()
    PLAYBACK_COMPLETED,  // after onCompletion() [but not when looping]
    END,         // after release()
    ERROR        // after onError()
}

@Composable
actual fun rememberMediaPlayer(): MediaPlayer {
    val context = LocalContext.current
    val player = remember { AndroidMediaPlayer(context) }
    DisposableEffect(player) { onDispose { player.destroy() } }
    return player
}

class AndroidMediaPlayer(private val context: Context) : MediaPlayer {
    override val native = NativeMediaPlayer()
    private val handler = Handler(Looper.getMainLooper())
    private var _state = AMPS.IDLE
    private var autostart = false

    init { native.setOnPreparedListener(Listener()) }
    private var endAction: (() -> Unit)? = null
    private inner class Listener : OnPreparedListener, OnCompletionListener, OnErrorListener {
        override fun onPrepared(mp: NativeMediaPlayer) {
            _state = AMPS.PREPARED
            if (autostart) {
                native.start()
                _state = AMPS.STARTED
                autostart = false
            }
        }
        override fun onCompletion(mp: NativeMediaPlayer) {
            _state = AMPS.PLAYBACK_COMPLETED
            endAction?.invoke()
        }
        override fun onError(mp: NativeMediaPlayer, what: Int, extra: Int): Boolean {
            // TODO: maybe handle specific error codes?
            println("AndroidMediaPlayer: Error occurred: what=$what, extra=$extra")
            _state = AMPS.ERROR
            return false
        }
    }

    override fun load(url: String, repeat: Boolean) {
        handler.runOrPost {
            if (_state != AMPS.IDLE) {
                native.reset()
                _state = AMPS.IDLE
            }
            val uri = url.toUri()
            if (uri.scheme == ContentResolver.SCHEME_FILE) {
                native.setDataSource(uri.path)
            } else if (uri.scheme == "asset") {
                val fd = context.assets.openFd(uri.path!!.removePrefix("/"))
                native.setDataSource(fd)
                fd.close()
            } else {
                native.setDataSource(context, uri)
            }
            _state = AMPS.INITIALIZED
            native.isLooping = repeat
            native.prepareAsync()
            _state = AMPS.PREPARING
        }
    }

    override fun runWhenMediaEnds(action: (() -> Unit)?) { endAction = action }

    override fun play() {
        handler.runOrPost {
            when (_state) {
                AMPS.STARTED -> { /* already playing, do nothing */ }
                AMPS.PREPARED, AMPS.PLAYBACK_COMPLETED, AMPS.PAUSED -> {
                    native.start()
                    _state = AMPS.STARTED
                }
                AMPS.INITIALIZED, AMPS.PREPARING, AMPS.STOPPED -> {
                    autostart = true
                    if (_state == AMPS.INITIALIZED || _state == AMPS.STOPPED) {
                        native.prepareAsync()
                        _state = AMPS.PREPARING
                    }
                }
                AMPS.IDLE, AMPS.END, AMPS.ERROR -> throw IllegalStateException("MediaPlayer must be loaded before playing")
            }
        }
    }
    override fun pause() {
        handler.runOrPost {
            when (_state) {
                AMPS.STARTED -> { native.pause(); _state = AMPS.PAUSED }
                AMPS.PAUSED, AMPS.PLAYBACK_COMPLETED, AMPS.STOPPED -> { /* already paused, do nothing */ }
                AMPS.INITIALIZED, AMPS.PREPARING, AMPS.PREPARED -> { autostart = false }
                AMPS.IDLE, AMPS.END, AMPS.ERROR -> throw IllegalStateException("MediaPlayer must be playing before pausing")
            }
        }
    }
    override fun stop() {
        handler.runOrPost {
            when (_state) {
                AMPS.STARTED, AMPS.PAUSED -> {
                    native.stop()
                    _state = AMPS.STOPPED
                    endAction?.invoke()
                }
                AMPS.INITIALIZED, AMPS.PREPARING, AMPS.PREPARED -> { autostart = false }
                AMPS.STOPPED, AMPS.PLAYBACK_COMPLETED -> { /* already stopped, do nothing */ }
                AMPS.IDLE, AMPS.END, AMPS.ERROR -> throw IllegalStateException("MediaPlayer must be playing or paused before stopping")
            }
        }
    }
    override fun destroy() { handler.runOrPost { native.release(); _state = AMPS.END } }

    private var _volume = 1f // default volume
    override var volume: Float
        get() = _volume // there is no native.getVolume() method, so we track it ourselves
        set(value) {
            _volume = value.coerceIn(0f, 1f)
            handler.runOrPost { native.setVolume(_volume, _volume) } // TODO: log scaling?
        }

    override val state get() = when (_state) {
        AMPS.IDLE -> MediaPlayerState.IDLE
        AMPS.INITIALIZED, AMPS.PREPARING -> MediaPlayerState.LOADING
        AMPS.PREPARED, AMPS.PAUSED -> MediaPlayerState.PAUSED
        AMPS.STARTED -> MediaPlayerState.PLAYING
        AMPS.STOPPED, AMPS.PLAYBACK_COMPLETED -> MediaPlayerState.STOPPED
        AMPS.END, AMPS.ERROR -> MediaPlayerState.DESTROYED
    }
}

private fun Handler.runOrPost(action: () -> Unit) {
    if (Looper.myLooper() == looper) action()
    else post(action)
}

