package `in`.gym.trak.studio.features.member

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.rememberAsyncImagePainter
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.data.model.WorkoutDetailExerciseDTO
import `in`.gym.trak.studio.data.model.WorkoutDetailResponse
import `in`.gym.trak.studio.data.model.createdByDisplayLabel
import `in`.gym.trak.studio.data.model.isDurationExercise
import `in`.gym.trak.studio.data.model.isDurationWeightExercise
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.BlueLightColor
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.GrayBorderColor
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.utils.DateUtils
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_dumble
import gym.composeapp.generated.resources.ic_duration
import gym.composeapp.generated.resources.ic_sets
import gym.composeapp.generated.resources.ic_volume
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.jvm.Transient

class MemberWorkoutLogDetailScreen(
    private val workoutId: String,
    @Transient private val startedAtIso: String? = null,
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { MemberWorkoutScreenModel() }
        val detail by screenModel.workoutDetail.collectAsState()

        LaunchedEffect(workoutId) {
            if (workoutId.isNotBlank()) {
                screenModel.loadWorkoutDetail(workoutId)
            }
        }
        DisposableEffect(Unit) {
            onDispose { screenModel.clearWorkoutDetail() }
        }

        LoadingScreenHandler(screenModel = screenModel) {
            WorkoutLogDetailContent(
                title = detail?.title?.ifBlank { "Workout" } ?: "Workout",
                detail = detail,
                startedAtIso = startedAtIso,
                onBackClick = { navigator?.pop() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutLogDetailContent(
    title: String,
    detail: WorkoutDetailResponse?,
    startedAtIso: String? = null,
    onBackClick: () -> Unit,
) {
    Scaffold(
        containerColor = Color.White,
        topBar = {
            GymAppBar(
                title = title,
                onBackClick = onBackClick,
            )
        },
    ) { padding ->
        if (detail == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = PrimaryColor)
            }
        } else {
            AppScrollableScreen(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                CommonCard(
                    content = {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                WorkoutMetricItem(
                                    label = "Duration",
                                    value = detail.duration?.ifBlank { "—" } ?: "—",
                                    icon = Res.drawable.ic_duration,
                                )
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(40.dp)
                                        .background(GrayBorderColor)
                                        .align(Alignment.CenterVertically),
                                )
                                WorkoutMetricItem(
                                    label = "Volume",
                                    value = detail.volume?.ifBlank { "—" } ?: "—",
                                    icon = Res.drawable.ic_volume,
                                )
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(40.dp)
                                        .background(GrayBorderColor)
                                        .align(Alignment.CenterVertically),
                                )
                                WorkoutMetricItem(
                                    label = "Sets",
                                    value = detail.sets.toString(),
                                    icon = Res.drawable.ic_sets,
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            val whenLabel = workoutLogWhenLabel(startedAtIso)
                            if (whenLabel.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "When:",
                                        style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = whenLabel,
                                        style = AppTextTheme.bold.copy(fontSize = 14.sp, color = PrimaryColor),
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            detail.createdByDisplayLabel()?.let { createdBy ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Created by:",
                                        style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = createdBy,
                                        style = AppTextTheme.bold.copy(fontSize = 14.sp, color = PrimaryColor),
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    },
                )

                Spacer(modifier = Modifier.height(24.dp))

                detail.exercises.forEach { ex ->
                    LogExerciseCardFromDetail(exercise = ex)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

private fun workoutLogWhenLabel(startedAtIso: String?): String {
    if (startedAtIso.isNullOrBlank()) return ""
    val datePart = DateUtils.formatBirthDateForDisplay(startedAtIso)
    val timePart = DateUtils.formatChatTime(startedAtIso)
    return when {
        datePart.isNotBlank() && timePart.isNotBlank() -> "$datePart, $timePart"
        datePart.isNotBlank() -> datePart
        else -> DateUtils.formatShortDateTime(startedAtIso)
    }
}

@Composable
fun WorkoutMetricItem(label: String, value: String, icon: DrawableResource) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = Color.Unspecified,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray),
        )
        Text(
            text = value,
            style = AppTextTheme.bold.copy(fontSize = 16.sp),
        )
    }
}

@Composable
private fun LogExerciseCardFromDetail(exercise: WorkoutDetailExerciseDTO) {
    val isDuration = exercise.isDurationExercise()
    val isDurationWeight = exercise.isDurationWeightExercise()

    Column {
        ExerciseAssetImage(
            assetUrl = exercise.asset_url,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp)),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = exercise.name,
            style = AppTextTheme.semiBold.copy(fontSize = 14.sp),
        )

        exercise.exercise_type?.takeIf { it.isNotBlank() }?.let { type ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = type.replace('_', ' '),
                style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "SET",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray),
            )
            when {
                isDurationWeight && exercise.sets.any { it.reps > 0 } -> {
                    Text(
                        text = "KG",
                        modifier = Modifier.weight(1.5f),
                        textAlign = TextAlign.Center,
                        style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray),
                    )
                    Text(
                        text = "TIME (SEC)",
                        modifier = Modifier.weight(1.5f),
                        textAlign = TextAlign.Center,
                        style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray),
                    )
                }
                isDuration -> {
                    Text(
                        text = "TIME (SEC)",
                        modifier = Modifier.weight(1.5f),
                        textAlign = TextAlign.Center,
                        style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray),
                    )
                }
                else -> {
                    Text(
                        text = "KG",
                        modifier = Modifier.weight(1.5f),
                        textAlign = TextAlign.Center,
                        style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray),
                    )
                    Text(
                        text = "REPS",
                        modifier = Modifier.weight(1.5f),
                        textAlign = TextAlign.Center,
                        style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        exercise.sets.forEach { set ->
            when {
                isDurationWeight && set.reps > 0 -> {
                    LogSetRow(
                        set = set.set_number.toString(),
                        middle = formatWorkoutLogWeight(set.weight),
                        trailing = formatWorkoutLogDuration(set.reps.toDouble()),
                    )
                }
                isDuration -> {
                    LogSetRow(
                        set = set.set_number.toString(),
                        middle = formatWorkoutLogDuration(set.weight),
                        trailing = null,
                    )
                }
                else -> {
                    LogSetRow(
                        set = set.set_number.toString(),
                        middle = formatWorkoutLogWeight(set.weight),
                        trailing = set.reps.toString(),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseAssetImage(
    assetUrl: String?,
    modifier: Modifier = Modifier,
) {
    val hasAsset = !assetUrl.isNullOrBlank()
    Box(
        modifier = modifier.background(BlueLightColor),
        contentAlignment = Alignment.Center,
    ) {
        if (hasAsset) {
            Image(
                painter = rememberAsyncImagePainter(assetUrl),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Icon(
                painter = painterResource(Res.drawable.ic_dumble),
                contentDescription = null,
                tint = PrimaryColor,
                modifier = Modifier.size(72.dp),
            )
        }
    }
}

private fun formatWorkoutLogWeight(weight: Double): String {
    if (weight <= 0.0) return "—"
    return if (weight % 1.0 == 0.0) weight.toInt().toString() else weight.toString()
}

private fun formatWorkoutLogDuration(value: Double): String {
    val seconds = value.toInt()
    if (seconds <= 0) return "—"
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return when {
        minutes > 0 && remainingSeconds > 0 -> "${minutes}m ${remainingSeconds}s"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

@Composable
fun LogSetRow(
    set: String,
    middle: String,
    trailing: String?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = set,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            style = AppTextTheme.bold.copy(fontSize = 16.sp),
        )

        LogSetValueChip(
            value = middle,
            modifier = Modifier.weight(1.5f),
        )

        if (trailing != null) {
            LogSetValueChip(
                value = trailing,
                modifier = Modifier.weight(1.5f),
            )
        }
    }
}

@Composable
private fun LogSetValueChip(
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(horizontal = 4.dp),
        color = BlueLightColor,
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(
            text = value,
            modifier = Modifier.padding(vertical = 12.dp),
            textAlign = TextAlign.Center,
            style = AppTextTheme.medium.copy(fontSize = 16.sp),
        )
    }
}
