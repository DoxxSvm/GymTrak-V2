package `in`.gym.trak.studio.features.member

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.core.model.rememberScreenModel
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.data.repository.WorkoutManager
import `in`.gym.trak.studio.theme.*
import gym.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import `in`.gym.trak.studio.data.model.WorkoutExerciseDTO
import `in`.gym.trak.studio.data.model.ActiveExercise
import `in`.gym.trak.studio.data.model.ExerciseType
import `in`.gym.trak.studio.data.model.WorkoutDetailExerciseDTO
import `in`.gym.trak.studio.data.model.WorkoutSet

data class WorkoutExercise(
    val name: String,
    val detail: String,
    val image: org.jetbrains.compose.resources.DrawableResource
)

val sampleExercises = listOf(
    WorkoutExercise("Bent Over Row", "4 Sets × 12 Reps", Res.drawable.ic_dumble),
    WorkoutExercise("Leg Press", "3 Sets × 15 Reps", Res.drawable.ic_dumble),
    WorkoutExercise("Chest Press (Machine)", "4 Sets × 10 Reps", Res.drawable.ic_dumble),
    WorkoutExercise("EZ Bar Biceps Curl", "3 Sets × 12 Reps", Res.drawable.ic_dumble),
    WorkoutExercise("Tricep Pushdown", "3 Sets × 15 Reps", Res.drawable.ic_dumble),
    WorkoutExercise("Cable Flyes", "4 Sets × 12 Reps", Res.drawable.ic_dumble),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailBottomSheet(
    title: String,
    category: String,
    image: org.jetbrains.compose.resources.DrawableResource,
    workoutId: String? = null,
    exercises: List<WorkoutExerciseDTO> = emptyList(),
    screenModel: MemberWorkoutScreenModel,
    onWorkoutFinished: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val navigator = LocalNavigator.currentOrThrow
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE5E7EB))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Back button row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onDismiss() }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier.size(20.dp)
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Workout header card
                item {
//                    CommonCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(image),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = title,
                                    style = AppTextTheme.bold.copy(fontSize = 18.sp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = category,
                                    style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Gray)
                                )
                            }
                        }
//                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Exercise count header
                item {
                    Text(
                        text = "${exercises.size} Workouts",
                        style = AppTextTheme.semiBold.copy(fontSize = 14.sp, color = Color.Black),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // Exercise list
                items(exercises) { workoutExercise ->
                    ExerciseListItem(workoutExercise = workoutExercise)
                    HorizontalDivider(
                        color = GrayBorderColor,
                        thickness = 1.dp,
                        modifier = Modifier.padding(start = 68.dp)
                    )
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
            }

            // Start Workout button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                CommonButton(
                    onClick = {
                        workoutId?.let { id ->
                            screenModel.startWorkout(id) { response ->
                                val sessionId = response.workoutId.ifBlank { id }
                                WorkoutManager.startWorkout(
                                    title = title.trim().ifBlank { "Workout" },
                                    workoutId = sessionId,
                                    exercises = workoutDetailExercisesToActiveForManager(response.exercises),
                                    isServerSessionStarted = true,
                                )
                                onDismiss()
                                navigator.push(
                                    ActiveWorkoutScreen(
                                        workoutId = id,
                                        showSkipSection = true,
                                        initialExercises = response.exercises,
                                        workoutIdForCompletion = sessionId,
                                        useCompleteApiOnFinish = true,
                                        onWorkoutSaved = onWorkoutFinished
                                    )
                                )
                            }
                        }
                    },
                    text = "Start Workout",
                    rightIcon = painterResource(Res.drawable.ic_thunder_filled)
                )
            }
        }
    }
}

private fun workoutDetailExercisesToActiveForManager(
    exercises: List<WorkoutDetailExerciseDTO>
): List<ActiveExercise> =
    exercises.map { dto -> activeExerciseFromDetail(dto) }

@Composable
fun ExerciseListItem(workoutExercise: WorkoutExerciseDTO) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(BlueLightColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_workout),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = workoutExercise.exercise?.name ?: "Unknown Exercise",
                style = AppTextTheme.bold.copy(fontSize = 14.sp, color = Color.Black)
            )
            Spacer(modifier = Modifier.height(2.dp))
            val detail = if (workoutExercise.sets.isNotEmpty()) {
                "${workoutExercise.sets.size} Sets × ${workoutExercise.sets.firstOrNull()?.reps ?: 0} Reps"
            } else {
                "No sets"
            }
            Text(
                text = detail,
                style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
            )
        }
    }
}
