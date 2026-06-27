package `in`.gym.trak.studio.features.member

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.components.CommonProgressOverlay
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import gym.composeapp.generated.resources.*
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.theme.*
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import `in`.gym.trak.studio.getCurrentTimeMillis
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.Month
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toLocalDateTime

@Composable
@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
fun MemberStatsScreen() {
    val screenModel = remember { OwnerDashboardScreenModel() }
    val stats by screenModel.memberStatistics.collectAsState()
    val loading by screenModel.memberStatisticsLoading.collectAsState()
    val refreshing by screenModel.memberStatisticsRefreshing.collectAsState()

    var selectedTimeframe by remember { mutableStateOf("Week") }
    var selectedDate by remember {
        mutableStateOf(Instant.fromEpochMilliseconds(getCurrentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault()).date)
    }
    var attendanceMonth by remember {
        mutableStateOf(YearMonth(selectedDate.year, selectedDate.month))
    }
    var selectedMetric by remember { mutableStateOf("Calories") }
    var showCalendarPicker by remember { mutableStateOf(false) }
    val metricOptions = remember { listOf("Calories", "Volume", "Sets", "Duration") }

    val period = when (selectedTimeframe) {
        "Month" -> "month"
        "Year" -> "year"
        else -> "week"
    }
    val refreshAction = {
        screenModel.loadMemberStatistics(
            period = period,
            date = selectedDate.toString(),
            calendarYear = selectedDate.year.toString(),
            calendarMonth = selectedDate.monthNumber.toString(),
            isRefresh = true
        )
    }

    LaunchedEffect(period, selectedDate) {
        screenModel.loadMemberStatistics(
            period = period,
            date = selectedDate.toString(),
            calendarYear = selectedDate.year.toString(),
            calendarMonth = selectedDate.monthNumber.toString()
        )
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = refreshAction
    )

    val datePickerState = rememberDatePickerState()

    val summary = stats?.summary
    val monthlyGoal = stats?.monthly_goal
    val attendanceDays = stats?.attendance?.days_with_activity.orEmpty().toSet()
    val attendanceStatusByDate = remember(stats?.attendance?.calendar) {
        stats?.attendance?.calendar.orEmpty().associate { day -> day.date to day.status }
    }

    LaunchedEffect(stats?.attendance?.year, stats?.attendance?.month) {
        val attendance = stats?.attendance ?: return@LaunchedEffect
        val year = attendance.year ?: return@LaunchedEffect
        val month = attendance.month ?: return@LaunchedEffect
        attendanceMonth = YearMonth(year, Month(month))
    }
    val activeCalories = summary?.active_calories?.value ?: 0
    val durationText = summary?.total_duration?.display ?: "0h"
    val workouts = summary?.total_workouts?.value ?: 0
    val workoutsPct = summary?.total_workouts?.percent_change
    val durationPct = summary?.total_duration?.percent_change
    val caloriesPct = summary?.active_calories?.percent_change
    val streak = summary?.best_streak?.display ?: "0d"

    val metricSeries = when (selectedMetric) {
        "Volume" -> stats?.weekly_activity?.by_metric?.volume
        "Sets" -> stats?.weekly_activity?.by_metric?.sets
        "Duration" -> stats?.weekly_activity?.by_metric?.duration
        else -> stats?.weekly_activity?.by_metric?.active_calories
    }
    val graphValues = metricSeries?.points?.map { it.value.toFloat() } ?: List(7) { 0f }
    val graphDays = metricSeries?.points?.map { it.weekday.take(3) }
        ?: listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val weeklyTotal = metricSeries?.total ?: 0
    val weeklyUnit = metricSeries?.unit ?: "kcal"

    LoadingScreenHandler(screenModel = screenModel) {
        if (showCalendarPicker) {
            DatePickerDialog(
                onDismissRequest = { showCalendarPicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                selectedDate = epochMillisToLocalDate(millis)
                            }
                            showCalendarPicker = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCalendarPicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            AppScrollableScreen(
                modifier = Modifier.fillMaxSize()
                    .padding(vertical = 20.dp)
            ) {
                StaggeredEntranceItem(index = 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Statistics", style = AppTextTheme.bold.copy(fontSize = 24.sp, color = DarkBlack))
                            Text("Your Progress", style = AppTextTheme.medium.copy(fontSize = 14.sp, color = PrimaryColor))
                        }
                        Surface(
                            onClick = { showCalendarPicker = true },
                            color = PrimaryColor,
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CalendarMonth, null, tint = White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "${selectedDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${selectedDate.year}",
                                    style = AppTextTheme.bold.copy(fontSize = 12.sp, color = White)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                StaggeredEntranceItem(index = 1) {
                    CommonCard(shape = RoundedCornerShape(100.dp), elevation = 0.dp, borderColor = Color(0xFFF3F4F6)) {
                        Row(
                            modifier = Modifier.padding(4.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("Week", "Month", "Year").forEach { timeframe ->
                                val isSelected = selectedTimeframe == timeframe
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(if (isSelected) PrimaryColor else Color.Transparent)
                                        .clickable { selectedTimeframe = timeframe },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = timeframe,
                                        style = AppTextTheme.bold.copy(
                                            fontSize = 14.sp,
                                            color = if (isSelected) White else Gray
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                StaggeredEntranceItem(index = 2) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            MiniStatCard(
                                modifier = Modifier.weight(1f),
                                icon = Res.drawable.ic_total_workout,
                                title = "Total Workouts",
                                value = workouts.toString(),
                                percentage = percentText(workoutsPct),
                                bgColor = Color(0xFFE2F7FA)
                            )
                            MiniStatCard(
                                modifier = Modifier.weight(1f),
                                icon = Res.drawable.ic_total_duration,
                                title = "Total Duration",
                                value = durationText,
                                percentage = percentText(durationPct),
                                bgColor = Color(0xFFE1F6E3)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            MiniStatCard(
                                modifier = Modifier.weight(1f),
                                icon = Res.drawable.ic_calories_antic,
                                title = "Calories Antic",
                                value = (summary?.active_calories?.display ?: "0"),
                                percentage = percentText(caloriesPct),
                                bgColor = Color(0xFFFFF6D8)
                            )
                            MiniStatCard(
                                modifier = Modifier.weight(1f),
                                icon = Res.drawable.ic_best_streak,
                                title = "Best Streak",
                                value = streak,
                                percentage = "${summary?.best_streak?.value_days ?: 0} days",
                                bgColor = Color(0xFFFFE3E6)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                StaggeredEntranceItem(index = 3) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(PrimaryColor)
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painterResource(Res.drawable.ic_workout),
                                    null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Total Workout", style = AppTextTheme.bold.copy(fontSize = 14.sp, color = White))
                                Spacer(modifier = Modifier.weight(1f))
                                Box(
                                    modifier = Modifier
                                        .background(White.copy(0.2f), RoundedCornerShape(100.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(durationText, style = AppTextTheme.medium.copy(fontSize = 12.sp, color = White))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(workouts.toString(), style = AppTextTheme.bold.copy(fontSize = 42.sp, color = White))
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .background(White, RoundedCornerShape(100.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        percentText(workoutsPct),
                                        style = AppTextTheme.bold.copy(fontSize = 12.sp, color = PrimaryColor)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = monthlyGoal?.message ?: "Keep going — your progress updates after each workout.",
                                style = AppTextTheme.regular.copy(fontSize = 12.sp, color = White.copy(0.8f))
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = 20.dp, y = 20.dp)
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(White.copy(0.1f))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                StaggeredEntranceItem(index = 4) {
                    CommonCard(shape = RoundedCornerShape(24.dp), elevation = 2.dp, borderColor = Color(0xFFF3F4F6)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                items(metricOptions) { metric ->
                                    ActivityChip(metric, selectedMetric == metric) {
                                        selectedMetric = metric
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Weekly Activity", style = AppTextTheme.bold.copy(fontSize = 14.sp, color = DarkBlack))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(weeklyTotal.toString(), style = AppTextTheme.bold.copy(fontSize = 14.sp, color = PrimaryColor))
                                    Text(" $weeklyUnit total", style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray))
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            GraphView(
                                modifier = Modifier.fillMaxWidth().height(150.dp),
                                points = graphValues
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                graphDays.forEach { day ->
                                    Text(day, style = AppTextTheme.medium.copy(fontSize = 10.sp, color = Gray))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                StaggeredEntranceItem(index = 5) {
                    CommonCard(shape = RoundedCornerShape(24.dp), elevation = 2.dp, borderColor = Color(0xFFF3F4F6)) {
                        StatsAttendanceCalendar(
                            modifier = Modifier.padding(16.dp),
                            visibleMonth = attendanceMonth,
                            presentDays = attendanceDays,
                            statusByDate = attendanceStatusByDate,
                            selectedDate = selectedDate,
                            onMonthChange = { month ->
                                attendanceMonth = month
                                screenModel.loadMemberStatistics(
                                    period = period,
                                    date = selectedDate.toString(),
                                    calendarYear = month.year.toString(),
                                    calendarMonth = LocalDate(month.year, month.month, 1).monthNumber.toString(),
                                )
                            },
                            onDateSelected = { date ->
                                selectedDate = date
                                attendanceMonth = YearMonth(date.year, date.month)
                            },
                        )
                    }
                }
                Spacer(modifier = Modifier.height(40.dp))
            }

            PullRefreshIndicator(
                refreshing = refreshing || loading,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = White,
                contentColor = PrimaryColor
            )

            if (loading && !refreshing) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                        .clickable(enabled = false) { }
                ) {
                    CommonProgressOverlay()
                }
            }
        }
    }
}

@Composable
fun MiniStatCard(
    modifier: Modifier,
    icon: DrawableResource,
    title: String,
    value: String,
    percentage: String,
    bgColor: Color,
) {
    CommonCard(
        modifier = modifier,
        elevation = 0.dp,
        borderColor = Color(0xFFF3F4F6),
        backgroundColor = bgColor
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            Icon(
                painterResource(icon),
                null,
                tint = Color.Unspecified,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(title, style = AppTextTheme.medium.copy(fontSize = 11.sp, color = Gray))
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, style = AppTextTheme.bold.copy(fontSize = 18.sp, color = DarkBlack))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    percentage.replace("This Month", ""),
                    style = AppTextTheme.medium.copy(fontSize = 10.sp, color = PrimaryColor)
                )
            }
            Text("This Month", style = AppTextTheme.medium.copy(fontSize = 10.sp, color = Gray))
        }
    }
}

@Composable
fun ActivityChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) PrimaryColor else Color.Transparent,
                RoundedCornerShape(100.dp)
            )
            .border(
                if (isSelected) 0.dp else 1.dp,
                if (isSelected) Color.Transparent else Color(0xFFE5E7EB),
                RoundedCornerShape(100.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (text == "Calories") {
                Icon(
                    painterResource(Res.drawable.ic_statistics),
                    null,
                    tint = if (isSelected) White else Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            } else if (text == "Volume") {
                Icon(
                    painterResource(Res.drawable.ic_log_meal),
                    null,
                    tint = Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = AppTextTheme.bold.copy(
                    fontSize = 12.sp,
                    color = if (isSelected) White else Gray
                )
            )
        }
    }
}

@Composable
fun CalendarDay(
    day: String,
    isPresent: Boolean,
    isToday: Boolean,
    isAbsent: Boolean = false,
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    isPresent -> PrimaryColor
                    isAbsent -> RedColor.copy(alpha = 0.14f)
                    isToday -> PrimaryColor.copy(alpha = 0.1f)
                    else -> Color(0xFFF3F4F6)
                }
            )
            .then(
                if (isToday) {
                    Modifier.border(
                        1.dp,
                        if (isAbsent) RedColor else PrimaryColor,
                        RoundedCornerShape(8.dp),
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day,
            style = AppTextTheme.medium.copy(
                fontSize = 12.sp,
                color = when {
                    isPresent -> White
                    isAbsent -> RedColor
                    isToday -> PrimaryColor
                    else -> Gray
                }
            )
        )
    }
}

@Composable
fun GraphView(modifier: Modifier, points: List<Float>) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val path = Path()
        val safe = if (points.isEmpty()) List(7) { 0f } else points
        val max = (safe.maxOrNull() ?: 0f).coerceAtLeast(1f)
        val normalized = safe.map { (it / max).coerceIn(0f, 1f) }
        val stepX = if (normalized.size > 1) width / (normalized.size - 1) else width

        path.moveTo(0f, height * (1 - normalized[0]))
        for (i in 1 until normalized.size) {
            path.lineTo(i * stepX, height * (1 - normalized[i]))
        }

        drawPath(
            path = path,
            color = PrimaryColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Shadow/Fill under the line
        val fillPath = Path()
        fillPath.addPath(path)
        fillPath.lineTo(width, height)
        fillPath.lineTo(0f, height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(PrimaryColor.copy(0.2f), Color.Transparent)
            )
        )

        // Highlight point
        drawCircle(
            color = White,
            radius = 6.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(
                (normalized.lastIndex.coerceAtMost(5)) * stepX,
                height * (1 - normalized[normalized.lastIndex.coerceAtMost(5)])
            )
        )
        drawCircle(
            color = PrimaryColor,
            radius = 4.dp.toPx(),
            center = androidx.compose.ui.geometry.Offset(
                (normalized.lastIndex.coerceAtMost(5)) * stepX,
                height * (1 - normalized[normalized.lastIndex.coerceAtMost(5)])
            )
        )

        // Tooltip highlight
        // ... omitted for simplicity
    }
}

private fun percentText(value: Double?): String = when {
    value == null -> "0%"
    value > 0 -> "+${value.toInt()}%"
    else -> "${value.toInt()}%"
}

private fun epochMillisToLocalDate(millis: Long): LocalDate {
    return Instant.fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
}
