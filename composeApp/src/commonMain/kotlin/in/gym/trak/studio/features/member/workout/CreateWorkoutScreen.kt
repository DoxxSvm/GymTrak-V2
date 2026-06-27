package `in`.gym.trak.studio.features.member

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.ConfirmationDialog
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import `in`.gym.trak.studio.data.model.*
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.GrayBorderColor
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_add
import gym.composeapp.generated.resources.ic_dumble
import gym.composeapp.generated.resources.ic_duration
import gym.composeapp.generated.resources.ic_sets
import gym.composeapp.generated.resources.ic_volume
import org.jetbrains.compose.resources.painterResource

private data class WorkoutSummaryStats(
    val durationSeconds: Int,
    val totalKg: Double,
    val totalReps: Int,
    val totalSets: Int
)

private fun formatDurationFromSeconds(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) {
        "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}

private fun formatKgValue(value: Double): String {
    return if (value % 1.0 == 0.0) value.toInt().toString() else ((value * 10).toInt() / 10.0).toString()
}

class CreateWorkoutScreen(
    val memberId: String,
    val selectedExercises: List<ExerciseRowDTO> = emptyList(),
    val workoutId: String? = null, // null = create mode, non-null = existing workout
    val readOnly: Boolean = false,
    val onWorkoutSaved: (() -> Unit)? = null
) : Screen {
    private val draftExercises = mutableStateListOf<ActiveExercise>()
    private var isDraftInitialized = false
    private var draftTitle: String = ""

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val isLoading by screenModel.isLoading.collectAsState()

        // ── State ───────────────────────────────────────────────────────────
        val isViewMode = workoutId != null
//        var isEditMode by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }
        var workoutTitle by remember { mutableStateOf(draftTitle) }
        var workoutTitleError by remember { mutableStateOf<String?>(null) }
        var workoutFormError by remember { mutableStateOf<String?>(null) }
        var statsVersion by remember { mutableStateOf(0) }

        val workoutExercises = remember { draftExercises }
        LaunchedEffect(Unit) {
            if (!isDraftInitialized) {
                draftExercises.clear()
                draftExercises.addAll(selectedExercises.map { row ->
                    ActiveExercise(
                        id = row.id,
                        name = row.name,
                        icon = Res.drawable.ic_dumble,
                        assetUrl = row.asset_url,
                        type = if (row.exercise_type?.contains("DURATION") == true) ExerciseType.TIME else ExerciseType.REPS,
                        sets = mutableListOf(WorkoutSet(1, "—", "0", "0"))
                    )
                })
                isDraftInitialized = true
            }
        }

        val summaryStats = remember(workoutExercises.size, statsVersion) {
            var durationSeconds = 0
            var totalKg = 0.0
            var totalReps = 0
            var totalSets = 0

            workoutExercises.forEach { exercise ->
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

            WorkoutSummaryStats(
                durationSeconds = durationSeconds,
                totalKg = totalKg,
                totalReps = totalReps,
                totalSets = totalSets
            )
        }

        // Load from API when viewing an existing workout
        LaunchedEffect(workoutId) {
            if (workoutId != null) {
                screenModel.getWorkoutDetail(workoutId) { detail ->
                    workoutTitle = detail.title
                    draftTitle = detail.title
                    workoutExercises.clear()
                    workoutExercises.addAll(detail.exercises.map { ex ->
                        ActiveExercise(
                            id = ex.exercise_id,
                            name = ex.name,
                            icon = Res.drawable.ic_dumble,
                            assetUrl = ex.asset_url,
                            type = if (ex.exercise_type?.contains("DURATION") == true) ExerciseType.TIME else ExerciseType.REPS,
                            sets = ex.sets.map { s ->
                                WorkoutSet(
                                    setNumber = s.set_number,
                                    prev = "—",
                                    kg = s.weight.toString(),
                                    reps = s.reps.toString(),
                                    isDone = s.completed
                                )
                            }.toMutableList().ifEmpty { mutableListOf(WorkoutSet(1, "—", "0", "0")) }
                        )
                    })
                    isDraftInitialized = true
                    statsVersion++
                }
            }
        }

        fun validateWorkoutForm(): Boolean {
            val normalizedTitle = workoutTitle.trim()
            if (normalizedTitle.isEmpty()) {
                workoutTitleError = "Workout title is required."
                workoutFormError = null
                return false
            }
            workoutTitleError = null

            if (workoutExercises.isEmpty()) {
                workoutFormError = "Add at least one exercise."
                return false
            }

            val invalidSet = workoutExercises.any { exercise ->
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

            if (invalidSet) {
                workoutFormError =
                    "Please enter valid set values. Reps must be numeric and greater than 0; time must be greater than 0."
                return false
            }

            workoutFormError = null
            return true
        }

        // ── Delete dialog ───────────────────────────────────────────────────
        if (showDeleteDialog) {
            ConfirmationDialog(
                title = "Delete Workout",
                message = "Are you sure you want to delete this workout? This action cannot be undone.",
                confirmText = "Delete",
                isDangerAction = true,
                onConfirm = {
                    showDeleteDialog = false
                    screenModel.deleteWorkout(workoutId!!) {
                        onWorkoutSaved?.invoke()
                        navigator.pop()
                    }
                },
                onDismissRequest = { showDeleteDialog = false }
            )
        }

        // ── Screen ──────────────────────────────────────────────────────────
        Scaffold(
            topBar = {
                GymAppBar(
                    title = if (isViewMode) "Workout Detail" else "Workout Details",
                    onBackClick = { navigator.pop() },
                    actions = {
                        // Delete button (existing workout, editable entry — not from read-only card tap)
                        if (isViewMode && !readOnly) {
                            IconButton(onClick = { showDeleteDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Workout",
                                    tint = Color.Red
                                )
                            }
                        }

                        // Finish button — creates workout only when tapped (routine flow).
                        if (!isViewMode) {
                            TextButton(onClick = {
                                if (!validateWorkoutForm()) return@TextButton
                                val request = CreateWorkoutRequest(
                                    member_id = memberId,
                                    title = workoutTitle.trim(),
                                    notes = "",
                                    created_by = "trainer",

                                    exercises = workoutExercises.map { exercise ->
                                        WorkoutExerciseRequest(

                                            exercise_id = exercise.id,
                                            sets = exercise.sets.map { set ->
                                                WorkoutSetRequest(
                                                    set_number = set.setNumber,
                                                    reps = set.reps.toIntOrNull() ?: 0,
                                                    weight = set.kg.toDoubleOrNull() ?: 0.0
                                                )
                                            }
                                        )
                                    }
                                )
                                screenModel.createWorkout(request) { _ ->
                                    onWorkoutSaved?.invoke()
                                    navigator.pop()
                                    navigator.pop()
                                }
                            }) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.height(16.dp).width(16.dp),
                                        strokeWidth = 2.dp,
                                        color = PrimaryColor
                                    )
                                } else {
                                    Text(
                                        "Finish",
                                        style = AppTextTheme.semiBold.copy(
                                            fontSize = 14.sp,
                                            color = PrimaryColor
                                        )
                                    )
                                }
                            }
                        }

                        // Edit button (view mode, not yet editing)
//                        if (isViewMode) {
//                            TextButton(onClick = { isEditMode = true }) {
//                                Text(
//                                    "Edit",
//                                    style = AppTextTheme.semiBold.copy(
//                                        fontSize = 14.sp,
//                                        color = PrimaryColor
//                                    )
//                                )
//                            }
//                        }
                    }
                )
            },
            bottomBar = {
                // Show "Add More Exercise" only in create/edit mode
                if (!isViewMode ) {
                    Column(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
                        CommonButton(
                            onClick = {
                                navigator.push(AddExerciseScreen(memberId = memberId, onExercisesSelected = { added ->
                                    added.forEach { row ->
                                        workoutExercises.add(
                                            ActiveExercise(
                                                id = row.id,
                                                name = row.name,
                                                icon = Res.drawable.ic_dumble,
                                                assetUrl = row.asset_url,
                                                type = if (row.exercise_type?.contains("DURATION") == true) ExerciseType.TIME else ExerciseType.REPS,
                                                sets = mutableListOf(WorkoutSet(1, "—", "0", "0"))
                                            )
                                        )
                                    }
                                    statsVersion++
                                }))
                            },
                            text = "Add More Exercise",
                            leftIcon = painterResource(Res.drawable.ic_add)
                        )
                    }
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            if (isViewMode && isLoading && workoutExercises.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
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
                                    value = workoutTitle,
                                    onValueChange = {
                                        workoutTitle = it
                                        draftTitle = it
                                        if (workoutTitleError != null && it.trim().isNotEmpty()) {
                                            workoutTitleError = null
                                        }
                                    },
                                    placeholder = "Enter workout title",
                                    readOnly = isViewMode || readOnly,
                                    errorText = workoutTitleError
                                )
                                if (workoutFormError != null) {
                                    Text(
                                        text = workoutFormError!!,
                                        style = AppTextTheme.regular.copy(
                                            fontSize = 12.sp,
                                            color = Color.Red
                                        ),
                                        modifier = Modifier.padding(top = 6.dp, start = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Stats Card
                    item {
                        StaggeredEntranceItem(index = 0) {
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
                                        value = formatDurationFromSeconds(summaryStats.durationSeconds),
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
                                        icon = Res.drawable.ic_volume,
                                        label = "Volume",
                                        value = "${formatKgValue(summaryStats.totalKg)} Kg / ${summaryStats.totalReps} Reps",
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
                    }

                    items(workoutExercises.size) { index ->
                        val exercise = workoutExercises[index]
                        StaggeredEntranceItem(index = index + 1) {
                            ExerciseCard(
                                exercise = exercise,
                                isCreateMode = workoutId == null,
                                readOnly = readOnly,
                                onSetValueChanged = { statsVersion++ },
                                onDelete = {
                                    if (workoutId == null && !readOnly) {
                                        workoutExercises.remove(exercise)
                                        statsVersion++
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
