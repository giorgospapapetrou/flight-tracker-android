package com.giorgospapapetrou.flightfinder.ui.flightdetail

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.preference.PreferenceManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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
private fun FlightPathMap(
    detail: FlightDetail,
    replayPosition: FlightPosition?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val startBitmap = remember { createDotBitmap(0xFF2E7D32.toInt()) }
    val endBitmap = remember { createDotBitmap(0xFFC62828.toInt()) }
    val replayBitmap = remember { createPlaneBitmap(0xFFFFB300.toInt()) }
    val mapState = remember { FlightDetailMapState() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            Configuration.getInstance().load(
                ctx.applicationContext,
                PreferenceManager.getDefaultSharedPreferences(ctx.applicationContext)
            )
            Configuration.getInstance().userAgentValue = ctx.packageName

            val mapView = MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                isHorizontalMapRepetitionEnabled = false
                isVerticalMapRepetitionEnabled = false
                minZoomLevel = 4.0
                maxZoomLevel = 19.0
                zoomController.setVisibility(
                    org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER
                )
                val tileSystem = org.osmdroid.views.MapView.getTileSystem()
                setScrollableAreaLimitLatitude(
                    tileSystem.maxLatitude,
                    tileSystem.minLatitude,
                    0
                )
                setScrollableAreaLimitLongitude(
                    tileSystem.minLongitude,
                    tileSystem.maxLongitude,
                    0
                )
            }
            mapState.mapView = mapView
            mapState.drawPath(detail, startBitmap, endBitmap)
            mapState.syncReplay(replayPosition, detail, replayBitmap)
            mapView
        },
        update = {
            mapState.syncReplay(replayPosition, detail, replayBitmap)
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            mapState.mapView?.onDetach()
            mapState.mapView = null
        }
    }
}

private class FlightDetailMapState {
    var mapView: MapView? = null
    private var replayMarker: Marker? = null

    fun drawPath(
        detail: FlightDetail,
        startBitmap: Bitmap,
        endBitmap: Bitmap,
    ) {
        val map = mapView ?: return
        val drawable = detail.drawablePositions
        if (drawable.isEmpty()) return

        // Draw the route as a polyline
        val points = drawable.map { GeoPoint(it.lat!!, it.lon!!) }
        val polyline = Polyline().apply {
            setPoints(points)
            outlinePaint.color = 0xFF1976D2.toInt()
            outlinePaint.strokeWidth = 6f
        }
        map.overlays.add(polyline)

        // Start marker (green)
        val startMarker = Marker(map).apply {
            position = points.first()
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = BitmapDrawable(map.resources, startBitmap)
            setInfoWindow(null)
        }
        map.overlays.add(startMarker)

        // End marker (red)
        val endMarker = Marker(map).apply {
            position = points.last()
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            icon = BitmapDrawable(map.resources, endBitmap)
            setInfoWindow(null)
        }
        map.overlays.add(endMarker)

        // Fit camera to bounds
        val lats = points.map { it.latitude }
        val lons = points.map { it.longitude }
        val box = BoundingBox(
            lats.max(),
            lons.max(),
            lats.min(),
            lons.min(),
        )
        map.post {
            map.zoomToBoundingBox(box, true, 100)
        }

        map.invalidate()
    }

    fun syncReplay(
        replayPosition: FlightPosition?,
        detail: FlightDetail,
        replayBitmap: Bitmap,
    ) {
        val map = mapView ?: return

        if (replayPosition == null || replayPosition.lat == null || replayPosition.lon == null) {
            replayMarker?.let {
                map.overlays.remove(it)
                replayMarker = null
            }
            map.invalidate()
            return
        }

        val pos = GeoPoint(replayPosition.lat, replayPosition.lon)
        val rotation = computeReplayHeading(replayPosition, detail)

        val existing = replayMarker
        if (existing == null) {
            val newMarker = Marker(map).apply {
                position = pos
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = BitmapDrawable(map.resources, replayBitmap)
                this.rotation = -rotation  // osmdroid CCW; aviation CW
                setInfoWindow(null)
            }
            map.overlays.add(newMarker)
            replayMarker = newMarker
        } else {
            existing.position = pos
            existing.rotation = -rotation
        }

        map.invalidate()
    }

    /**
     * Compute heading from path geometry: bearing toward the next position.
     */
    private fun computeReplayHeading(
        replay: FlightPosition,
        detail: FlightDetail,
    ): Float {
        val points = detail.drawablePositions
        if (points.size < 2) return (replay.headingDeg ?: 0).toFloat()

        val replayMs = replay.timestamp.toEpochMilli()
        val next = points.firstOrNull { it.timestamp.toEpochMilli() > replayMs }
            ?: return (replay.headingDeg ?: 0).toFloat()

        val lat = replay.lat ?: return (replay.headingDeg ?: 0).toFloat()
        val lon = replay.lon ?: return (replay.headingDeg ?: 0).toFloat()
        return bearing(lat, lon, next.lat!!, next.lon!!)
    }

    private fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)
        val deg = (Math.toDegrees(theta) + 360.0) % 360.0
        return deg.toFloat()
    }
}

private fun createPlaneBitmap(color: Int): Bitmap {
    val size = 72
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Color.parseColor("#5D4037")
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
    canvas.drawPath(path, fill)
    canvas.drawPath(path, outline)
    return bmp
}

private fun createDotBitmap(color: Int): Bitmap {
    val size = 36
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, fill)
    canvas.drawCircle(size / 2f, size / 2f, size / 2f - 4f, ring)
    return bmp
}