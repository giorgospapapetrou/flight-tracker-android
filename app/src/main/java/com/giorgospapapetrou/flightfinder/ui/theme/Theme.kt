package com.giorgospapapetrou.flightfinder.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColors = darkColorScheme(
    primary = AircraftBlue,
    onPrimary = Color.White,
    primaryContainer = NavActiveBg,
    onPrimaryContainer = AircraftBlueLt,
    secondary = ReplayAmber,
    onSecondary = Color.Black,
    background = BgDefault,
    onBackground = OnSurfaceDark,
    surface = BgDefault,
    onSurface = OnSurfaceDark,
    surfaceContainer = BgCard,
    surfaceContainerHigh = BgElevated,
    surfaceVariant = BgCardAlt,
    onSurfaceVariant = OnSurfaceVar,
    outline = OutlineDark,
    error = EndRed,
    onError = Color.White,
)

@Composable
fun FlightFinderTheme(
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = BgDefault.toArgb()
            window.navigationBarColor = BgDefault.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = DarkColors,
        typography = Typography,
        content = content,
    )
}