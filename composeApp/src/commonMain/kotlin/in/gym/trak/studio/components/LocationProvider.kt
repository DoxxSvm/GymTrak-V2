package `in`.gym.trak.studio.components

import androidx.compose.runtime.Composable

@Composable
expect fun rememberLocationProvider(): LocationProvider

sealed class LocationResult {
    data class Success(val latitude: Double, val longitude: Double) : LocationResult()
    data object PermissionDenied : LocationResult()
    data object ServiceDisabled : LocationResult()
    data object Unavailable : LocationResult()
}

interface LocationProvider {
    suspend fun getCurrentLocation(): LocationResult
}
