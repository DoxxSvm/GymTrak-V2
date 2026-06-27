package `in`.gym.trak.studio.components

import androidx.compose.runtime.Composable

data class SearchResult(
    val name: String,
    val address: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeId: String? = null
)

@Composable
expect fun rememberLocationSearchService(): LocationSearchService

interface LocationSearchService {
    suspend fun search(query: String): List<SearchResult>
    suspend fun resolveCoordinates(result: SearchResult): Pair<Double, Double>?
    suspend fun reverseGeocode(latitude: Double, longitude: Double): String?
}
