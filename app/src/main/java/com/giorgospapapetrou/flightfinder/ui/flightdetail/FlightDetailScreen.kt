package com.giorgospapapetrou.flightfinder.ui.flightdetail

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.preference.PreferenceManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giorgospapapetrou.flightfinder.domain.model.FlightDetail
import com.giorgospapapetrou.flightfinder.domain.model.FlightPosition
import com.giorgospapapetrou.flightfinder.domain.model.FlightSummary
import com.giorgospapapetrou.flightfinder.ui.theme.BgCard
import com.giorgospapapetrou.flightfinder.ui.theme.OnSurfaceDark
import com.giorgospapapetrou.flightfinder.ui.theme.OnSurfaceVar
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

private val OutlineDark = androidx.compose.ui.graphics.Color(0xFF2A2A3A)

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
                .padding(14.dp),
        )

        if (detail.drawablePositions.size >= 2) {
            ReplayControls(
                detail = detail,
                fraction = replayFraction ?: 0f,
                onFractionChange = onReplayFractionChange,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(14.dp),
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = BgCard.copy(alpha = 0.92f),
        ),
        border = BorderStroke(1.dp, OutlineDark),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = summary.displayName,
                color = OnSurfaceDark,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (summary.aircraftType != null) {
                Text(
                    text = summary.aircraftType,
                    color = OnSurfaceVar,
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                val start = timeFormatter.format(summary.startedAt.atZone(zone))
                val end = if (summary.isLiveAt(now)) "live"
                else summary.effectiveEndAt?.let { timeFormatter.format(it.atZone(zone)) } ?: "\u2014"
                Text(
                    text = "$start \u2192 $end",
                    color = OnSurfaceVar,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                )
                summary.maxAltitudeFt?.let {
                    Text(
                        text = "$it ft",
                        color = OnSurfaceVar,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            if (replayPosition != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    val tLabel = timeFormatter.format(replayPosition.timestamp.atZone(zone))
                    Text(
                        text = "@ $tLabel",
                        color = OnSurfaceDark,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                    replayPosition.altitudeFt?.let {
                        Text(
                            text = "$it ft",
                            color = OnSurfaceDark,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    replayPosition.groundSpeedKt?.let {
                        Text(
                            text = "$it kt",
                            color = OnSurfaceDark,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    replayPosition.headingDeg?.let {
                        Text(
                            text = "$it\u00B0",
                            color = OnSurfaceDark,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                        )
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
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = BgCard.copy(alpha = 0.92f),
        ),
        border = BorderStroke(1.dp, OutlineDark),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Slider(
                value = fraction,
                onValueChange = { onFractionChange(it) },
                valueRange = 0f..1f,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = firstLabel,
                    color = OnSurfaceVar,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
                Text(
                    text = "Replay",
                    color = OnSurfaceVar,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = lastLabel,
                    color = OnSurfaceVar,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                )
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
    private var activePolyline: Polyline? = null
    private var allPoints: List<GeoPoint> = emptyList()
    private var allTimestamps: List<Long> = emptyList()

    fun drawPath(
        detail: FlightDetail,
        startBitmap: Bitmap,
        endBitmap: Bitmap,
    ) {
        val map = mapView ?: return
        val drawable = detail.drawablePositions
        if (drawable.isEmpty()) return

        val points = drawable.map { GeoPoint(it.lat!!, it.lon!!) }
        allPoints = points
        allTimestamps = drawable.map { it.timestamp.toEpochMilli() }

        // Ghost route — full path, dashed white, semi-transparent
        val ghostPolyline = Polyline().apply {
            setPoints(points)
            outlinePaint.color = Color.WHITE
            outlinePaint.alpha = 90 // ~35%
            outlinePaint.strokeWidth = 4f
            outlinePaint.pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
        }
        map.overlays.add(ghostPolyline)

        // Active route — solid blue, will be updated as replay progresses
        val active = Polyline().apply {
            setPoints(points) // initially full
            outlinePaint.color = 0xFF1976D2.toInt()
            outlinePaint.strokeWidth = 6f
        }
        map.overlays.add(active)
        activePolyline = active

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
            // No active scrubbing — show full active route, no marker
            replayMarker?.let {
                map.overlays.remove(it)
                replayMarker = null
            }
            activePolyline?.setPoints(allPoints)
            map.invalidate()
            return
        }

        // Trim active polyline to up-to-replay segment + interpolated point
        val replayMs = replayPosition.timestamp.toEpochMilli()
        val cutoffIndex = allTimestamps.indexOfLast { it <= replayMs }
            .let { if (it < 0) 0 else it }
        val trimmed = allPoints.take(cutoffIndex + 1).toMutableList()
        // Append the interpolated current position so the line ends at the marker
        trimmed.add(GeoPoint(replayPosition.lat, replayPosition.lon))
        activePolyline?.setPoints(trimmed)

        val pos = GeoPoint(replayPosition.lat, replayPosition.lon)
        val rotation = computeReplayHeading(replayPosition, detail)

        val existing = replayMarker
        if (existing == null) {
            val newMarker = Marker(map).apply {
                position = pos
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = BitmapDrawable(map.resources, replayBitmap)
                this.rotation = -rotation
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
    val cx = size / 2f

    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        style = Paint.Style.FILL
    }
    val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = Color.argb(180, 20, 28, 36)
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    val path = Path().apply {
        moveTo(cx, 8f)
        lineTo(cx + 4f, 28f)
        lineTo(size - 6f, 40f)
        lineTo(size - 6f, 46f)
        lineTo(cx + 4f, 42f)
        lineTo(cx + 3f, 56f)
        lineTo(cx + 14f, 62f)
        lineTo(cx + 14f, 65f)
        lineTo(cx, 64f)
        lineTo(cx - 14f, 65f)
        lineTo(cx - 14f, 62f)
        lineTo(cx - 3f, 56f)
        lineTo(cx - 4f, 42f)
        lineTo(6f, 46f)
        lineTo(6f, 40f)
        lineTo(cx - 4f, 28f)
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