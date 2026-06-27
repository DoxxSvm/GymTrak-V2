package `in`.gym.trak.studio.features.payments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.gym_boy
import gym.composeapp.generated.resources.img_no_payment
import `in`.gym.trak.studio.base.Constants
import `in`.gym.trak.studio.components.AppEmptyStateView
import `in`.gym.trak.studio.components.ChipDropdownMenu
import `in`.gym.trak.studio.components.PaymentHistoryCard
import `in`.gym.trak.studio.components.SearchBar
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import `in`.gym.trak.studio.components.toPaymentHistoryCardModel
import `in`.gym.trak.studio.data.model.PaymentAnalyticsResponse
import `in`.gym.trak.studio.data.model.PaymentItemDTO
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.PrimaryLightColor
import `in`.gym.trak.studio.utils.ShareService
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import kotlin.math.max
import kotlin.math.roundToInt

private fun formatChartAxisValue(value: Float): String {
    val v = value.roundToInt()
    return when {
        v >= 1_000_000 -> "${v / 1_000_000}M"
        v >= 1_000 -> "${v / 1_000}K"
        else -> v.toString()
    }
}

private fun formatChartBarValue(value: Float): String {
    val v = value.roundToInt()
    return when {
        v >= 100_000 -> "${(v / 1_000f).roundToInt()}K"
        v >= 1_000 -> {
            val thousands = v / 1_000f
            val tenths = (thousands * 10).roundToInt()
            if (tenths % 10 == 0) "${tenths / 10}K" else "${tenths / 10f}K"
        }
        else -> v.toString()
    }
}

internal fun formatRevenueTotalDisplay(amount: Double): String {
    val v = amount.roundToInt()
    return if (v >= 100_000) {
        formatChartBarValue(amount.toFloat())
    } else {
        v.toString()
    }
}

private fun chartMonthShortName(month: Int): String = when (month) {
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
    else -> month.toString()
}

private data class RevenueChartPoint(
    val label: String,
    val value: Float,
)

/** Maps analytics `chart_data` into chart labels and bar heights for the selected period. */
private fun buildRevenueChartPoints(
    analytics: PaymentAnalyticsResponse?,
    selectedPeriod: String,
): List<RevenueChartPoint> {
    if (analytics == null) return emptyList()
    val responseRange = analytics.range.trim().lowercase()
    val period = selectedPeriod.trim().lowercase()
    if (responseRange.isNotEmpty() && responseRange != period) return emptyList()

    return analytics.chart_data
        .mapNotNull { point ->
            val label = point.axisLabel.takeIf { it != "—" } ?: return@mapNotNull null
            val revenue = point.revenue
            if (!revenue.isFinite()) return@mapNotNull null
            RevenueChartPoint(label = label, value = revenue.toFloat().coerceAtLeast(0f))
        }
}

private fun resolveAnalyticsTotalRevenue(analytics: PaymentAnalyticsResponse?): Double {
    if (analytics == null) return 0.0
    val total = analytics.total_revenue
    if (total.isFinite() && total > 0.0) return total
    val fromChart = analytics.chart_data.sumOf { entry ->
        if (entry.revenue.isFinite()) entry.revenue else 0.0
    }
    if (fromChart > 0.0) return fromChart
    val fromCents = analytics.totalCents
    return if (fromCents > 0) fromCents.toDouble() else 0.0
}

/** Turns API `month` values (`05`, `2026-05`, `2026-05-26`, `May`) into readable axis labels. */
private fun formatChartPeriodLabel(label: String, period: String): String {
    val trimmed = label.trim()
    if (trimmed.isEmpty()) return "—"
    if (trimmed.length <= 3 && trimmed.all { it.isLetter() }) return trimmed

    if (trimmed.length <= 2 && trimmed.all { it.isDigit() }) {
        val monthNum = trimmed.toIntOrNull()
        if (monthNum != null && monthNum in 1..12) {
            return when (period.lowercase()) {
                "weekly" -> "W$monthNum"
                else -> chartMonthShortName(monthNum)
            }
        }
    }

    val datePart = trimmed.substringBefore("T").take(10)
    if (datePart.length >= 7 && datePart[4] == '-') {
        val year = datePart.take(4)
        val monthNum = datePart.substring(5, 7).toIntOrNull()
        val day = datePart.getOrNull(8)?.let {
            if (datePart.length >= 10) datePart.substring(8, 10).toIntOrNull() else null
        }
        return when (period.lowercase()) {
            "yearly" -> year.takeLast(2).let { "'$it" }
            "weekly" -> if (day != null && monthNum != null) {
                "$day ${chartMonthShortName(monthNum)}"
            } else {
                monthNum?.let { chartMonthShortName(it) } ?: trimmed.take(6)
            }
            else -> monthNum?.let { chartMonthShortName(it) } ?: trimmed.take(6)
        }
    }

    return trimmed.take(8)
}

@Composable
fun PaymentsScreen() {
    val screenModel = remember{ OwnerDashboardScreenModel() }
    val analytics by screenModel.paymentAnalytics.collectAsState()
    val analyticsLoading by screenModel.paymentAnalyticsLoading.collectAsState()
    val analyticsError by screenModel.paymentAnalyticsError.collectAsState()
    val payments by screenModel.payments.collectAsState()
    val isLoading by screenModel.isLoading.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedPeriod by remember { mutableStateOf("monthly") }

    LaunchedEffect(selectedPeriod) {
        screenModel.loadPaymentAnalytics(range = selectedPeriod)
    }

    LaunchedEffect(searchQuery) {
        // Debounce search
        if (searchQuery.isNotEmpty()) {
            delay(500)
        }
        screenModel.loadPayments(search = searchQuery, reset = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp)
        ) {
            // Revenue Overview chart
            item {
                Spacer(modifier = Modifier.height(16.dp))

                StaggeredEntranceItem(index = 0) {
                    RevenueOverviewCard(
                        analytics = analytics,
                        selectedPeriod = selectedPeriod,
                        isLoading = analyticsLoading,
                        errorMessage = analyticsError,
                        onPeriodChange = { selectedPeriod = it },
                        onRetry = { screenModel.loadPaymentAnalytics(range = selectedPeriod) },
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Payment History header
            item {
                StaggeredEntranceItem(index = 1) {
                    Column {
                        Text(
                            text = "Payment History",
                            style = AppTextTheme.bold.copy(fontSize = 20.sp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        // History search
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            placeholder = "Search by name or Phone"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Payment History items
            if (!isLoading && payments.isEmpty()) {
                item {
                    StaggeredEntranceItem(index = 2) {
                        AppEmptyStateView(
                            image = Res.drawable.img_no_payment,
                            title = "No Payment Records Found",
                            subtitle = if (searchQuery.isNotEmpty()) {
                                "No payments match your search query."
                            } else {
                                "You haven't added any payment records yet."
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        )
                    }
                }
            } else {
                itemsIndexed(payments) { index, item: PaymentItemDTO ->
                    StaggeredEntranceItem(index = 2 + index) {
                        PaymentHistoryCard(
                            model = item.toPaymentHistoryCardModel(),
                            onShare = { ShareService.sharePaymentItem(item) },
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // Pagination: load more when near end
                    if (index >= payments.size - 5 && !isLoading) {
                        val currentSize = payments.size
                        LaunchedEffect(currentSize) {
                            screenModel.loadPayments(search = searchQuery, reset = false)
                        }
                    }
                }
            }



            if (isLoading) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryColor)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// ─── Revenue Overview Card ────────────────────────────────────────────────────

@Composable
fun RevenueOverviewCard(
    analytics: PaymentAnalyticsResponse?,
    selectedPeriod: String,
    isLoading: Boolean,
    errorMessage: String?,
    onPeriodChange: (String) -> Unit,
    onRetry: () -> Unit,
) {
    val periods = listOf("Weekly", "Monthly", "Yearly")

    val chartPoints = buildRevenueChartPoints(analytics, selectedPeriod)
    val months = chartPoints.map { it.label }
    val values = chartPoints.map { it.value }
    val peakIndex = if (values.isNotEmpty()) {
        values.indices.maxByOrNull { values[it] } ?: -1
    } else {
        -1
    }
    val maxValue = values.maxOrNull() ?: 0f
    val totalRevenue = resolveAnalyticsTotalRevenue(analytics)
    val txnCount = analytics?.total_transactions ?: 0
    var chartExpanded by remember { mutableStateOf(true) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { chartExpanded = !chartExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Revenue Overview",
                            style = AppTextTheme.bold.copy(fontSize = 18.sp)
                        )
                        Text(
                            text = buildString {
                                append("Total: ${Constants.RUPEE} ")
                                append(formatRevenueTotalDisplay(totalRevenue))
                                if (txnCount > 0) append(" · $txnCount payments")
                            },
                            style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray)
                        )
                    }
                    Icon(
                        imageVector = if (chartExpanded) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = if (chartExpanded) "Collapse chart" else "Expand chart",
                        tint = Gray,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(22.dp),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                ChipDropdownMenu(
                    selectedLabel = selectedPeriod.replaceFirstChar { it.uppercase() },
                    options = periods,
                    onOptionSelected = { period ->
                        onPeriodChange(period.lowercase())
                    },
                )
            }

            AnimatedVisibility(
                visible = chartExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))

                    RevenueBarChart(
                        months = months,
                        values = values,
                        peakIndex = peakIndex,
                        maxValue = maxValue,
                        selectedPeriod = selectedPeriod,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        onRetry = onRetry,
                    )
                }
            }
        }
    }
}

@Composable
fun RevenueBarChart(
    months: List<String>,
    values: List<Float>,
    peakIndex: Int,
    maxValue: Float,
    selectedPeriod: String,
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
) {
    val chartHeight = 172.dp
    val hasValidSeries = months.size == values.size && months.isNotEmpty()
    val hasData = hasValidSeries && maxValue > 0f
    val chartMax = if (hasData) maxValue * 1.12f else 1f
    val yAxisSteps = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
    val yLabels = if (hasData) {
        yAxisSteps.map { step -> formatChartAxisValue(chartMax * step) }
    } else {
        listOf("0", "0", "0", "0", "0")
    }
    val chartBackground = Color(0xFFF8FAFC)

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .width(12.dp)
                .height(chartHeight),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            yLabels.reversed().forEach { label ->
                Text(
                    text = label,
                    style = AppTextTheme.regular.copy(fontSize = 10.sp, color = Gray),
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .background(chartBackground)
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = PrimaryColor,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }

                    !errorMessage.isNullOrBlank() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = errorMessage,
                                style = AppTextTheme.medium.copy(fontSize = 13.sp, color = Gray),
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tap to retry",
                                style = AppTextTheme.semiBold.copy(fontSize = 13.sp, color = PrimaryColor),
                                modifier = Modifier.clickable(onClick = onRetry),
                            )
                        }
                    }

                    !hasValidSeries || !hasData -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (hasValidSeries) {
                                    "No revenue data for this period"
                                } else {
                                    "No chart data available"
                                },
                                style = AppTextTheme.medium.copy(fontSize = 13.sp, color = Gray),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                    }

                    else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        values.forEachIndexed { index, value ->
                            val periodLabel = months.getOrNull(index).orEmpty()
                            RevenueChartBarColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                value = value,
                                chartMax = chartMax,
                                periodLabel = formatChartPeriodLabel(periodLabel, selectedPeriod),
                                isPeak = index == peakIndex,
                                barCount = values.size,
                            )
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun RevenueChartBarColumn(
    modifier: Modifier = Modifier,
    value: Float,
    chartMax: Float,
    periodLabel: String,
    isPeak: Boolean,
    barCount: Int,
) {
    val heightFraction = if (chartMax > 0f) (value / chartMax).coerceIn(0f, 1f) else 0f
    val barMaxHeight = 96.dp
    val barHeight = max(4, (barMaxHeight.value * heightFraction).roundToInt()).dp
    val barWidth = when {
        barCount <= 2 -> 44.dp
        barCount <= 4 -> 36.dp
        else -> 28.dp
    }

    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            if (value > 0f) {
                Text(
                    text = "${Constants.RUPEE}${formatChartBarValue(value)}",
                    style = AppTextTheme.bold.copy(
                        fontSize = 9.sp,
                        color = if (isPeak) PrimaryColor else Gray
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 72.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(barHeight)
                    .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .background(
                        if (isPeak) {
                            Brush.verticalGradient(
                                colors = listOf(PrimaryColor, PrimaryLightColor.copy(alpha = 0.9f))
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    PrimaryColor.copy(alpha = 0.45f),
                                    PrimaryLightColor.copy(alpha = 0.22f)
                                )
                            )
                        }
                    )
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = periodLabel,
            style = AppTextTheme.semiBold.copy(
                fontSize = 10.sp,
                color = if (isPeak) PrimaryColor else Gray
            ),
            textAlign = TextAlign.Center,
            maxLines = 2,
            lineHeight = 12.sp,
            modifier = Modifier.heightIn(min = 24.dp)
        )
    }
}
