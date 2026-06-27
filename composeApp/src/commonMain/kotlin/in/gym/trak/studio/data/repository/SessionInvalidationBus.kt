package `in`.gym.trak.studio.data.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Emitted when [SessionManager] is cleared after HTTP 401, 403, or 5xx so the app can navigate to login. */
object SessionInvalidationBus {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun notifySessionInvalidated() {
        _events.tryEmit(Unit)
    }
}
