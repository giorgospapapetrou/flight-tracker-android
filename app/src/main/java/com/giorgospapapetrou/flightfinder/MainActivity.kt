package com.giorgospapapetrou.flightfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.giorgospapapetrou.flightfinder.ui.map.MapScreen
import com.giorgospapapetrou.flightfinder.ui.splash.SplashScreen
import com.giorgospapapetrou.flightfinder.ui.theme.FlightFinderTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FlightFinderTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
                    Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                        AppRoot()
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRoot() {
    var showMap by remember { mutableStateOf(false) }
    if (showMap) {
        MapScreen()
    } else {
        SplashScreen(onConnected = { showMap = true })
    }
}