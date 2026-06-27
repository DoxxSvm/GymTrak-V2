package `in`.gym.trak.studio.components

import kotlinx.cinterop.useContents
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.MapKit.*
import platform.CoreLocation.*
import platform.Foundation.*
import kotlin.coroutines.resume

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi

@Composable
actual fun rememberLocationSearchService(): LocationSearchService {
    return remember { LocationSearchServiceImpl() }
}

class LocationSearchServiceImpl : LocationSearchService {
    @OptIn(ExperimentalForeignApi::class)
    override suspend fun search(query: String): List<SearchResult> = suspendCancellableCoroutine { continuation ->
        val request = MKLocalSearchRequest().apply {
            naturalLanguageQuery = query
        }
        val search = MKLocalSearch(request)
        search.startWithCompletionHandler { response, error ->
            if (response != null && error == null) {
                val results = response.mapItems.map { item ->
                    val mapItem = item as MKMapItem
                    val coordinate = mapItem.placemark.coordinate
                    coordinate.useContents {
                        SearchResult(
                            name = mapItem.name ?: "Location",
                            address = mapItem.placemark.title ?: "",
                            latitude = latitude,
                            longitude = longitude
                        )
                    }
                }
                continuation.resume(results)
            } else {
                continuation.resume(emptyList())
            }
        }
    }

    override suspend fun resolveCoordinates(result: SearchResult): Pair<Double, Double>? {
        val lat = result.latitude
        val lng = result.longitude
        return if (lat != null && lng != null) Pair(lat, lng) else null
    }

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): String? = suspendCancellableCoroutine { continuation ->
        val geocoder = CLGeocoder()
        val location = CLLocation(latitude = latitude, longitude = longitude)
        geocoder.reverseGeocodeLocation(location) { placemarks, error ->
            if (placemarks != null && error == null) {
                val placemark = placemarks.firstOrNull() as? CLPlacemark
                val address = placemark?.let {
                    listOfNotNull(it.name, it.thoroughfare, it.locality, it.administrativeArea).joinToString(", ")
                }
                continuation.resume(address)
            } else {
                continuation.resume(null)
            }
        }
    }
}
