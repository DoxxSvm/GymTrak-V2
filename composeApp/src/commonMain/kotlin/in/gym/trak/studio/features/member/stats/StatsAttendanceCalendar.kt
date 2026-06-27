package `in`.gym.trak.studio.features.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kizitonwose.calendar.compose.VerticalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.minusMonths
import com.kizitonwose.calendar.core.plusMonths
import `in`.gym.trak.studio.getCurrentTimeMillis
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.DarkBlack
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.RedColor
import `in`.gym.trak.studio.theme.White
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime

internal enum class AttendanceCalendarStatus {
    Present,
    Absent,
    Leave,
    Holiday,
    Future,
    None,
}

@Composable
internal fun StatsAttendanceCalendar(
    visibleMonth: YearMonth,
    presentDays: Set<String>,
    statusByDate: Map<String, String>,
    selectedDate: LocalDate,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember {
        Instant.fromEpochMilliseconds(getCurrentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val daysOfWeek = remember { daysOfWeek(firstDayOfWeek = DayOfWeek.MONDAY) }
    val calendarState = rememberCalendarState(
        startMonth = visibleMonth,
        endMonth = visibleMonth,
        firstVisibleMonth = visibleMonth,
        firstDayOfWeek = DayOfWeek.MONDAY,
    )

    LaunchedEffect(visibleMonth) {
        calendarState.scrollToMonth(visibleMonth)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Attendance",
                style = AppTextTheme.bold.copy(fontSize = 14.sp, color = DarkBlack),
            )
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(8.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = { onMonthChange(visibleMonth.minusMonths(1)) },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Default.ChevronLeft,
                        contentDescription = "Previous month",
                        modifier = Modifier.size(18.dp),
                        tint = Gray,
                    )
                }
                Row(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${visibleMonth.toMonthNumber()}/${visibleMonth.year}",
                        style = AppTextTheme.medium.copy(fontSize = 12.sp, color = DarkBlack),
                    )
                }
                IconButton(
                    onClick = { onMonthChange(visibleMonth.plusMonths(1)) },
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = "Next month",
                        modifier = Modifier.size(18.dp),
                        tint = Gray,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            daysOfWeek.forEach { dayOfWeek ->
                Text(
                    text = dayOfWeek.displayText(),
                    style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        VerticalCalendar(
            state = calendarState,
            userScrollEnabled = false,
            dayContent = { day ->
                StatsAttendanceDayCell(
                    day = day,
                    status = resolveAttendanceStatus(
                        date = day.date,
                        presentDays = presentDays,
                        statusByDate = statusByDate,
                        today = today,
                    ),
                    isSelected = day.date == selectedDate,
                    onClick = { clicked ->
                        if (clicked.position == DayPosition.MonthDate) {
                            onDateSelected(clicked.date)
                        }
                    },
                )
            },
            monthHeader = {},
            modifier = Modifier
                .fillMaxWidth()
                .height(252.dp),
        )

        Spacer(modifier = Modifier.height(12.dp))

        AttendanceStatusLegend()
    }
}

@Composable
private fun StatsAttendanceDayCell(
    day: CalendarDay,
    status: AttendanceCalendarStatus,
    isSelected: Boolean,
    onClick: (CalendarDay) -> Unit,
) {
    val isInMonth = day.position == DayPosition.MonthDate
    val backgroundColor = when {
        !isInMonth -> Color.Transparent
        isSelected -> PrimaryColor.copy(alpha = 0.18f)
        status == AttendanceCalendarStatus.Present -> PrimaryColor
        status == AttendanceCalendarStatus.Absent -> RedColor.copy(alpha = 0.14f)
        status == AttendanceCalendarStatus.Leave -> Color(0xFFE0F2FE)
        status == AttendanceCalendarStatus.Holiday -> Color(0xFFFFF6D8)
        else -> Color(0xFFF3F4F6)
    }
    val textColor = when {
        !isInMonth -> Color.Transparent
        status == AttendanceCalendarStatus.Present -> White
        status == AttendanceCalendarStatus.Absent -> RedColor
        status == AttendanceCalendarStatus.Leave -> Color(0xFF0284C7)
        status == AttendanceCalendarStatus.Holiday -> Color(0xFFD97706)
        isSelected -> PrimaryColor
        else -> Gray
    }
    val borderColor = when {
        isSelected && isInMonth -> PrimaryColor
        status == AttendanceCalendarStatus.Absent && isInMonth -> RedColor.copy(alpha = 0.35f)
        else -> Color.Transparent
    }

    Box(
        modifier = Modifier
            .padding(vertical = 2.dp)
            .size(width = 36.dp, height = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .then(
                if (borderColor != Color.Transparent) {
                    Modifier.border(1.dp, borderColor, RoundedCornerShape(10.dp))
                } else {
                    Modifier
                },
            )
            .clickable(enabled = isInMonth) { onClick(day) },
        contentAlignment = Alignment.Center,
    ) {
        if (isInMonth) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = AppTextTheme.medium.copy(fontSize = 12.sp, color = textColor),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AttendanceStatusLegend() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AttendanceLegendItem(color = PrimaryColor, label = "Present")
        AttendanceLegendItem(color = RedColor.copy(alpha = 0.35f), label = "Absent")
        AttendanceLegendItem(color = Color(0xFF0284C7), label = "Leave")
        AttendanceLegendItem(color = Color(0xFFD97706), label = "Holiday")
    }
}

@Composable
private fun AttendanceLegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(RoundedCornerShape(50.dp))
                .background(color),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = AppTextTheme.regular.copy(fontSize = 10.sp, color = Gray),
        )
    }
}

private fun resolveAttendanceStatus(
    date: LocalDate,
    presentDays: Set<String>,
    statusByDate: Map<String, String>,
    today: LocalDate,
): AttendanceCalendarStatus {
    val dateKey = date.toString()
    statusByDate[dateKey]?.let { raw ->
        return when (raw.lowercase()) {
            "present" -> AttendanceCalendarStatus.Present
            "absent" -> AttendanceCalendarStatus.Absent
            "leave" -> AttendanceCalendarStatus.Leave
            "holiday" -> AttendanceCalendarStatus.Holiday
            else -> AttendanceCalendarStatus.None
        }
    }
    if (presentDays.contains(dateKey)) return AttendanceCalendarStatus.Present
    if (date > today) return AttendanceCalendarStatus.Future
    if (date < today) return AttendanceCalendarStatus.Absent
    return AttendanceCalendarStatus.None
}

private fun YearMonth.toMonthNumber(): Int =
    LocalDate(year, month, 1).monthNumber

private fun DayOfWeek.displayText(): String = when (this) {
    DayOfWeek.MONDAY -> "M"
    DayOfWeek.TUESDAY -> "T"
    DayOfWeek.WEDNESDAY -> "W"
    DayOfWeek.THURSDAY -> "T"
    DayOfWeek.FRIDAY -> "F"
    DayOfWeek.SATURDAY -> "S"
    DayOfWeek.SUNDAY -> "S"
}
