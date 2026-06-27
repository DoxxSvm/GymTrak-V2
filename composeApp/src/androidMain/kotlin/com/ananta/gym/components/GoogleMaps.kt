package `in`.gym.trak.studio.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
actual fun GoogleMap(
    modifier: Modifier,
    targetLocation: Pair<Double, Double>?,
    zoom: Float,
    tilt: Float,
    bearing: Float,
    onLocationUpdate: (lat: Double, lng: Double) -> Unit
) {
    val context = LocalContext.current
    val rajkot = LatLng(22.3039, 70.8022)
    val hasLocationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(rajkot, zoom)
    }

    // Move camera to targetLocation when provided
    LaunchedEffect(targetLocation, zoom, tilt, bearing) {
        targetLocation?.let { (lat, lng) ->
            val update = com.google.android.gms.maps.CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder()
                    .target(LatLng(lat, lng))
                    .zoom(zoom)
                    .tilt(tilt)
                    .bearing(bearing)
                    .build()
            )
            cameraPositionState.animate(update)
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving) {
            val target = cameraPositionState.position.target
            onLocationUpdate(target.latitude, target.longitude)
        }
    }

    com.google.maps.android.compose.GoogleMap(
        modifier = modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = hasLocationPermission
        ),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            myLocationButtonEnabled = true
        )
    )
}
