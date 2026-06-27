package `in`.gym.trak.studio.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationCoordinate2DMake
import platform.MapKit.*
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun GoogleMap(
    modifier: Modifier,
    targetLocation: Pair<Double, Double>?,
    zoom: Float,
    tilt: Float,
    bearing: Float,
    onLocationUpdate: (lat: Double, lng: Double) -> Unit
) {
    val rajkot = CLLocationCoordinate2DMake(22.3039, 70.8022)
    val mapView = remember { MKMapView() }

    val delegate = remember {
        object : NSObject(), MKMapViewDelegateProtocol {
            override fun mapView(mapView: MKMapView, regionDidChangeAnimated: Boolean) {
                val center = mapView.centerCoordinate.useContents { 
                    onLocationUpdate(latitude, longitude)
                }
            }
        }
    }

    LaunchedEffect(targetLocation, zoom, tilt, bearing) {
        targetLocation?.let { (lat, lng) ->
            val center = CLLocationCoordinate2DMake(lat, lng)
            val camera = MKMapCamera.cameraLookingAtCenterCoordinate(
                center,
                fromDistance = 500.0 * (20.0 - zoom.toDouble()).coerceAtLeast(1.0),
                pitch = tilt.toDouble(),
                heading = bearing.toDouble()
            )
            mapView.setCamera(camera, animated = true)
        }
    }

    UIKitView(factory = {
                val region = MKCoordinateRegionMakeWithDistance(rajkot, 2000.0, 2000.0)
                mapView.setRegion(region, animated = false)
                mapView.setDelegate(delegate)
                mapView.showsUserLocation = true
                mapView
            }, modifier = modifier.fillMaxSize(), update = { }, properties = UIKitInteropProperties(isInteractive = true, isNativeAccessibilityEnabled = true))
}
