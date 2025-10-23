package vr.app

import PanoramaVideo
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import cardboard.rememberNativeRenderView
import cardboard.util.getAssetURL
import cardboard.util.rememberMediaPlayer

@Composable
fun VrPlayerScreen(
    assetFileName: String,
    onExit: () -> Unit
) {
    var showContent by remember { mutableStateOf(true) }

    val nativeRenderView = rememberNativeRenderView()
    val mediaPlayer = rememberMediaPlayer()

    val cardboard = remember(assetFileName) {
        mediaPlayer.load(getAssetURL(assetFileName))
        PanoramaVideo(nativeRenderView, mediaPlayer)
    }

    LaunchedEffect(showContent) {
        if (showContent) mediaPlayer.play() else mediaPlayer.pause()
    }

    DisposableEffect(Unit) {
        onDispose {
            try { mediaPlayer.pause() } catch (_: Throwable) {}
        }
    }

    AnimatedVisibility(showContent) {
        cardboard.View(Modifier.fillMaxSize())
    }
}
