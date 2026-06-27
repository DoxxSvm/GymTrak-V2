package `in`.gym.trak.studio.network

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

expect fun isNetworkAvailable(): Boolean

object NetworkMonitor {
    private val _isConnected = MutableStateFlow(true)
    val isConnected = _isConnected.asStateFlow()

    fun updateStatus(connected: Boolean) {
        _isConnected.value = connected
    }

    fun recheck() {
        _isConnected.value = isNetworkAvailable()
    }
}
