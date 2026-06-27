package `in`.gym.trak.studio.features.member.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import `in`.gym.trak.studio.components.AppEmptyStateView
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import `in`.gym.trak.studio.data.model.MemberWorkoutHistoryItemDTO
import `in`.gym.trak.studio.features.expenses.ExpenseFilterChip
import `in`.gym.trak.studio.features.expenses.MonthRangePickerSheetContent
import `in`.gym.trak.studio.features.member.MemberWorkoutLogDetailScreen
import `in`.gym.trak.studio.features.member.WorkoutCard
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.utils.DateUtils
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.img_no_wrokout
import gym.composeapp.generated.resources.img_workout

/**
 * Paginated self workout history from `GET workouts/history` (JWT subject; optional `gymId` from session).
 * List rows reuse [WorkoutCard] without play / more actions (same layout as [MemberWorkoutScreen]).
 */
class MemberWorkoutHistoryScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { MemberWorkoutHistoryScreenModel() }
        val items by screenModel.items.collectAsState()
        val total by screenModel.total.collectAsState()
        val hasMore by screenModel.hasMore.collectAsState()
        val isRefreshing by screenModel.isRefreshing.collectAsState()
        val isLoadingMore by screenModel.isLoadingMore.collectAsState()
        val isLoading by screenModel.isLoading.collectAsState()

        var selectedFilter by remember { mutableStateOf("This month") }
        var showMonthRangePicker by remember { mutableStateOf(false) }
        var isApplyingMonthRange by remember { mutableStateOf(false) }
        var dateFrom by remember { mutableStateOf<String?>(null) }
        var dateTo by remember { mutableStateOf<String?>(null) }
        var rangeLabel by remember { mutableStateOf(DateUtils.expenseFilterRangeLabel("This month")) }
        val monthRangeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        LaunchedEffect(selectedFilter, dateFrom, dateTo) {
            val (from, to) = DateUtils.resolveWorkoutHistoryDateRange(selectedFilter, dateFrom, dateTo)
            screenModel.setDateRange(from, to)
            screenModel.resetAndLoad(showFullLoader = true)
        }
        LaunchedEffect(isLoading) {
            if (!isLoading) {
                if (isApplyingMonthRange) {
                    isApplyingMonthRange = false
                    showMonthRangePicker = false
                }
            }
        }

        LoadingScreenHandler(screenModel = screenModel) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    GymAppBar(
                        title = "Workout History",
                        onBackClick = { navigator?.pop() },
                    )
                },
            ) { padding ->
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { screenModel.refresh() },
                    state = rememberPullToRefreshState(),
                    indicator = {},
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                ) {
                    when {
                        items.isEmpty() && !isLoading && !isRefreshing && !isLoadingMore -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                item { WorkoutHistoryFilterSection(
                                    selectedFilter = selectedFilter,
                                    rangeLabel = rangeLabel,
                                    dateFromIsNull = dateFrom == null,
                                    onFilterSelected = { filter ->
                                        selectedFilter = filter
                                        dateFrom = null
                                        dateTo = null
                                        rangeLabel = DateUtils.expenseFilterRangeLabel(filter)
                                    },
                                    onRangePickerClick = { showMonthRangePicker = true },
                                ) }
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 40.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        AppEmptyStateView(
                                            image = Res.drawable.img_no_wrokout,
                                            title = "No history yet",
                                            subtitle = "Completed workouts will show up here for $rangeLabel.",
                                        )
                                    }
                                }
                            }
                        }
                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                item {
                                    WorkoutHistoryFilterSection(
                                        selectedFilter = selectedFilter,
                                        rangeLabel = rangeLabel,
                                        dateFromIsNull = dateFrom == null,
                                        onFilterSelected = { filter ->
                                            selectedFilter = filter
                                            dateFrom = null
                                            dateTo = null
                                            rangeLabel = DateUtils.expenseFilterRangeLabel(filter)
                                        },
                                        onRangePickerClick = { showMonthRangePicker = true },
                                    )
                                }
                                if (total > 0) {
                                    item {
                                        Text(
                                            text = "$total workout${if (total == 1) "" else "s"}",
                                            style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray),
                                        )
                                    }
                                }
                                items(items, key = { it.workoutId }) { row ->
                                    StaggeredEntranceItem(index = items.indexOf(row)) {
                                        HistoryWorkoutRow(
                                            row = row,
                                            onClick = {
                                                navigator?.push(
                                                    MemberWorkoutLogDetailScreen(
                                                        workoutId = row.workoutId,
                                                        startedAtIso = row.started_at,
                                                    ),
                                                )
                                            },
                                        )
                                    }
                                }
                                if (hasMore && items.isNotEmpty()) {
                                    item {
                                        LaunchedEffect(items.size, selectedFilter, dateFrom, dateTo) {
                                            screenModel.loadNextPage()
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            if (isLoadingMore) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(28.dp),
                                                    color = PrimaryColor,
                                                    strokeWidth = 2.dp,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showMonthRangePicker) {
                ModalBottomSheet(
                    onDismissRequest = {
                        if (!isApplyingMonthRange) showMonthRangePicker = false
                    },
                    sheetState = monthRangeSheetState,
                    containerColor = White,
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .padding(top = 12.dp, bottom = 8.dp)
                                .width(40.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color(0xFFE2E8F0))
                        )
                    },
                ) {
                    MonthRangePickerSheetContent(
                        isApplying = isApplyingMonthRange,
                        isLoading = isLoading && isApplyingMonthRange,
                        onDismiss = {
                            if (!isApplyingMonthRange) showMonthRangePicker = false
                        },
                        onRangeSelected = { from, to ->
                            val months = DateUtils.getFullMonthNames()
                            val fromName = months[from].take(3)
                            val toName = months[to].take(3)
                            rangeLabel = if (from == to) fromName else "$fromName - $toName"
                            val (start, end) = DateUtils.getMonthRangeDates(from, to)
                            dateFrom = start
                            dateTo = end
                            selectedFilter = "Custom"
                            isApplyingMonthRange = true
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutHistoryFilterSection(
    selectedFilter: String,
    rangeLabel: String,
    dateFromIsNull: Boolean,
    onFilterSelected: (String) -> Unit,
    onRangePickerClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ExpenseFilterChip(
                "This month",
                selectedFilter == "This month" && dateFromIsNull,
            ) { onFilterSelected("This month") }
            ExpenseFilterChip(
                "Last month",
                selectedFilter == "Last month" && dateFromIsNull,
            ) { onFilterSelected("Last month") }
            ExpenseFilterChip(
                "Yearly",
                selectedFilter == "Yearly" && dateFromIsNull,
            ) { onFilterSelected("Yearly") }
        }
        Surface(
            color = Color(0xFFF8FAFC),
            shape = RoundedCornerShape(12.dp),
            onClick = onRangePickerClick,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Period: $rangeLabel",
                    style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black),
                )
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Select month range",
                    tint = PrimaryColor,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun HistoryWorkoutRow(
    row: MemberWorkoutHistoryItemDTO,
    onClick: () -> Unit,
) {
    val dateText = DateUtils.formatWorkoutHistoryDate(row.started_at)
    val durationText = row.duration?.takeIf { it.isNotBlank() } ?: "0 min"
    val volumeText = if (row.total_volume > 0) "${row.total_volume} kg" else "0 kg"
    val subtitle = buildString {
        if (dateText.isNotBlank()) append(dateText)
        append(" · ${row.exercise_count} Exercises")
        append(" · ${row.total_sets} Sets")
        append(" · $durationText")
        append(" · $volumeText")
    }.trimStart(' ', '·')

    WorkoutCard(
        title = row.title.ifBlank { "Workout" },
        exercises = subtitle,
        image = Res.drawable.img_workout,
        showPlayAndMenu = false,
        onClick = onClick,
    )
}
