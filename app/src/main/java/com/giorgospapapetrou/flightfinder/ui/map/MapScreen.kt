package com.giorgospapapetrou.flightfinder.ui.map

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
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
import com.google.gson.JsonObject
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val DEFAULT_LAT = 34.7
private const val DEFAULT_LON = 33.0
private const val DEFAULT_ZOOM = 8.0
private const val PLANE_ICON_ID = "plane-icon"
private const val AIRCRAFT_SOURCE_ID = "aircraft-source"
private const val AIRCRAFT_LAYER_ID = "aircraft-layer"
private const val PROP_ICAO = "icao"
private const val PROP_HEADING = "heading"

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
            aircraft = state.aircraftList,
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
private fun MapLibreSurface(
    aircraft: List<Aircraft>,
    onAircraftClick: (String) -> Unit,
) {
    val context = LocalContext.current
    val mapState = remember { LiveMapState() }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val mapView = MapView(ctx)
            mapView.onCreate(null)
            mapView.getMapAsync { map ->
                mapState.map = map
                mapState.mapView = mapView
                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(DEFAULT_LAT, DEFAULT_LON))
                    .zoom(DEFAULT_ZOOM)
                    .build()

                map.setStyle(Style.Builder().fromJson(OSM_STYLE_JSON)) { style ->
                    // 1. Register the plane icon as a style image
                    style.addImage(PLANE_ICON_ID, createPlaneBitmap())

                    // 2. Add the GeoJSON source (initially empty)
                    val source = GeoJsonSource(AIRCRAFT_SOURCE_ID, FeatureCollection.fromFeatures(emptyList()))
                    style.addSource(source)
                    mapState.source = source

                    // 3. Add the symbol layer driven by the source
                    val layer = SymbolLayer(AIRCRAFT_LAYER_ID, AIRCRAFT_SOURCE_ID).withProperties(
                        PropertyFactory.iconImage(PLANE_ICON_ID),
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
                    style.addLayer(layer)

                    // 4. Push initial data
                    mapState.applyAircraft(aircraft)

                    // 5. Tap-to-select via queryRenderedFeatures
                    map.addOnMapClickListener { latLng ->
                        val pixel = map.projection.toScreenLocation(latLng)
                        val features = map.queryRenderedFeatures(
                            PointF(pixel.x, pixel.y),
                            AIRCRAFT_LAYER_ID
                        )
                        if (features.isNotEmpty()) {
                            val icao = features.first().getStringProperty(PROP_ICAO)
                            if (icao != null) {
                                onAircraftClick(icao)
                                return@addOnMapClickListener true
                            }
                        }
                        false
                    }
                }
            }
            mapView.onStart()
            mapView.onResume()
            mapView
        },
        update = {
            mapState.applyAircraft(aircraft)
        },
    )

    DisposableEffect(Unit) {
        onDispose {
            mapState.source = null
            mapState.map = null
            mapState.mapView = null
        }
    }
}

private class LiveMapState {
    var map: MapLibreMap? = null
    var mapView: MapView? = null
    var source: GeoJsonSource? = null

    fun applyAircraft(aircraft: List<Aircraft>) {
        val src = source ?: return

        val features = aircraft.mapNotNull { a ->
            val lat = a.lat ?: return@mapNotNull null
            val lon = a.lon ?: return@mapNotNull null
            val props = JsonObject().apply {
                addProperty(PROP_ICAO, a.icao)
                addProperty(PROP_HEADING, (a.headingDeg ?: 0).toFloat())
            }
            Feature.fromGeometry(Point.fromLngLat(lon, lat), props)
        }
        src.setGeoJson(FeatureCollection.fromFeatures(features))
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