package `in`.gym.trak.studio.features.members

import `in`.gym.trak.studio.components.AppScrollableSheetColumn

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import `in`.gym.trak.studio.components.AppScrollDefaults
import `in`.gym.trak.studio.components.AppScrollableScreen
import `in`.gym.trak.studio.components.bringIntoViewOnFocus
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import `in`.gym.trak.studio.base.Constants
import `in`.gym.trak.studio.components.AppEmptyStateView
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.MemberHeaderCard
import `in`.gym.trak.studio.data.model.PlanDTO
import `in`.gym.trak.studio.features.plans.PlanListRowFromDto
import `in`.gym.trak.studio.features.plans.planListDurationLabel
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.utils.DateUtils
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_cale
import gym.composeapp.generated.resources.ic_check
import gym.composeapp.generated.resources.ic_king
import gym.composeapp.generated.resources.img_no_plan
import `in`.gym.trak.studio.components.GymAppBar
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import kotlin.math.max

class AssignSubscriptionScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        AssignSubscriptionContent(onBackClick = { navigator?.pop() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignSubscriptionContent(
    onBackClick: () -> Unit,
    memberName: String = "John Den",
    memberImageUrl: String = "",
    membershipType: String = "Gold Annual Membership",
    memberPhone: String = "",
    telUri: String? = null,
    whatsappUrl: String? = null,
    plans: List<PlanDTO> = emptyList(),
    plansLoading: Boolean = false,
    isSubmitting: Boolean = false,
    onSubmit: (planId: String, startDateIso: String) -> Unit = { _, _ -> },
    onCreatePlanClick: () -> Unit = {},
) {
    val selectablePlans = remember(plans) { plans.filter { it.isActive } }
    var selectedPlan by remember(selectablePlans) { mutableStateOf(selectablePlans.firstOrNull()) }
    LaunchedEffect(selectablePlans) {
        if (selectedPlan == null || selectablePlans.none { it.id == selectedPlan?.id }) {
            selectedPlan = selectablePlans.firstOrNull()
        }
    }

    var startDateIso by remember { mutableStateOf(DateUtils.getCurrentDateIso()) }
    var finalPrice by remember(selectedPlan?.id) {
        mutableStateOf(selectedPlan?.let { centsToPriceInput(it.priceCents) } ?: "")
    }
    LaunchedEffect(selectedPlan?.id) {
        selectedPlan?.let { finalPrice = centsToPriceInput(it.priceCents) }
    }

    var paymentMethod by remember { mutableStateOf("cash") }
    var showPlanSheet by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()
    val planSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val startDateDisplay = remember(startDateIso) { formatAssignSubscriptionDate(startDateIso) }
    val endDateDisplay = remember(startDateIso, selectedPlan?.durationDays) {
        selectedPlan?.durationDays?.let { days ->
            formatAssignSubscriptionDate(calculateEndDateIso(startDateIso, days))
        }.orEmpty()
    }

    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            startDateIso = Instant.fromEpochMilliseconds(millis).toString()
                        }
                        showDatePicker = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                GymAppBar(
                    title = "Assign Subscription",
                    onBackClick = onBackClick

                )

            },
            containerColor = Color.Transparent
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AppScrollableScreen(
                    modifier = Modifier.fillMaxSize(),
                    scrollState = scrollState,
                    contentPadding = AppScrollDefaults.screenContentPadding(
                        topInset = AppScrollDefaults.transparentTopBarContentInset,
                    ),
                ) {
                    MemberHeaderCard(
                        imageUrl = memberImageUrl,
                        name = memberName.ifBlank { "Guest" },
                        membershipType = membershipType.ifBlank { "No Active Plan" },
                        onCallClick = {
                            val tel = telUri?.takeIf { it.isNotBlank() }
                            if (!tel.isNullOrBlank()) {
                                uriHandler.openUri(tel)
                            } else if (memberPhone.isNotBlank()) {
                                uriHandler.openUri("tel:$memberPhone")
                            }
                        },
                        onMessageClick = {
                            val wa = whatsappUrl?.takeIf { it.isNotBlank() }
                            if (!wa.isNullOrBlank()) {
                                uriHandler.openUri(wa)
                            } else if (memberPhone.isNotBlank()) {
                                val digits = memberPhone.filter { it.isDigit() }
                                if (digits.isNotBlank()) uriHandler.openUri("https://wa.me/$digits")
                            }
                        }
                    )

                    if (plansLoading) {
                        Spacer(modifier = Modifier.height(48.dp))
                    } else if (selectablePlans.isEmpty()) {
                        AppEmptyStateView(
                            image = Res.drawable.img_no_plan,
                            title = "No plans available",
                            subtitle = "Create a gym plan first, then assign it here."
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        CommonButton(
                            text = "Create plan",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = onCreatePlanClick,
                            enabled = !isSubmitting
                        )
                    } else {
                        Text(
                            text = "Subscription Plans",
                            style = AppTextTheme.bold.copy(fontSize = 14.sp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(White, RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFE8ECF4), RoundedCornerShape(12.dp))
                                .clickable(enabled = !isSubmitting && !plansLoading) {
                                    showPlanSheet = true
                                }
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = selectedPlan?.name ?: "Select plan",
                                style = AppTextTheme.medium.copy(fontSize = 16.sp)
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Gray
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        selectedPlan?.let { plan ->
                            CommonCard(
                                borderColor = Color(0xFFE8ECF4),
                                content = {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    painter = painterResource(Res.drawable.ic_king),
                                                    contentDescription = null,
                                                    tint = Color.Unspecified,
                                                    modifier = Modifier.size(36.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = plan.name,
                                                    style = AppTextTheme.bold.copy(fontSize = 16.sp)
                                                )
                                            }
                                            Text(
                                                text = plan.planListDurationLabel(),
                                                style = AppTextTheme.medium.copy(
                                                    fontSize = 14.sp,
                                                    color = PrimaryColor
                                                )
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))
                                        HorizontalDivider(
                                            modifier = Modifier.fillMaxWidth(),
                                            thickness = 1.dp,
                                            color = Color(0xFFDADADA).copy(alpha = 0.5f)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))

                                        Text(
                                            text = "Start Date",
                                            style = AppTextTheme.regular.copy(
                                                fontSize = 14.sp,
                                                color = Gray
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .background(White, RoundedCornerShape(12.dp))
                                                .border(
                                                    1.dp,
                                                    Color(0xFFE8ECF4),
                                                    RoundedCornerShape(12.dp)
                                                )
                                                .clickable(enabled = !isSubmitting) {
                                                    showDatePicker = true
                                                }
                                                .padding(horizontal = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = startDateDisplay.ifBlank { "Select date" },
                                                style = AppTextTheme.medium.copy(fontSize = 14.sp)
                                            )
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_cale),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = PrimaryColor
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(16.dp))

                                        AutoCalculatedEndDateBox(endDate = endDateDisplay)

//                                        Spacer(modifier = Modifier.height(20.dp))
//
//                                        Text(
//                                            text = "Payment Method",
//                                            style = AppTextTheme.regular.copy(
//                                                fontSize = 14.sp,
//                                                color = Gray
//                                            )
//                                        )
//                                        Spacer(modifier = Modifier.height(8.dp))
//                                        AssignSubscriptionPaymentToggle(
//                                            selected = paymentMethod,
//                                            enabled = !isSubmitting,
//                                            onSelected = { paymentMethod = it }
//                                        )
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            verticalAlignment = Alignment.CenterVertically,
//                            horizontalArrangement = Arrangement.SpaceBetween
//                        ) {
//                            Text(
//                                text = "Final Selling Price",
//                                style = AppTextTheme.bold.copy(fontSize = 16.sp)
//                            )
//                            Surface(
//                                color = PrimaryColor.copy(alpha = 0.20f),
//                                shape = RoundedCornerShape(100.dp)
//                            ) {
//                                Text(
//                                    text = "Adjustable",
//                                    modifier = Modifier.padding(
//                                        horizontal = 12.dp,
//                                        vertical = 4.dp
//                                    ),
//                                    style = AppTextTheme.medium.copy(
//                                        fontSize = 12.sp,
//                                        color = Black
//                                    )
//                                )
//                            }
//                        }
//                        Spacer(modifier = Modifier.height(8.dp))
//                        Row(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .bringIntoViewOnFocus(scrollState)
//                                .height(56.dp)
//                                .background(White, RoundedCornerShape(12.dp))
//                                .border(1.dp, Color(0xFFE8ECF4), RoundedCornerShape(12.dp))
//                                .padding(horizontal = 16.dp),
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Text(
//                                text = Constants.RUPEE,
//                                style = AppTextTheme.medium.copy(fontSize = 16.sp, color = Gray)
//                            )
//                            Spacer(modifier = Modifier.width(8.dp))
//                            BasicTextField(
//                                value = finalPrice,
//                                onValueChange = { input ->
//                                    finalPrice = input.filter { it.isDigit() || it == '.' }
//                                },
//                                enabled = !isSubmitting,
//                                textStyle = AppTextTheme.bold.copy(fontSize = 18.sp, color = Black),
//                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
//                                modifier = Modifier.fillMaxWidth()
//                            )
//                        }

//                        Spacer(modifier = Modifier.height(24.dp))

                        CommonButton(
                            onClick = {
                                val plan = selectedPlan ?: return@CommonButton
                                if (startDateIso.isNotBlank()) {
                                    onSubmit(plan.id, startDateIso)
                                }
                            },
                            text = if (isSubmitting) "Please wait…" else "Add Subscription",
                            enabled = !isSubmitting && selectedPlan != null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }

                if (plansLoading) {
                    AssignSubscriptionPlansLoadingOverlay()
                }
            }
        }

        if (showPlanSheet) {
            ModalBottomSheet(
                onDismissRequest = {
                    if (!isSubmitting) showPlanSheet = false
                },
                sheetState = planSheetState,
                containerColor = White,
                dragHandle = null
            ) {
                AssignSubscriptionPlanPickerSheet(
                    plans = selectablePlans,
                    plansLoading = plansLoading,
                    enabled = !isSubmitting && !plansLoading,
                    onPlanSelected = { plan ->
                        selectedPlan = plan
                        showPlanSheet = false
                    }
                )
            }
        }
    }
}

@Composable
private fun AssignSubscriptionPlansLoadingOverlay(
    message: String = "Loading plans…",
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = PrimaryColor)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray)
            )
        }
    }
}

@Composable
private fun AssignSubscriptionPlanPickerSheet(
    plans: List<PlanDTO>,
    plansLoading: Boolean,
    enabled: Boolean,
    onPlanSelected: (PlanDTO) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(Color(0xFFE0E0E0))
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Subscription Plans",
                style = AppTextTheme.bold.copy(fontSize = 18.sp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select a plan to assign",
                style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 160.dp, max = 480.dp)
            ) {
                if (plansLoading) {
                    AssignSubscriptionPlansLoadingOverlay()
                } else if (plans.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No plans available",
                            style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray)
                        )
                    }
                } else {
                    AppScrollableSheetColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = AppScrollDefaults.sheetContentPadding(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        plans.forEach { plan ->
                            PlanListRowFromDto(
                                plan = plan,
                                primaryButtonText = "Select plan",
                                enabled = enabled,
                                onPrimaryClick = { onPlanSelected(plan) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoCalculatedEndDateBox(endDate: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    color = Gray.copy(alpha = 0.45f),
                    style = Stroke(
                        width = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    ),
                    cornerRadius = CornerRadius(12.dp.toPx())
                )
            }
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Auto-Calculated End Date",
                    style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = endDate.ifBlank { "—" },
                    style = AppTextTheme.bold.copy(fontSize = 16.sp)
                )
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PrimaryColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_check),
                    contentDescription = null,
                    tint = PrimaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AssignSubscriptionPaymentToggle(
    selected: String,
    enabled: Boolean,
    onSelected: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf("cash" to "Cash", "upi" to "UPI").forEach { (id, label) ->
            val isSelected = selected == id
            Surface(
                onClick = { if (enabled) onSelected(id) },
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp),
                color = if (isSelected) PrimaryColor else Color(0xFFF7F8F9),
                border = if (!isSelected) {
                    BorderStroke(1.dp, Color(0xFFE8ECF4))
                } else null
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = label,
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

private fun centsToPriceInput(cents: Int): String {
    val whole = cents / 100
    val fraction = (cents % 100).toString().padStart(2, '0')
    return "$cents"
}

private fun parseStartToLocalDate(startIso: String): LocalDate {
    val raw = startIso.trim()
    val dateOnly = raw.substringBefore("T").take(10)
    return if (dateOnly.length == 10 && dateOnly[4] == '-' && dateOnly[7] == '-') {
        LocalDate.parse(dateOnly)
    } else {
        Instant.parse(raw).toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
}

fun calculateEndDateIso(startIso: String, durationDays: Int): String {
    if (durationDays <= 0) return startIso
    return try {
        val start = parseStartToLocalDate(startIso)
        val end = start.plus(max(0, durationDays - 1), DateTimeUnit.DAY)
        end.toString()
    } catch (_: Throwable) {
        startIso
    }
}

fun formatAssignSubscriptionDate(isoValue: String): String {
    if (isoValue.isBlank()) return ""
    return try {
        val date = parseStartToLocalDate(isoValue)
        val month = shortMonthName(date.monthNumber)
        "${month} ${date.dayOfMonth}, ${date.year}"
    } catch (_: Throwable) {
        DateUtils.formatBirthDateForDisplay(isoValue)
    }
}

private fun shortMonthName(month: Int): String = when (month) {
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
