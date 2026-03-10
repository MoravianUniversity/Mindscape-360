package edu.moravian.mindscape360

import PanoramaVideo
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import edu.moravian.cardboard.rememberNativeRenderView
import edu.moravian.cardboard.sdk.QrCode
import edu.moravian.cardboard.util.rememberMediaPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import vrapp.composeapp.generated.resources.Res
import vrapp.composeapp.generated.resources.arrow_back
import vrapp.composeapp.generated.resources.help
import vrapp.composeapp.generated.resources.QR_code


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var showVR by remember { mutableStateOf(false) }
    var selectedVideoUrl by remember { mutableStateOf("") }

    // State for videos data
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var videos by remember { mutableStateOf<List<VideoData>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()

    // Load video data when screen is first composed
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isLoading = true
                errorMessage = null
                videos = VideoDataApi.getVideoData(refresh = false)
                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Error loading videos: ${e.message}"
                isLoading = false
            }
        }
    }

    val onRefresh: () -> Unit = {
        scope.launch {
            try {
                isRefreshing = true
                errorMessage = null
                videos = VideoDataApi.getVideoData(refresh = true)
                isRefreshing = false
            } catch (e: Exception) {
                errorMessage = "Error refreshing videos: ${e.message}"
                isRefreshing = false
            }
        }
    }

    if (showVR) {
        VRPlayerContent(
            videoUrl = selectedVideoUrl,
            onBack = { showVR = false }
        )
    } else {
        EnsureCardboardInit()
        Scaffold(
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // QR Code Scanner Button
                    FloatingActionButton(
                        onClick = {
                            QrCode.scanQrCodeAndSaveDeviceParams()
                        },
                        containerColor = Color(0xFF4F7942),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.QR_code),
                            contentDescription = "Scan QR Code",
                            modifier = Modifier.padding(8.dp),
                            tint = Color.White,
                        )
                    }

                    // Help/About Button
                    FloatingActionButton(
                        onClick = { navController.navigate("about") },
                        containerColor = Color(0xFF4F7942),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.help),
                            contentDescription = "Help",
                            modifier = Modifier.padding(8.dp),
                            tint = Color.White,
                        )
                    }
                }
            }
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF4F7942)
                        )
                    }
                }
                errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = errorMessage!!,
                                color = Color.Red,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onRefresh,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4F7942)
                                )
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = onRefresh,
                        modifier = modifier
                    ) {
                        // Separate videos into two categories
                        val videosWithMindfulness = videos.filter { video ->
                            !video.id.endsWith("NoMindfulness", ignoreCase = true)
                        }
                        val videosWithoutMindfulness = videos.filter { video ->
                            video.id.endsWith("NoMindfulness", ignoreCase = true)
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFE8F5E9))
                        ) {
                            // Section 1: Videos WITH Mindfulness
                            stickyHeader {
                                SectionHeader("Nature Sceneries with Mindfulness")
                            }

                            items(videosWithMindfulness) { video ->
                                VideoItem(
                                    video = video,
                                    scope = scope,
                                    onClick = {
                                        video.files.firstOrNull()?.url?.let { url ->
                                            selectedVideoUrl = url
                                            showVR = true
                                        }
                                    }
                                )
                            }

                            // Section 2: Videos WITHOUT Mindfulness
                            stickyHeader {
                                SectionHeader("Nature Sceneries with Natural Sounds")
                            }

                            items(videosWithoutMindfulness) { video ->
                                VideoItem(
                                    video = video,
                                    scope = scope,
                                    onClick = {
                                        video.files.firstOrNull()?.url?.let { url ->
                                            selectedVideoUrl = url
                                            showVR = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF4F7942))
            .statusBarsPadding()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun VideoItem(
    video: VideoData,
    scope: CoroutineScope,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Photo(
                video.preview,
                video.title,
                scope,
                Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(4.dp, Color.LightGray, RoundedCornerShape(12.dp))
            )

            Text(
                text = video.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black,
                modifier = Modifier.padding(top = 12.dp)
            )

            Text(
                text = video.description,
                fontSize = 13.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(top = 6.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Duration: ${video.approxMin} min",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                video.files.firstOrNull()?.byteSize?.let { size ->
                    Text(
                        text = "Size: ${size.toInt()} MB",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun VRPlayerContent(videoUrl: String, onBack: () -> Unit) {
    val renderView = rememberNativeRenderView()
    val mediaPlayer = rememberMediaPlayer()

    var isLoadingVideo by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var videoUri by remember { mutableStateOf<String?>(null) }

    val panoramaVideo = remember(renderView, mediaPlayer) {
        PanoramaVideo(
            view = renderView,
            player = mediaPlayer,
        )
    }

    LaunchedEffect(videoUrl) {
        try {
            isLoadingVideo = true
            errorMessage = null

            getVideoPath(videoUrl, refresh = false) { uri ->
                println("Loading video from URI: $uri")
                mediaPlayer.load(uri, repeat = true)
                mediaPlayer.play()
                videoUri = uri
                isLoadingVideo = false
            }
        } catch (e: Exception) {
            println("Error loading video from $videoUrl: ${e.message}")
            e.printStackTrace()
            errorMessage = "Error: ${e.message}"
            isLoadingVideo = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer.pause()
            } catch (e: Exception) {
                println("Error pausing video: ${e.message}")
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoadingVideo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF4F7942),
                        modifier = Modifier.size(60.dp),
                        strokeWidth = 6.dp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Loading video...",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "This may take a moment on first play",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.help),
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color.Red,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4F7942)
                        )
                    ) {
                        Text("Go Back")
                    }
                }
            }
        }

        if (!isLoadingVideo && errorMessage == null && videoUri != null) {
            panoramaVideo.View(modifier = Modifier.fillMaxSize())
        }

        FloatingActionButton(
            onClick = {
                try {
                    mediaPlayer.pause()
                } catch (e: Exception) {
                    println("Error pausing on back: ${e.message}")
                }
                onBack()
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .size(40.dp),
            containerColor = Color(0xFF4F7942),
            contentColor = Color.White
        ) {
            Icon(
                painter = painterResource(Res.drawable.arrow_back),
                contentDescription = "Back",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
expect fun EnsureCardboardInit()