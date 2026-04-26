package com.giorgospapapetrou.flightfinder.ui.flightdetail

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
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
import com.google.gson.JsonObject
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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

// Style image IDs
private const val START_ICON_ID = "fd-start-icon"
private const val END_ICON_ID = "fd-end-icon"
private const val REPLAY_ICON_ID = "fd-replay-plane-icon"

// Sources & layers
private const val PATH_SOURCE_ID = "fd-path-source"
private const val PATH_LAYER_ID = "fd-path-layer"
private const val ENDPOINTS_SOURCE_ID = "fd-endpoints-source"
private const val ENDPOINTS_LAYER_ID = "fd-endpoints-layer"
private const val REPLAY_SOURCE_ID = "fd-replay-source"
private const val REPLAY_LAYER_ID = "fd-replay-layer"

// Feature properties
private const val PROP_ROLE = "role"  // "start" or "end"
private const val PROP_HEADING = "heading"

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
    val mapState = remember { FlightDetailMapState() }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val mapView = MapView(ctx)
            mapView.onCreate(null)
            mapView.getMapAsync { map ->
                mapState.map = map
                map.setStyle(Style.Builder().fromJson(OSM_STYLE_JSON)) { style ->
                    // Register icons
                    style.addImage(START_ICON_ID, createDotBitmap(0xFF2E7D32.toInt()))
                    style.addImage(END_ICON_ID, createDotBitmap(0xFFC62828.toInt()))
                    style.addImage(REPLAY_ICON_ID, createPlaneBitmap(0xFFFFB300.toInt()))

                    // Path source + line layer
                    val pathSource = GeoJsonSource(
                        PATH_SOURCE_ID,
                        FeatureCollection.fromFeatures(emptyList())
                    )
                    style.addSource(pathSource)
                    mapState.pathSource = pathSource
                    val pathLayer = LineLayer(PATH_LAYER_ID, PATH_SOURCE_ID).withProperties(
                        PropertyFactory.lineColor("#1976D2"),
                        PropertyFactory.lineWidth(3f),
                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    )
                    style.addLayer(pathLayer)

                    // Endpoints source + symbol layer (start/end pins)
                    val endpointsSource = GeoJsonSource(
                        ENDPOINTS_SOURCE_ID,
                        FeatureCollection.fromFeatures(emptyList())
                    )
                    style.addSource(endpointsSource)
                    mapState.endpointsSource = endpointsSource
                    val endpointsLayer = SymbolLayer(ENDPOINTS_LAYER_ID, ENDPOINTS_SOURCE_ID)
                        .withProperties(
                            PropertyFactory.iconImage(
                                Expression.match(
                                    Expression.get(PROP_ROLE),
                                    Expression.literal(END_ICON_ID),
                                    Expression.stop(Expression.literal("start"), Expression.literal(START_ICON_ID)),
                                    Expression.stop(Expression.literal("end"), Expression.literal(END_ICON_ID)),
                                )
                            ),
                            PropertyFactory.iconAllowOverlap(true),
                            PropertyFactory.iconIgnorePlacement(true),
                            PropertyFactory.iconKeepUpright(false),
                            PropertyFactory.iconOptional(false),
                            PropertyFactory.iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_MAP),
                            PropertyFactory.iconSize(1.0f),
                            PropertyFactory.symbolPlacement(Property.SYMBOL_PLACEMENT_POINT),
                        )
                    style.addLayer(endpointsLayer)

                    // Replay source + symbol layer (gold plane)
                    val replaySource = GeoJsonSource(
                        REPLAY_SOURCE_ID,
                        FeatureCollection.fromFeatures(emptyList())
                    )
                    style.addSource(replaySource)
                    mapState.replaySource = replaySource
                    val replayLayer = SymbolLayer(REPLAY_LAYER_ID, REPLAY_SOURCE_ID).withProperties(
                        PropertyFactory.iconImage(REPLAY_ICON_ID),
                        PropertyFactory.iconRotate(Expression.get(PROP_HEADING)),
                        PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                        PropertyFactory.iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_MAP),
                        PropertyFactory.iconAllowOverlap(true),
                        PropertyFactory.iconIgnorePlacement(true),
                        PropertyFactory.iconKeepUpright(false),
                        PropertyFactory.iconOptional(false),
                        PropertyFactory.iconSize(1.0f),
                        PropertyFactory.symbolPlacement(Property.SYMBOL_PLACEMENT_POINT),
                    )
                    style.addLayer(replayLayer)

                    // Initial draw
                    mapState.drawPath(detail)
                    mapState.syncReplay(replayPosition, detail)
                }
            }
            mapView.onStart()
            mapView.onResume()
            mapView
        },
        update = {
            mapState.syncReplay(replayPosition, detail)
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            mapState.pathSource = null
            mapState.endpointsSource = null
            mapState.replaySource = null
            mapState.map = null
        }
    }
}

private class FlightDetailMapState {
    var map: MapLibreMap? = null
    var pathSource: GeoJsonSource? = null
    var endpointsSource: GeoJsonSource? = null
    var replaySource: GeoJsonSource? = null

    fun drawPath(detail: FlightDetail) {
        val path = pathSource ?: return
        val endpoints = endpointsSource ?: return
        val map = map ?: return
        val drawable = detail.drawablePositions
        if (drawable.isEmpty()) return

        // Build path linestring
        val pathPoints = drawable.map { Point.fromLngLat(it.lon!!, it.lat!!) }
        val lineFeature = Feature.fromGeometry(LineString.fromLngLats(pathPoints))
        path.setGeoJson(FeatureCollection.fromFeatures(listOf(lineFeature)))

        // Build endpoints features
        val startProps = JsonObject().apply { addProperty(PROP_ROLE, "start") }
        val endProps = JsonObject().apply { addProperty(PROP_ROLE, "end") }
        val startFeature = Feature.fromGeometry(pathPoints.first(), startProps)
        val endFeature = Feature.fromGeometry(pathPoints.last(), endProps)
        endpoints.setGeoJson(FeatureCollection.fromFeatures(listOf(startFeature, endFeature)))

        // Fit camera to bounds
        val boundsBuilder = LatLngBounds.Builder()
        drawable.forEach { boundsBuilder.include(LatLng(it.lat!!, it.lon!!)) }
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 100), 600)
    }

    fun syncReplay(replayPosition: FlightPosition?, detail: FlightDetail) {
        val src = replaySource ?: return
        if (replayPosition == null || replayPosition.lat == null || replayPosition.lon == null) {
            src.setGeoJson(FeatureCollection.fromFeatures(emptyList()))
            return
        }
        val rotation = computeReplayHeading(replayPosition, detail)
        val props = JsonObject().apply {
            addProperty(PROP_HEADING, rotation)
        }
        val feature = Feature.fromGeometry(
            Point.fromLngLat(replayPosition.lon, replayPosition.lat),
            props
        )
        src.setGeoJson(FeatureCollection.fromFeatures(listOf(feature)))
    }

    /**
     * Compute heading from path geometry: bearing toward the next position.
     * Falls back to the position's headingDeg if available, else 0.
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