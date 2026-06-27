package `in`.gym.trak.studio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ActiveWorkoutState(
    val workoutId: String? = null,
    val title: String,
    val exercises: List<ActiveExerciseState>,
    val startTimeMillis: Long,
    val lastUpdateMillis: Long = 0L,
    val isPaused: Boolean = false,
    val pausedDurationMillis: Long = 0L,
    val pausedAtMillis: Long? = null,
    val isServerSessionStarted: Boolean = false,
    val showFloatingOverlay: Boolean = true,
)

@Serializable
data class ActiveExerciseState(
    val id: String,
    val name: String,
    val assetUrl: String? = null,
    val type: ExerciseType,
    val workoutExerciseId: String? = null,
    val sets: List<WorkoutSetState>,
    val timerStartTimeMillis: Long? = null // if currently timing
)

@Serializable
data class WorkoutSetState(
    val setNumber: Int,
    val kg: String,
    val reps: String,
    val isDone: Boolean,
    val serverSetId: String? = null,
)
