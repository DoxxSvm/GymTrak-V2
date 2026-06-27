package `in`.gym.trak.studio.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import `in`.gym.trak.studio.data.repository.WorkoutManager
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White

@Composable
fun ActiveWorkoutOverlay(
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val activeWorkout by WorkoutManager.activeWorkout.collectAsState()
    val bottomPadding by WorkoutManager.bottomPaddingFlow.collectAsState()
    // Server-backed session (set when Start Workout API succeeds, or synced from ActiveWorkoutScreen).
    val showOverlay = activeWorkout?.workoutId?.isNotBlank() == true &&
        activeWorkout?.showFloatingOverlay != false

    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    AnimatedVisibility(
        visible = showOverlay,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        activeWorkout?.let { workout ->
            val elapsedMillis = WorkoutManager.elapsedMillis(workout)
            val seconds = (elapsedMillis / 1000) % 60
            val minutes = (elapsedMillis / (1000 * 60)) % 60
            val hours = (elapsedMillis / (1000 * 60 * 60))
            
            val timerText = if (hours > 0) {
                "${hours}h ${minutes}m"
            } else {
                "${minutes}min ${seconds}s"
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(bottom = bottomPadding),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier
                        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                        .pointerInput(Unit) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                offsetX += dragAmount.x
                                offsetY += dragAmount.y
                            }
                        }
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .height(72.dp)
                        .shadow(12.dp, RoundedCornerShape(36.dp))
                        .clip(RoundedCornerShape(36.dp))
                        .background(Color(0xFFE2F9F0)) // Light green from design
                        .clickable { onClick() }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Expand arrow
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(White),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = workout.title,
                                style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Color.Black)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = timerText,
                                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Color(0xFF22C55E))
                            )
                        }
                        val currentExercise = workout.exercises.firstOrNull()?.name ?: "Resting"
                        Text(
                            text = currentExercise,
                            style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Color.Gray)
                        )
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete workout",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
