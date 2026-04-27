package com.giorgospapapetrou.flightfinder.ui.aircraftlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Image
import com.giorgospapapetrou.flightfinder.R
import com.giorgospapapetrou.flightfinder.domain.model.Aircraft
import com.giorgospapapetrou.flightfinder.ui.theme.AircraftBlueLt
import com.giorgospapapetrou.flightfinder.ui.theme.BgCard
import com.giorgospapapetrou.flightfinder.ui.theme.NavActiveBg
import com.giorgospapapetrou.flightfinder.ui.theme.OnSurfaceDark
import com.giorgospapapetrou.flightfinder.ui.theme.OnSurfaceVar

private val OutlineDark = androidx.compose.ui.graphics.Color(0xFF2A2A3A)

@Composable
fun AircraftListScreen(
    viewModel: AircraftListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading && state.aircraft.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            state.aircraft.isEmpty() -> {
                Text(
                    "No aircraft currently tracked.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = OnSurfaceVar,
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.sortedAircraft, key = { it.icao }) { aircraft ->
                        AircraftRow(aircraft)
                    }
                }
            }
        }
    }
}

@Composable
private fun AircraftRow(aircraft: Aircraft) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, OutlineDark),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Icon box
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NavActiveBg),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_airplane),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(AircraftBlueLt),
                )
            }

            // Callsign + ICAO
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = aircraft.callsign?.takeIf { it.isNotBlank() } ?: aircraft.icao,
                    color = OnSurfaceDark,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "ICAO ${aircraft.icao}",
                    color = OnSurfaceVar,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }

            // Telemetry stack
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = aircraft.altitudeFt?.let { "$it ft" } ?: "—",
                    color = OnSurfaceDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = aircraft.groundSpeedKt?.let { "$it kt" } ?: "—",
                        color = OnSurfaceVar,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    Text(
                        text = aircraft.headingDeg?.let { "$it\u00B0" } ?: "—",
                        color = OnSurfaceVar,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}