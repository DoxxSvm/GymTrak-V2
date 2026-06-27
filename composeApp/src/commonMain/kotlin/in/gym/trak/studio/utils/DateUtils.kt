package `in`.gym.trak.studio.utils

import `in`.gym.trak.studio.getCurrentTimeMillis
import kotlinx.datetime.*

object DateUtils {
    fun formatEnquiryDate(isoString: String?): String {
        if (isoString.isNullOrEmpty()) return "N/A"
        return try {
            val instant = Instant.parse(isoString)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val day = localDateTime.dayOfMonth
            val month = getShortMonthName(localDateTime.monthNumber)
            "Received $day $month"
        } catch (e: Exception) {
            "N/A"
        }
    }

    fun formatEnquiryTime(isoString: String?): String {
        if (isoString.isNullOrEmpty()) return ""
        return try {
            val instant = Instant.parse(isoString)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val hour = localDateTime.hour
            val minute = localDateTime.minute
            val amPm = if (hour < 12) "AM" else "PM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            val displayMinute = minute.toString().padStart(2, '0')
            "  •  $displayHour:$displayMinute $amPm"
        } catch (e: Exception) {
            "  •  00:00 AM"
        }
    }

    private fun getShortMonthName(month: Int): String {
        return when (month) {
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
    }
    fun getCurrentDateIso(): String {
        val now = Instant.fromEpochMilliseconds(getCurrentTimeMillis())
        return now.toString()
    }

    fun getCurrentMonthName(): String {
        val now = Instant.fromEpochMilliseconds(getCurrentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault())
        return getFullMonthName(now.monthNumber)
    }

    fun getCurrentShortMonthName(): String {
        val now = Instant.fromEpochMilliseconds(getCurrentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault())
        return getShortMonthName(now.monthNumber)
    }

    fun getLastMonthShortName(): String {
        val now = Instant.fromEpochMilliseconds(getCurrentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault())
        val lastMonthDate = now.date.minus(1, DateTimeUnit.MONTH)
        return getShortMonthName(lastMonthDate.monthNumber)
    }

    fun getCurrentYear(): Int {
        return Instant.fromEpochMilliseconds(getCurrentTimeMillis())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .year
    }

    fun expenseFilterRangeLabel(filter: String): String = when (filter) {
        "This month" -> getCurrentShortMonthName()
        "Last month" -> getLastMonthShortName()
        "Yearly" -> getCurrentYear().toString()
        else -> getCurrentShortMonthName()
    }

    fun getFullMonthNames(): List<String> {
        return listOf(
            "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
        )
    }

    private fun getFullMonthName(month: Int): String {
        return getFullMonthNames().getOrNull(month - 1) ?: ""
    }

    fun getMonthRangeDates(fromMonthIndex: Int, toMonthIndex: Int): Pair<String, String> {
        val now = Instant.fromEpochMilliseconds(getCurrentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault())
        val year = now.year
        
        // Month index is 0-based from getFullMonthNames()
        val startMonth = fromMonthIndex + 1
        val endMonth = toMonthIndex + 1
        
        val startDate = LocalDate(year, startMonth, 1)
        
        // For end date, get the last day of the end month
        val nextMonth = if (endMonth == 12) 1 else endMonth + 1
        val nextYear = if (endMonth == 12) year + 1 else year
        val firstDayOfNextMonth = LocalDate(nextYear, nextMonth, 1)
        val endDate = firstDayOfNextMonth.minus(1, DateTimeUnit.DAY)
        
        return startDate.toString() to endDate.toString()
    }

    fun getThisMonthDateRange(): Pair<String, String> {
        val today = Instant.fromEpochMilliseconds(getCurrentTimeMillis())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        val start = LocalDate(today.year, today.monthNumber, 1)
        val end = start.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
        return start.toString() to end.toString()
    }

    fun getLastMonthDateRange(): Pair<String, String> {
        val today = Instant.fromEpochMilliseconds(getCurrentTimeMillis())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        val lastDay = LocalDate(today.year, today.monthNumber, 1).minus(1, DateTimeUnit.DAY)
        val firstDay = LocalDate(lastDay.year, lastDay.monthNumber, 1)
        return firstDay.toString() to lastDay.toString()
    }

    fun getYearlyDateRange(): Pair<String, String> {
        val year = getCurrentYear()
        return LocalDate(year, 1, 1).toString() to LocalDate(year, 12, 31).toString()
    }

    /** Maps expense-style filter chips to `from` / `to` query params for workout history. */
    fun resolveWorkoutHistoryDateRange(
        selectedFilter: String,
        dateFrom: String?,
        dateTo: String?,
    ): Pair<String?, String?> {
        if (!dateFrom.isNullOrBlank() && !dateTo.isNullOrBlank()) {
            return dateFrom to dateTo
        }
        return when (selectedFilter) {
            "This month" -> getThisMonthDateRange()
            "Last month" -> getLastMonthDateRange()
            "Yearly" -> getYearlyDateRange()
            else -> null to null
        }
    }

    fun formatWorkoutHistoryDate(isoString: String?): String {
        if (isoString.isNullOrEmpty()) return ""
        return try {
            val instant = Instant.parse(isoString)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val day = localDateTime.dayOfMonth
            val month = getShortMonthName(localDateTime.monthNumber)
            val hour = localDateTime.hour
            val minute = localDateTime.minute
            val amPm = if (hour < 12) "AM" else "PM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            val displayMinute = minute.toString().padStart(2, '0')
            "$day $month · $displayHour:$displayMinute $amPm"
        } catch (e: Exception) {
            ""
        }
    }

    fun epochMillisToLocalDate(millis: Long): LocalDate {
        return Instant.fromEpochMilliseconds(millis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
    }

    fun formatBroadcastDate(isoString: String?): String {
        if (isoString.isNullOrEmpty()) return "Created Oct 12" // Default as per design
        return try {
            val instant = Instant.parse(isoString)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val day = localDateTime.dayOfMonth
            val month = getShortMonthName(localDateTime.monthNumber)
            "Created $month $day"
        } catch (e: Exception) {
            "Created Oct 12"
        }
    }

    fun formatChatTime(isoString: String?): String {
        if (isoString.isNullOrEmpty()) return ""
        return try {
            val instant = Instant.parse(isoString)
            val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val hour = localDateTime.hour
            val minute = localDateTime.minute
            val amPm = if (hour < 12) "AM" else "PM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            val displayMinute = minute.toString().padStart(2, '0')
            "$displayHour:$displayMinute $amPm"
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * ISO instant → local `dd/MM/yyyy, hh:mm AM/PM` (same convention as trainer attendance logs).
     */
    fun formatShortDateTime(isoString: String?): String {
        if (isoString.isNullOrEmpty()) return ""
        return try {
            val instant = Instant.parse(isoString)
            val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val date = "${local.dayOfMonth.toString().padStart(2, '0')}/${
                local.monthNumber.toString().padStart(2, '0')
            }/${local.year}"
            val hour24 = local.hour
            val minute = local.minute.toString().padStart(2, '0')
            val amPm = if (hour24 >= 12) "PM" else "AM"
            val hour12 = when {
                hour24 == 0 -> 12
                hour24 > 12 -> hour24 - 12
                else -> hour24
            }
            "$date, ${hour12.toString().padStart(2, '0')}:$minute $amPm"
        } catch (_: Throwable) {
            isoString
        }
    }

    /**
     * Birth dates and other calendar-only values from APIs (`yyyy-MM-dd`, `...T00:00:00.000Z`, etc.)
     * → **`dd/MM/yyyy`** (single app-wide display format; uses the calendar date part, not local midnight shift).
     */
    fun formatBirthDateForDisplay(value: String?): String {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty()) return ""
        return try {
            val dateOnly = raw.substringBefore("T").take(10)
            val date = if (dateOnly.length == 10 && dateOnly[4] == '-' && dateOnly[7] == '-') {
                LocalDate.parse(dateOnly)
            } else {
                Instant.parse(raw).toLocalDateTime(TimeZone.UTC).date
            }
            "${date.dayOfMonth.toString().padStart(2, '0')}/${
                date.monthNumber.toString().padStart(2, '0')
            }/${date.year}"
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * Normalizes birth/API date strings to `yyyy-MM-dd` for payloads or age math.
     */
    fun birthDateToIsoDateOnly(value: String?): String? {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty()) return null
        val head = raw.substringBefore("T").take(10)
        if (head.length == 10 && head[4] == '-' && head[7] == '-') {
            return runCatching { LocalDate.parse(head).toString() }.getOrNull()
        }
        return runCatching {
            Instant.parse(raw).toLocalDateTime(TimeZone.UTC).date.toString()
        }.getOrNull()
    }

    /**
     * Values stored from date pickers (often full ISO instants) → `yyyy-MM-dd` for payment API `date`.
     */
    fun paymentStoredInstantToApiDate(stored: String): String {
        val trimmed = stored.trim()
        if (trimmed.isEmpty()) {
            return Instant.fromEpochMilliseconds(getCurrentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        }
        birthDateToIsoDateOnly(trimmed)?.let { return it }
        return runCatching {
            Instant.parse(trimmed).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        }.getOrElse { trimmed.take(10) }
    }

    /** Short weekday label matching dashboard traffic_trend weekly `day` values (Mon, Tue, …). */
    fun getCurrentWeekdayShortName(): String {
        val dayOfWeek = Instant.fromEpochMilliseconds(getCurrentTimeMillis())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .dayOfWeek
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> "Mon"
            DayOfWeek.TUESDAY -> "Tue"
            DayOfWeek.WEDNESDAY -> "Wed"
            DayOfWeek.THURSDAY -> "Thu"
            DayOfWeek.FRIDAY -> "Fri"
            DayOfWeek.SATURDAY -> "Sat"
            DayOfWeek.SUNDAY -> "Sun"
        }
    }

    /** e.g. `12 May` — for workout completion pill labels. */
    fun formatNowAsDayShortMonth(): String {
        val ldt = Instant.fromEpochMilliseconds(getCurrentTimeMillis())
            .toLocalDateTime(TimeZone.currentSystemDefault())
        return "${ldt.dayOfMonth} ${getShortMonthName(ldt.monthNumber)}"
    }

    /**
     * Dashboard recent payments: `Today · 10:42 AM` or `26 May · 10:42 AM`.
     */
    fun formatDashboardTransactionDateTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return ""
        return try {
            val instant = Instant.parse(isoString)
            val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
            val now = Instant.fromEpochMilliseconds(getCurrentTimeMillis())
                .toLocalDateTime(TimeZone.currentSystemDefault())
            val datePart = if (local.date == now.date) {
                "Today"
            } else {
                "${local.dayOfMonth} ${getShortMonthName(local.monthNumber)}"
            }
            val hour24 = local.hour
            val minute = local.minute.toString().padStart(2, '0')
            val amPm = if (hour24 >= 12) "PM" else "AM"
            val hour12 = when {
                hour24 == 0 -> 12
                hour24 > 12 -> hour24 - 12
                else -> hour24
            }
            val timePart = "${hour12.toString().padStart(2, '0')}:$minute $amPm"
            "$datePart · $timePart"
        } catch (_: Throwable) {
            formatShortDateTime(isoString)
        }
    }

    /** e.g. `12 May 2026, 4:16 PM` — workout save / success “when” row. */
    fun formatNowAsDayMonthYearTime(): String {
        val ldt = Instant.fromEpochMilliseconds(getCurrentTimeMillis())
            .toLocalDateTime(TimeZone.currentSystemDefault())
        val month = getShortMonthName(ldt.monthNumber)
        val hour = ldt.hour
        val minute = ldt.minute
        val amPm = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val displayMinute = minute.toString().padStart(2, '0')
        return "${ldt.dayOfMonth} $month ${ldt.year}, $displayHour:$displayMinute $amPm"
    }

    fun parseIsoToLocalDate(isoString: String?): LocalDate? {
        if (isoString.isNullOrBlank()) return null
        return try {
            Instant.parse(isoString).toLocalDateTime(TimeZone.currentSystemDefault()).date
        } catch (_: Exception) {
            null
        }
    }

    fun todayLocalDate(): LocalDate =
        Instant.fromEpochMilliseconds(getCurrentTimeMillis())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date

    /** Chat / broadcast channel list timestamp (Today → time, Yesterday, or date). */
    fun formatConversationListTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return ""
        return try {
            val local = Instant.parse(isoString).toLocalDateTime(TimeZone.currentSystemDefault())
            val today = todayLocalDate()
            val date = local.date
            when {
                date == today -> formatChatTime(isoString)
                date == today.minus(1, DateTimeUnit.DAY) -> "Yesterday"
                date.year == today.year ->
                    "${local.dayOfMonth} ${getShortMonthName(local.monthNumber)}"
                else -> formatBirthDateForDisplay(isoString)
            }
        } catch (_: Exception) {
            ""
        }
    }

    /** Relative label for notification list rows (e.g. `2min ago`, `1d ago`). */
    fun formatRelativeNotificationTime(isoString: String?): String {
        if (isoString.isNullOrBlank()) return ""
        return try {
            val instant = Instant.parse(isoString)
            val nowMs = getCurrentTimeMillis()
            val diffMs = (nowMs - instant.toEpochMilliseconds()).coerceAtLeast(0)
            val diffMinutes = diffMs / 60_000
            val diffHours = diffMs / 3_600_000
            val diffDays = diffMs / 86_400_000
            when {
                diffMinutes < 1 -> "Just now"
                diffMinutes < 60 -> "${diffMinutes}min ago"
                diffHours < 24 -> "${diffHours}h ago"
                diffDays == 1L -> "1d ago"
                diffDays < 7 -> "${diffDays}d ago"
                else -> formatShortDateTime(isoString)
            }
        } catch (_: Exception) {
            ""
        }
    }

    fun notificationSectionLabel(isoString: String?): String {
        val date = parseIsoToLocalDate(isoString) ?: return "Earlier"
        val today = todayLocalDate()
        val yesterday = today.minus(1, DateTimeUnit.DAY)
        return when (date) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> {
                val month = getShortMonthName(date.monthNumber)
                "$month ${date.dayOfMonth}, ${date.year}"
            }
        }
    }
}

