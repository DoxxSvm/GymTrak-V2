package `in`.gym.trak.studio.features.trainers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.TimePickerModal
import `in`.gym.trak.studio.components.formatTime
import `in`.gym.trak.studio.theme.*

/** Trainer: at least one day required. Batch plan: time window only (days live on batch_details.working_days). */
enum class AddShiftDaySelection {
    Required,
    None
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddShiftDialog(
    onDismiss: () -> Unit,
    onAddShift: (String, String, String, List<Int>) -> Unit,
    initialDays: List<Int> = emptyList(),
    initialStartTime: String = "08:00",
    initialEndTime: String = "09:00",
    isEdit: Boolean = false,
    daySelection: AddShiftDaySelection = AddShiftDaySelection.Required
) {
    var shiftTitle by remember { mutableStateOf(if (isEdit) "Edit Shift" else "Shift 1") }
    
    // Day Selection State (Indexed)
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    var selectedDayIndices by remember { mutableStateOf(initialDays.map { it - 1 }.toSet()) }

    // Parse initial times
    val parsedStartHour = initialStartTime.split(":").getOrNull(0)?.toIntOrNull() ?: 8
    val parsedStartMinute = initialStartTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0
    val parsedEndHour = initialEndTime.split(":").getOrNull(0)?.toIntOrNull() ?: 9
    val parsedEndMinute = initialEndTime.split(":").getOrNull(1)?.toIntOrNull() ?: 0

    // Time picker states
    var startHour by remember { mutableIntStateOf(parsedStartHour) }
    var startMinute by remember { mutableIntStateOf(parsedStartMinute) }
    var endHour by remember { mutableIntStateOf(parsedEndHour) }
    var endMinute by remember { mutableIntStateOf(parsedEndMinute) }

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val startTimeString = formatTime24(startHour, startMinute)
    val endTimeString = formatTime24(endHour, endMinute)

    val startTimeInMinutes = startHour * 60 + startMinute
    val endTimeInMinutes = endHour * 60 + endMinute
    val durationMinutes = endTimeInMinutes - startTimeInMinutes
    val daysOk = when (daySelection) {
        AddShiftDaySelection.Required -> selectedDayIndices.isNotEmpty()
        AddShiftDaySelection.None -> true
    }
    val isValid = durationMinutes >= 30 && daysOk && endTimeInMinutes > startTimeInMinutes

    // Start Time Picker Dialog
    if (showStartTimePicker) {
        TimePickerModal(
            title = "Start time",
            initialHour = startHour,
            initialMinute = startMinute,
            onClose = { showStartTimePicker = false },
            onSelect = { h, m ->
                startHour = h
                startMinute = m
                showStartTimePicker = false
            }
        )
    }

    // End Time Picker Dialog
    if (showEndTimePicker) {
        TimePickerModal(
            title = "End time",
            initialHour = endHour,
            initialMinute = endMinute,
            onClose = { showEndTimePicker = false },
            onSelect = { h, m ->
                endHour = h
                endMinute = m
                showEndTimePicker = false
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = White)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(PrimaryColor.copy(alpha = 0.10f), RoundedCornerShape(100.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = shiftTitle,
                            style = AppTextTheme.medium.copy(fontSize = 14.sp, color = PrimaryDarkColor)
                        )
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Black)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (daySelection == AddShiftDaySelection.Required) {
                    // Day Selection (Horizontal Scrollable)
                    Text(text = "Day", style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        days.forEachIndexed { index, day ->
                            val isSelected = selectedDayIndices.contains(index)
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) PrimaryColor else White)
                                    .border(1.dp, if (isSelected) PrimaryColor else TextFiledBorderColor, CircleShape)
                                    .clickable {
                                        selectedDayIndices = if (isSelected) {
                                            selectedDayIndices - index
                                        } else {
                                            selectedDayIndices + index
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day,
                                    style = AppTextTheme.bold.copy(
                                        fontSize = 14.sp,
                                        color = if (isSelected) White else Black
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Time selection boxes
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TimeSelectionBox(
                        label = "Start time",
                        time = startTimeString,
                        modifier = Modifier.weight(1f),
                        onClick = { showStartTimePicker = true }
                    )
                    TimeSelectionBox(
                        label = "End time",
                        time = endTimeString,
                        modifier = Modifier.weight(1f),
                        onClick = { showEndTimePicker = true }
                    )
                }

                if (!isValid) {
                    val errorText = when {
                        endTimeInMinutes <= startTimeInMinutes -> "End time should be later than start time"
                        durationMinutes < 30 -> "Shift duration should be at least 30 minutes"
                        daySelection == AddShiftDaySelection.Required && selectedDayIndices.isEmpty() ->
                            "Select at least one day"

                        else -> ""
                    }
                    if (errorText.isNotEmpty()) {
                        Text(
                            text = errorText,
                            color = RedColor,
                            style = AppTextTheme.regular.copy(fontSize = 12.sp),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                CommonButton(
                    onClick = {
                        if (isValid) {
                            val dayInts = when (daySelection) {
                                AddShiftDaySelection.Required -> selectedDayIndices.map { it + 1 }.sorted()
                                AddShiftDaySelection.None -> emptyList()
                            }
                            onAddShift(
                                shiftTitle,
                                startTimeString,
                                endTimeString,
                                dayInts
                            )
                            onDismiss()
                        }
                    },
                    text = if (isEdit) "Update Shift" else "Add Shift",
                    color = if (isValid) PrimaryColor else Gray,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                )
            }
        }
    }
}


fun formatTime24(hour: Int, minute: Int): String {
    return "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
}

@Composable
fun TimeSelectionBox(
    label: String,
    time: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = AppTextTheme.bold.copy(fontSize = 15.sp, color = Black),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFFF8F9FA),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
        ) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = time,
                    style = AppTextTheme.medium.copy(fontSize = 15.sp, color = Black.copy(alpha = 0.6f))
                )
            }
        }
    }
}

