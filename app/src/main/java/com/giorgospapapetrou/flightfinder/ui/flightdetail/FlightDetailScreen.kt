package com.giorgospapapetrou.flightfinder.ui.flightdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giorgospapapetrou.flightfinder.domain.model.FlightDetail
import com.giorgospapapetrou.flightfinder.domain.model.FlightPosition
import com.giorgospapapetrou.flightfinder.domain.model.FlightSummary
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val OSM_STYLE_JSON = """
{
  "version": 8,
  "sources": {
    "osm": {
      "type": "raster",
      "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
      "tileSize": 256,
      "attribution": "© OpenStreetMap contributors"
    }
  },
  "layers": [
    { "id": "osm", "type": "raster", "source": "osm" }
  ]
}
"""

@Composable
fun FlightDetailScreen(
    flightId: Int,
    viewModel: FlightDetailViewModel = hiltViewModel(),
) {
    LaunchedEffect(flightId) { viewModel.load(flightId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            state.errorMessage != null -> {
                Text(
                    text = state.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
            }
            state.detail != null -> {
                FlightDetailContent(detail = state.detail!!)
            }
        }
    }
}

@Composable
private fun FlightDetailContent(detail: FlightDetail) {
    Box(modifier = Modifier.fillMaxSize()) {
        FlightPathMap(positions = detail.positions, modifier = Modifier.fillMaxSize())
        FlightSummaryCard(
            summary = detail.summary,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
        )
    }
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

@Composable
private fun FlightSummaryCard(
    summary: FlightSummary,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val now = Instant.now()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = summary.displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            if (summary.aircraftType != null) {
                Text(
                    text = summary.aircraftType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                val start = timeFormatter.format(summary.startedAt.atZone(zone))
                val end = if (summary.isLiveAt(now)) "live"
                else summary.effectiveEndAt?.let { timeFormatter.format(it.atZone(zone)) } ?: "—"
                Text(
                    text = "$start → $end",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                summary.maxAltitudeFt?.let {
                    Text(
                        text = "${it} ft",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
@Suppress("DEPRECATION")
private fun FlightPathMap(
    positions: List<FlightPosition>,
    modifier: Modifier = Modifier,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val mapHolder = remember { mutableMapOf<String, MapLibreMap>() }
    val drawnHolder = remember { mutableMapOf<String, Boolean>() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val mapView = MapView(ctx)
            mapView.onCreate(null)
            mapView.getMapAsync { map ->
                mapHolder["map"] = map
                map.setStyle(Style.Builder().fromJson(OSM_STYLE_JSON)) {
                    drawPath(map, positions)
                    drawnHolder["drawn"] = true
                }
            }
            mapView.onStart()
            mapView.onResume()
            mapView
        },
        update = {
            val map = mapHolder["map"] ?: return@AndroidView
            if (drawnHolder["drawn"] != true) return@AndroidView
            drawPath(map, positions)
        },
    )

    DisposableEffect(Unit) {
        onDispose { mapHolder.clear(); drawnHolder.clear() }
    }
}

@Suppress("DEPRECATION")
private fun drawPath(
    map: MapLibreMap,
    positions: List<FlightPosition>,
) {
    map.clear()
    val points = positions.filter { it.hasPosition }
        .map { LatLng(it.lat!!, it.lon!!) }
    if (points.isEmpty()) return

    val polyline = PolylineOptions()
        .addAll(points)
        .color(0xFF1976D2.toInt())
        .width(3f)
    map.addPolyline(polyline)

    map.addMarker(MarkerOptions().position(points.first()).title("Start"))
    map.addMarker(MarkerOptions().position(points.last()).title("End"))

    val boundsBuilder = LatLngBounds.Builder()
    points.forEach { boundsBuilder.include(it) }
    val bounds = boundsBuilder.build()
    map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100), 600)
}