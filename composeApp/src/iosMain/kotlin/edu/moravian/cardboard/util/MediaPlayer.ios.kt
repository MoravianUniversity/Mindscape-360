package edu.moravian.cardboard.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.*
import platform.CoreMedia.CMTimeMake
import platform.Foundation.NSURL

actual typealias NativeMediaPlayer = AVPlayer

@Composable
actual fun rememberMediaPlayer(): MediaPlayer {
    return remember { IosMediaPlayer(AVPlayer()) }
}

class IosMediaPlayer(
    override val native: NativeMediaPlayer
) : MediaPlayer {
    private var _state: MediaPlayerState = MediaPlayerState.IDLE
    private var item: AVPlayerItem? = null
    private var autostart = false
    @OptIn(ExperimentalForeignApi::class)
    private val statusObserver = createKVObserver<AVPlayerStatus>(native, "status") { old, new ->
        if (new == AVPlayerStatusReadyToPlay) {
            println("Media player is ready to play media")
            println("Player description: ${native.description}")
            println("Player Item: ${native.currentItem?.status} ${native.currentItem?.description}")
            if (_state == MediaPlayerState.LOADING) {
                if (autostart) {
                    native.play()
                    _state = MediaPlayerState.PLAYING
                } else {
                    _state = MediaPlayerState.PAUSED
                }
            }
        } else if (new == AVPlayerStatusFailed) {
            println("Media player failed to load media: ${native.error?.localizedDescription}")
            destroy()
        } else if (new == AVPlayerStatusUnknown) {
            println("Media player status is unknown/idle")
            _state = MediaPlayerState.IDLE
        }
    }.apply { enable() }
    private var endAction: (() -> Unit)? = null
    private var endObserver: Observer? = null

    @OptIn(ExperimentalForeignApi::class)
    override fun load(url: String, repeat: Boolean) {
        val nsUrl = NSURL.URLWithString(url) ?: throw IllegalArgumentException("Invalid URL: $url")
        item = AVPlayerItem.playerItemWithURL(nsUrl)
        autostart = false
        _state = MediaPlayerState.LOADING
        native.replaceCurrentItemWithPlayerItem(item)
        native.actionAtItemEnd = if (repeat) AVPlayerActionAtItemEndNone else AVPlayerActionAtItemEndPause
        endObserver = createNotificationObserver("AVPlayerItemDidPlayToEndTimeNotification", item) {
            if (repeat) {
                native.seekToTime(CMTimeZero)
                native.play() // probably not needed since we don't pause the player when it reaches the end
            } else {
                _state = MediaPlayerState.STOPPED
                endAction?.invoke()
            }
        }.apply { enable() }
    }

    override fun runWhenMediaEnds(action: (() -> Unit)?) { endAction = action }

    override fun play() {
        if (_state == MediaPlayerState.LOADING) {
            autostart = true
        } else if (_state == MediaPlayerState.PAUSED || _state == MediaPlayerState.STOPPED) {
            native.play()
            _state = MediaPlayerState.PLAYING
        }
    }

    override fun pause() {
        native.pause()
        _state = MediaPlayerState.PAUSED
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun stop() {
        native.pause()
        native.seekToTime(CMTimeZero)
        _state = MediaPlayerState.STOPPED
        endAction?.invoke()
    }

    override fun destroy() {
        endObserver?.disable()
        endObserver = null
        statusObserver.disable()
        item = null
        native.replaceCurrentItemWithPlayerItem(null)
        _state = MediaPlayerState.DESTROYED
    }

    override var volume: Float
        get() = native.volume
        set(value) { native.volume = value.coerceIn(0f, 1f) }

    override val state: MediaPlayerState
        get() = _state

    companion object {
        @OptIn(ExperimentalForeignApi::class)
        val CMTimeZero = CMTimeMake(0, 1)  // could also use kCMTimeZero but that isn't a CValue so a bit more cumbersome
    }
}
