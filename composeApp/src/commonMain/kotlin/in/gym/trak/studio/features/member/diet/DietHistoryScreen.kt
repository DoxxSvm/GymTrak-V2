package `in`.gym.trak.studio.features.member

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.CircularProgress
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import `in`.gym.trak.studio.theme.*
import gym.composeapp.generated.resources.*
import `in`.gym.trak.studio.components.AppScrollDefaults
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.getCurrentTimeMillis
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource

/**
 * Screen showing the historical diet logs and nutritional summary for a specific day.
 */
class DietHistoryScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = remember { OwnerDashboardScreenModel() }
        val history by screenModel.memberDietHistory.collectAsState()
        val loading by screenModel.dietHistoryLoading.collectAsState()
        val refreshing by screenModel.dietHistoryRefreshing.collectAsState()
        var selectedDate by remember {
            mutableStateOf(Instant.fromEpochMilliseconds(getCurrentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault()).date)
        }
        var showCalendarPicker by remember { mutableStateOf(false) }
        val datePickerState = rememberDatePickerState()
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

        if (showCalendarPicker) {
            DatePickerDialog(
                onDismissRequest = { showCalendarPicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                selectedDate = `in`.gym.trak.studio.utils.DateUtils.epochMillisToLocalDate(millis)
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

        Scaffold(
            topBar = {
                GymAppBar(
                    title = "Diet History",
                    onBackClick = { navigator.pop() },
                    actions = {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = White,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            modifier = Modifier.padding(end = 16.dp),
                            onClick = { showCalendarPicker = true },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    selectedDate.toString(),
                                    style = AppTextTheme.medium.copy(fontSize = 12.sp, color = PrimaryColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(painterResource(Res.drawable.ic_cale), null, tint = PrimaryColor, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .pullRefresh(pullRefreshState)
            ) {
                AppScrollableScreen(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = AppScrollDefaults.screenContentPadding(horizontal = 20.dp),
                ) {
                    Spacer(modifier = Modifier.height(20.dp))

                    val summary = history?.daily_summary
                    val macros = history?.macros
                    val target = (summary?.target_kcal ?: 0).coerceAtLeast(1)
                    val consumed = summary?.consumed_kcal ?: 0
                    val progress = (consumed.toFloat() / target.toFloat()).coerceIn(0f, 1f)

                    CommonCard(
                        elevation = 2.dp,
                        borderColor = GrayBorderColor
                    ) {
                        Row(
                            modifier = Modifier.padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgress(
                                    progress = if (progress <= 0f) 0.01f else progress,
                                    size = 110.dp,
                                    color = PrimaryColor,
                                    trackColor = Color(0xFFF3F4F6)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "${summary?.remaining_kcal ?: 0}",
                                        style = AppTextTheme.bold.copy(fontSize = 18.sp)
                                    )
                                    Text("Remaining", style = AppTextTheme.regular.copy(fontSize = 10.sp, color = Gray))
                                }
                            }

                            Spacer(modifier = Modifier.width(28.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                                NutritionRow("Consumed", "$consumed Kcal", PrimaryColor)
                                HorizontalDivider(color = Color(0xFFF1F5F9))
                                NutritionRow("Target", "${summary?.target_kcal ?: 0} Kcal", Black)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MacroBox(
                            Modifier.weight(1f),
                            "Protein",
                            "${macros?.protein_g ?: 0}g",
                            ((macros?.protein_g ?: 0).toFloat() / 200f).coerceIn(0f, 1f),
                            Res.drawable.ic_protein,
                            Color(0xFFB3E9BA)
                        )
                        MacroBox(
                            Modifier.weight(1f),
                            "Carbs",
                            "${macros?.carbs_g ?: 0}g",
                            ((macros?.carbs_g ?: 0).toFloat() / 300f).coerceIn(0f, 1f),
                            Res.drawable.ic_carbs,
                            Color(0xFFB6ECF3)
                        )
                        MacroBox(
                            Modifier.weight(1f),
                            "Fats",
                            "${macros?.fat_g ?: 0}g",
                            ((macros?.fat_g ?: 0).toFloat() / 100f).coerceIn(0f, 1f),
                            Res.drawable.ic_fat,
                            Color(0xFFFEC775)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text("Meal Logs", style = AppTextTheme.bold.copy(fontSize = 18.sp))

                    Spacer(modifier = Modifier.height(16.dp))

                    val mealLogs = history?.meal_logs.orEmpty()
                    if (mealLogs.isEmpty() && !loading) {
                        Text(
                            text = "No meal logs found for this date.",
                            style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray)
                        )
                    } else {
                        mealLogs.forEachIndexed { index, meal ->
                            MealHistoryCard(
                                title = meal.meal_label?.ifBlank { meal.meal_type ?: "Meal" } ?: (meal.meal_type ?: "Meal"),
                                time = meal.time ?: "-",
                                kcal = meal.total_calories,
                                icon = mealIconForType(meal.meal_type ?: ""),
                                items = meal.items.map { item ->
                                    Triple(
                                        item.name ?: "Food item",
                                        item.amount_display?.ifBlank { "-" } ?: "-",
                                        "${item.calories} kcal"
                                    )
                                }
                            )
                            if (index < mealLogs.lastIndex) {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
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
            }
        }
    }

    @Composable
    fun NutritionRow(label: String, value: String, valueColor: Color) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = AppTextTheme.medium.copy(fontSize = 13.sp, color = Gray))
            Text(value, style = AppTextTheme.bold.copy(fontSize = 14.sp, color = valueColor))
        }
    }

    @Composable
    fun MacroBox(
        modifier: Modifier,
        label: String,
        value: String,
        progress: Float,
        icon: org.jetbrains.compose.resources.DrawableResource,
        iconBg: Color
    ) {
        CommonCard(modifier = modifier, elevation = 0.dp, borderColor = GrayBorderColor) {
            Column(modifier = Modifier.padding(12.dp)) {
                Box(
                    modifier = Modifier.size(28.dp).clip(CircleShape).background(iconBg.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(painterResource(icon), null, tint = Color.Unspecified, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(label, style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray))
                Text(value, style = AppTextTheme.bold.copy(fontSize = 16.sp))
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = iconBg, // Using the same color theme for progress
                    trackColor = iconBg.copy(alpha = 0.1f)
                )
            }
        }
    }

    @Composable
    fun MealHistoryCard(
        title: String,
        time: String,
        kcal: Int,
        icon: org.jetbrains.compose.resources.DrawableResource,
        items: List<Triple<String, String, String>>
    ) {
        CommonCard(elevation = 1.dp, borderColor = GrayBorderColor) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(CircleShape).background(OffGreenColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(painterResource(icon), null, tint = Color.Unspecified, modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(title, style = AppTextTheme.bold.copy(fontSize = 18.sp))
                        Text("$time • $kcal kcal", style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Gray))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items.forEach { (name, weight, cal) ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(name, style = AppTextTheme.medium.copy(fontSize = 14.sp))
                                Row {
                                    Text("$weight • ", style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray))
                                    Text(cal, style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun mealIconForType(type: String): org.jetbrains.compose.resources.DrawableResource {
        return when (type.uppercase()) {
            "BREAKFAST" -> Res.drawable.ic_breakfast
            "LUNCH" -> Res.drawable.ic_lunch
            "DINNER" -> Res.drawable.ic_dinner
            "SNACK", "SNACKS" -> Res.drawable.ic_breakfast
            else -> Res.drawable.ic_breakfast
        }
    }
}
