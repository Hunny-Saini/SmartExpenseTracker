package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.alpha
import com.example.R

@Composable
fun SplashScreen(
    onNavigateToDashboard: () -> Unit,
    onNavigateToAuth: () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    val scale = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (startAnimation) 1.2f else 0.5f,
        animationSpec = androidx.compose.animation.core.tween(
            durationMillis = 800,
            easing = { fraction -> android.view.animation.OvershootInterpolator(2f).getInterpolation(fraction) }
        ),
        label = "scale"
    )
    val alpha = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000),
        label = "alpha"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(2000L)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            onNavigateToDashboard()
        } else {
            onNavigateToAuth()
        }
    }

    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val splashTextColor = if (isSystemDark) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.scale(scale.value).alpha(alpha.value)
        ) {
            SmartSpendLogo(
                modifier = Modifier.size(140.dp),
                primaryColor = MaterialTheme.colorScheme.primary, // Azure Blue
                secondaryColor = splashTextColor // High contrast
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "SMART SPEND",
                fontSize = 28.sp,
                letterSpacing = 2.sp,
                fontWeight = FontWeight.Bold,
                color = splashTextColor
            )
        }
    }
}
