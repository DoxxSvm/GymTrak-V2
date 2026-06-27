package `in`.gym.trak.studio.notifications

import `in`.gym.trak.studio.data.model.ActiveWorkoutState
import `in`.gym.trak.studio.data.repository.WorkoutManager
import `in`.gym.trak.studio.getCurrentTimeMillis

/**
 * Syncs [WorkoutManager] state to the platform live workout notification.
 */
object WorkoutLiveNotificationController {

    private val manager: WorkoutLiveNotificationManager = createWorkoutLiveNotificationManager()
    private var isActive = false
    private var activeStartTimeMillis: Long? = null
    private var lastProgressSnapshot: ProgressSnapshot? = null

    fun sync(workout: ActiveWorkoutState?) {
        if (workout == null) {
            if (isActive) {
                manager.stop()
                isActive = false
                activeStartTimeMillis = null
                lastProgressSnapshot = null
            }
            return
        }

        val data = workout.toLiveNotificationData()
        val isNewSession = activeStartTimeMillis != workout.startTimeMillis
        val progressSnapshot = data.toProgressSnapshot()

        if (!isActive || isNewSession) {
            if (isActive) {
                manager.stop()
            }
            manager.start(data)
            isActive = true
            activeStartTimeMillis = workout.startTimeMillis
            lastProgressSnapshot = progressSnapshot
        } else if (progressSnapshot != lastProgressSnapshot) {
            manager.update(data)
            lastProgressSnapshot = progressSnapshot
        }
    }

    fun refreshPresentation(workout: ActiveWorkoutState) {
        if (!isActive) return
        manager.restoreVisibility(workout.toLiveNotificationData())
    }

    private data class ProgressSnapshot(
        val title: String,
        val exerciseCount: Int,
        val completedSets: Int,
        val totalSets: Int,
    )

    private fun WorkoutLiveNotificationData.toProgressSnapshot() = ProgressSnapshot(
        title = title,
        exerciseCount = exerciseCount,
        completedSets = completedSets,
        totalSets = totalSets,
    )

    private fun ActiveWorkoutState.toLiveNotificationData(): WorkoutLiveNotificationData {
        val now = getCurrentTimeMillis()
        val elapsed = WorkoutManager.elapsedMillis(this)
        val totalSets = exercises.sumOf { it.sets.size }
        val completedSets = exercises.sumOf { exercise -> exercise.sets.count { it.isDone } }
        return WorkoutLiveNotificationData(
            title = title.ifBlank { "Workout" },
            startTimeMillis = startTimeMillis,
            durationLabel = formatWorkoutDurationLabel(elapsed),
            exerciseCount = exercises.size,
            completedSets = completedSets,
            totalSets = totalSets,
            isPaused = isPaused,
            chronometerBaseMillis = now - elapsed,
        )
    }
}
