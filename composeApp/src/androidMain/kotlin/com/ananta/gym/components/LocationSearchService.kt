package `in`.gym.trak.studio.components

import android.content.pm.PackageManager
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.gms.maps.model.LatLng
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

@Composable
actual fun rememberLocationSearchService(): LocationSearchService {
    val context = LocalContext.current
    return remember(context) {
        LocationSearchServiceImpl(context)
    }
}

class LocationSearchServiceImpl(private val context: android.content.Context) : LocationSearchService {
    private val placesClient: PlacesClient by lazy {
        ensurePlacesInitialized()
        Places.createClient(context)
    }
    private var activeSessionToken: AutocompleteSessionToken? = null
    private var activeSessionQuery: String = ""

    private val rajkotSouthWest = LatLng(22.20, 70.70)
    private val rajkotNorthEast = LatLng(22.40, 70.90)

    override suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) {
            activeSessionToken = null
            activeSessionQuery = ""
            return@withContext emptyList()
        }

        if (activeSessionToken == null || !query.startsWith(activeSessionQuery.take(1), ignoreCase = true)) {
            activeSessionToken = AutocompleteSessionToken.newInstance()
        }
        activeSessionQuery = query

        try {
            placesClient
                .findAutocompletePredictions(
                    FindAutocompletePredictionsRequest.builder()
                        .setQuery(query)
                        .setSessionToken(activeSessionToken)
                        .setLocationBias(RectangularBounds.newInstance(rajkotSouthWest, rajkotNorthEast))
                        .build()
                )
                .await()
                .autocompletePredictions
                .take(8)
                .map { prediction ->
                    SearchResult(
                        name = prediction.getPrimaryText(null).toString(),
                        address = prediction.getFullText(null).toString(),
                        placeId = prediction.placeId
                    )
                }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    override suspend fun resolveCoordinates(result: SearchResult): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        val cachedLat = result.latitude
        val cachedLng = result.longitude
        if (cachedLat != null && cachedLng != null) {
            return@withContext Pair(cachedLat, cachedLng)
        }

        val placeId = result.placeId ?: return@withContext null
        try {
            val place = placesClient
                .fetchPlace(
                    FetchPlaceRequest.builder(
                        placeId,
                        listOf(Place.Field.LOCATION)
                    ).build()
                )
                .await()
                .place
            val latLng = place.location ?: return@withContext null
            Pair(latLng.latitude, latLng.longitude)
        } catch (_: Throwable) {
            null
        }
    }

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): String? = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context)
        try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.getAddressLine(0)
        } catch (e: IOException) {
            null
        }
    }

    private fun ensurePlacesInitialized() {
        if (Places.isInitialized()) return
        val apiKey = getMapsApiKeyFromManifest() ?: return
        // Default initialization uses the same API key as Maps.
        Places.initialize(context, apiKey)
    }

    private fun getMapsApiKeyFromManifest(): String? {
        return try {
            val ai = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            ai.metaData?.getString("com.google.android.geo.API_KEY")
        } catch (_: Throwable) {
            null
        }
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result -> cont.resume(result) }
    addOnFailureListener { e -> cont.resumeWithException(e) }
    addOnCanceledListener { cont.cancel() }
}
