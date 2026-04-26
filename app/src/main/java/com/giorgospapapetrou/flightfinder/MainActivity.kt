package com.giorgospapapetrou.flightfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.giorgospapapetrou.flightfinder.ui.aircraftlist.AircraftListScreen
import com.giorgospapapetrou.flightfinder.ui.flightdetail.FlightDetailScreen
import com.giorgospapapetrou.flightfinder.ui.history.HistoryScreen
import com.giorgospapapetrou.flightfinder.ui.map.MapScreen
import com.giorgospapapetrou.flightfinder.ui.splash.SplashScreen
import com.giorgospapapetrou.flightfinder.ui.theme.AircraftBlueLt
import com.giorgospapapetrou.flightfinder.ui.theme.BgCardAlt
import com.giorgospapapetrou.flightfinder.ui.theme.FlightFinderTheme
import com.giorgospapapetrou.flightfinder.ui.theme.NavActiveBg
import com.giorgospapapetrou.flightfinder.ui.theme.OnSurfaceDark
import com.giorgospapapetrou.flightfinder.ui.theme.OnSurfaceVar
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
    val historyListState = androidx.compose.foundation.lazy.rememberLazyListState()

    if (!connected) {
        SplashScreen(onConnected = { connected = true })
        return
    }

    val openFlight = openFlightId

    BackHandler(enabled = openFlight != null) {
        openFlightId = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (openFlight == null) {
                            Icon(
                                painter = painterResource(R.drawable.ic_airplane),
                                contentDescription = null,
                                tint = AircraftBlueLt,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                        Text(
                            "Flight Finder",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgCardAlt,
                    titleContentColor = OnSurfaceDark,
                    navigationIconContentColor = OnSurfaceDark,
                ),
            )
        },
        bottomBar = {
            if (openFlight == null) {
                NavigationBar(
                    containerColor = BgCardAlt,
                    tonalElevation = 0.dp,
                ) {
                    Tab.entries.forEach { tab ->
                        val selected = currentTab == tab
                        NavigationBarItem(
                            selected = selected,
                            onClick = { currentTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = {
                                Text(
                                    tab.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = AircraftBlueLt,
                                selectedTextColor = AircraftBlueLt,
                                unselectedIconColor = OnSurfaceVar,
                                unselectedTextColor = OnSurfaceVar,
                                indicatorColor = NavActiveBg,
                            ),
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
                        listState = historyListState,
                        onFlightClick = { id -> openFlightId = id },
                    )
                }
            }
        }
    }
}