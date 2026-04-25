package com.giorgospapapetrou.flightfinder.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
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
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.IconFactory
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private const val DEFAULT_LAT = 34.7
private const val DEFAULT_LON = 33.0
private const val DEFAULT_ZOOM = 8.0
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(modifier = Modifier.fillMaxSize()) {
        MapLibreSurface(
            aircraft = state.aircraft,
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
        Text(text = aircraft.aircraftType ?: "Unknown type")
        Text(text = "ICAO: ${aircraft.icao}", style = MaterialTheme.typography.bodySmall)
        aircraft.altitudeFt?.let { Text("Altitude: $it ft") }
        aircraft.groundSpeedKt?.let { Text("Speed: $it kt") }
        aircraft.headingDeg?.let { Text("Heading: $it°") }
    }
}

@Composable
private fun MapLibreSurface(
    aircraft: List<Aircraft>,
    onAircraftClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val planeBitmap = remember { createPlaneBitmap() }
    val markers = remember { mutableMapOf<String, Marker>() }
    val mapHolder = remember { mutableMapOf<String, MapLibreMap>() }


    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val mapView = MapView(ctx)
            mapView.onCreate(null)
            mapView.getMapAsync { map ->
                mapHolder["map"] = map
                map.setStyle(Style.Builder().fromJson(OSM_STYLE_JSON)) {
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(DEFAULT_LAT, DEFAULT_LON))
                        .zoom(DEFAULT_ZOOM)
                        .build()
                    syncMarkers(ctx, map, markers, aircraft, planeBitmap)
                }
                map.setOnMarkerClickListener { marker ->
                    val icao = markers.entries.firstOrNull { it.value == marker }?.key
                    if (icao != null) {
                        onAircraftClick(icao)
                        true
                    } else false
                }
            }
            mapView.onStart()
            mapView.onResume()
            mapView
        },
        update = {
            val map = mapHolder["map"] ?: return@AndroidView
            syncMarkers(context, map, markers, aircraft, planeBitmap)
        },
    )

    DisposableEffect(Unit) {
        onDispose { mapHolder.clear() }
    }
}

private fun syncMarkers(
    context: Context,
    map: MapLibreMap,
    markers: MutableMap<String, Marker>,
    aircraft: List<Aircraft>,
    planeBitmap: Bitmap,
) {
    val iconFactory = IconFactory.getInstance(context)
    val seen = mutableSetOf<String>()
    for (a in aircraft) {
        val lat = a.lat ?: continue
        val lon = a.lon ?: continue
        seen.add(a.icao)
        val icon = iconFactory.fromBitmap(rotateBitmap(planeBitmap, (a.headingDeg ?: 0).toFloat()))
        val existing = markers[a.icao]
        if (existing == null) {
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(lat, lon))
                    .icon(icon)
                    .title(a.callsign ?: a.icao)
            )
            markers[a.icao] = marker
        } else {
            existing.position = LatLng(lat, lon)
            existing.icon = icon
        }
    }
    val gone = markers.keys - seen
    for (icao in gone) {
        markers[icao]?.let { map.removeMarker(it) }
        markers.remove(icao)
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

private fun rotateBitmap(src: Bitmap, degrees: Float): Bitmap {
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
}