import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import cardboard.rememberNativeRenderView
import cardboard.sdk.QrCode
import cardboard.util.getAssetURL
import cardboard.util.rememberMediaPlayer
import org.jetbrains.compose.resources.painterResource
import vr.app.sphere
import vrapp.composeapp.generated.resources.Res
import vrapp.composeapp.generated.resources.arrow_back
import vrapp.composeapp.generated.resources.help
import vrapp.composeapp.generated.resources.quietstream
import vrapp.composeapp.generated.resources.rockybeach
import vrapp.composeapp.generated.resources.serenebeach
import vrapp.composeapp.generated.resources.sunriseforest

@Composable
fun VideosScreen(navController: NavController, onClick: () -> Unit) {
    var showVR by remember { mutableStateOf(false) }
    var selectedVideo by remember { mutableStateOf("") }

    if (showVR) {
        VRPlayerContent(
            videoFileName = selectedVideo,
            onBack = { showVR = false }
        )
    } else {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { navController.navigate("actual") },
                    containerColor = Color(0xFF4F7942),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.help),
                        contentDescription = "Help",
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                stickyHeader {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF4F7942))
                            .statusBarsPadding()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Mindfulness Videos",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item {
                    VideoItem(
                        imageRes = Res.drawable.sunriseforest,
                        title = "Sunrise Forest with Mindfulness",
                        duration = "6 min 30 sec",
                        fileName = "SunriseForest.mp4"
                    ) {
                        selectedVideo = "SunriseForest.mp4"
                        showVR = true
                    }
                }

                item {
                    VideoItem(
                        imageRes = Res.drawable.rockybeach,
                        title = "Rocky Beach with Mindfulness",
                        duration = "6 min 40 sec",
                        fileName = "RockyBeach.mp4"
                    ) {
                        selectedVideo = "RockyBeach.mp4"
                        showVR = true
                    }
                }

                item {
                    VideoItem(
                        imageRes = Res.drawable.serenebeach,
                        title = "Serene Beach with Mindfulness",
                        duration = "6 min 40 sec",
                        fileName = "SereneBeach.mp4"
                    ) {
                        selectedVideo = "SereneBeach.mp4"
                        showVR = true
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoItem(
    imageRes: org.jetbrains.compose.resources.DrawableResource,
    title: String,
    duration: String,
    fileName: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = title,
            modifier = Modifier
                .width(400.dp)
                .clickable { onClick() }
        )

        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(top = 12.dp)
        )

        Text(
            text = "Length: $duration",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun VRPlayerContent(videoFileName: String, onBack: () -> Unit) {
    val renderView = rememberNativeRenderView()
    val mediaPlayer = rememberMediaPlayer()

    val panoramaVideo = remember(renderView, mediaPlayer) {
        PanoramaVideo(
            view = renderView,
            player = mediaPlayer,
            mesh = sphere(5f),
            floorHeight = -1.7f
        )
    }

    LaunchedEffect(videoFileName) {
        try {
            val videoUrl = getAssetURL(videoFileName)
            mediaPlayer.load(videoUrl, repeat = true)
            mediaPlayer.play()
        } catch (e: Exception) {
            println("Error loading video $videoFileName: ${e.message}")
            onBack()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        panoramaVideo.View(modifier = Modifier.fillMaxSize())

        FloatingActionButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .size(40.dp),
            containerColor = Color(0xFF4F7942),
            contentColor = Color.White
        ) {
            Icon(
                painter = painterResource(vrapp.composeapp.generated.resources.Res.drawable.arrow_back),
                contentDescription = "Back",
                modifier = Modifier.size(20.dp)
            )
        }
    }
//    Button(
//        onClick = {
//            QrCode.scanQrCodeAndSaveDeviceParams()
//        },
//        modifier = Modifier
//            .align(Alignment.BottomCenter)
//            .padding(20.dp)
//    ) {
//        Text("Scan QR")
//    }

}










