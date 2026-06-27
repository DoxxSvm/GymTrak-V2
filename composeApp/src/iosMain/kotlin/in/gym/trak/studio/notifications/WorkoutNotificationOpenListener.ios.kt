package `in`.gym.trak.studio.notifications

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue

private const val WORKOUT_OPEN_NOTIFICATION = "in.gym.trak.workout.open"

@OptIn(ExperimentalForeignApi::class)
actual fun startWorkoutNotificationOpenListener() {
    NSNotificationCenter.defaultCenter.addObserverForName(
        name = WORKOUT_OPEN_NOTIFICATION,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
    ) {
        WorkoutNavigationBus.requestOpenActiveWorkout()
    }
}
