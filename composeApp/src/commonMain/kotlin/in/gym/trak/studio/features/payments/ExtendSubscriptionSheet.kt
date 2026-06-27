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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.base.Constants
import `in`.gym.trak.studio.components.AppScrollableSheetColumn
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.DatePickerField
import `in`.gym.trak.studio.components.PaymentModeSelector
import `in`.gym.trak.studio.components.SectionLabel
import `in`.gym.trak.studio.components.SubscriptionInfoCard
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.PrimaryGreenColor
import `in`.gym.trak.studio.theme.RedDarkColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.getCurrentTimeMillis
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Extend-plan payment: same sheet layout as [FreezeSubscriptionSheet], with extend-specific fields.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtendSubscriptionSheet(
    planTitle: String,
    dateRangeLabel: String,
    /** Agreed selling price shown read-only (from subscription / plan). */
    displaySellingAmount: Int,
    /** Pre-filled extension fee for this payment. */
    defaultFeesAmount: Int = 0,
    isRenewPlan: Boolean = false,
    initialPaymentDateStored: String? = null,
    /** Exact plan duration in days (from PlanDTO.durationDays). Used to auto-calculate expiry date. */
    planDurationDays: Int? = null,
    isSubmitting: Boolean = false,
    onDismiss: () -> Unit,
    onConfirm: (
        paymentMode: String,
        paymentDateStored: String,
        sellingPrice: Int,
        additionFee: Int,
        startDateStored: String?,
    ) -> Unit,
) {
    var selectedMode by remember { mutableStateOf("Cash") }
    val minExtendDate = remember(dateRangeLabel) { parseExpiryFromDateRangeLabel(dateRangeLabel) }
    val minSelectableExtendDate = remember(minExtendDate) {
        minExtendDate?.plus(1, DateTimeUnit.DAY)
    }
    // True when the subscription has already passed its expiry date
    val isPlanExpired = remember(minExtendDate) {
        minExtendDate != null && minExtendDate < todayLocal()
    }
    var startDateStored by remember(dateRangeLabel, initialPaymentDateStored, minExtendDate) {
        mutableStateOf(
            run {
                val baseStart = initialPaymentDateStored?.takeIf { it.isNotBlank() }
                    ?: defaultExtendPaymentDate(dateRangeLabel)
                if (isRenewPlan && minExtendDate != null) {
                    val startLocalDate = paymentDateToLocalDate(baseStart) ?: todayLocal()
                    if (startLocalDate < minExtendDate) {
                        localDateToStoredIso(minExtendDate)
                    } else {
                        baseStart
                    }
                } else {
                    baseStart
                }
            }
        )
    }
    val durationMonths = remember(dateRangeLabel) {
        val parts = dateRangeLabel.split(" to ", limit = 2)
        if (parts.size >= 2) {
            val start = parseAnyDate(parts[0].trim())
            val end = parseAnyDate(parts[1].trim())
            if (start != null && end != null) {
                val m = (end.year - start.year) * 12 + (end.monthNumber - start.monthNumber)
                if (m > 0) m else 1
            } else 1
        } else 1
    }
    // Max selectable date for extend = current expiry + one plan duration (prevents over-extension)
    val maxExtendDate = remember(minExtendDate, planDurationDays, durationMonths) {
        minExtendDate?.let { min ->
            if (planDurationDays != null && planDurationDays > 0) {
                min.plus(planDurationDays, DateTimeUnit.DAY)
            } else {
                min.plus(durationMonths, DateTimeUnit.MONTH)
            }
        }
    }
    var paymentDate by remember(startDateStored, planDurationDays, isRenewPlan) {
        mutableStateOf(
            if (isRenewPlan) {
                val start = paymentDateToLocalDate(startDateStored) ?: todayLocal()
                if (planDurationDays != null && planDurationDays > 0) {
                    // Exact day-based calculation from plan data
                    localDateToStoredIso(start.plus(planDurationDays, DateTimeUnit.DAY))
                } else {
                    // Fallback: month approximation from date range label
                    localDateToStoredIso(start.plus(durationMonths, DateTimeUnit.MONTH))
                }
            } else {
                initialPaymentDateStored?.takeIf { it.isNotBlank() }
                    ?: defaultExtendPaymentDate(dateRangeLabel)
            }
        )
    }
    var extendDateError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var fees by remember(defaultFeesAmount) {
        mutableStateOf(defaultFeesAmount.coerceAtLeast(0).toString())
    }
    var comment by remember { mutableStateOf("") }

    if (showDatePicker) {
        val initialPickerMillis = paymentDateToLocalDate(paymentDate)
            ?.let(::localDateToMillis)
            ?: minSelectableExtendDate?.let(::localDateToMillis)
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialPickerMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val selectedDate = millisToLocalDate(utcTimeMillis)
                    if (isRenewPlan) {
                        val start = paymentDateToLocalDate(startDateStored) ?: return true
                        return selectedDate > start
                    }
                    val minDate = minExtendDate ?: return true
                    return selectedDate >= minDate && (maxExtendDate == null || selectedDate <= maxExtendDate)
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate = datePickerState.selectedDateMillis?.let(::millisToLocalDate)
                    val start = paymentDateToLocalDate(startDateStored)
                    when {
                        selectedDate == null -> {
                            extendDateError = "Please select a date"
                        }
                        isRenewPlan && start != null && selectedDate <= start -> {
                            extendDateError = "Expiry date must be after the start date"
                        }
                        !isRenewPlan && minExtendDate != null && selectedDate < minExtendDate -> {
                            extendDateError =
                                "Extension date must be on or after the current expiry date (${formatMonthDayYear(minExtendDate)})"
                        }
                        else -> {
                            paymentDate = Instant.fromEpochMilliseconds(
                                datePickerState.selectedDateMillis!!
                            ).toString()
                            extendDateError = null
                        }
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showStartDatePicker) {
        val initialPickerMillis = paymentDateToLocalDate(startDateStored)
            ?.let(::localDateToMillis)
            ?: todayLocal().let(::localDateToMillis)
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialPickerMillis,
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val selectedDate = millisToLocalDate(utcTimeMillis)
                    val minDate = minExtendDate ?: return true
                    return selectedDate >= minDate
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDate = datePickerState.selectedDateMillis?.let(::millisToLocalDate)
                    if (selectedDate != null) {
                        if (minExtendDate != null && selectedDate < minExtendDate) {
                            extendDateError = "Start date must be on or after the current expiry date (${formatMonthDayYear(minExtendDate)})"
                            return@TextButton
                        }
                        startDateStored = localDateToStoredIso(selectedDate)
                        // Use exact plan duration in days if available; fall back to month approximation
                        val calculatedExpiry = if (planDurationDays != null && planDurationDays > 0) {
                            selectedDate.plus(planDurationDays, DateTimeUnit.DAY)
                        } else {
                            selectedDate.plus(durationMonths, DateTimeUnit.MONTH)
                        }
                        paymentDate = localDateToStoredIso(calculatedExpiry)
                        extendDateError = null
                    }
                    showStartDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    val (startDateDisplay, expiryDateDisplay, durationLabel) = remember(dateRangeLabel) {
        parseExtendDateRange(dateRangeLabel)
    }

    val paymentDateDisplay = remember(paymentDate) {
        try {
            val instant = Instant.parse(paymentDate)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            "${localDateTime.dayOfMonth} / ${localDateTime.monthNumber} / ${localDateTime.year}"
        } catch (_: Exception) {
            paymentDate
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
                    text = if (isRenewPlan) "Renew Plan" else "Extend Plan",
                    style = AppTextTheme.semiBold.copy(fontSize = 16.sp)
                )
                Text(
                    text = "Extend Membership Payment",
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
            // When the plan is already expired, show today's date as the effective expiry
            expiryDate = if (isPlanExpired) {
                todayLocal().let { formatMonthDayYear(it) }
            } else {
                expiryDateDisplay.ifBlank { "—" }
            },
            basePrice = displaySellingAmount.takeIf { it > 0 }?.let { "${Constants.RUPEE} $it" },
            basePriceLabel = "Selling Price",
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (isRenewPlan) {
            SectionLabel(text = "Validity Period")
            Spacer(modifier = Modifier.height(8.dp))
            androidx.compose.material3.Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8ECF4))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Start Date column
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Start Date",
                                style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            DatePickerField(
                                value = remember(startDateStored) {
                                    try {
                                        val instant = Instant.parse(startDateStored)
                                        val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                                        "${shortMonthName(localDateTime.monthNumber)} ${localDateTime.dayOfMonth}, ${localDateTime.year}"
                                    } catch (_: Exception) {
                                        startDateStored
                                    }
                                },
                                onPickerClick = { if (!isSubmitting) showStartDatePicker = true }
                            )
                        }
                        // Expiry Date column
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Expiry Date",
                                style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            // Expiry date is always auto-calculated from start date — non-editable
                            DatePickerField(
                                value = paymentDateDisplay,
                                enabled = false,
                                onPickerClick = {}
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Auto Calculated Base On Start Date",
                        style = AppTextTheme.regular.copy(
                            fontSize = 11.sp,
                            color = PrimaryColor
                        )
                    )
                }
            }
            extendDateError?.let { error ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = error,
                    style = AppTextTheme.regular.copy(fontSize = 12.sp, color = RedDarkColor),
                )
            }
        } else {
            SectionLabel(text = "Extend Days")
            Spacer(modifier = Modifier.height(8.dp))
            DatePickerField(
                value = paymentDateDisplay,
                onPickerClick = { if (!isSubmitting) showDatePicker = true },
            )
            extendDateError?.let { error ->
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = error,
                    style = AppTextTheme.regular.copy(fontSize = 12.sp, color = RedDarkColor),
                )
            }
            if (minExtendDate != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select a date on or after ${formatMonthDayYear(minExtendDate)}",
                    style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray),
                )
            }
            if (durationLabel.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Extension period: $durationLabel",
                    style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray),
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SectionLabel(text = "Fees")
        Spacer(modifier = Modifier.height(8.dp))
        CommonTextField(
            value = fees,
            onValueChange = { raw ->
                if (!isSubmitting) {
                    fees = raw.filter { it.isDigit() }.ifBlank { "0" }
                }
            },
            placeholder = "0",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            enabled = !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(modifier = Modifier.height(20.dp))

//        SectionLabel(text = "Payment Mode")
//        Spacer(modifier = Modifier.height(8.dp))
//        PaymentModeSelector(
//            selectedOption = selectedMode,
//            onOptionSelected = { if (!isSubmitting) selectedMode = it },
//        )
//
//        Spacer(modifier = Modifier.height(20.dp))

//        SectionLabel(text = "Reason / Comments")
//        Spacer(modifier = Modifier.height(8.dp))
//        CommonTextField(
//            value = comment,
//            onValueChange = { comment = it },
//            placeholder = "Enter Reason For Extension...",
//            modifier = Modifier.fillMaxWidth(),
//            isMultiline = true,
//            singleLine = false,
//            enabled = !isSubmitting,
//        )

//        Spacer(modifier = Modifier.height(20.dp))

        // Hide this card when the plan is already expired — there's no meaningful "current expiry" to show
        if (!isPlanExpired) {
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
                        text = "Current Expiry Date",
                        style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
                    )
                    Text(
                        text = expiryDateDisplay.ifBlank { "—" },
                        style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        fun validateExtendDate(): Boolean {
            val selectedDate = paymentDateToLocalDate(paymentDate)
            val selectedStartDate = paymentDateToLocalDate(startDateStored)
            return when {
                selectedDate == null -> {
                    extendDateError = "Please select an expiry date"
                    false
                }
                isRenewPlan -> {
                    if (selectedStartDate != null && selectedDate <= selectedStartDate) {
                        extendDateError = "Expiry date must be after the start date"
                        false
                    } else if (minExtendDate != null && selectedStartDate != null && selectedStartDate < minExtendDate) {
                        extendDateError = "Start date must be on or after the current expiry date (${formatMonthDayYear(minExtendDate)})"
                        false
                    } else {
                        extendDateError = null
                        true
                    }
                }
                minExtendDate != null && selectedDate < minExtendDate -> {
                    extendDateError =
                        "Extension date must be on or after the current expiry date (${formatMonthDayYear(minExtendDate)})"
                    false
                }
                else -> {
                    extendDateError = null
                    true
                }
            }
        }

        val selectedExtendDate = paymentDateToLocalDate(paymentDate)
        val selectedStartDate = paymentDateToLocalDate(startDateStored)
        val extendDateValid = selectedExtendDate != null &&
            if (isRenewPlan) {
                selectedStartDate != null && selectedExtendDate > selectedStartDate && (minExtendDate == null || selectedStartDate >= minExtendDate)
            } else {
                minExtendDate == null || selectedExtendDate >= minExtendDate
            }
        val sellingPriceAmount = displaySellingAmount.coerceAtLeast(0)
        val feesAmount = fees.toIntOrNull()?.coerceAtLeast(0) ?: 0
        val feeOk = if (isRenewPlan) sellingPriceAmount > 0 else feesAmount > 0
        CommonButton(
            enabled = feeOk && extendDateValid && !isSubmitting,
            onClick = {
                if (!validateExtendDate()) return@CommonButton
                onConfirm(selectedMode, paymentDate, sellingPriceAmount, feesAmount, if (isRenewPlan) startDateStored else null)
            },
            text = if (isSubmitting) "Please wait…" else if (isRenewPlan) "Confirm Renewal" else "Extend Membership"
        )

        if (isSubmitting) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = PrimaryColor,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Submitting…",
                    style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onDismiss,
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
        .toLocalDateTime(TimeZone.UTC)
        .date

private fun localDateToMillis(date: LocalDate): Long =
    date.atTime(0, 0, 0)
        .toInstant(TimeZone.UTC)
        .toEpochMilliseconds()

private fun paymentDateToLocalDate(stored: String): LocalDate? =
    runCatching {
        Instant.parse(stored.trim())
            .toLocalDateTime(TimeZone.UTC)
            .date
    }.getOrNull()

private fun localDateToStoredIso(date: LocalDate): String =
    date.atTime(0, 0, 0)
        .toInstant(TimeZone.UTC)
        .toString()

private fun defaultExtendPaymentDate(dateRangeLabel: String): String {
    // Default to the current expiry date so the user can see it pre-filled and extend from there
    val expiry = parseExpiryFromDateRangeLabel(dateRangeLabel)
    return localDateToStoredIso(expiry ?: todayLocal())
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

private fun parseExpiryFromDateRangeLabel(dateRangeLabel: String): LocalDate? {
    val parts = dateRangeLabel.split(" to ", limit = 2)
    if (parts.size < 2) return null
    return parseAnyDate(parts[1].trim())
}

private fun formatMonthDayYear(date: LocalDate): String {
    return "${shortMonthName(date.monthNumber)} ${date.dayOfMonth}, ${date.year}"
}

private fun parseExtendDateRange(dateRangeLabel: String): Triple<String, String, String> {
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
        computeMonths(startRaw, expiryRaw)
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
