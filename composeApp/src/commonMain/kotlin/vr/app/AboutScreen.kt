package vr.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import navController
import vrapp.composeapp.generated.resources.arrow_back

@Composable
fun AboutScreen() {
    FloatingActionButton(onClick = {navController.popBackStack() },
        modifier = Modifier
            .padding(top = 35.dp, start = 10.dp),
        containerColor = Color(0xFF4F7942),
    ) {
        Icon(
            painter = org.jetbrains.compose.resources.painterResource(vrapp.composeapp.generated.resources.Res.drawable.arrow_back),
            contentDescription = "Back",
            modifier = Modifier.padding(8.dp)
        )
    }
}