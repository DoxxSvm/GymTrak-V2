package `in`.gym.trak.studio.notifications

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Routes notification taps to [ActiveWorkoutScreen] while a session is active. */
object WorkoutNavigationBus {
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private var pendingOpenActiveWorkout = false

    fun requestOpenActiveWorkout() {
        pendingOpenActiveWorkout = true
        _events.tryEmit(Unit)
    }

    fun hasPendingOpenActiveWorkout(): Boolean = pendingOpenActiveWorkout

    fun clearPendingOpenActiveWorkout() {
        pendingOpenActiveWorkout = false
    }
}

/** iOS listens for notification tap events from Swift. No-op on Android. */
expect fun startWorkoutNotificationOpenListener()
