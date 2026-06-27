package `in`.gym.trak.studio.features.payments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.base.Constants
import `in`.gym.trak.studio.components.AppScrollableSheetColumn
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.DatePickerField
import `in`.gym.trak.studio.components.SectionLabel
import `in`.gym.trak.studio.components.SubscriptionInfoCard
import `in`.gym.trak.studio.getCurrentTimeMillis
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.PrimaryGreenColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.utils.DateUtils
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Bottom sheet for temporarily freezing a member's subscription.
 * Inputs: freeze start date, duration (days), freeze fee, reason.
 * Displays the auto-calculated resume date at the bottom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FreezeSubscriptionSheet(
    planTitle: String = "",
    dateRangeLabel: String = "",
    /** Base plan price in same units as collect payment. */
    basePriceAmount: Int = 0,
    /** Additional selling component on top of base (API selling_price). */
    sellingPriceAmount: Int? = null,
    /** Existing freeze start (ISO instant or `yyyy-MM-dd`); defaults to today when null. */
    initialFreezeStartDateIso: String? = null,
    initialFreezeFee: String = "0",
    initialReason: String = "",
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onKeepCurrentPlan: () -> Unit = {},
    onFreezeSubscription: (startDate: String, duration: String, freezeFee: String, reason: String) -> Unit = { _, _, _, _ -> },
) {
    val (startDateDisplay, expiryDateDisplay, durationLabel) = remember(dateRangeLabel) {
        parseFreezeDateRange(dateRangeLabel)
    }
    // Parse actual LocalDate boundaries for the date picker
    val subscriptionStartDate = remember(dateRangeLabel) {
        val parts = dateRangeLabel.split(" to ", limit = 2)
        if (parts.size >= 2) parseAnyDate(parts[0].trim()) else null
    }
    val subscriptionExpiryDate = remember(dateRangeLabel) {
        val parts = dateRangeLabel.split(" to ", limit = 2)
        if (parts.size >= 2) parseAnyDate(parts[1].trim()) else null
    }
    val basePrice = basePriceAmount.coerceAtLeast(0)

    val defaultStartStored = remember(initialFreezeStartDateIso) {
        initialFreezeStartDateIso?.takeIf { it.isNotBlank() }
            ?.let { normalizeToStoredIso(it) }
            ?: localDateToStoredIso(todayLocal())
    }

    var freezeStartDateStored by remember(defaultStartStored) {
        mutableStateOf(defaultStartStored)
    }
    val duration = remember(freezeStartDateStored, subscriptionExpiryDate) {
        formatFreezeDurationLabel(freezeDurationDaysFromStart(freezeStartDateStored, subscriptionExpiryDate))
    }
    var freezeFee by remember(initialFreezeFee) {
        mutableStateOf(initialFreezeFee.filter { it.isDigit() }.ifBlank { "0" })
    }
    var reason by remember(initialReason) { mutableStateOf(initialReason) }
    var showDatePicker by remember { mutableStateOf(false) }

    val freezeStartDateDisplay = remember(freezeStartDateStored) {
        storedIsoToPickerDisplay(freezeStartDateStored)
    }

    val resumeDateDisplay = remember(freezeStartDateStored, duration) {
        val start = storedIsoToLocalDate(freezeStartDateStored) ?: return@remember "—"
        val days = parseDurationDays(duration) ?: freezeDurationDaysFromStart(freezeStartDateStored, subscriptionExpiryDate)
        formatMonthDayYear(start.plus(DatePeriod(days = days)))
    }

    if (showDatePicker) {
        val today = todayLocal()
        val initialPickerMillis = storedIsoToLocalDate(freezeStartDateStored)
            ?.let(::localDateToMillis)
            ?: localDateToMillis(today)
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialPickerMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val date = millisToLocalDate(utcTimeMillis)
                    // Freeze date must fall within the subscription's active period
                    return (subscriptionStartDate == null || date >= subscriptionStartDate) &&
                           (subscriptionExpiryDate == null || date <= subscriptionExpiryDate)
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        freezeStartDateStored = Instant.fromEpochMilliseconds(millis).toString()
//                        durationManuallyEdited = false
                    }
                    showDatePicker = false
                }) { Text("OK", color = PrimaryColor) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel", color = Gray) }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AppScrollableSheetColumn {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(48.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color(0xFFE0E0E0))
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Freeze Subscription",
                    style = AppTextTheme.semiBold.copy(fontSize = 16.sp)
                )
                Text(
                    text = "Temporary Membership Pause",
                    style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            IconButton(
                onClick = onDismiss,
                enabled = !isSubmitting,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1F1F1))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SubscriptionInfoCard(
            planName = planTitle.ifBlank { "Current Plan" },
            duration = durationLabel.ifBlank { "—" },
            startDate = startDateDisplay.ifBlank { "—" },
            expiryDate = expiryDateDisplay.ifBlank { "—" },
            basePrice = basePrice.toString(),
//            sellingPriceAmount = sellingPriceAmount,
        )

        Spacer(modifier = Modifier.height(20.dp))

        SectionLabel(text = "Freeze Start Date")
        Spacer(modifier = Modifier.height(8.dp))
        DatePickerField(
            value = freezeStartDateDisplay,
            onPickerClick = { if (!isSubmitting) showDatePicker = true },
        )

        Spacer(modifier = Modifier.height(20.dp))
        SectionLabel(text = "Freeze Fee")
        Spacer(modifier = Modifier.height(8.dp))
        CommonTextField(
            value = freezeFee,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            onValueChange = { raw ->
                freezeFee = raw.filter { it.isDigit() }.ifBlank { "0" }
            },
            placeholder = "0",
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))

        SectionLabel(text = "Reason / Comments")
        Spacer(modifier = Modifier.height(8.dp))
        CommonTextField(
            value = reason,
            onValueChange = { reason = it },
            placeholder = "Enter Reason For Freezing...",
            modifier = Modifier.fillMaxWidth(),
            isMultiline = true,
            singleLine = false,
            enabled = !isSubmitting,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(PrimaryGreenColor.copy(alpha = 0.20f))
                .border(
                    width = 1.dp,
                    color = PrimaryGreenColor.copy(alpha = 0.30f),
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Now Resume Date",
                    style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
                )
                Text(
                    text = resumeDateDisplay,
                    style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        CommonButton(
            enabled = !isSubmitting && freezeStartDateStored.isNotBlank(),
            onClick = {
                onFreezeSubscription(
                    freezeStartDateDisplay,
                    duration,
                    freezeFee,
                    reason,
                )
            },
            text = if (isSubmitting) "Please wait…" else "Freeze Subscription"
        )

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onKeepCurrentPlan,
            enabled = !isSubmitting,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Keep Current Plan Active",
                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun todayLocal(): LocalDate =
    Instant.fromEpochMilliseconds(getCurrentTimeMillis())
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date

private fun millisToLocalDate(utcTimeMillis: Long): LocalDate =
    Instant.fromEpochMilliseconds(utcTimeMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date

private fun localDateToMillis(date: LocalDate): Long =
    date.atTime(0, 0, 0)
        .toInstant(TimeZone.currentSystemDefault())
        .toEpochMilliseconds()

private fun localDateToStoredIso(date: LocalDate): String =
    date.atTime(0, 0, 0)
        .toInstant(TimeZone.currentSystemDefault())
        .toString()

private fun normalizeToStoredIso(raw: String): String {
    val trimmed = raw.trim()
    DateUtils.birthDateToIsoDateOnly(trimmed)?.let { isoDateOnly ->
        return runCatching { LocalDate.parse(isoDateOnly) }
            .getOrNull()
            ?.let(::localDateToStoredIso)
            ?: trimmed
    }
    return storedIsoToLocalDate(trimmed)?.let(::localDateToStoredIso) ?: trimmed
}

private fun storedIsoToLocalDate(stored: String): LocalDate? {
    val trimmed = stored.trim()
    if (trimmed.isEmpty()) return null
    val dateOnly = trimmed.substringBefore("T").take(10)
    if (dateOnly.length == 10 && dateOnly[4] == '-' && dateOnly[7] == '-') {
        return runCatching { LocalDate.parse(dateOnly) }.getOrNull()
    }
    return runCatching {
        Instant.parse(trimmed).toLocalDateTime(TimeZone.currentSystemDefault()).date
    }.getOrNull()
}

private fun storedIsoToPickerDisplay(stored: String): String {
    val date = storedIsoToLocalDate(stored) ?: return stored
    return "${date.dayOfMonth.toString().padStart(2, '0')} / ${
        date.monthNumber.toString().padStart(2, '0')
    } / ${date.year}"
}

/** Duration in days = expiry date − freeze start date. */
private fun freezeDurationDaysFromStart(startStored: String, expiryDate: LocalDate?): Int {
    val start = storedIsoToLocalDate(startStored) ?: return 0
    val end = expiryDate ?: return 0
    return (end.toEpochDays() - start.toEpochDays()).toInt().coerceAtLeast(1)
}

private fun formatFreezeDurationLabel(days: Int): String = when (days) {
    0 -> "0 Days"
    1 -> "1 Day"
    else -> "$days Days"
}

private fun parseDurationDays(duration: String): Int? {
    val digits = duration.filter { it.isDigit() }
    if (digits.isEmpty()) return null
    return digits.toIntOrNull()
}

private fun parseMonthDayYear(raw: String): LocalDate? {
    val clean = raw.replace(",", " ").trim()
    val parts = clean.split(" ").filter { it.isNotBlank() }
    if (parts.size != 3) return null
    val monthStr = parts[0].take(3).lowercase()
    val month = when (monthStr) {
        "jan" -> 1
        "feb" -> 2
        "mar" -> 3
        "apr" -> 4
        "may" -> 5
        "jun" -> 6
        "jul" -> 7
        "aug" -> 8
        "sep" -> 9
        "oct" -> 10
        "nov" -> 11
        "dec" -> 12
        else -> return null
    }
    val day = parts[1].toIntOrNull() ?: return null
    val year = parts[2].toIntOrNull() ?: return null
    return runCatching { LocalDate(year, month, day) }.getOrNull()
}

private fun parseAnyDate(raw: String): LocalDate? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    parseMonthDayYear(trimmed)?.let { return it }
    parseSlashDate(trimmed)?.let { return it }
    val dateOnly = trimmed.substringBefore("T").take(10)
    if (dateOnly.length == 10 && dateOnly[4] == '-' && dateOnly[7] == '-') {
        runCatching { LocalDate.parse(dateOnly) }.getOrNull()?.let { return it }
    }
    return null
}

private fun parseSlashDate(raw: String): LocalDate? {
    val seg = raw.trim().split("/")
    if (seg.size != 3) return null
    val day = seg[0].toIntOrNull() ?: return null
    val month = seg[1].toIntOrNull() ?: return null
    val year = seg[2].toIntOrNull() ?: return null
    return runCatching { LocalDate(year, month, day) }.getOrNull()
}

private fun formatMonthDayYear(date: LocalDate): String {
    return "${shortMonthName(date.monthNumber)} ${date.dayOfMonth}, ${date.year}"
}

private fun parseFreezeDateRange(dateRangeLabel: String): Triple<String, String, String> {
    val parts = dateRangeLabel.split(" to ", limit = 2)
    if (parts.size < 2) {
        return Triple(dateRangeLabel, "", "")
    }

    val startRaw = parts[0].trim()
    val expiryRaw = parts[1].trim()

    fun toMonthDayYear(raw: String): String {
        return parseAnyDate(raw)?.let { formatMonthDayYear(it) } ?: raw
    }

    fun computeMonths(rawStart: String, rawEnd: String): String {
        val start = parseAnyDate(rawStart) ?: return "—"
        val end = parseAnyDate(rawEnd) ?: return "—"
        val months = (end.year - start.year) * 12 + (end.monthNumber - start.monthNumber)
        return when {
            months >= 12 -> "12 Months"
            months > 0 -> "$months Months"
            else -> "—"
        }
    }

    return Triple(
        toMonthDayYear(startRaw),
        toMonthDayYear(expiryRaw),
        computeMonths(startRaw, expiryRaw),
    )
}

private fun shortMonthName(monthNumber: Int): String = when (monthNumber) {
    1 -> "Jan"
    2 -> "Feb"
    3 -> "Mar"
    4 -> "Apr"
    5 -> "May"
    6 -> "Jun"
    7 -> "Jul"
    8 -> "Aug"
    9 -> "Sep"
    10 -> "Oct"
    11 -> "Nov"
    12 -> "Dec"
    else -> ""
}
