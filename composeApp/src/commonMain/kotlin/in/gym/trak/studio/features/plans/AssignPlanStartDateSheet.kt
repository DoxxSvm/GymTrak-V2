package `in`.gym.trak.studio.features.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.DatePickerField
import `in`.gym.trak.studio.components.SectionLabel
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.utils.DateUtils
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Bottom sheet content: pick subscription [startDateIso] for a plan, then confirm.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignPlanStartDateSheet(
    planId: String,
    planName: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (startDateIso: String) -> Unit,
) {
    var startDateIso by remember(planId) { mutableStateOf(DateUtils.getCurrentDateIso()) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val instant = Instant.fromEpochMilliseconds(it)
                            startDateIso = instant.toString()
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val dateDisplay = DateUtils.formatBirthDateForDisplay(startDateIso).ifBlank {
        runCatching {
            Instant.parse(startDateIso).toLocalDateTime(TimeZone.currentSystemDefault()).let { ldt ->
                "${ldt.dayOfMonth.toString().padStart(2, '0')}/${
                    ldt.monthNumber.toString().padStart(2, '0')
                }/${ldt.year}"
            }
        }.getOrElse { "" }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color.White)
            .padding(24.dp)
            .padding(bottom = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(48.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color(0xFFE0E0E0))
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Assign plan",
                    style = AppTextTheme.semiBold.copy(fontSize = 18.sp, color = Black)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = planName,
                    style = AppTextTheme.medium.copy(fontSize = 15.sp, color = Gray)
                )
            }
            IconButton(
                onClick = onDismiss,
                enabled = !isSubmitting,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Black
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        SectionLabel(text = "Start date")
        Spacer(modifier = Modifier.height(8.dp))
        DatePickerField(
            value = dateDisplay,
            onPickerClick = { if (!isSubmitting) showDatePicker = true }
        )
        Spacer(modifier = Modifier.height(24.dp))
        CommonButton(
            onClick = { onConfirm(startDateIso) },
            text = if (isSubmitting) "Please wait…" else "Confirm",
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
