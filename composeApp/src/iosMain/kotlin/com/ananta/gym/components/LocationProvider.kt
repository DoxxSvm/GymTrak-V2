package `in`.gym.trak.studio.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreLocation.*
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
actual fun rememberLocationProvider(): LocationProvider {
    return remember { IosLocationProvider() }
}

class IosLocationProvider : LocationProvider {
    private val locationManager = CLLocationManager()
    private val delegate = LocationDelegate()

    init {
        locationManager.delegate = delegate
        locationManager.requestWhenInUseAuthorization()
    }

    override suspend fun getCurrentLocation(): LocationResult = suspendCancellableCoroutine { continuation ->
        val status = CLLocationManager.authorizationStatus()
        if (status == kCLAuthorizationStatusDenied || status == kCLAuthorizationStatusRestricted) {
            continuation.resume(LocationResult.PermissionDenied)
            return@suspendCancellableCoroutine
        }
        delegate.callback = { location ->
            if (location != null) {
                continuation.resume(
                    LocationResult.Success(
                        latitude = location.first,
                        longitude = location.second
                    )
                )
            } else {
                continuation.resume(LocationResult.Unavailable)
            }
            delegate.callback = null
        }
        locationManager.startUpdatingLocation()
    }

    private class LocationDelegate : NSObject(), CLLocationManagerDelegateProtocol {
        var callback: ((Pair<Double, Double>?) -> Unit)? = null
        private val locationManager = CLLocationManager() // for stopping

        @OptIn(ExperimentalForeignApi::class)
        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
            val location = didUpdateLocations.lastOrNull() as? CLLocation
            if (location != null) {
                manager.stopUpdatingLocation()
                location.coordinate.useContents {
                    callback?.invoke(Pair(latitude, longitude))
                }
            }
        }

        override fun locationManager(manager: CLLocationManager, didFailWithError: platform.Foundation.NSError) {
            manager.stopUpdatingLocation()
            callback?.invoke(null)
        }
    }
}
