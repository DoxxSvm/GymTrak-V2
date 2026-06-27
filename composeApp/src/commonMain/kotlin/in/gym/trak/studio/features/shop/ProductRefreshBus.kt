package `in`.gym.trak.studio.features.shop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Increments when products are created, updated, or deleted so list/detail screens can reload.
 */
object ProductRefreshBus {
    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    fun bump() {
        _version.value = _version.value + 1
    }
}
