package `in`.gym.trak.studio.notifications

private class AndroidWorkoutLiveNotificationManager : WorkoutLiveNotificationManager {

    override fun start(data: WorkoutLiveNotificationData) {
        updateWorkoutLiveNotification(data)
    }

    override fun update(data: WorkoutLiveNotificationData) {
        updateWorkoutLiveNotification(data)
    }

    override fun restoreVisibility(data: WorkoutLiveNotificationData) {
        updateWorkoutLiveNotification(data)
    }

    override fun stop() {
        stopWorkoutLiveNotification()
    }
}

actual fun createWorkoutLiveNotificationManager(): WorkoutLiveNotificationManager =
    AndroidWorkoutLiveNotificationManager()
