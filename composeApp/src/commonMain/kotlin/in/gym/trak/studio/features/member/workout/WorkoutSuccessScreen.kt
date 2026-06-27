package `in`.gym.trak.studio.features.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.data.model.WorkoutCompletionResponse
import `in`.gym.trak.studio.data.model.WorkoutDetailExerciseDTO
import `in`.gym.trak.studio.data.model.WorkoutDetailResponse
import `in`.gym.trak.studio.utils.DateUtils
import `in`.gym.trak.studio.utils.ShareChannel
import `in`.gym.trak.studio.utils.ShareService
import `in`.gym.trak.studio.theme.*
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import gym.composeapp.generated.resources.*
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.jvm.Transient

class WorkoutSuccessScreen(
    private val workoutId: String = "",
    @Transient private val workout: WorkoutDetailResponse? = null,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val resolvedWorkoutId = workoutId.takeIf { it.isNotBlank() }
            ?: workout?.workoutId.orEmpty()
        val canEditDuration = resolvedWorkoutId.isNotBlank()

        var completion by remember { mutableStateOf<WorkoutCompletionResponse?>(null) }
        var durationMinutes by remember {
            mutableStateOf(parseDurationMinutesFallback(workout?.duration).toString())
        }
        var isLoadingCompletion by remember { mutableStateOf(canEditDuration) }

        LaunchedEffect(resolvedWorkoutId) {
            if (resolvedWorkoutId.isBlank()) {
                isLoadingCompletion = false
                return@LaunchedEffect
            }
            screenModel.loadWorkoutCompletion(
                workoutId = resolvedWorkoutId,
                onSuccess = { data ->
                    completion = data
                    durationMinutes = data.duration_minutes.coerceAtLeast(0).toString()
                    isLoadingCompletion = false
                },
                onError = {
                    durationMinutes = parseDurationMinutesFallback(workout?.duration).toString()
                    isLoadingCompletion = false
                },
            )
        }

        val durationLabel = formatDurationMinutesLabel(durationMinutes)
        val volumeLabel = completion?.volume?.takeIf { it.isNotBlank() }
            ?: workout?.volume?.takeIf { it.isNotBlank() }
            ?: "0 kg"
        val setsLabel = completion?.sets?.toString()
            ?: workout?.let { "${it.sets}" }
            ?: "0"
        val datePill = completion?.date_label?.takeIf { it.isNotBlank() }
            ?: DateUtils.formatNowAsDayShortMonth()
        val workoutTitle = completion?.title?.takeIf { it.isNotBlank() }
            ?: workout?.title?.takeIf { it.isNotBlank() }
        val exercises = completion?.exercises?.takeIf { it.isNotEmpty() }
            ?: workout?.exercises.orEmpty()
        val shareSubject = workoutTitle ?: "Workout"
        val savedDurationMinutes = completion?.duration_minutes
            ?: parseDurationMinutesFallback(workout?.duration)

        fun shareWorkoutImage(channel: ShareChannel) {
            scope.launch {
                val pngBytes = captureWorkoutShareCardImage() ?: return@launch
                ShareService.shareWorkoutImage(
                    pngBytes = pngBytes,
                    channel = channel,
                    subject = shareSubject,
                )
            }
        }

        fun finishScreen() {
            navigator.replaceAll(MemberDashboardScreen())
        }

        fun handleDone() {
            if (!canEditDuration) {
                finishScreen()
                return
            }
            val editedMinutes = durationMinutes.toIntOrNull()?.coerceAtLeast(0) ?: 0
            if (editedMinutes == savedDurationMinutes) {
                finishScreen()
                return
            }
            screenModel.updateWorkoutCompletion(
                workoutId = resolvedWorkoutId,
                durationMinutes = editedMinutes,
                onSuccess = { updated ->
                    completion = updated
                    durationMinutes = updated.duration_minutes.coerceAtLeast(0).toString()
                    finishScreen()
                },
            )
        }

        val isLoading by screenModel.isLoading.collectAsState()

        LoadingScreenHandler(screenModel = screenModel) {
            Scaffold(
                bottomBar = {
                    Box(modifier = Modifier.padding(24.dp).navigationBarsPadding()) {
                        Column (
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ){

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ShareCircleButton(
                                    icon = Res.drawable.ic_share,
                                    iconMaterial = null,
                                    onClick = { shareWorkoutImage(ShareChannel.System) },
                                )
                                ShareCircleButton(
                                    icon = Res.drawable.ic_baseline_whatsapp,
                                    iconMaterial = null,
                                    tint = PrimaryColor,
                                    onClick = { shareWorkoutImage(ShareChannel.WhatsApp) },
                                )
                            }
                            Spacer(modifier = Modifier.height(20.dp))

                            CommonButton(
                                text = if (isLoading) "Saving…" else "Done",
                                onClick = { handleDone() },
                                enabled = !isLoading && !isLoadingCompletion,
                                leftIcon = painterResource(Res.drawable.ic_check),
                            )
                        }

                    }
                },
                containerColor = Color.Transparent,
            ) { paddingValues ->
                if (isLoadingCompletion) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = PrimaryColor)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(modifier = Modifier.height(40.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            NiceWorkoutShareCard(
                                durationLabel = durationLabel,
                                durationMinutes = durationMinutes,
                                onDurationMinutesChange = if (canEditDuration) {
                                    { durationMinutes = it }
                                } else {
                                    null
                                },
                                volumeLabel = volumeLabel,
                                setsLabel = setsLabel,
                                datePill = datePill,
                                workoutTitle = workoutTitle,
                                exercises = exercises,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            NiceWorkoutShareCardCaptureHost(
                                modifier = Modifier
                                    .matchParentSize()
                                    .alpha(0f),
                            ) {
                                NiceWorkoutShareCard(
                                    durationLabel = formatDurationMinutesLabel(durationMinutes),
                                    volumeLabel = volumeLabel,
                                    setsLabel = setsLabel,
                                    datePill = datePill,
                                    workoutTitle = workoutTitle,
                                    exercises = exercises,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))



                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
internal fun NiceWorkoutShareCard(
    durationLabel: String,
    durationMinutes: String = parseDurationMinutesFromLabel(durationLabel).toString(),
    onDurationMinutesChange: ((String) -> Unit)? = null,
    volumeLabel: String,
    setsLabel: String,
    datePill: String,
    workoutTitle: String?,
    exercises: List<WorkoutDetailExerciseDTO>,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .padding(top = 16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF5E5),
                        Color(0xFFF3E8FF),
                    ),
                ),
            )
            .padding(horizontal = 20.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth(),
        ) {

            Text(
                text = "Nice Work!",
                style = AppTextTheme.bold.copy(fontSize = 24.sp, color = Color.Black),
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Workout Completed Successfully",
                style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray),
            )
            if (!workoutTitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = workoutTitle,
                    style = AppTextTheme.semiBold.copy(fontSize = 16.sp, color = Black),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            CommonCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = 0.dp,
                backgroundColor = White,
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50.dp))
                                .background(PrimaryColor)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                        ) {
                            Text(
                                datePill,
                                style = AppTextTheme.medium.copy(fontSize = 12.sp, color = White),
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (onDurationMinutesChange != null) {
                            EditableWorkoutDurationStat(
                                modifier = Modifier.weight(1f),
                                minutes = durationMinutes,
                                onMinutesChange = onDurationMinutesChange,
                            )
                        } else {
                            WorkoutDetailStat(
                                modifier = Modifier.weight(1f),
                                icon = Res.drawable.ic_duration,
                                label = "Duration",
                                value = durationLabel,
                                valueColor = PrimaryColor,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(40.dp)
                                .background(GrayBorderColor),
                        )
                        WorkoutDetailStat(
                            modifier = Modifier.weight(1f),
                            icon = Res.drawable.ic_volume,
                            label = "Volume",
                            value = volumeLabel,
                            valueColor = Black,
                        )
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(40.dp)
                                .background(GrayBorderColor),
                        )
                        WorkoutDetailStat(
                            modifier = Modifier.weight(1f),
                            icon = Res.drawable.ic_sets,
                            label = "Sets",
                            value = setsLabel,
                            valueColor = Gray,
                        )
                    }
                }
            }

            if (exercises.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                CommonCard(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = 0.dp,
                    backgroundColor = White.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text(
                            text = "Exercises",
                            style = AppTextTheme.semiBold.copy(fontSize = 15.sp, color = Black),
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        exercises.forEach { ex ->
                            val setSummary = ex.sets.joinToString { s ->
                                "${s.reps}×${formatWorkoutWeightKg(s.weight)}"
                            }.ifBlank { "—" }
                            Text(
                                text = ex.name,
                                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black),
                            )
                            Text(
                                text = "${ex.sets.size} sets · $setSummary",
                                style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray),
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun formatDurationMinutesLabel(minutesRaw: String): String {
    val minutes = minutesRaw.toIntOrNull()?.coerceAtLeast(0) ?: 0
    return "$minutes min"
}

private fun parseDurationMinutesFallback(raw: String?): Int {
    if (raw.isNullOrBlank()) return 0
    return Regex("(\\d+)").find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
}

private fun parseDurationMinutesFromLabel(label: String): Int =
    parseDurationMinutesFallback(label)

private fun formatWorkoutWeightKg(w: Double): String =
    if (w % 1.0 == 0.0) w.toInt().toString() else ((w * 10).toInt() / 10.0).toString()

@Composable
fun GifImage(resource: DrawableResource) {
    KamelImage(
        resource = asyncPainterResource(resource),
        contentDescription = "Success",
        modifier = Modifier.size(160.dp),
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun ShareCircleButton(
    icon: DrawableResource?,
    iconMaterial: androidx.compose.ui.graphics.vector.ImageVector?,
    tint: Color = Color.Black,
    onClick: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .border(1.dp, Color(0xFFE5E7EB), CircleShape)
            .background(White)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (iconMaterial != null) {
            Icon(
                imageVector = iconMaterial,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
        } else if (icon != null) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
