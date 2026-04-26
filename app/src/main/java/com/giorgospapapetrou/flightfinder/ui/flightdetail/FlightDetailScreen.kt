package com.giorgospapapetrou.flightfinder.ui.flightdetail

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giorgospapapetrou.flightfinder.domain.model.FlightDetail
import com.giorgospapapetrou.flightfinder.domain.model.FlightPosition
import com.giorgospapapetrou.flightfinder.domain.model.FlightSummary
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
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
                FlightDetailContent(
                    detail = state.detail!!,
                    replayFraction = state.replayFraction,
                    replayPosition = state.replayPosition,
                    onReplayFractionChange = viewModel::setReplayFraction,
                )
            }
        }
    }
}

@Composable
private fun FlightDetailContent(
    detail: FlightDetail,
    replayFraction: Float?,
    replayPosition: FlightPosition?,
    onReplayFractionChange: (Float?) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        FlightPathMap(
            detail = detail,
            replayPosition = replayPosition,
            modifier = Modifier.fillMaxSize(),
        )

        FlightSummaryCard(
            summary = detail.summary,
            replayPosition = replayPosition,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
        )

        if (detail.drawablePositions.size >= 2) {
            ReplayControls(
                detail = detail,
                fraction = replayFraction ?: 0f,
                onFractionChange = onReplayFractionChange,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }
    }
}

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")

@Composable
private fun FlightSummaryCard(
    summary: FlightSummary,
    replayPosition: FlightPosition?,
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

            // When scrubbing, show the live state below
            if (replayPosition != null) {
                Spacer(modifier = Modifier.padding(top = 8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val tLabel = timeFormatter.format(replayPosition.timestamp.atZone(zone))
                    Text(text = "@ $tLabel", style = MaterialTheme.typography.bodySmall)
                    replayPosition.altitudeFt?.let {
                        Text(text = "${it} ft", style = MaterialTheme.typography.bodySmall)
                    }
                    replayPosition.groundSpeedKt?.let {
                        Text(text = "${it} kt", style = MaterialTheme.typography.bodySmall)
                    }
                    replayPosition.headingDeg?.let {
                        Text(text = "${it}°", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReplayControls(
    detail: FlightDetail,
    fraction: Float,
    onFractionChange: (Float?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val drawable = detail.drawablePositions
    val firstT = drawable.first().timestamp
    val lastT = drawable.last().timestamp
    val firstLabel = timeFormatter.format(firstT.atZone(zone))
    val lastLabel = timeFormatter.format(lastT.atZone(zone))

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.95f),
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Slider(
                value = fraction,
                onValueChange = { onFractionChange(it) },
                valueRange = 0f..1f,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = firstLabel, style = MaterialTheme.typography.bodySmall)
                Text(text = "Replay", style = MaterialTheme.typography.bodySmall)
                Text(text = lastLabel, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
@Suppress("DEPRECATION")
private fun FlightPathMap(
    detail: FlightDetail,
    replayPosition: FlightPosition?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val mapHolder = remember { mutableMapOf<String, MapLibreMap>() }
    val replayMarkerHolder = remember { mutableMapOf<String, Marker>() }
    val pathDrawn = remember { mutableMapOf<String, Boolean>() }
    val planeBitmap = remember { createPlaneBitmap() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val mapView = MapView(ctx)
            mapView.onCreate(null)
            mapView.getMapAsync { map ->
                mapHolder["map"] = map
                map.setStyle(Style.Builder().fromJson(OSM_STYLE_JSON)) {
                    drawPath(map, detail)
                    pathDrawn["drawn"] = true
                    syncReplayMarker(ctx, map, replayMarkerHolder, replayPosition, planeBitmap)
                }
            }
            mapView.onStart()
            mapView.onResume()
            mapView
        },
        update = {
            val map = mapHolder["map"] ?: return@AndroidView
            if (pathDrawn["drawn"] != true) return@AndroidView
            syncReplayMarker(context, map, replayMarkerHolder, replayPosition, planeBitmap)
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            mapHolder.clear()
            replayMarkerHolder.clear()
            pathDrawn.clear()
        }
    }
}

@Suppress("DEPRECATION")
private fun drawPath(
    map: MapLibreMap,
    detail: FlightDetail,
) {
    map.clear()
    val points = detail.drawablePositions.map { LatLng(it.lat!!, it.lon!!) }
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

@Suppress("DEPRECATION")
private fun syncReplayMarker(
    context: android.content.Context,
    map: MapLibreMap,
    holder: MutableMap<String, Marker>,
    replayPosition: FlightPosition?,
    planeBitmap: Bitmap,
) {
    val existing = holder["replay"]
    if (replayPosition == null || replayPosition.lat == null || replayPosition.lon == null) {
        if (existing != null) {
            map.removeMarker(existing)
            holder.remove("replay")
        }
        return
    }
    val pos = LatLng(replayPosition.lat, replayPosition.lon)
    val rotated = rotateBitmap(planeBitmap, (replayPosition.headingDeg ?: 0).toFloat())
    val icon = IconFactory.getInstance(context).fromBitmap(rotated)

    if (existing == null) {
        val m = map.addMarker(MarkerOptions().position(pos).icon(icon))
        holder["replay"] = m
    } else {
        existing.position = pos
        existing.icon = icon
    }
}

private fun createPlaneBitmap(): Bitmap {
    val size = 72
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFB300") // amber gold
        style = Paint.Style.FILL
    }
    val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#5D4037")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    val path = Path().apply {
        moveTo(size / 2f, 8f)
        lineTo(size - 12f, size - 16f)
        lineTo(size / 2f, size - 24f)
        lineTo(12f, size - 16f)
        close()
    }
    canvas.drawPath(path, paint)
    canvas.drawPath(path, outline)
    return bmp
}

private fun rotateBitmap(src: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
}