package `in`.gym.trak.studio.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Looper
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.ananta.gym.MainActivity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult as GmsLocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@Composable
actual fun rememberLocationProvider(): LocationProvider {
    val context = LocalContext.current
    return remember(context) { 
        AndroidLocationProvider(context)
    }
}

class AndroidLocationProvider(private val context: Context) : LocationProvider {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    private fun requestLocationPermissionIfPossible() {
        val activity = MainActivity.instance ?: return
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            102
        )
    }

    private fun isLocationServiceEnabled(): Boolean {
        val manager = locationManager ?: return false
        val gpsEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val networkEnabled = manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        return gpsEnabled || networkEnabled
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationResult = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermission()) {
            requestLocationPermissionIfPossible()
            continuation.resume(LocationResult.PermissionDenied)
            return@suspendCancellableCoroutine
        }

        if (!isLocationServiceEnabled()) {
            continuation.resume(LocationResult.ServiceDisabled)
            return@suspendCancellableCoroutine
        }

        val cancellationTokenSource = CancellationTokenSource()
        var resumed = false
        fun resumeIfNeeded(value: LocationResult) {
            if (!resumed) {
                resumed = true
                continuation.resume(value)
            }
        }

        val updateCallback = object : LocationCallback() {
            override fun onLocationResult(result: GmsLocationResult) {
                val location = result.lastLocation
                if (location != null) {
                    fusedLocationClient.removeLocationUpdates(this)
                    resumeIfNeeded(
                        LocationResult.Success(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    )
                }
            }
        }

        fun requestSingleUpdateFallback() {
            val request = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                1_000L
            ).setWaitForAccurateLocation(false)
                .setMaxUpdates(1)
                .build()
            fusedLocationClient.requestLocationUpdates(
                request,
                updateCallback,
                Looper.getMainLooper()
            ).addOnFailureListener {
                resumeIfNeeded(LocationResult.Unavailable)
            }
        }

        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    resumeIfNeeded(
                        LocationResult.Success(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    )
                } else {
                    // Fallback when fresh location is unavailable.
                    fusedLocationClient.lastLocation
                        .addOnSuccessListener { last ->
                            if (last != null) {
                                resumeIfNeeded(
                                    LocationResult.Success(
                                        latitude = last.latitude,
                                        longitude = last.longitude
                                    )
                                )
                            } else {
                                requestSingleUpdateFallback()
                            }
                        }
                        .addOnFailureListener {
                            requestSingleUpdateFallback()
                        }
                }
            }.addOnFailureListener {
                fusedLocationClient.lastLocation
                    .addOnSuccessListener { last ->
                        if (last != null) {
                            resumeIfNeeded(
                                LocationResult.Success(
                                    latitude = last.latitude,
                                    longitude = last.longitude
                                )
                            )
                        } else {
                            requestSingleUpdateFallback()
                        }
                    }
                    .addOnFailureListener {
                        requestSingleUpdateFallback()
                    }
            }
        } catch (_: SecurityException) {
            resumeIfNeeded(LocationResult.PermissionDenied)
        }

        continuation.invokeOnCancellation {
            cancellationTokenSource.cancel()
            fusedLocationClient.removeLocationUpdates(updateCallback)
        }
    }
}
