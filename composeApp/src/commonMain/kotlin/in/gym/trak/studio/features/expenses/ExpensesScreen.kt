package `in`.gym.trak.studio.features.expenses

import `in`.gym.trak.studio.base.Constants

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import `in`.gym.trak.studio.components.AppEmptyStateView
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.data.repository.SessionManager.hasPermission
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.utils.DateUtils
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_gym
import gym.composeapp.generated.resources.ic_money
import gym.composeapp.generated.resources.ic_subscription
import gym.composeapp.generated.resources.ic_workout
import gym.composeapp.generated.resources.img_expenes_bg
import gym.composeapp.generated.resources.img_no_expenses
import gym.composeapp.generated.resources.img_no_plan
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonOutlineButton
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import org.jetbrains.compose.resources.painterResource

/**
 * Screen for viewing and filtering gym expenses (Image 4).
 */
class ExpensesScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val expenses by screenModel.expenses.collectAsState()
        val totalExpenses by screenModel.totalExpenseAmount.collectAsState()
        val lastMonthPer by screenModel.lastMonthPer.collectAsState()
        val loading by screenModel.expensesLoading.collectAsState()
        var isPullRefreshRequested by remember { mutableStateOf(false) }
        var selectedFilter by remember { mutableStateOf("This month") }

        var showMonthRangePicker by remember { mutableStateOf(false) }
        var isApplyingMonthRange by remember { mutableStateOf(false) }
        var dateFrom by remember { mutableStateOf<String?>(null) }
        var dateTo by remember { mutableStateOf<String?>(null) }
        var rangeLabel by remember { mutableStateOf(DateUtils.expenseFilterRangeLabel("This month")) }
        val monthRangeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        val pullToRefreshState = rememberPullToRefreshState()

        fun reloadExpenses() {
            val (month, from, to) = resolveExpenseQueryParams(selectedFilter, dateFrom, dateTo)
            screenModel.loadExpenses(month = month, dateFrom = from, dateTo = to)
        }

        LaunchedEffect(selectedFilter, dateFrom, dateTo) {
            reloadExpenses()
        }
        LaunchedEffect(loading) {
            if (!loading) {
                isPullRefreshRequested = false
                if (isApplyingMonthRange) {
                    isApplyingMonthRange = false
                    showMonthRangePicker = false
                }
            }
        }

        LoadingScreenHandler(
            screenModel = screenModel,
            showLoadingOverlay = !isPullRefreshRequested
        ) {
            Scaffold(
                topBar = {
                    GymAppBar(
                        title = "Expenses",
                        onBackClick = { navigator?.pop() },
                        actions = {}
                    )
                },
                floatingActionButton = {
                    if (hasPermission(SessionManager.PermissionKeys.KEY_EXPENSE_CREATE)) {
                        FloatingActionButton(
                            onClick = {
                                navigator?.push(AddExpenseScreen(onRefresh = { reloadExpenses() }))
                            },
                            containerColor = PrimaryColor,
                            contentColor = White,
                            shape = CircleShape
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Expense")
                        }
                    }
                },
                containerColor = Color.Transparent
            ) { padding ->
                PullToRefreshBox(
                    isRefreshing = isPullRefreshRequested && loading,
                    onRefresh = {
                        isPullRefreshRequested = true
                        reloadExpenses()
                    },
                    state = pullToRefreshState,
                    indicator = {},
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item { Spacer(modifier = Modifier.height(0.dp)) }

                        // Total Expenses Summary Card
                        item {
                            StaggeredEntranceItem(index = 0) {
                                Box() {
                                    Image(
                                        painter = painterResource(Res.drawable.img_expenes_bg),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clip(RoundedCornerShape(16.dp))
                                    )
                                    Column(modifier = Modifier.padding(20.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Total Expenses",
                                                style = AppTextTheme.medium.copy(
                                                    fontSize = 14.sp,
                                                    color = White.copy(alpha = 0.8f)
                                                )
                                            )
                                            Surface(
                                                color = White.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(8.dp),
                                                onClick = { showMonthRangePicker = true }
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(
                                                        horizontal = 8.dp,
                                                        vertical = 4.dp
                                                    ),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = rangeLabel,
                                                        style = AppTextTheme.medium.copy(
                                                            fontSize = 12.sp,
                                                            color = White
                                                        )
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Icon(
                                                        imageVector = Icons.Default.CalendarToday,
                                                        contentDescription = null,
                                                        tint = White,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        if (loading && expenses.isEmpty()) {
                                            CircularProgressIndicator(
                                                color = White,
                                                modifier = Modifier.size(32.dp),
                                                strokeWidth = 3.dp
                                            )
                                        } else {
                                            Text(
                                                text = "${Constants.RUPEE} ${totalExpenses.toString()}",
                                                style = AppTextTheme.bold.copy(
                                                    fontSize = 32.sp,
                                                    color = White
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Surface(
                                            color = White.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(100.dp)
                                        ) {
                                            Text(
                                                text = "+${lastMonthPer}% Vs Last Month",
                                                modifier = Modifier.padding(
                                                    horizontal = 12.dp,
                                                    vertical = 4.dp
                                                ),
                                                style = AppTextTheme.bold.copy(
                                                    fontSize = 12.sp,
                                                    color = White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Filter Chips
                        item {
                            StaggeredEntranceItem(index = 1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ExpenseFilterChip(
                                        "This month",
                                        selectedFilter == "This month" && dateFrom == null
                                    ) {
                                        selectedFilter = "This month"
                                        dateFrom = null
                                        dateTo = null
                                        rangeLabel = DateUtils.expenseFilterRangeLabel("This month")
                                    }
                                    ExpenseFilterChip("Last month", selectedFilter == "Last month" && dateFrom == null) {
                                        selectedFilter = "Last month"
                                        dateFrom = null
                                        dateTo = null
                                        rangeLabel = DateUtils.expenseFilterRangeLabel("Last month")
                                    }
                                    ExpenseFilterChip(
                                        "Yearly",
                                        selectedFilter == "Yearly" && dateFrom == null
                                    ) {
                                        selectedFilter = "Yearly"
                                        dateFrom = null
                                        dateTo = null
                                        rangeLabel = DateUtils.expenseFilterRangeLabel("Yearly")
                                    }
                                }
                            }
                        }

                        // Expense List
                        if (loading && expenses.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(220.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = PrimaryColor)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Loading expenses…",
                                            style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray)
                                        )
                                    }
                                }
                            }
                        } else if (expenses.isEmpty()) {
                            item {
                                StaggeredEntranceItem(index = 2) {
                                    AppEmptyStateView(
                                        image = Res.drawable.img_no_expenses,
                                        title = "No Expense Records",
                                        subtitle = "You haven't added any expenses for this period yet.",
                                        modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                    )
                                }
                            }
                        } else {
                            if (loading) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = PrimaryColor,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                            itemsIndexed(expenses) { index, expense ->
                                StaggeredEntranceItem(index = index + 2) {
                                    val (icon, color) = getCategoryResources(expense.category)
                                    Box(modifier = Modifier.clickable {
                                        navigator?.push(
                                            AddExpenseScreen(
                                                expenseId = expense.id,
                                                onRefresh = { reloadExpenses() },
                                            )
                                        )
                                    }) {
                                        ExpenseListItem(
                                            name = expense.bill_name ?: "Unknown Expense",
                                            category = expense.category,
                                            date = expense.date ?: "",
                                            amount = "${Constants.RUPEE} ${expense.amount}",
                                            icon = icon,
                                            iconBgColor = color
                                        )
                                    }
                                }
                            }
                        }

                        item { Spacer(modifier = Modifier.height(40.dp)) }
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
                }
            ) {
                MonthRangePickerSheetContent(
                    isApplying = isApplyingMonthRange,
                    isLoading = loading && isApplyingMonthRange,
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
                    }
                )
            }
        }
    }
}



/** Maps UI filter chips to expenses API query params (`filter`, `dateFrom`, `dateTo`). */
private fun resolveExpenseQueryParams(
    selectedFilter: String,
    dateFrom: String?,
    dateTo: String?,
): Triple<String?, String?, String?> {
    if (!dateFrom.isNullOrBlank() && !dateTo.isNullOrBlank()) {
        return Triple(null, dateFrom, dateTo)
    }
    val monthFilter = when (selectedFilter) {
        "This month" -> "this_month"
        "Last month" -> "last_month"
        "Yearly" -> "yearly"
        else -> null
    }
    return Triple(monthFilter, null, null)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MonthRangePickerSheetContent(
    isApplying: Boolean,
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onRangeSelected: (Int, Int) -> Unit
) {
    val showSheetLoader = isApplying || isLoading
    var fromIndex by remember { mutableStateOf<Int?>(null) }
    var toIndex by remember { mutableStateOf<Int?>(null) }
    val months = DateUtils.getFullMonthNames()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Select Month Range",
                style = AppTextTheme.bold.copy(fontSize = 18.sp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap to select start and end month",
                style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                FlowRow(
                    modifier = Modifier
                        .heightIn(max = 320.dp)
                        .wrapContentWidth(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 3
                ) {
                    months.forEachIndexed { index, month ->
                        val isSelected = when {
                            fromIndex == index || toIndex == index -> true
                            fromIndex != null && toIndex != null && index in (fromIndex!!..toIndex!!) -> true
                            else -> false
                        }
                        val isStartOrEnd = fromIndex == index || toIndex == index

                        Surface(
                            onClick = {
                                if (showSheetLoader) return@Surface
                                when {
                                    fromIndex == null -> fromIndex = index
                                    toIndex == null -> {
                                        if (index < fromIndex!!) {
                                            toIndex = fromIndex
                                            fromIndex = index
                                        } else {
                                            toIndex = index
                                        }
                                    }
                                    else -> {
                                        fromIndex = index
                                        toIndex = null
                                    }
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                PrimaryColor.copy(alpha = if (isStartOrEnd) 1f else 0.2f)
                            } else {
                                Color(0xFFF1F5F9)
                            },
                            modifier = Modifier
                                .width(80.dp)
                                .height(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = month.take(3),
                                    style = AppTextTheme.medium.copy(
                                        fontSize = 14.sp,
                                        color = if (isStartOrEnd) White else if (isSelected) PrimaryColor else Black
                                    )
                                )
                            }
                        }
                    }
                }
            }
//            FlowRow(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .heightIn(max = 320.dp),
//                horizontalArrangement = Arrangement.spacedBy(8.dp),
//                verticalArrangement = Arrangement.spacedBy(8.dp),
//                maxItemsInEachRow = 3
//            ) {
//                months.forEachIndexed { index, month ->
//                    val isSelected = when {
//                        fromIndex == index || toIndex == index -> true
//                        fromIndex != null && toIndex != null && index in (fromIndex!!..toIndex!!) -> true
//                        else -> false
//                    }
//                    val isStartOrEnd = fromIndex == index || toIndex == index
//
//                    Surface(
//                        onClick = {
//                            if (showSheetLoader) return@Surface
//                            when {
//                                fromIndex == null -> fromIndex = index
//                                toIndex == null -> {
//                                    if (index < fromIndex!!) {
//                                        toIndex = fromIndex
//                                        fromIndex = index
//                                    } else {
//                                        toIndex = index
//                                    }
//                                }
//                                else -> {
//                                    fromIndex = index
//                                    toIndex = null
//                                }
//                            }
//                        },
//                        shape = RoundedCornerShape(12.dp),
//                        color = if (isSelected) {
//                            PrimaryColor.copy(alpha = if (isStartOrEnd) 1f else 0.2f)
//                        } else {
//                            Color(0xFFF1F5F9)
//                        },
//                        modifier = Modifier.width(80.dp).height(40.dp)
//                    ) {
//                        Box(contentAlignment = Alignment.Center) {
//                            Text(
//                                text = month.take(3),
//                                style = AppTextTheme.medium.copy(
//                                    fontSize = 14.sp,
//                                    color = if (isStartOrEnd) White else if (isSelected) PrimaryColor else Black
//                                )
//                            )
//                        }
//                    }
//                }
//            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = Color(0xFFF1F5F9))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CommonOutlineButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    textColor = Black,
                    modifier = Modifier.weight(1f),
                    enabled = !showSheetLoader
                )
                CommonButton(
                    text = if (showSheetLoader) "Applying…" else "Apply",
                    onClick = {
                        if (fromIndex != null) {
                            onRangeSelected(fromIndex!!, toIndex ?: fromIndex!!)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = fromIndex != null && !showSheetLoader
                )
            }
        }

        if (showSheetLoader) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(White.copy(alpha = 0.78f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Loading expenses…",
                        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseFilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(100.dp),
        color = if (isSelected) PrimaryColor else White,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFFF1F5F9)
        ),
        modifier = Modifier.height(36.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = AppTextTheme.regular.copy(
                    fontSize = 14.sp,
                    color = if (isSelected) White else Gray
                )
            )
        }
    }
}

fun getCategoryResources(category: String): Pair<org.jetbrains.compose.resources.DrawableResource, Color> {
    return when (category.uppercase()) {
        "RENT" -> Res.drawable.ic_gym to Color(0xFFFFF7E6)
        "SALARY" -> Res.drawable.ic_money to Color(0xFFE7F7F2)
        "EQUIPMENT" -> Res.drawable.ic_workout to Color(0xFFE7F7F2)
        "MAINTENANCE" -> Res.drawable.ic_gym to Color(0xFFFFF7E6)
        "MARKETING" -> Res.drawable.ic_subscription to Color(0xFFF1F5F9)
        "UTILITIES" -> Res.drawable.ic_money to Color(0xFFE7F7F2)
        else -> Res.drawable.ic_gym to Color(0xFFF1F5F9)
    }
}

fun formatCurrency(amount: Double): String {
    val rounded = amount.toLong()
    val parts = amount.toString().split(".")
    val decimal = if (parts.size > 1) parts[1].padEnd(2, '0').take(2) else "00"
    val whole = rounded.toString()
    val withCommas = whole.reversed().chunked(3).joinToString(",").reversed()
    return "${Constants.RUPEE} $withCommas.$decimal"
}
