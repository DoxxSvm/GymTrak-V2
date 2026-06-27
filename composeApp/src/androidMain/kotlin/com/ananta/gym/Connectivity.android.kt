package `in`.gym.trak.studio.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import `in`.gym.trak.studio.appContext

actual fun isNetworkAvailable(): Boolean {
    val connectivityManager = appContext?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    if (connectivityManager != null) {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
    return false
}
