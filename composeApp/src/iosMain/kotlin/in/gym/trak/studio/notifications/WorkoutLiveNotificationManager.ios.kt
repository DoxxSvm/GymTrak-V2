package `in`.gym.trak.studio.notifications

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSNumber

internal const val WORKOUT_LIVE_START = "in.gym.trak.workout.live.start"
internal const val WORKOUT_LIVE_UPDATE = "in.gym.trak.workout.live.update"
internal const val WORKOUT_LIVE_RESTORE = "in.gym.trak.workout.live.restore"
internal const val WORKOUT_LIVE_STOP = "in.gym.trak.workout.live.stop"

private const val KEY_TITLE = "title"
private const val KEY_START_TIME_MILLIS = "startTimeMillis"
private const val KEY_DURATION_LABEL = "durationLabel"
private const val KEY_EXERCISE_COUNT = "exerciseCount"
private const val KEY_COMPLETED_SETS = "completedSets"
private const val KEY_TOTAL_SETS = "totalSets"

@OptIn(ExperimentalForeignApi::class)
private class IOSWorkoutLiveNotificationManager : WorkoutLiveNotificationManager {

    override fun start(data: WorkoutLiveNotificationData) {
        postWorkoutLiveEvent(WORKOUT_LIVE_START, data)
    }

    override fun update(data: WorkoutLiveNotificationData) {
        postWorkoutLiveEvent(WORKOUT_LIVE_UPDATE, data)
    }

    override fun restoreVisibility(data: WorkoutLiveNotificationData) {
        postWorkoutLiveEvent(WORKOUT_LIVE_RESTORE, data)
    }

    override fun stop() {
        NSNotificationCenter.defaultCenter.postNotificationName(
            aName = WORKOUT_LIVE_STOP,
            `object` = null,
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
private fun postWorkoutLiveEvent(name: String, data: WorkoutLiveNotificationData) {
    NSNotificationCenter.defaultCenter.postNotificationName(
        aName = name,
        `object` = null,
        userInfo = mapOf(
            KEY_TITLE to data.title,
            KEY_START_TIME_MILLIS to NSNumber(long = data.startTimeMillis),
            KEY_DURATION_LABEL to data.durationLabel,
            KEY_EXERCISE_COUNT to NSNumber(int = data.exerciseCount),
            KEY_COMPLETED_SETS to NSNumber(int = data.completedSets),
            KEY_TOTAL_SETS to NSNumber(int = data.totalSets),
        ),
    )
}

actual fun createWorkoutLiveNotificationManager(): WorkoutLiveNotificationManager =
    IOSWorkoutLiveNotificationManager()
