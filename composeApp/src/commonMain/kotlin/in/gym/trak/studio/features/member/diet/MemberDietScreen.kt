package `in`.gym.trak.studio.features.member

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.components.AppEmptyStateView
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import `in`.gym.trak.studio.components.CircularProgress
import `in`.gym.trak.studio.components.CommonOutlineButton
import `in`.gym.trak.studio.data.model.DietHistoryFoodItem
import `in`.gym.trak.studio.data.model.DietHistoryMealLog
import `in`.gym.trak.studio.data.model.recurringMealsForDay
import `in`.gym.trak.studio.data.model.toDietRepeatDayIndex
import `in`.gym.trak.studio.data.model.toDietMealDTO
import `in`.gym.trak.studio.data.model.toHistoryMealLog
import `in`.gym.trak.studio.theme.*
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import gym.composeapp.generated.resources.*
import `in`.gym.trak.studio.data.repository.SessionManager.memberGymUserId
import org.jetbrains.compose.resources.painterResource
import `in`.gym.trak.studio.getCurrentTimeMillis
import kotlinx.datetime.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.DrawableResource

private enum class DietMealTab {
    Consumed,
    Recurring,
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun MemberDietScreen() {
    val navigator = LocalNavigator.currentOrThrow
    val screenModel = remember { OwnerDashboardScreenModel() }
    val history by screenModel.memberDietHistory.collectAsState()
    val loading by screenModel.dietHistoryLoading.collectAsState()
    val refreshing by screenModel.dietHistoryRefreshing.collectAsState()
    var selectedDate by remember {
        mutableStateOf(Instant.fromEpochMilliseconds(getCurrentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault()).date)
    }
    var selectedMealTab by remember { mutableStateOf(DietMealTab.Consumed) }
    val refreshAction = {
        screenModel.loadMemberDietHistory(date = selectedDate.toString(), isRefresh = true)
    }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = refreshAction
    )

    LaunchedEffect(selectedDate) {
        screenModel.loadMemberDietHistory(date = selectedDate.toString())
    }

    LoadingScreenHandler(screenModel = screenModel) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                item {
                    StaggeredEntranceItem(index = 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Diet",
                                    style = AppTextTheme.semiBold.copy(fontSize = 24.sp, color = Color.Black)
                                )
                                Text(
                                    text = "Track Your Nutrition",
                                    style = AppTextTheme.regular.copy(fontSize = 14.sp, color = PrimaryColor)
                                )
                            }

                            CommonButton(
                                text = "Explore",
                                onClick = { navigator.push(ExploreDietScreen()) },
                                modifier = Modifier.width(100.dp)
                            )
                        }
                    }
                }

                item {
                    StaggeredEntranceItem(index = 1) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(100.dp),
                            color = OffGreenColor
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    null,
                                    tint = Black,
                                    modifier = Modifier.clickable {
                                        selectedDate = selectedDate.minus(1, DateTimeUnit.DAY)
                                    }
                                )
                                Text(
                                    text = dateLabel(selectedDate),
                                    style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black)
                                )
                                Icon(
                                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    null,
                                    tint = Black,
                                    modifier = Modifier.clickable {
                                        val today = Instant.fromEpochMilliseconds(getCurrentTimeMillis())
                                            .toLocalDateTime(TimeZone.currentSystemDefault()).date
                                        if (selectedDate < today) {
                                            selectedDate = selectedDate.plus(1, DateTimeUnit.DAY)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    StaggeredEntranceItem(index = 2) {
                        val summary = history?.daily_summary
                        val macros = history?.macros
                        val consumedKcal = summary?.consumed_kcal ?: 0
                        val targetKcal = (summary?.target_kcal ?: 0).coerceAtLeast(1)
                        val progress = (consumedKcal.toFloat() / targetKcal.toFloat()).coerceIn(0f, 1f)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CommonCard(
                                modifier = Modifier.weight(1f).height(280.dp),
                                backgroundColor = DarkBlack,
                                elevation = 0.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        "Nutrition",
                                        modifier = Modifier.fillMaxWidth(),
                                        style = AppTextTheme.semiBold.copy(fontSize = 14.sp, color = PrimaryColor)
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    Box(contentAlignment = Alignment.Center) {
                                        CircularProgress(
                                            progress = if (progress <= 0f) 0.01f else progress,
                                            size = 140.dp,
                                            color = PrimaryColor,
                                            trackColor = White
                                        )
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                painterResource(Res.drawable.ic_fire),
                                                null,
                                                tint = Color.Unspecified,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Text(
                                                consumedKcal.toString(),
                                                style = AppTextTheme.bold.copy(fontSize = 16.sp, color = White)
                                            )
                                            Text(
                                                "${summary?.remaining_kcal ?: 0} Kcal left",
                                                style = AppTextTheme.medium.copy(
                                                    fontSize = 8.sp,
                                                    color = White.copy(0.6f)
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.weight(1f))

                                    Text(
                                        "Target ${summary?.target_kcal ?: 0} kcal",
                                        style = AppTextTheme.semiBold.copy(fontSize = 14.sp, color = White)
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(0.8f).height(280.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                MacroCard(
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFFB3E9BA),
                                    label = "Protein",
                                    icon = Res.drawable.ic_protein,
                                    value = macros?.protein_g?.toInt()?.toString() ?: "0",
                                    target = "${macros?.protein_g?.toInt() ?: 0}g"
                                )
                                MacroCard(
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFFB6ECF3),
                                    label = "Carbs",
                                    icon = Res.drawable.ic_carbs,
                                    value = macros?.carbs_g?.toInt()?.toString() ?: "0",
                                    target = "${macros?.carbs_g?.toInt() ?: 0}g"
                                )
                                MacroCard(
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFFFEC775),
                                    label = "Fat",
                                    icon = Res.drawable.ic_fat,
                                    value = macros?.fat_g?.toInt()?.toString() ?: "0",
                                    target = "${macros?.fat_g?.toInt() ?: 0}g"
                                )
                            }
                        }
                    }
                }

                item {
                    StaggeredEntranceItem(index = 3) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                DietListTab(
                                    label = "Consumed",
                                    selected = selectedMealTab == DietMealTab.Consumed,
                                    onClick = { selectedMealTab = DietMealTab.Consumed },
                                    modifier = Modifier.weight(1f),
                                )
                                DietListTab(
                                    label = "Recurring",
                                    selected = selectedMealTab == DietMealTab.Recurring,
                                    onClick = { selectedMealTab = DietMealTab.Recurring },
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            if (selectedMealTab == DietMealTab.Consumed) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        "Meal Logs",
                                        style = AppTextTheme.bold.copy(fontSize = 18.sp),
                                    )
                                    Row(
                                        modifier = Modifier.clickable {
                                            navigator.push(
                                                CreateMealScreen(
                                                    readOnly = false,
                                                    memberGymUserId = memberGymUserId,
                                                    hideRepeat = true,
                                                    consumeAfterCreate = true,
                                                    consumedOnDate = selectedDate.toString(),
                                                    onMealCreated = {
                                                        screenModel.loadMemberDietHistory(
                                                            date = selectedDate.toString(),
                                                            isRefresh = true,
                                                        )
                                                    },
                                                    onConsumed = {
                                                        screenModel.loadMemberDietHistory(
                                                            date = selectedDate.toString(),
                                                            isRefresh = true,
                                                        )
                                                        selectedMealTab = DietMealTab.Consumed
                                                    },
                                                )
                                            )
                                        },
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            "Add Meal ",
                                            style = AppTextTheme.medium.copy(fontSize = 14.sp, color = PrimaryColor),
                                        )
                                        Icon(
                                            Icons.Default.Add,
                                            null,
                                            tint = White,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(PrimaryColor),
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    "Recurring Meals",
                                    style = AppTextTheme.bold.copy(fontSize = 18.sp),
                                )
                            }
                        }
                    }
                }

                if (loading && history == null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryColor)
                        }
                    }
                } else {
                    when (selectedMealTab) {
                        DietMealTab.Consumed -> {
                            val mealLogs = history?.meal_logs.orEmpty()
                            if (mealLogs.isEmpty()) {
                                item {
                                    AppEmptyStateView(
                                        image = Res.drawable.img_no_wrokout,
                                        title = "No Meal added yet",
                                        subtitle = "No consumed meals found for this date.",
                                    )
                                }
                            } else {
                                items(mealLogs, key = { it.meal_label + (it.time ?: "") }) { log ->
                                    StaggeredEntranceItem(index = 4 + mealLogs.indexOf(log)) {
                                        DietMealHistoryCard(log)
                                    }
                                }
                            }
                        }

                        DietMealTab.Recurring -> {
                            val recurringMeals = recurringMealsForDay(
                                meals = history?.recurring_meals.orEmpty(),
                                dayIndex = selectedDate.toDietRepeatDayIndex(),
                            )
                            if (recurringMeals.isEmpty()) {
                                item {
                                    AppEmptyStateView(
                                        image = Res.drawable.img_no_wrokout,
                                        title = "No recurring meals",
                                        subtitle = "No recurring meals scheduled for this day.",
                                    )
                                }
                            } else {
                                items(
                                    items = recurringMeals,
                                    key = { meal -> "recurring_${selectedDate}_${meal.meal_id}" },
                                ) { meal ->
                                    StaggeredEntranceItem(
                                        index = 4 + recurringMeals.indexOf(meal),
                                    ) {
                                        DietMealHistoryCard(
                                            log = meal.toHistoryMealLog(),
                                            onConsume = {
                                                navigator.push(
                                                    ConsumeFoodScreen(
                                                        meal = meal.toDietMealDTO(),
                                                        memberGymUserId = memberGymUserId,
                                                        consumedOnDate = selectedDate.toString(),
                                                        onConsumed = {
                                                            screenModel.loadMemberDietHistory(
                                                                date = selectedDate.toString(),
                                                                isRefresh = true,
                                                            )
                                                            selectedMealTab = DietMealTab.Consumed
                                                        },
                                                    )
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = refreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                backgroundColor = White,
                contentColor = PrimaryColor
            )
        }
    }
}

@Composable
fun MacroCard(
    modifier: Modifier,
    color: Color,
    label: String,
    icon: DrawableResource,
    value: String,
    target: String
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = color,
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(icon), null, modifier = Modifier.size(16.dp), tint = Color.Unspecified)
                Spacer(modifier = Modifier.width(4.dp))
                Text(label, style = AppTextTheme.bold.copy(fontSize = 13.sp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { 0f },
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = PrimaryColor,
                trackColor = White.copy(0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(value, style = AppTextTheme.medium.copy(fontSize = 12.sp))
                Text(target, style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Black.copy(0.6f)))
            }
        }
    }
}

@Composable
private fun DietListTab(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(100.dp),
        color = if (selected) PrimaryColor else OffGreenColor,
        modifier = modifier,
    ) {
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            textAlign = TextAlign.Center,
            style = AppTextTheme.semiBold.copy(
                fontSize = 14.sp,
                color = if (selected) White else Black,
            ),
        )
    }
}

@Composable
private fun DietMealHistoryCard(
    log: DietHistoryMealLog,
    onConsume: (() -> Unit)? = null,
) {
    CommonCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = PrimaryColor.copy(alpha = 0.15f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(PrimaryColor.copy(alpha = 0.04f))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(log.meal_label, style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black))
                    Text(
                        "${log.total_calories} kcal${log.time?.let { "  •  $it" } ?: ""}",
                        style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
                    )
                }
            }
            log.items.forEach { item ->
                DietHistoryFoodRow(item)
            }
            if (onConsume != null) {
                CommonOutlineButton(
                    text = "Consume Food",
                    onClick = onConsume,
                    textColor = PrimaryColor,
                    borderColor = PrimaryColor.copy(alpha = 0.35f),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun DietHistoryFoodRow(item: DietHistoryFoodItem) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(item.name, style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black))
        Text(
            "${item.amount_display ?: ""} ${if (item.amount_display.isNullOrBlank()) "" else "•"} ${item.calories} kcal".trim(),
            style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
        )
    }
}

private fun dateLabel(date: LocalDate): String {
    val today = Instant.fromEpochMilliseconds(getCurrentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault()).date
    val title = if (date == today) "Today" else date.toString()
    val month = when (date.monthNumber) {
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
        else -> "Dec"
    }
    return "$title , $month ${date.dayOfMonth}"
}

