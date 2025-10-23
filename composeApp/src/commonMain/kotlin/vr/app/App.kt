import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview
import vr.app.AboutScreen
import vr.app.ActualScreen
import vr.app.WelcomeScreen


var _navController: NavHostController? = null
val navController: NavHostController
    get() = _navController ?: error("NavController is not initialized")

@Composable
@Preview
fun App() {
    MaterialTheme {
        val welcomeScreen = "welcome"
        val videosScreen = "videos"
        val aboutScreen = "about"
        val actualScreen = "actual"

        _navController = rememberNavController()

        NavHost(navController = navController, startDestination = welcomeScreen) {
            composable(welcomeScreen) { WelcomeScreen() }
            composable("videos") {
                VideosScreen(
                    navController = navController,
                    onClick = {
                        // This onClick is now handled within VideosScreen itself
                        // No navigation needed here since VR mode is embedded
                    }
                )
            }
            composable(aboutScreen) { AboutScreen() }
            composable(actualScreen) { ActualScreen(navController) }
        }
    }
}









