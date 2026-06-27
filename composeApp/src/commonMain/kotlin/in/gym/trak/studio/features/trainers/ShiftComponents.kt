package `in`.gym.trak.studio.features.trainers

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.formatTime
import `in`.gym.trak.studio.theme.*
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_clock
import org.jetbrains.compose.resources.painterResource

data class ShiftData(val title: String, val time: String, val dayOfWeek: Int = 1)

@Composable
fun ShiftListItem(
    shift: ShiftData,
    onDelete: () -> Unit,
    onEdit: () -> Unit,
    /** When true, hide the weekday label (e.g. batch plan time windows). */
    suppressDayLabel: Boolean = false
) {
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val dayName = daysOfWeek.getOrNull(shift.dayOfWeek ) ?: "Mon"

    // Convert 24h format from state to readable format for UI
    val displayTime = remember(shift.time) {
        try {
            val parts = shift.time.split(" to ")
            if (parts.size == 2) {
                fun toAmPm(t: String): String {
                    val tParts = t.split(":")
                    if (tParts.size == 2) {
                        val h = tParts[0].toIntOrNull() ?: return t
                        val m = tParts[1].toIntOrNull() ?: return t
                        return formatTime(h, m)
                    }
                    return t
                }
                "${toAmPm(parts[0])} to ${toAmPm(parts[1])}"
            } else shift.time
        } catch (e: Exception) {
            shift.time
        }
    }

    CommonCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = White,
        borderColor = Color(0xFFF1F5F9),
        elevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Clock Icon Box
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(PrimaryColor, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_clock),
                    contentDescription = "Time",
                    tint = White,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Top Row: Day + Shift Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!suppressDayLabel) {
                        Text(
                            text = dayName,
                            style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Box(
                        modifier = Modifier
                            .background(
                                PrimaryColor.copy(alpha = 0.10f),
                                RoundedCornerShape(100.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = shift.title,
                            style = AppTextTheme.medium.copy(
                                fontSize = 12.sp,
                                color = Black.copy(alpha = 0.60f)
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Row: Time tag
                Box(
                    modifier = Modifier
                        .background(BlueLightColor, RoundedCornerShape(100.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = displayTime,
                        style = AppTextTheme.medium.copy(fontSize = 13.sp, color = Color.Black)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color(0xFFDC2828),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LabeledField(
    label: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = AppTextTheme.bold.copy(fontSize = 14.sp, color = Black),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        content()
    }
}
