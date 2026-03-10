package edu.moravian.mindscape360

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import navController
import org.jetbrains.compose.resources.painterResource
import vrapp.composeapp.generated.resources.Res
import vrapp.composeapp.generated.resources.arrow_back


@Composable
fun AboutScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8F5E9))
            .padding(top = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 68.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // Title Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "MindScape 360",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4F7942)
                    )
                    Text(
                        text = "Immersive Mindfulness for Pain and Anxiety Relief",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF6B8E23),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // About Card
            InfoCard(
                title = "About",
                content = {
                    Text(
                        text = "MindScape 360 is a virtual reality mindfulness experience designed for anyone seeking a calming, low-cost, and portable way to manage pain and anxiety at the point of care.",
                        fontSize = 16.sp,
                        color = Color.DarkGray,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Text(
                        text = "Developed through research, the app delivers guided mindfulness sessions set in immersive 360° nature scenes. Using a smartphone and an affordable VR viewer, users can experience nature-based relaxation anywhere—in hospitals, rehab centers, or at home.",
                        fontSize = 16.sp,
                        color = Color.DarkGray,
                        lineHeight = 24.sp
                    )
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Features Card
            InfoCard(
                title = "Features",
                content = {
                    BulletPoint("Nature-based, guided mindfulness sessions for pain and stress management")
                    BulletPoint("360° immersive visuals with calming audio")
                    BulletPoint("Simple design: launch and start instantly")
                    BulletPoint("Compatible with any VR viewer or 3D-printed headset")
                    BulletPoint("Created to be affordable, accessible, and clinically informed")
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // What You'll Need Card
            InfoCard(
                title = "What You'll Need",
                content = {
                    BulletPoint("A smartphone (Android or iOS)")
                    BulletPoint("A VR viewer (VR headset or Google Cardboard-style viewer)")
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // How to Use Card
            val uriHandler = LocalUriHandler.current
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF6B8E23)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "How to Use Your VR Headset",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "Watch our quick tutorial video to learn how to set up and use your VR viewer with MindScape 360",
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Button(
                        onClick = {
                            uriHandler.openUri("https://www.youtube.com/shorts/9TFiNWiEghk")
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF4F7942)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "▶  Watch Tutorial Video",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Attribution Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5DC)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Attribution",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4F7942),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Guided scripts in this app are adapted from the Free Mindfulness Project, shared under the Creative Commons BY-NC-SA 3.0 License. Used with gratitude for non-commercial, educational purposes.",
                        fontSize = 14.sp,
                        color = Color(0xFF555555),
                        lineHeight = 20.sp
                    )
                }
            }

            // Developed By Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF4F7942)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Developed By",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Jeffrey Bush\nMarena Abboud\nSara Benham",
                        fontSize = 16.sp,
                        color = Color.White,
                        lineHeight = 24.sp,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "at\nMoravian University",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFE8F5E9),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Back button
        FloatingActionButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 0.dp, start = 16.dp),
            containerColor = Color(0xFF4F7942),
        ) {
            Icon(
                painter = painterResource(Res.drawable.arrow_back),
                contentDescription = "Back",
                modifier = Modifier.padding(8.dp),
                tint = Color.White,
            )
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4F7942),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "•",
            fontSize = 20.sp,
            color = Color(0xFF4F7942),
            modifier = Modifier.padding(end = 12.dp, top = 2.dp)
        )
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color.DarkGray,
            lineHeight = 24.sp,
            modifier = Modifier.weight(1f)
        )
    }
}