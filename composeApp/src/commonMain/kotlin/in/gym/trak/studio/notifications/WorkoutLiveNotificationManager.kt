package `in`.gym.trak.studio.notifications

/**
 * Platform bridge for persistent live workout notifications.
 * Android → ongoing notification with system chronometer (no foreground service).
 * iOS     → Live Activity lock-screen timer + single updatable local notification.
 */
interface WorkoutLiveNotificationManager {
    fun start(data: WorkoutLiveNotificationData)
    fun update(data: WorkoutLiveNotificationData)
    /** Re-shows the notification after a tap or app resume without changing workout state. */
    fun restoreVisibility(data: WorkoutLiveNotificationData)
    fun stop()
}

expect fun createWorkoutLiveNotificationManager(): WorkoutLiveNotificationManager
