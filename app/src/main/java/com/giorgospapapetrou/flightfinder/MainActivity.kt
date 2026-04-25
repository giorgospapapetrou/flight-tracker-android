package com.giorgospapapetrou.flightfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.giorgospapapetrou.flightfinder.ui.aircraftlist.AircraftListScreen
import com.giorgospapapetrou.flightfinder.ui.flightdetail.FlightDetailScreen
import com.giorgospapapetrou.flightfinder.ui.history.HistoryScreen
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
                AppRoot()
            }
        }
    }
}

private enum class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Map("Map", Icons.Default.Map),
    Aircraft("Aircraft", Icons.Default.FlightTakeoff),
    History("History", Icons.Default.History),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot() {
    var connected by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(Tab.Map) }
    var openFlightId by remember { mutableStateOf<Int?>(null) }

    if (!connected) {
        SplashScreen(onConnected = { connected = true })
        return
    }

    val openFlight = openFlightId

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Flight Finder") },
                navigationIcon = {
                    if (openFlight != null) {
                        IconButton(onClick = { openFlightId = null }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (openFlight == null) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (openFlight != null) {
                FlightDetailScreen(flightId = openFlight)
            } else {
                when (currentTab) {
                    Tab.Map -> MapScreen()
                    Tab.Aircraft -> AircraftListScreen()
                    Tab.History -> HistoryScreen(
                        onFlightClick = { id -> openFlightId = id }
                    )
                }
            }
        }
    }
}