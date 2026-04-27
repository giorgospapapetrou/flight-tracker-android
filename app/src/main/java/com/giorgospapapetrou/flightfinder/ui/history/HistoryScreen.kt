package com.giorgospapapetrou.flightfinder.ui.history

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giorgospapapetrou.flightfinder.R
import com.giorgospapapetrou.flightfinder.domain.model.FlightSummary
import com.giorgospapapetrou.flightfinder.ui.theme.AircraftBlueLt
import com.giorgospapapetrou.flightfinder.ui.theme.BgCard
import com.giorgospapapetrou.flightfinder.ui.theme.NavActiveBg
import com.giorgospapapetrou.flightfinder.ui.theme.OnSurfaceDark
import com.giorgospapapetrou.flightfinder.ui.theme.OnSurfaceVar
import com.giorgospapapetrou.flightfinder.ui.theme.StartGreen
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val OutlineDark = androidx.compose.ui.graphics.Color(0xFF2A2A3A)
private val BgCardAlt = androidx.compose.ui.graphics.Color(0xFF16161F)

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = hiltViewModel(),
    listState: LazyListState = rememberLazyListState(),
    onFlightClick: (Int) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading && state.flights.isEmpty() -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            state.errorMessage != null && state.flights.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        state.errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                    )
                    TextButton(onClick = viewModel::refresh) {
                        Text("Retry")
                    }
                }
            }
            state.flights.isEmpty() -> {
                Text(
                    "No flights observed today yet.",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = OnSurfaceVar,
                )
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.flights, key = { it.id }) { flight ->
                        FlightRow(
                            flight = flight,
                            onClick = { onFlightClick(flight.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FlightRow(
    flight: FlightSummary,
    onClick: () -> Unit,
) {
    val hasPath = flight.positionCountWithCoords >= 5
    val isLive = flight.isLiveAt(Instant.now())

    Card(
        modifier = Modifier.fillMaxWidth().let {
            if (hasPath) it else it.alpha(0.45f)
        },
        onClick = if (hasPath) onClick else { -> },
        enabled = hasPath,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasPath) BgCard else BgCardAlt,
        ),
        border = BorderStroke(1.dp, OutlineDark),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Icon box — green if live, blue otherwise
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isLive) StartGreen.copy(alpha = 0.18f) else NavActiveBg,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_airplane),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    colorFilter = ColorFilter.tint(
                        if (isLive) StartGreen else AircraftBlueLt,
                    ),
                )
            }

            // Title + time range
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = flight.displayName,
                        color = OnSurfaceDark,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (isLive) LivePill()
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = formatTimeRange(flight),
                    color = OnSurfaceVar,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }

            // Altitude + chevron
            Column(horizontalAlignment = Alignment.End) {
                flight.maxAltitudeFt?.let {
                    Text(
                        text = "$it ft",
                        color = OnSurfaceVar,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                if (hasPath) {
                    Spacer(Modifier.height(2.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = OnSurfaceVar,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LivePill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(StartGreen.copy(alpha = 0.18f))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    ) {
        Text(
            text = "LIVE",
            color = StartGreen,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
        )
    }
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

private fun formatTimeRange(flight: FlightSummary): String {
    val zone = ZoneId.systemDefault()
    val now = Instant.now()
    val start = timeFormatter.format(flight.startedAt.atZone(zone))

    return if (flight.isLiveAt(now)) {
        "$start \u2013 live"
    } else {
        val effectiveEnd = flight.effectiveEndAt
        if (effectiveEnd != null) {
            val end = timeFormatter.format(effectiveEnd.atZone(zone))
            "$start \u2013 $end"
        } else {
            start
        }
    }
}