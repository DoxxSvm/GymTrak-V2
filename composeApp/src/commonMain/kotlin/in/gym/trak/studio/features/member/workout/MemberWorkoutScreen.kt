package `in`.gym.trak.studio.features.member

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.components.AppEmptyStateView
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.ConfirmationDialog
import `in`.gym.trak.studio.components.CommonOutlineButton
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_add
import gym.composeapp.generated.resources.ic_play
import gym.composeapp.generated.resources.ic_workout
import gym.composeapp.generated.resources.img_dummy_product
import gym.composeapp.generated.resources.img_no_wrokout
import gym.composeapp.generated.resources.img_workout
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import `in`.gym.trak.studio.data.model.WorkoutExerciseDTO
import `in`.gym.trak.studio.data.model.WorkoutDetailExerciseDTO
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import `in`.gym.trak.studio.data.model.WorkoutDTO
import `in`.gym.trak.studio.data.model.createdByDisplayLabel

private fun workoutFilterLabelToCreatedBy(label: String): String = when (label) {
    "My Workout" -> "member"
    "Trainer" -> "trainer"
    "Recommended", "All" -> "all"
    else -> "all"
}

@Composable
fun MemberWorkoutScreen() {
    val navigator = LocalNavigator.currentOrThrow
    val screenModel = remember { MemberWorkoutScreenModel() }
    val workouts by screenModel.workouts.collectAsState()
    val isRefreshing by screenModel.isRefreshing.collectAsState()
    var selectedFilter by remember { mutableStateOf("All") }
    val filters = listOf("All", "My Workout", "Trainer", "Recommended")
    var showWorkoutSheet by remember { mutableStateOf(false) }
    var selectedWorkoutId by remember { mutableStateOf("") }
    var selectedWorkoutTitle by remember { mutableStateOf("") }
    var selectedWorkoutCategory by remember { mutableStateOf("") }
    var selectedWorkoutImage by remember { mutableStateOf(Res.drawable.img_dummy_product) }
    var selectedWorkoutExercises by remember { mutableStateOf<List<WorkoutExerciseDTO>>(emptyList()) }
    var workoutPendingDelete by remember { mutableStateOf<WorkoutDTO?>(null) }

    LaunchedEffect(selectedFilter) {
        screenModel.loadWorkouts(
            showFullLoader = true,
            createdBy = workoutFilterLabelToCreatedBy(selectedFilter),
        )
    }

    LoadingScreenHandler(screenModel = screenModel) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { screenModel.refresh() },
            state = rememberPullToRefreshState(),
            indicator = {}
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text("Workout", style = AppTextTheme.bold.copy(fontSize = 24.sp))
                }

                item {
                    CommonButton(
                        onClick = {
                            navigator.push(
                                ActiveWorkoutScreen(
                                    showSkipSection = false,
                                    memberIdForCreate = SessionManager.userId,
                                    useCompleteApiOnFinish = false,
                                    autoCreateOnFirstExercise = true,
                                    onWorkoutSaved = { screenModel.loadWorkouts(showFullLoader = false) },
                                )
                            )
                        },
                        text = "Start Empty Workout",
                        leftIcon = painterResource(Res.drawable.ic_add)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    CommonOutlineButton(
                        onClick = {
                            val memberId = SessionManager.userId
                            navigator.push(
                                AddExerciseScreen(
                                    memberId = memberId,
                                    onExercisesSelected = { selected ->
                                        navigator.push(
                                            ActiveWorkoutScreen(
                                                showSkipSection = false,
                                                memberIdForCreate = memberId,
                                                autoCreateOnFirstExercise = false,
                                                manageLiveSession = false,
                                                initialExerciseRows = selected,
                                                workoutTitle = "New Routine",
                                                onWorkoutCreated = {
                                                    screenModel.loadWorkouts(showFullLoader = false)
                                                    screenModel.showSuccessToast("Workout Created Successfully")
                                                },
                                            ),
                                        )
                                    },
                                ),
                            )
                        },
                        text = "Create New Routine",
                        textColor = Black,
                        borderColor = Color.Transparent
                    )
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(filters) { filter ->
                            FilterChip(
                                label = filter,
                                isSelected = selectedFilter == filter,
                                onClick = { selectedFilter = filter }
                            )
                        }
                    }
                }

                item {
                    AnimatedContent(
                        targetState = workouts.isEmpty(),
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "workoutListState"
                    ) { isEmpty ->
                        if (isEmpty) {
                            AppEmptyStateView(
                                image = Res.drawable.img_no_wrokout,
                                title = "No Workout Yet",
                                subtitle = "Your personal workout history will appear here."
                            )
                        }
                    }
                }

                if (workouts.isNotEmpty()) {
                    items(workouts, key = { it.workoutId }) { workout ->
                        StaggeredEntranceItem(index = workouts.indexOf(workout)) {
                            WorkoutCard(
                                title = workout.title?.ifBlank { "Workout" } ?: "Workout",
                                exercises = "${workout.exercise_count ?: 0} Exercises",
                                createdBy = workout.createdByDisplayLabel(),
                                image = Res.drawable.img_workout,
                                onEditClick = {
                                    val editExercises = workout.exercises.map { ex ->
                                        WorkoutDetailExerciseDTO(
                                            exercise_id_alt = ex.exerciseIdResolved.ifBlank { ex.id ?: "" },
                                            name = ex.exercise?.name ?: "Exercise",
                                            asset_url_alt = ex.exercise?.asset_url,
                                            exercise_type_alt = ex.exercise?.exercise_type,
                                            sets = ex.sets
                                        )
                                    }
                                    navigator.push(
                                        ActiveWorkoutScreen(
                                            showSkipSection = false,
                                            workoutIdForCompletion = workout.workoutId,
                                            useCompleteApiOnFinish = false,
                                            isEditMode = true,
                                            workoutTitle = workout.title?.ifBlank { "Workout" } ?: "Workout",
                                            initialExercises = editExercises,
                                            onWorkoutSaved = { screenModel.loadWorkouts(showFullLoader = false) }
                                        )
                                    )
                                },
                                onDeleteClick = { workoutPendingDelete = workout },
                                onClick = {
                                    selectedWorkoutId = workout.workoutId
                                    selectedWorkoutTitle = workout.title?.ifBlank { "Workout" } ?: "Workout"
                                    selectedWorkoutCategory = "Personal"
                                    selectedWorkoutImage = Res.drawable.img_workout
                                    selectedWorkoutExercises = workout.exercises
                                    showWorkoutSheet = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showWorkoutSheet) {
        WorkoutDetailBottomSheet(
            title = selectedWorkoutTitle,
            category = selectedWorkoutCategory,
            image = selectedWorkoutImage,
            workoutId = selectedWorkoutId.takeIf { it.isNotBlank() },
            exercises = selectedWorkoutExercises,
            screenModel = screenModel,
            onWorkoutFinished = { screenModel.loadWorkouts(showFullLoader = false) },
            onDismiss = { showWorkoutSheet = false }
        )
    }

    if (workoutPendingDelete != null) {
        ConfirmationDialog(
            onDismissRequest = { workoutPendingDelete = null },
            onConfirm = {
                val id = workoutPendingDelete?.workoutId.orEmpty()
                workoutPendingDelete = null
                if (id.isNotBlank()) {
                    screenModel.deleteWorkoutLegacy(id) {
                        screenModel.loadWorkouts(showFullLoader = false)
                    }
                }
            },
            title = "Delete Workout",
            message = "Are you sure you want to delete this workout? This action cannot be undone.",
            confirmText = "Delete",
            isDangerAction = true
        )
    }
}


@Composable
fun WorkoutCard(
    title: String,
    exercises: String,
    image: DrawableResource,
    createdBy: String? = null,
    showPlayAndMenu: Boolean = true,
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    CommonCard(shape = RoundedCornerShape(24.dp), modifier = Modifier.clickable { onClick() }) {
        Column {
            Image(
                painter = painterResource(image),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentScale = ContentScale.Crop
            )
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = AppTextTheme.bold.copy(fontSize = 18.sp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(Res.drawable.ic_workout), null,  modifier = Modifier.size(14.dp), Color.Unspecified)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(exercises, color = Gray, style = AppTextTheme.regular.copy(fontSize = 12.sp))
                    }
                    createdBy?.takeIf { it.isNotBlank() }?.let { author ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Created by $author",
                            color = Gray,
                            style = AppTextTheme.regular.copy(fontSize = 12.sp),
                            maxLines = 1,
                        )
                    }
                }
                if (showPlayAndMenu) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painterResource(Res.drawable.ic_play), null, tint = White, modifier = Modifier.size(20.dp))
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More",
                                    tint = Gray
                                )
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Edit") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onEditClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Delete") },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444)
                                        )
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDeleteClick()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

