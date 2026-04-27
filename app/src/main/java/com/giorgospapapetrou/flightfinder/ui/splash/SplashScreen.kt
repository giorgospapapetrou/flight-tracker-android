package com.giorgospapapetrou.flightfinder.ui.splash

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giorgospapapetrou.flightfinder.R
import com.giorgospapapetrou.flightfinder.ui.theme.AircraftBlueLt
import com.giorgospapapetrou.flightfinder.ui.theme.BgDefault
import com.giorgospapapetrou.flightfinder.ui.theme.EndRed
import com.giorgospapapetrou.flightfinder.ui.theme.OnSurfaceDark
import com.giorgospapapetrou.flightfinder.ui.theme.OnSurfaceVar
import com.giorgospapapetrou.flightfinder.ui.theme.StartGreen
import kotlinx.coroutines.delay
import kotlin.math.sin

// ── Floating dots (positions in % of screen) — from design spec ───────────
private data class FloatingDot(
    val xPct: Float,
    val yPct: Float,
    val radiusDp: Int,
    val color: Color,
)

private val FloatingDots = listOf(
    FloatingDot(0.18f, 0.09f, 7, Color(0x59_64B4FF)), // blue   ~0.35
    FloatingDot(0.72f, 0.14f, 6, Color(0x4D_FFA0B4)), // pink   ~0.30
    FloatingDot(0.08f, 0.35f, 9, Color(0x47_64DCA0)), // green  ~0.28
    FloatingDot(0.85f, 0.28f, 5, Color(0x47_A08CFF)), // purple ~0.28
    FloatingDot(0.12f, 0.52f, 7, Color(0x38_FFB464)), // orange ~0.22
    FloatingDot(0.78f, 0.48f, 8, Color(0x40_64C8FF)), // blue   ~0.25
    FloatingDot(0.25f, 0.68f, 5, Color(0x38_FF8CA0)), // pink   ~0.22
    FloatingDot(0.88f, 0.65f, 6, Color(0x38_8CDC8C)), // green  ~0.22
    FloatingDot(0.55f, 0.08f, 5, Color(0x33_FFC878)), // yellow ~0.20
    FloatingDot(0.40f, 0.78f, 6, Color(0x38_A0B4FF)), // lavender ~0.22
)

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
    onConnected: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is SplashUiState.Connected) {
            delay(500)
            onConnected()
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDefault),
    ) {
        val w = maxWidth
        val h = maxHeight

        // Floating pastel dots
        FloatingDotsLayer(screenWidthDp = w.value, screenHeightDp = h.value)

        // Center content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AppIconBadge(size = 110)

            Spacer(Modifier.height(18.dp))

            Text(
                text = "Flight Finder",
                color = OnSurfaceDark,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.3).sp,
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Track flights in real-time worldwide.",
                color = OnSurfaceVar,
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(28.dp))

            StatusContent(state = state, onRetry = viewModel::connect)
        }

        // Bottom progress dots + small plane
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            ProgressIndicator(active = state is SplashUiState.Connecting)
        }
    }
}

@Composable
private fun AppIconBadge(size: Int) {
    val gradient = Brush.linearGradient(
        colors = listOf(Color(0xFF1E88E5), Color(0xFF0D47A1)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
    )
    Box(
        modifier = Modifier
            .size(size.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape((size * 0.24f).dp),
                ambientColor = Color(0xFF1976D2),
                spotColor = Color(0xFF1976D2),
            )
            .clip(RoundedCornerShape((size * 0.24f).dp))
            .background(gradient),
        contentAlignment = Alignment.Center,
    ) {
        // Orbit ring (decorative) — drawn behind the plane
        Box(
            modifier = Modifier
                .size((size * 0.62f).dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Transparent),
        ) {
            // Faint ring via outline approximation: a transparent box with a thin
            // border isn't trivial without the border modifier, so we use a
            // slightly-tinted circle behind the plane to evoke the orbit.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.06f)),
            )
        }

        Image(
            painter = painterResource(id = R.drawable.ic_airplane),
            contentDescription = null,
            modifier = Modifier.size((size * 0.55f).dp),
            colorFilter = ColorFilter.tint(Color.White),
        )
    }
}

@Composable
private fun StatusContent(
    state: SplashUiState,
    onRetry: () -> Unit,
) {
    when (state) {
        is SplashUiState.Connecting -> {
            Text(
                text = "Connecting to backend\u2026",
                color = OnSurfaceVar,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        is SplashUiState.Connected -> {
            Text(
                text = "Connected",
                color = StartGreen,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tracking ${state.status.aircraftCount} aircraft",
                color = OnSurfaceVar,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        is SplashUiState.Failed -> {
            Text(
                text = "Backend unavailable",
                color = EndRed,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.message,
                color = OnSurfaceVar,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E)),
            ) {
                Text("Retry")
            }
        }
    }
}

@Composable
private fun FloatingDotsLayer(screenWidthDp: Float, screenHeightDp: Float) {
    val transition = rememberInfiniteTransition(label = "floatingDots")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "floatingDotsT",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        FloatingDots.forEachIndexed { i, dot ->
            val baseX = (dot.xPct * screenWidthDp).dp
            val baseY = (dot.yPct * screenHeightDp).dp
            val phase = i * 0.6f
            val bobDp = (sin(t + phase) * 5f).dp
            Box(
                modifier = Modifier
                    .offset(x = baseX, y = baseY + bobDp)
                    .size((dot.radiusDp * 2).dp)
                    .clip(RoundedCornerShape(50))
                    .background(dot.color),
            )
        }
    }
}

@Composable
private fun ProgressIndicator(active: Boolean) {
    val count = 6
    var dotIdx by remember { mutableIntStateOf(0) }

    LaunchedEffect(active) {
        if (active) {
            while (true) {
                delay(320)
                dotIdx = (dotIdx + 1) % count
            }
        }
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Small plane icon, rotated 90° to face right
        Image(
            painter = painterResource(id = R.drawable.ic_airplane),
            contentDescription = null,
            modifier = Modifier
                .size(22.dp)
                .rotate(90f),
            colorFilter = ColorFilter.tint(OnSurfaceVar),
        )

        Spacer(Modifier.size(4.dp))

        repeat(count) { i ->
            val isActive = active && i == dotIdx
            val isFilled = active && i <= dotIdx
            val dotSize = if (isActive) 10 else 7
            val color = when {
                isFilled -> StartGreen
                else -> OnSurfaceVar.copy(alpha = 0.35f)
            }
            Box(
                modifier = Modifier
                    .size(dotSize.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color),
            )
        }
    }
}