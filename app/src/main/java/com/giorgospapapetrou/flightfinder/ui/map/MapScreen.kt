package com.giorgospapapetrou.flightfinder.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.drawable.BitmapDrawable
import android.preference.PreferenceManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.giorgospapapetrou.flightfinder.domain.model.Aircraft
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.time.Instant

private const val DEFAULT_LAT = 34.7
private const val DEFAULT_LON = 33.0
private const val DEFAULT_ZOOM = 8.0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = Modifier.fillMaxSize()) {
        OsmdroidSurface(
            aircraft = state.aircraftList,
            now = state.now,
            onAircraftClick = viewModel::selectAircraft,
        )
    }

    val selected = state.selectedAircraft
    if (selected != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectAircraft(null) },
            sheetState = sheetState,
        ) {
            AircraftDetailSheet(selected)
        }
    }
}

@Composable
private fun AircraftDetailSheet(aircraft: Aircraft) {
    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
        Text(
            text = aircraft.callsign?.takeIf { it.isNotBlank() } ?: aircraft.icao,
            style = MaterialTheme.typography.headlineSmall,
        )
        if (aircraft.aircraftType != null) {
            Text(text = aircraft.aircraftType)
        }
        Text(text = "ICAO: ${aircraft.icao}", style = MaterialTheme.typography.bodySmall)
        aircraft.altitudeFt?.let { Text("Altitude: $it ft") }
        aircraft.groundSpeedKt?.let { Text("Speed: $it kt") }
        aircraft.headingDeg?.let { Text("Heading: $it°") }
    }
}

@Composable
private fun OsmdroidSurface(
    aircraft: List<Aircraft>,
    now: Instant,
    onAircraftClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val planeBitmap = remember { createPlaneBitmap() }
    val mapState = remember { LiveMapState() }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            // Required osmdroid initialization
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
                // Lock pan to valid lat/lon range
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
                controller.setZoom(DEFAULT_ZOOM)
                controller.setCenter(GeoPoint(DEFAULT_LAT, DEFAULT_LON))
            }
            mapState.mapView = mapView
            mapState.applyAircraft(aircraft, now, planeBitmap, onAircraftClick)
            mapView
        },
        update = {
            mapState.applyAircraft(aircraft,now,  planeBitmap, onAircraftClick)
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            mapState.mapView?.onDetach()
            mapState.mapView = null
        }
    }
}

private class LiveMapState {
    var mapView: MapView? = null
    private val markers = mutableMapOf<String, Marker>()

    fun applyAircraft(
        aircraft: List<Aircraft>,
        now: Instant,
        planeBitmap: Bitmap,
        onAircraftClick: (String) -> Unit,
    ) {
        val map = mapView ?: return

        val seen = mutableSetOf<String>()
        for (a in aircraft) {
            val lat = a.lat ?: continue
            val lon = a.lon ?: continue
            seen.add(a.icao)

            val rotation = (a.headingDeg ?: 0).toFloat()
            val alpha = computeAlpha(a.lastPositionAt, now)

            val existing = markers[a.icao]
            if (existing == null) {
                val marker = Marker(map).apply {
                    position = GeoPoint(lat, lon)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                    icon = BitmapDrawable(map.resources, planeBitmap)
                    this.rotation = -rotation
                    this.alpha = alpha
                    setOnMarkerClickListener { _, _ ->
                        onAircraftClick(a.icao)
                        true
                    }
                }
                map.overlays.add(marker)
                markers[a.icao] = marker
            } else {
                existing.position = GeoPoint(lat, lon)
                existing.rotation = -rotation
                existing.alpha = alpha
            }
        }

        val gone = markers.keys - seen
        for (icao in gone) {
            markers[icao]?.let { map.overlays.remove(it) }
            markers.remove(icao)
        }

        map.invalidate()
    }

    private fun computeAlpha(lastPositionAt: Instant?, now: Instant): Float {
        if (lastPositionAt == null) return 1f
        val ageSeconds = now.epochSecond - lastPositionAt.epochSecond
        return when {
            ageSeconds < 5 -> 1.0f
            ageSeconds < 30 -> 1.0f - ((ageSeconds - 5) / 25f) * 0.55f  // 1.0 → 0.45
            ageSeconds < 60 -> 0.45f - ((ageSeconds - 30) / 30f) * 0.30f  // 0.45 → 0.15
            else -> 0.15f
        }
    }
}

private fun createPlaneBitmap(): Bitmap {
    val size = 64
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1976D2")
        style = Paint.Style.FILL
    }
    val path = Path().apply {
        moveTo(size / 2f, 8f)
        lineTo(size - 12f, size - 16f)
        lineTo(size / 2f, size - 24f)
        lineTo(12f, size - 16f)
        close()
    }
    canvas.drawPath(path, paint)
    return bmp
}