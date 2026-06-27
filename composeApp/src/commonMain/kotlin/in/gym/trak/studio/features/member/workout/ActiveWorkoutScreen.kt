package `in`.gym.trak.studio.features.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.components.AppEmptyStateView
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.CommonOutlineButton
import `in`.gym.trak.studio.theme.*
import gym.composeapp.generated.resources.*
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import `in`.gym.trak.studio.data.model.*
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import `in`.gym.trak.studio.data.repository.WorkoutManager
import `in`.gym.trak.studio.data.repository.WorkoutManager.toDomain
import `in`.gym.trak.studio.getCurrentTimeMillis
import kotlin.jvm.Transient


private data class ActiveWorkoutSummaryStats(
    val durationSeconds: Int,
    val totalKg: Double,
    val totalReps: Int,
    val totalSets: Int
)

private fun formatActiveDurationFromSeconds(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) {
        "${hours.toString().padStart(2, '0')}:${
            minutes.toString().padStart(2, '0')
        }:${seconds.toString().padStart(2, '0')}"
    } else {
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}

private fun formatActiveKgValue(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else ((value * 10).toInt() / 10.0).toString()
}

private data class WorkoutDraftSnapshot(
    val title: String,
    val exerciseSignatures: List<String>,
)

private fun buildWorkoutDraftSnapshot(
    title: String,
    exercises: List<ActiveExercise>,
): WorkoutDraftSnapshot {
    val signatures = exercises.map { exercise ->
        val setsSignature = exercise.sets.joinToString("|") { set ->
            "${set.setNumber}:${set.kg}:${set.reps}:${set.isDone}"
        }
        "${exercise.id}#$setsSignature"
    }
    return WorkoutDraftSnapshot(title = title.trim(), exerciseSignatures = signatures)
}

private fun activeExercisesToWorkoutRequests(
    exercises: List<ActiveExercise>,
): List<WorkoutExerciseRequest> =
    exercises.map { exercise ->
        WorkoutExerciseRequest(
            exercise_id = exercise.id,
            sets = exercise.sets.map { set ->
                WorkoutSetRequest(
                    set_number = set.setNumber,
                    reps = set.reps.toIntOrNull() ?: 0,
                    weight = set.kg.toDoubleOrNull() ?: 0.0,
                )
            },
        )
    }

private fun lastWorkoutSetInDraft(exercises: List<ActiveExercise>): WorkoutSet? =
    exercises.lastOrNull()?.sets?.lastOrNull()

private fun exerciseRowToActiveExercise(
    row: ExerciseRowDTO,
    previousSet: WorkoutSet? = null,
): ActiveExercise {
    val isTime = row.exercise_type?.contains("DURATION") == true
    return ActiveExercise(
        id = row.id,
        name = row.name,
        icon = Res.drawable.ic_dumble,
        assetUrl = row.asset_url,
        type = if (isTime) ExerciseType.TIME else ExerciseType.REPS,
        sets = mutableListOf(
            workoutSetFromPrevious(
                setNumber = 1,
                previousSet = previousSet,
                isTime = isTime,
            ),
        ),
    )
}

/** Success UI uses [WorkoutDetailResponse]; create-workout API returns no body, so we mirror local session data. */
private fun buildWorkoutDetailFromCompletedLocal(
    title: String,
    exercises: List<ActiveExercise>,
    durationDisplay: String,
    summary: ActiveWorkoutSummaryStats,
): WorkoutDetailResponse {
    val exercisesDto = exercises.map { ex ->
        WorkoutDetailExerciseDTO(
            exercise_id_alt = ex.id,
            name = ex.name,
            asset_url_alt = ex.assetUrl,
            exercise_type_alt = ex.type.name,
            sets = ex.sets.map { s ->
                WorkoutDetailSetDTO(
                    setNumber = s.setNumber,
                    reps = s.reps.toIntOrNull() ?: 0,
                    weight = s.kg.toDoubleOrNull() ?: 0.0,
                    completed = s.isDone
                )
            }
        )
    }
    return WorkoutDetailResponse(
        title = title,
        duration = durationDisplay,
        volume = "${formatActiveKgValue(summary.totalKg)} Kg / ${summary.totalReps} Reps",
        sets = summary.totalSets,
        exercises = exercisesDto
    )
}

class ActiveWorkoutScreen(
    private val showSkipSection: Boolean = true,
    private val workoutIdForCompletion: String? = null,
    private val workoutId: String? = null,
    private val useCompleteApiOnFinish: Boolean = false,
    private val memberIdForCreate: String = "",
    private val initialExercises: List<WorkoutDetailExerciseDTO> = emptyList(),
    @Transient private val onWorkoutSaved: (() -> Unit)? = null,
    @Transient private val onWorkoutCreated: (() -> Unit)? = null,
    private val isEditMode: Boolean = false,
    private val autoCreateOnFirstExercise: Boolean = false,
    private val initialExerciseRows: List<ExerciseRowDTO> = emptyList(),
    private val manageLiveSession: Boolean = true,
    private val workoutTitle: String = "Active Workout",
) : Screen {

    private fun buildInitialDraftExercises(): List<ActiveExercise> {
        val active = WorkoutManager.activeWorkout.value
        if (active != null && manageLiveSession) {
            return active.exercises.map { it.toDomain() }
        }
        if (initialExercises.isNotEmpty()) {
            return initialExercises.map { activeExerciseFromDetail(it) }
        }
        val exercises = mutableListOf<ActiveExercise>()
        var previousSet: WorkoutSet? = null
        initialExerciseRows.forEach { row ->
            val exercise = exerciseRowToActiveExercise(row, previousSet = previousSet)
            exercises.add(exercise)
            previousSet = exercise.sets.lastOrNull()
        }
        return exercises
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val isLoading by screenModel.isLoading.collectAsState()

        val exercises = remember(manageLiveSession, initialExercises, initialExerciseRows) {
            mutableStateListOf<ActiveExercise>().apply {
                addAll(buildInitialDraftExercises())
            }
        }
        var statsVersion by remember { mutableStateOf(0) }

        var workoutTitleState by remember { mutableStateOf(WorkoutManager.activeWorkout.value?.title ?: workoutTitle) }
        var workoutTitleError by remember { mutableStateOf<String?>(null) }
        var serverSessionWorkoutId by remember {
            mutableStateOf(workoutIdForCompletion?.takeIf { it.isNotBlank() })
        }
        var hasStartedServerWorkout by remember {
            mutableStateOf(useCompleteApiOnFinish && !workoutIdForCompletion.isNullOrBlank())
        }
        var isAutoCreatingWorkout by remember { mutableStateOf(false) }
        var autoCreateAttempted by remember { mutableStateOf(false) }
        var savedDraftSnapshot by remember { mutableStateOf<WorkoutDraftSnapshot?>(null) }
        var isPauseResumeLoading by remember { mutableStateOf(false) }

        val resolvedMemberId = memberIdForCreate.ifBlank { SessionManager.userId }
        val gymID = SessionManager.gymId

        DisposableEffect(Unit) {
            WorkoutManager.updateWorkoutSession(showFloatingOverlay = false)
            onDispose {
                if (WorkoutManager.activeWorkout.value != null) {
                    WorkoutManager.updateWorkoutSession(showFloatingOverlay = true)
                }
            }
        }

        SideEffect {
            if (WorkoutManager.activeWorkout.value?.showFloatingOverlay == true) {
                WorkoutManager.updateWorkoutSession(showFloatingOverlay = false)
            }
        }

        fun buildCreateWorkoutRequest(): CreateWorkoutRequest =
            CreateWorkoutRequest(
                member_id = if (gymID.isBlank()) null else resolvedMemberId,
                title = workoutTitleState.trim().ifBlank { workoutTitle },
                notes = "",
                created_by = "member",
                exercises = activeExercisesToWorkoutRequests(exercises.toList()),
            )

        fun buildUpdateWorkoutRequest(workoutId: String): CompleteWorkoutRequest =
            CompleteWorkoutRequest(
                workoutId = workoutId,
                title = workoutTitleState.trim().ifBlank { workoutTitle },
                notes = "",
                exercises = activeExercisesToWorkoutRequests(exercises.toList()),
            )

        fun resolveActiveWorkoutStopId(): String? =
            WorkoutManager.activeWorkout.value?.workoutId?.takeIf { it.isNotBlank() }
                ?: serverSessionWorkoutId?.takeIf { it.isNotBlank() }
                ?: workoutIdForCompletion?.takeIf { it.isNotBlank() }
                ?: workoutId?.takeIf { it.isNotBlank() }

        LaunchedEffect(useCompleteApiOnFinish) {
            if (!useCompleteApiOnFinish) return@LaunchedEffect
            WorkoutManager.activeWorkout.value?.workoutId?.takeIf { it.isNotBlank() }?.let { activeId ->
                serverSessionWorkoutId = activeId
                hasStartedServerWorkout = true
                WorkoutManager.markServerSessionStarted()
            }
        }

        LaunchedEffect(manageLiveSession, isEditMode) {
            if (!manageLiveSession || isEditMode) return@LaunchedEffect
            val active = WorkoutManager.activeWorkout.value ?: return@LaunchedEffect
            if (active.isServerSessionStarted) {
                hasStartedServerWorkout = true
                active.workoutId?.takeIf { it.isNotBlank() }?.let { activeId ->
                    if (serverSessionWorkoutId.isNullOrBlank()) {
                        serverSessionWorkoutId = activeId
                    }
                }
            }
        }

        fun autoCreateWorkout() {
            if (
                !autoCreateOnFirstExercise ||
                isEditMode ||
                hasStartedServerWorkout ||
                autoCreateAttempted ||
                isAutoCreatingWorkout ||
                exercises.isEmpty() ||
                !serverSessionWorkoutId.isNullOrBlank()
            ) {
                return
            }
            autoCreateAttempted = true
            isAutoCreatingWorkout = true
            screenModel.createWorkoutAndResolveId(
                request = buildCreateWorkoutRequest(),
                onCreated = { workoutId ->
                    serverSessionWorkoutId = workoutId
                    isAutoCreatingWorkout = false
                    savedDraftSnapshot = buildWorkoutDraftSnapshot(
                        title = workoutTitleState,
                        exercises = exercises.toList(),
                    )
                },
                onFailure = {
                    isAutoCreatingWorkout = false
                },
            )
        }

        fun beginServerWorkoutSession(sessionId: String, detail: WorkoutDetailResponse? = null) {
            serverSessionWorkoutId = sessionId
            hasStartedServerWorkout = true
            detail?.let { mergeWorkoutDetailIntoExercises(exercises.toList(), it.exercises) }
            savedDraftSnapshot = buildWorkoutDraftSnapshot(
                title = workoutTitleState,
                exercises = exercises.toList(),
            )
            val active = WorkoutManager.activeWorkout.value
            if (active == null) {
                WorkoutManager.startWorkout(
                    title = workoutTitleState.trim().ifBlank { workoutTitle },
                    workoutId = sessionId,
                    exercises = exercises.toList(),
                    showFloatingOverlay = false,
                )
            } else {
                WorkoutManager.updateWorkoutSession(
                    workoutId = sessionId,
                    showFloatingOverlay = false,
                )
                WorkoutManager.updateExercises(exercises.toList())
            }
        }

        fun createExerciseSetOnServer(
            exercise: ActiveExercise,
            set: WorkoutSet,
            onSuccess: () -> Unit = {},
            onFailure: () -> Unit = {},
        ) {
            val workoutExerciseId = exercise.workoutExerciseId?.takeIf { it.isNotBlank() } ?: return
            if (set.serverSetId?.isNotBlank() == true) return

            val isTime = exercise.type == ExerciseType.TIME
            screenModel.createWorkoutSet(
                request = buildCreateSetRequest(
                    workoutExerciseId = workoutExerciseId,
                    set = set,
                    isTime = isTime,
                ),
                onSuccess = { response ->
                    response.id.takeIf { it.isNotBlank() }?.let { set.serverSetId = it }
                    onSuccess()
                    statsVersion++
                },
                onFailure = onFailure,
            )
        }

        fun handleSetAdded(exercise: ActiveExercise, set: WorkoutSet) {
            if (!hasStartedServerWorkout || !manageLiveSession || isEditMode) return
            createExerciseSetOnServer(exercise, set)
        }

        fun handleSetCompleted(exercise: ActiveExercise, set: WorkoutSet, completed: Boolean) {
            if (!hasStartedServerWorkout || !manageLiveSession || isEditMode) return

            val isTime = exercise.type == ExerciseType.TIME
            val serverSetId = set.serverSetId?.takeIf { it.isNotBlank() }

            if (serverSetId != null) {
                screenModel.updateWorkoutSet(
                    setId = serverSetId,
                    request = buildUpdateSetRequest(set, isTime, completed),
                    onSuccess = { response ->
                        response.id.takeIf { it.isNotBlank() }?.let { set.serverSetId = it }
                        statsVersion++
                    },
                    onFailure = {
                        set.isDone = !completed
                        statsVersion++
                    },
                )
                return
            }

            if (!completed) return

            createExerciseSetOnServer(
                exercise = exercise,
                set = set,
                onSuccess = {
                    val createdId = set.serverSetId?.takeIf { it.isNotBlank() } ?: return@createExerciseSetOnServer
                    screenModel.updateWorkoutSet(
                        setId = createdId,
                        request = buildUpdateSetRequest(set, isTime, completed = true),
                        onSuccess = {
                            set.isDone = true
                            statsVersion++
                        },
                        onFailure = {
                            set.isDone = false
                            statsVersion++
                        },
                    )
                },
                onFailure = {
                    set.isDone = false
                    statsVersion++
                },
            )
        }

        fun handleSetUpdated(exercise: ActiveExercise, set: WorkoutSet) {
            if (!hasStartedServerWorkout || !manageLiveSession || isEditMode) return

            val serverSetId = set.serverSetId?.takeIf { it.isNotBlank() } ?: return
            val isTime = exercise.type == ExerciseType.TIME
            screenModel.updateWorkoutSet(
                setId = serverSetId,
                request = buildUpdateSetRequest(set, isTime, set.isDone),
                onSuccess = { response ->
                    response.id.takeIf { it.isNotBlank() }?.let { set.serverSetId = it }
                },
            )
        }

        fun startEmptyWorkoutSession() {
            val workoutId = serverSessionWorkoutId?.takeIf { it.isNotBlank() } ?: return
            if (workoutTitleState.trim().isEmpty()) {
                workoutTitleError = "Workout title is required."
                return
            }
            if (exercises.isEmpty()) return

            val currentSnapshot = buildWorkoutDraftSnapshot(
                title = workoutTitleState,
                exercises = exercises.toList(),
            )
            val needsUpdate = savedDraftSnapshot != null && savedDraftSnapshot != currentSnapshot

            if (needsUpdate) {
                screenModel.updateWorkout(
                    request = buildUpdateWorkoutRequest(workoutId),
                    onSuccess = {
                        savedDraftSnapshot = currentSnapshot
                        screenModel.startWorkout(workoutId) { response ->
                            beginServerWorkoutSession(
                                sessionId = response.workoutId.ifBlank { workoutId },
                                detail = response,
                            )
                        }
                    },
                )
            } else {
                screenModel.startWorkout(workoutId) { response ->
                    beginServerWorkoutSession(
                        sessionId = response.workoutId.ifBlank { workoutId },
                        detail = response,
                    )
                }
            }
        }

        LaunchedEffect(autoCreateOnFirstExercise, exercises.size, hasStartedServerWorkout, serverSessionWorkoutId) {
            if (
                autoCreateOnFirstExercise &&
                exercises.isNotEmpty() &&
                !hasStartedServerWorkout &&
                serverSessionWorkoutId.isNullOrBlank() &&
                !autoCreateAttempted
            ) {
                autoCreateWorkout()
            }
        }

        LaunchedEffect(hasStartedServerWorkout) {
            if (hasStartedServerWorkout) {
                WorkoutManager.markServerSessionStarted()
            }
        }

        /** Keep local workout session in sync; empty draft clears the global active workout. */
        LaunchedEffect(statsVersion, exercises.size, isEditMode, serverSessionWorkoutId, manageLiveSession, hasStartedServerWorkout, autoCreateOnFirstExercise) {
            if (!manageLiveSession) return@LaunchedEffect
            if (autoCreateOnFirstExercise && !hasStartedServerWorkout) return@LaunchedEffect
            if (isEditMode) {
                if (WorkoutManager.activeWorkout.value != null && exercises.isNotEmpty()) {
                    WorkoutManager.updateExercises(exercises.toList())
                }
                return@LaunchedEffect
            }
            if (exercises.isEmpty()) {
                if (WorkoutManager.activeWorkout.value != null) {
                    WorkoutManager.stopWorkout()
                }
                return@LaunchedEffect
            }
            if (WorkoutManager.activeWorkout.value == null) {
                WorkoutManager.startWorkout(
                    title = workoutTitleState.trim().ifBlank { workoutTitle },
                    workoutId = serverSessionWorkoutId,
                    exercises = exercises.toList(),
                    showFloatingOverlay = false,
                )
            } else {
                WorkoutManager.updateExercises(exercises.toList())
            }
        }

        LaunchedEffect(workoutTitleState, isEditMode, manageLiveSession) {
            if (!manageLiveSession || isEditMode) return@LaunchedEffect
            WorkoutManager.updateTitle(workoutTitleState)
        }

        val activeWorkoutState by WorkoutManager.activeWorkout.collectAsState()
        val syncSetsToServer = hasStartedServerWorkout && manageLiveSession && !isEditMode
        val isWorkoutPaused = activeWorkoutState?.isPaused == true
        val canPauseResume = manageLiveSession &&
            !isEditMode &&
            !resolveActiveWorkoutStopId().isNullOrBlank() &&
            (hasStartedServerWorkout || activeWorkoutState?.isServerSessionStarted == true)
        val totalWorkoutDuration = if (autoCreateOnFirstExercise && !hasStartedServerWorkout) {
            formatActiveDurationFromSeconds(0)
        } else {
            formatActiveDurationFromSeconds(
                (WorkoutManager.elapsedMillis(activeWorkoutState) / 1000).coerceAtLeast(0).toInt(),
            )
        }
        val showStartWorkoutButton = autoCreateOnFirstExercise &&
            !hasStartedServerWorkout &&
            !serverSessionWorkoutId.isNullOrBlank() &&
            exercises.isNotEmpty()

        fun pauseWorkoutSession() {
            val workoutId = resolveActiveWorkoutStopId() ?: return
            if (isWorkoutPaused || isPauseResumeLoading) return
            val snapshot = WorkoutManager.activeWorkout.value ?: return
            isPauseResumeLoading = true
            WorkoutManager.pauseWorkout()
            screenModel.pauseWorkout(
                workoutId = workoutId,
                onSuccess = { isPauseResumeLoading = false },
                onError = {
                    WorkoutManager.restoreWorkoutState(snapshot)
                    isPauseResumeLoading = false
                },
            )
        }

        fun resumeWorkoutSession() {
            val workoutId = resolveActiveWorkoutStopId() ?: return
            if (!isWorkoutPaused || isPauseResumeLoading) return
            val snapshot = WorkoutManager.activeWorkout.value ?: return
            isPauseResumeLoading = true
            WorkoutManager.resumeWorkout()
            screenModel.resumeWorkout(
                workoutId = workoutId,
                onSuccess = { isPauseResumeLoading = false },
                onError = {
                    WorkoutManager.restoreWorkoutState(snapshot)
                    isPauseResumeLoading = false
                },
            )
        }

        val summaryStats = remember(exercises.size, statsVersion) {
            var durationSeconds = 0
            var totalKg = 0.0
            var totalReps = 0
            var totalSets = 0

            exercises.forEach { exercise ->
                totalSets += exercise.sets.size
                exercise.sets.forEach { set ->
                    if (exercise.type == ExerciseType.TIME) {
                        durationSeconds += set.kg.toIntOrNull() ?: 0
                    } else {
                        totalKg += set.kg.toDoubleOrNull() ?: 0.0
                        totalReps += set.reps.toIntOrNull() ?: 0
                    }
                }
            }

            ActiveWorkoutSummaryStats(
                durationSeconds = durationSeconds,
                totalKg = totalKg,
                totalReps = totalReps,
                totalSets = totalSets
            )
        }
        val hasInvalidSetData = remember(exercises.size, statsVersion) {
            exercises.any { exercise ->
                exercise.sets.isEmpty() || exercise.sets.any { set ->
                    val weight = set.kg.toDoubleOrNull()
                    if (weight == null || weight < 0) return@any true
                    if (exercise.type == ExerciseType.TIME) {
                        weight <= 0
                    } else {
                        val reps = set.reps.toIntOrNull()
                        reps == null || reps <= 0
                    }
                }
            }
        }
        val canFinish = exercises.isNotEmpty() && !hasInvalidSetData // Logic for internal data validity
        val shouldCompleteOnServer = useCompleteApiOnFinish || hasStartedServerWorkout

        fun validateAndFinish() {
            if (workoutTitleState.trim().isEmpty()) {
                workoutTitleError = "Workout title is required."
                return
            }
            if (!canFinish) return

            if (isEditMode) {
                if (workoutId.isNullOrBlank()) return
                val request = UpdateWorkoutLegacyRequest(
                    workoutId = workoutId,
                    title = workoutTitleState.trim(),
                    notes = ""
                )
                screenModel.updateWorkoutLegacy(
                    request = request,
                    onSuccess = {
                        onWorkoutSaved?.invoke()
                        navigator.pop()
                    }
                )
            } else if (shouldCompleteOnServer || useCompleteApiOnFinish) {
                val stopId = resolveActiveWorkoutStopId() ?: return
                screenModel.stopWorkout(
                    workoutId = stopId,
                    onSuccess = { response ->
                        WorkoutManager.stopWorkout()
                        onWorkoutSaved?.invoke()
                        navigator.replace(
                            WorkoutSuccessScreen(
                                workoutId = stopId,
                                workout = response.takeIf { it.workoutId.isNotBlank() }
                                    ?: response.copy(workout_id = stopId),
                            )
                        )
                    },
                )
            } else {
                screenModel.createWorkout(buildCreateWorkoutRequest().copy(title = workoutTitleState.trim())) { _ ->
                    WorkoutManager.stopWorkout()
                    onWorkoutCreated?.invoke() ?: onWorkoutSaved?.invoke()
                    navigator.pop()
                }
            }
        }

        Scaffold(
            topBar = {
                GymAppBar(
                    title = "Workout",
                    onBackClick = {
                        navigator.pop()
                    },
                    actions =
                        {
                            Box(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(if (canFinish && workoutTitleState.isNotBlank()) PrimaryColor else Color(0xFFE5E7EB))
                                    .clickable(enabled = !isLoading) {
                                        validateAndFinish()
                                    }
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = if (canFinish) White else Gray
                                    )
                                } else {
                                    Text(
                                        "Finish",
                                        style = AppTextTheme.semiBold.copy(
                                            fontSize = 14.sp,
                                            color = if (canFinish) White else Gray
                                        )
                                    )
                                }
                            }
                        }
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 12.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                // Workout Title Card
                item {
                    CommonCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 0.dp,
                        borderColor = Color(0xFFE5E7EB),
                        backgroundColor = White
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "Workout Title",
                                style = AppTextTheme.semiBold.copy(fontSize = 13.sp, color = Black),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            CommonTextField(
                                value = workoutTitleState,
                                onValueChange = {
                                    workoutTitleState = it
                                    if (workoutTitleError != null && it.trim().isNotEmpty()) {
                                        workoutTitleError = null
                                    }
                                },
                                placeholder = "Enter workout title",
                                errorText = workoutTitleError
                            )
                        }
                    }
                }

                // Stats Card
                item {
                    CommonCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 0.dp,
                        borderColor = Color(0xFFE5E7EB),
                        backgroundColor = White
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WorkoutDetailStat(
                                modifier = Modifier.weight(1f),
                                icon = Res.drawable.ic_duration,
                                label = "Duration",
                                value = totalWorkoutDuration,
                                valueColor = if (isWorkoutPaused) Gray else Black
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(GrayBorderColor)
                            )
                            WorkoutDetailStat(
                                modifier = Modifier.weight(1f),
                                icon = Res.drawable.ic_volume,
                                label = "Volume",
                                value = "${formatActiveKgValue(summaryStats.totalKg)} Kg / ${summaryStats.totalReps} Reps",
                                valueColor = Black
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(GrayBorderColor)
                            )
                            WorkoutDetailStat(
                                modifier = Modifier.weight(1f),
                                icon = Res.drawable.ic_sets,
                                label = "Sets",
                                value = summaryStats.totalSets.toString(),
                                valueColor = Black
                            )
                        }
                    }
                }

                if (canPauseResume) {
                    item {
                        if (isWorkoutPaused) {
                            CommonButton(
                                onClick = { resumeWorkoutSession() },
                                text = "Resume Workout",
                            )
                        } else {
                            CommonOutlineButton(
                                onClick = { pauseWorkoutSession() },
                                text = "Pause Workout",
                                textColor = PrimaryColor,
                                borderColor = PrimaryColor,
                            )
                        }
                    }
                }
                if (exercises.isNotEmpty() && hasInvalidSetData) {
                    item {
                        Text(
                            text = "Please enter valid values: Duration/Time > 0, KG >= 0, and Reps > 0.",
                            style = AppTextTheme.regular.copy(fontSize = 12.sp, color = RedColor)
                        )
                    }
                }

                // Exercise Cards / empty state
                if (exercises.isEmpty()) {
                    item {
                        AppEmptyStateView(
                            image = Res.drawable.img_no_wrokout,
                            title = "No Exercise Added",
                            subtitle = "Tap Add Exercise to build your workout."
                        )
                    }
                } else {
                    items(
                        count = exercises.size,
                        key = { index -> "${exercises[index].id}-$index" },
                    ) { index ->
                        val exercise = exercises[index]
                        StaggeredEntranceItem(index = index) {
                            ExerciseCard(
                                exercise = exercise,
                                isCreateMode = !syncSetsToServer,
                                readOnly = false,
                                onSetValueChanged = { statsVersion++ },
                                onSetCompleted = { ex, set, completed ->
                                    handleSetCompleted(ex, set, completed)
                                },
                                onSetUpdated = { ex, set ->
                                    handleSetUpdated(ex, set)
                                },
                                onSetAdded = { ex, set ->
                                    handleSetAdded(ex, set)
                                },
                                onDelete = {
                                    exercises.remove(exercise)
                                    statsVersion++
                                },
                            )
                        }
                    }
                }

                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(top = 8.dp, bottom = 8.dp),
                    ) {
                        CommonButton(
                            onClick = {
                                navigator.push(AddExerciseScreen(onExercisesSelected = { selected ->
                                    selected.forEach { row ->
                                        exercises.add(
                                            exerciseRowToActiveExercise(
                                                row = row,
                                                previousSet = lastWorkoutSetInDraft(exercises.toList()),
                                            ),
                                        )
                                    }
                                    statsVersion++
                                }))
                            },
                            text = "Add Exercise",
                            leftIcon = painterResource(Res.drawable.ic_add),
                        )
                        if (showStartWorkoutButton) {
                            Spacer(modifier = Modifier.height(10.dp))
                            CommonButton(
                                onClick = { startEmptyWorkoutSession() },
                                text = "Start Workout",
                                enabled = !isLoading && !isAutoCreatingWorkout,
                                rightIcon = painterResource(Res.drawable.ic_thunder_filled),
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        CommonOutlineButton(
                            onClick = {
                                val stopId = serverSessionWorkoutId?.takeIf { it.isNotBlank() }
                                    ?: workoutIdForCompletion?.takeIf { it.isNotBlank() }
                                if (!isEditMode && !stopId.isNullOrBlank()) {
                                    screenModel.stopWorkout(stopId)
                                }
                                if (!isEditMode) {
                                    WorkoutManager.stopWorkout()
                                }
                                navigator.pop()
                            },
                            borderColor = Color.Transparent,
                            color = RedColor.copy(alpha = 0.1f),
                            textColor = RedColor.copy(alpha = 0.8f),
                            text = "Discard Workout",
                        )
                    }
                }
            }
        }
    }
}


