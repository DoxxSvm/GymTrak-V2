package `in`.gym.trak.studio.data.repository

import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_workout
import `in`.gym.trak.studio.data.model.*
import `in`.gym.trak.studio.getCurrentTimeMillis
import `in`.gym.trak.studio.getPlatform
import `in`.gym.trak.studio.notifications.WorkoutLiveNotificationController
import `in`.gym.trak.studio.utils.requestNotificationPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object WorkoutManager {
    private val platform = getPlatform()
    private const val KEY_ACTIVE_WORKOUT = "active_workout_state"
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _activeWorkout = MutableStateFlow<ActiveWorkoutState?>(loadState())
    val activeWorkout = _activeWorkout.asStateFlow()

    private val _bottomPadding = MutableStateFlow(0.dp)
    var bottomPadding: Dp
        get() = _bottomPadding.value
        set(value) { _bottomPadding.value = value }
    
    val bottomPaddingFlow = _bottomPadding.asStateFlow()

    init {
        // Restore live notification if a workout was persisted before process death.
        _activeWorkout.value?.let { WorkoutLiveNotificationController.sync(it) }

        // Start a tick for the timer if a workout is active
        scope.launch {
            while (true) {
                delay(1000)
                val current = _activeWorkout.value
                if (current != null && !current.isPaused) {
                    // This triggers UI updates for the timer
                    val updated = current.copy(lastUpdateMillis = getCurrentTimeMillis())
                    _activeWorkout.value = updated
                    WorkoutLiveNotificationController.sync(updated)
                }
            }
        }
    }

    fun elapsedMillis(state: ActiveWorkoutState? = _activeWorkout.value): Long {
        state ?: return 0L
        val now = getCurrentTimeMillis()
        val end = if (state.isPaused) {
            state.pausedAtMillis ?: now
        } else {
            state.lastUpdateMillis.takeIf { it > 0L } ?: now
        }
        return (end - state.startTimeMillis - state.pausedDurationMillis).coerceAtLeast(0L)
    }

    fun pauseWorkout() {
        val current = _activeWorkout.value ?: return
        if (current.isPaused) return
        val now = getCurrentTimeMillis()
        saveAndEmit(
            current.copy(
                isPaused = true,
                pausedAtMillis = now,
                lastUpdateMillis = now,
            ),
        )
    }

    fun resumeWorkout() {
        val current = _activeWorkout.value ?: return
        if (!current.isPaused) return
        val now = getCurrentTimeMillis()
        val pauseStart = current.pausedAtMillis ?: now
        saveAndEmit(
            current.copy(
                isPaused = false,
                pausedAtMillis = null,
                pausedDurationMillis = current.pausedDurationMillis + (now - pauseStart),
                lastUpdateMillis = now,
            ),
        )
    }

    fun restoreWorkoutState(state: ActiveWorkoutState) {
        saveAndEmit(state)
    }

    fun startWorkout(
        title: String,
        workoutId: String? = null,
        exercises: List<ActiveExercise>,
        showFloatingOverlay: Boolean = true,
        isServerSessionStarted: Boolean = false,
    ) {
        requestNotificationPermission()
        val existing = _activeWorkout.value
        val preserveSession = existing != null && shouldPreserveWorkoutSession(
            existingWorkoutId = existing.workoutId,
            incomingWorkoutId = workoutId,
        )
        val now = getCurrentTimeMillis()
        val newState = ActiveWorkoutState(
            workoutId = workoutId ?: existing?.workoutId,
            title = title,
            exercises = exercises.map { it.toState() },
            startTimeMillis = if (preserveSession) existing.startTimeMillis else now,
            lastUpdateMillis = now,
            showFloatingOverlay = showFloatingOverlay,
            isServerSessionStarted = isServerSessionStarted ||
                existing?.isServerSessionStarted == true,
        )
        saveAndEmit(newState)
    }

    private fun shouldPreserveWorkoutSession(
        existingWorkoutId: String?,
        incomingWorkoutId: String?,
    ): Boolean {
        if (incomingWorkoutId.isNullOrBlank()) return true
        if (existingWorkoutId.isNullOrBlank()) return true
        return existingWorkoutId == incomingWorkoutId
    }

    fun updateExercises(exercises: List<ActiveExercise>) {
        val current = _activeWorkout.value ?: return
        val now = getCurrentTimeMillis()
        val updated = current.copy(
            exercises = exercises.map { it.toState() },
            lastUpdateMillis = if (current.isPaused) current.lastUpdateMillis else now,
        )
        saveAndEmit(updated)
    }

    fun updateTitle(title: String) {
        val current = _activeWorkout.value ?: return
        val updated = current.copy(
            title = title,
            lastUpdateMillis = getCurrentTimeMillis()
        )
        saveAndEmit(updated)
    }

    fun updateWorkoutSession(
        workoutId: String? = null,
        showFloatingOverlay: Boolean? = null,
        isServerSessionStarted: Boolean? = null,
    ) {
        val current = _activeWorkout.value ?: return
        val updated = current.copy(
            workoutId = workoutId ?: current.workoutId,
            showFloatingOverlay = showFloatingOverlay ?: current.showFloatingOverlay,
            isServerSessionStarted = isServerSessionStarted ?: current.isServerSessionStarted,
            lastUpdateMillis = getCurrentTimeMillis(),
        )
        saveAndEmit(updated)
    }

    fun markServerSessionStarted() {
        updateWorkoutSession(isServerSessionStarted = true)
    }

    fun stopWorkout() {
        platform.remove(KEY_ACTIVE_WORKOUT)
        _activeWorkout.value = null
        WorkoutLiveNotificationController.sync(null)
    }

    /** Refreshes elapsed timer after returning from notification / background. */
    fun touchActiveWorkoutClock() {
        val current = _activeWorkout.value ?: return
        if (current.isPaused) return
        val now = getCurrentTimeMillis()
        val updated = current.copy(lastUpdateMillis = now)
        _activeWorkout.value = updated
        WorkoutLiveNotificationController.sync(updated)
    }

    private fun saveAndEmit(state: ActiveWorkoutState) {
        try {
            val string = json.encodeToString(state)
            platform.setString(KEY_ACTIVE_WORKOUT, string)
            _activeWorkout.value = state
            WorkoutLiveNotificationController.sync(state)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadState(): ActiveWorkoutState? {
        val string = platform.getString(KEY_ACTIVE_WORKOUT, "")
        if (string.isEmpty()) return null
        return try {
            json.decodeFromString<ActiveWorkoutState>(string)
        } catch (e: Exception) {
            null
        }
    }

    // Helper extensions
    fun ActiveExerciseState.toDomain(): ActiveExercise = ActiveExercise(
        id = id,
        name = name,
        icon = Res.drawable.ic_workout,
        assetUrl = assetUrl,
        type = type,
        workoutExerciseId = workoutExerciseId,
        sets = sets.map { it.toDomain() }.toMutableList()
    )

    fun WorkoutSetState.toDomain(): WorkoutSet = WorkoutSet(
        setNumber = setNumber,
        kg = kg,
        reps = reps,
        isDone = isDone,
        serverSetId = serverSetId,
    )

    private fun ActiveExercise.toState(): ActiveExerciseState = ActiveExerciseState(
        id = id,
        name = name,
        assetUrl = assetUrl,
        type = type,
        workoutExerciseId = workoutExerciseId,
        sets = sets.map { it.toState() }
    )

    private fun WorkoutSet.toState(): WorkoutSetState = WorkoutSetState(
        setNumber = setNumber,
        kg = kg,
        reps = reps,
        isDone = isDone,
        serverSetId = serverSetId,
    )
}
