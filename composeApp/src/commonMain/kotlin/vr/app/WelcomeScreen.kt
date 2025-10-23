package vr.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import navController
import org.jetbrains.compose.resources.painterResource
import vrapp.composeapp.generated.resources.Res
import vrapp.composeapp.generated.resources.logo
import vr.app.data.AppContent

@Composable
fun WelcomeScreen() {
    val appContent = AppContent.getWelcomeContent()

    Image(
        painter = painterResource(Res.drawable.logo),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop
    )

    var isTextVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(2000) // Delay for 2 seconds
        isTextVisible = true // Show the text after the delay
    }

    AnimatedVisibility(
        visible = isTextVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 5000)),
        exit = fadeOut(animationSpec = tween(durationMillis = 1000))
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append("${appContent.title}\n\n")
                            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                                append("${appContent.subtitle}\n\n")
                            }
                        }
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("Before you begin:\n")
                        }

                        appContent.instructions.forEach { instruction ->
                            append("$instruction\n")
                        }

                        append("\n${appContent.helpText}\n")
                    },
                    fontSize = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .widthIn(max = 400.dp)
                        .wrapContentHeight()
                )

                ElevatedButton(
                    onClick = {
                        navController.navigate(route = "videos", navOptions = null)
                    },
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 15.dp
                    ),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color(0xFF4F7942),
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(appContent.buttonText, fontSize = 20.sp)
                }
            }
        }
    }
}