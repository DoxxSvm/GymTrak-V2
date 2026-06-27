package `in`.gym.trak.studio.notifications

/**
 * Platform-agnostic payload for the persistent live workout notification.
 */
data class WorkoutLiveNotificationData(
    val title: String,
    val startTimeMillis: Long,
    val durationLabel: String,
    val exerciseCount: Int,
    val completedSets: Int,
    val totalSets: Int,
    val isPaused: Boolean = false,
    val chronometerBaseMillis: Long = startTimeMillis,
) {
    val setsLabel: String get() = "$completedSets/$totalSets sets"
    val exerciseSummary: String
        get() = when (exerciseCount) {
            0 -> "No exercises"
            1 -> "1 exercise"
            else -> "$exerciseCount exercises"
        }
}

fun formatWorkoutDurationLabel(elapsedMillis: Long): String {
    val totalSeconds = (elapsedMillis / 1000).coerceAtLeast(0).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "${hours}h ${minutes.toString().padStart(2, '0')}m"
    } else {
        "${minutes}:${seconds.toString().padStart(2, '0')}"
    }
}
