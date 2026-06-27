package `in`.gym.trak.studio.features.members

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import `in`.gym.trak.studio.components.AppScrollableScreen
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.CommonOutlineButton
import `in`.gym.trak.studio.components.ConfirmationDialog
import `in`.gym.trak.studio.components.DetailTabChip
import `in`.gym.trak.studio.components.AppEmptyStateView
import `in`.gym.trak.studio.components.DietPlanCard
import `in`.gym.trak.studio.components.FoodItem
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.components.MemberHeaderCard
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import `in`.gym.trak.studio.components.WorkoutPlanCard
import `in`.gym.trak.studio.data.model.CurrentSubscriptionDTO
import `in`.gym.trak.studio.data.model.activeSubscriptions
import `in`.gym.trak.studio.data.model.isCurrentSubscription
import `in`.gym.trak.studio.data.model.resolvedAmountPaid
import `in`.gym.trak.studio.data.model.resolvedAmountPending
import `in`.gym.trak.studio.data.model.contractTotalAmount
import `in`.gym.trak.studio.data.model.hasSellingPrice
import `in`.gym.trak.studio.data.model.resolvedSellingPrice
import `in`.gym.trak.studio.data.model.subscriptionActionLabel
import `in`.gym.trak.studio.data.model.ExtendPlanPaymentRequest
import `in`.gym.trak.studio.data.model.FreezeSubscriptionRequest
import `in`.gym.trak.studio.data.model.MemberDetailResponse
import `in`.gym.trak.studio.components.PaymentHistoryCard
import `in`.gym.trak.studio.components.toPaymentHistoryCardModel
import `in`.gym.trak.studio.data.model.UnfreezeSubscriptionRequest
import `in`.gym.trak.studio.features.payments.ExtendSubscriptionSheet
import `in`.gym.trak.studio.features.payments.FreezeSubscriptionSheet
import `in`.gym.trak.studio.features.payments.ReceivePaymentLockedContext
import `in`.gym.trak.studio.features.payments.ReceivePaymentSheet
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.GrayBorderColor
import `in`.gym.trak.studio.theme.GreenCardColor
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.PrimaryDarkColor
import `in`.gym.trak.studio.theme.PrimaryGreenColor
import `in`.gym.trak.studio.theme.RedColor
import `in`.gym.trak.studio.theme.RedDarkColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.theme.YellowColor
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_active
import gym.composeapp.generated.resources.ic_active_status
import gym.composeapp.generated.resources.ic_atm
import gym.composeapp.generated.resources.ic_cale
import gym.composeapp.generated.resources.ic_delete
import gym.composeapp.generated.resources.ic_diet
import gym.composeapp.generated.resources.ic_dob
import gym.composeapp.generated.resources.ic_gender
import gym.composeapp.generated.resources.ic_king
import gym.composeapp.generated.resources.ic_mail
import gym.composeapp.generated.resources.ic_optional
import gym.composeapp.generated.resources.ic_overdue
import gym.composeapp.generated.resources.ic_payment
import gym.composeapp.generated.resources.ic_phone
import gym.composeapp.generated.resources.ic_profile
import gym.composeapp.generated.resources.ic_share
import gym.composeapp.generated.resources.ic_subscription
import gym.composeapp.generated.resources.ic_wallet_filled
import gym.composeapp.generated.resources.ic_workout
import gym.composeapp.generated.resources.img_no_checkin
import gym.composeapp.generated.resources.img_no_expenses
import gym.composeapp.generated.resources.img_no_member_enrolled
import gym.composeapp.generated.resources.img_no_payment
import gym.composeapp.generated.resources.img_no_plan
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.base.Constants
import `in`.gym.trak.studio.utils.DateUtils
import `in`.gym.trak.studio.utils.ShareService
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import `in`.gym.trak.studio.features.member.AddExerciseScreen
import `in`.gym.trak.studio.features.member.CalendarDay
import `in`.gym.trak.studio.features.member.CreateMealScreen
import `in`.gym.trak.studio.data.model.DietMealDTO
import `in`.gym.trak.studio.data.model.formatDietMealFoodSummary
import `in`.gym.trak.studio.data.model.MemberAttendanceSummaryResponse
import `in`.gym.trak.studio.data.model.RecentLogDTO
import `in`.gym.trak.studio.data.model.filteredByAttendanceDate
import `in`.gym.trak.studio.data.model.PlanDTO
import `in`.gym.trak.studio.data.model.ReceivePaymentRequest
import `in`.gym.trak.studio.data.model.WorkoutDTO
import `in`.gym.trak.studio.data.model.createdByDisplayLabel
import `in`.gym.trak.studio.features.member.CreateWorkoutScreen
import `in`.gym.trak.studio.features.plans.AddPlanScreen
import `in`.gym.trak.studio.data.repository.SessionManager

import kotlin.jvm.Transient
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TextButton
import `in`.gym.trak.studio.getCurrentTimeMillis

class MemberDetailScreen(
    val memberId: String,
    @Transient private val onMemberDeleted: (() -> Unit)? = null,
    /** Tab id matching [MemberDetailContent] chips, e.g. `"Profile"`, `"Attendance"`. */
    @Transient val initialTab: String = "Profile",
    @Transient val initialShowReceivePayment: Boolean = false,
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val memberDetail by screenModel.memberDetail.collectAsState()
        val attendanceSummary by screenModel.memberAttendanceSummary.collectAsState()
        val isLoading by screenModel.isLoading.collectAsState()
        var isPullRefreshRequested by remember { mutableStateOf(false) }


        LaunchedEffect(memberId) {
            screenModel.loadMemberDetail(memberId)
            screenModel.loadPlans(showGlobalLoader = false)
        }
        LaunchedEffect(isLoading) {
            if (!isLoading) isPullRefreshRequested = false
        }

        LoadingScreenHandler(
            screenModel = screenModel,
            showLoadingOverlay = !isPullRefreshRequested
        ) {
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isPullRefreshRequested && isLoading,
                onRefresh = {
                    isPullRefreshRequested = true
                    screenModel.loadMemberDetail(memberId)
                },
                state = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState(),
                modifier = Modifier.fillMaxSize(),
                indicator = {}
            ) {
                MemberDetailContent(
                    memberId = memberId,
                    memberDetail = memberDetail,
                    onBackClick = { navigator?.pop() },
                    screenModel = screenModel,
                    onMemberDeleted = onMemberDeleted,
                    initialSelectedTab = initialTab,
                    initialShowReceivePayment = initialShowReceivePayment,
                )

            }
        }
    }
}

@Composable
@Preview
fun MemberDetailScreenPreview() {
    MemberDetailContent(
        memberId = "123",
        onBackClick = {},
        memberDetail = null,
        screenModel = OwnerDashboardScreenModel(),
        onMemberDeleted = null
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailContent(
    memberId: String,
    memberDetail: MemberDetailResponse?,
    onBackClick: () -> Unit,
    screenModel: OwnerDashboardScreenModel,
    onMemberDeleted: (() -> Unit)? = null,
    initialSelectedTab: String = "Profile",
    initialShowReceivePayment: Boolean = false,
) {

    val navigator = LocalNavigator.current
    var selectedTab by rememberSaveable { mutableStateOf(initialSelectedTab) }
    val attendanceSummary by screenModel.memberAttendanceSummary.collectAsState()
    val plans by screenModel.plans.collectAsState()
    val workouts by screenModel.workouts.collectAsState()
    val memberDietMeals by screenModel.memberDietMeals.collectAsState()
    val dietMealsLoading by screenModel.dietMealsLoading.collectAsState()

    /** True only when any active plan still has rupees pending (drives Receive / Collect payment UI). */
    val hasPendingPayment = remember(memberDetail) {
        memberDetail?.subscription?.activeSubscriptions()
            ?.any { it.resolvedAmountPending() > 0 } == true
    }

    /** Convert is for leads/enquiries only — not expired or inactive members. */
    val showConvertToMember = remember(memberDetail) {
        memberDetail?.isLead == true ||
            memberDetail?.lifecycleStatus.equals("lead", ignoreCase = true)
    }

    val receivePaymentLockedContext = remember(memberDetail, plans) {
        val sub = memberDetail?.subscription?.activeSubscriptions()
            ?.firstOrNull { it.resolvedAmountPending() > 0 }
            ?: memberDetail?.subscription?.activeSubscriptions()?.firstOrNull()
            ?: return@remember null
        val planId = resolveGymPlanForSubscription(sub, plans) ?: return@remember null
        val msId = sub.member_subscription_id?.takeIf { it.isNotBlank() } ?: return@remember null
        ReceivePaymentLockedContext(
            gymPlanId = planId,
            memberSubscriptionId = msId,
            planDisplayName = sub.plan_name,
            defaultAmountRupee = sub.resolvedAmountPending().takeIf { it > 0 } ?: 1,
        )
    }

    var selectedAttendanceDate by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedTab, memberDetail?.gymUserId, selectedAttendanceDate) {
        val gymUserId = memberDetail?.gymUserId?.takeIf { it.isNotBlank() } ?: return@LaunchedEffect
        if (selectedTab == "Attendance") {
            loadMemberAttendanceSummaryForDate(
                screenModel = screenModel,
                memberId = gymUserId,
                dateIso = selectedAttendanceDate,
            )
        }
    }
    LaunchedEffect(selectedTab) {
        if (selectedTab == "Workout") {
            screenModel.loadWorkouts(memberId)
        }
        if (selectedTab == "Diet" && memberId.isNotBlank()) {
            screenModel.loadMemberDiet(
                createdBy ="trainer",
                memberGymUserId =  memberId)
        }
    }


    // Bottom sheet state flags
    var showReceivePayment by remember { mutableStateOf(false) }
    var showExtend by remember { mutableStateOf(false) }
    var showFreeze by remember { mutableStateOf(false) }
    var selectedExtendSubscription by remember { mutableStateOf<CurrentSubscriptionDTO?>(null) }
    var collectPaymentSubmitting by remember { mutableStateOf(false) }
    var extendSubmitting by remember { mutableStateOf(false) }
    var freezeSubmitting by remember { mutableStateOf(false) }
    var unfreezeSubmitting by remember { mutableStateOf(false) }

    var didAutoOpenReceive by rememberSaveable(memberId) { mutableStateOf(false) }
    LaunchedEffect(initialShowReceivePayment, memberDetail, hasPendingPayment, memberId) {
        if (initialShowReceivePayment && !didAutoOpenReceive && memberDetail != null && hasPendingPayment) {
            didAutoOpenReceive = true
            selectedTab = "Subscriptions"
            showReceivePayment = true
        }
    }

    val receivePaymentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val extendSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val freezeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(showReceivePayment) {
        if (!showReceivePayment) collectPaymentSubmitting = false
    }

    // Dialog states
    var showDeleteMemberDialog by remember { mutableStateOf(false) }
    var showDeleteBiometricDialog by remember { mutableStateOf(false) }
    var workoutToDelete by remember { mutableStateOf<String?>(null) }
    var mealToDelete by remember { mutableStateOf<DietMealDTO?>(null) }

    if (showDeleteMemberDialog) {
        ConfirmationDialog(
            onDismissRequest = { showDeleteMemberDialog = false },
            onConfirm = {
                showDeleteMemberDialog = false
                screenModel.deleteMember(memberId) {
                    onMemberDeleted?.invoke()
                    onBackClick()
                }
            },
            title = "Delete Member",
            message = "Are you sure you want to delete this member? All their data will be permanently removed.",
            confirmText = "Delete",
            isDangerAction = true
        )
    }

    if (showDeleteBiometricDialog) {
        ConfirmationDialog(
            onDismissRequest = { showDeleteBiometricDialog = false },
            onConfirm = {
                // Actual biometric delete logic
            },
            title = "Delete Biometric",
            message = "Are you sure you want to delete this member's biometric data?",
            confirmText = "Delete",
            isDangerAction = true
        )
    }

    if (workoutToDelete != null) {
        ConfirmationDialog(
            onDismissRequest = { workoutToDelete = null },
            onConfirm = {
                val id = workoutToDelete!!
                workoutToDelete = null
                screenModel.deleteWorkout(id) {
                    screenModel.loadWorkouts(memberId)
                }
            },
            title = "Delete Workout",
            message = "Are you sure you want to delete this workout plan? This action cannot be undone.",
            confirmText = "Delete",
            isDangerAction = true
        )
    }

    if (mealToDelete != null) {
        ConfirmationDialog(
            onDismissRequest = { mealToDelete = null },
            onConfirm = {
                val meal = mealToDelete!!
                mealToDelete = null
                screenModel.deleteDietMeal(meal.id) {
                    screenModel.loadMemberDiet(memberDetail?.gymUserId ?: memberId)
                }
            },
            title = "Delete Meal",
            message = "Are you sure you want to delete correctly?",
            confirmText = "Delete",
            isDangerAction = true
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            GymAppBar(
                title = memberDetail?.user?.fullName ?: "Loading...",
                onBackClick = { navigator?.pop() }
            )
        },
        bottomBar = {
            if (selectedTab == "Profile") {
                Column(
                    modifier = Modifier.padding(16.dp)
                        .navigationBarsPadding()
                ) {

                    Row {
//                        // If member is expired but has a pending amount, show Collect Payment UI instead.
//                        if (memberDetail?.lifecycleStatus != "active" && !hasPendingPayment  && memberDetail?.lifecycleStatus != "expired" && memberDetail?.lifecycleStatus != "inactive" ) {
//                            CommonOutlineButton(
//                                onClick = {
//                                    memberDetail?.let {
//                                        screenModel.convertMember(it.gymUserId) {
//                                            // Refresh or notify
//                                        }
//                                    }
//                                },
//                                text = "Convert To Member",
//                                textColor = Black,
//                                modifier = Modifier.weight(1f)
//                            )
//                            Spacer(modifier = Modifier.width(16.dp))
//                        }

                        if (hasPendingPayment) {
                            CommonButton(
                                onClick = { showReceivePayment = true },
                                text = "Collect Payment",
                                modifier = Modifier.weight(1f)
                            )
                        }

                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    CommonButton(
                        onClick = { showDeleteMemberDialog = true },
                        color = RedColor,
                        leftIcon = painterResource(Res.drawable.ic_delete),
                        text = "Delete Member",


                        )


                }
            }
        },
        floatingActionButton = {
            if (selectedTab == "Subscriptions") {
                FloatingActionButton(
                    onClick = {
                        navigator?.push(
                            AssignSubscriptionPlanScreen(
                                memberGymUserId = memberDetail?.gymUserId ?: memberId,
                                memberName = memberDetail?.user?.fullName.orEmpty(),
                                memberImageUrl = memberDetail.memberAvatarUrl(),
                                membershipType = memberDetail?.subscription?.current_subscription?.plan_name
                                    ?: "No Active Plan",
                                memberPhone = memberDetail?.user?.phone.orEmpty(),
                                telUri = memberDetail?.contact?.telUri,
                                whatsappUrl = memberDetail?.contact?.whatsappUrl,
                                onSubscriptionCreated = {
                                    screenModel.loadMemberDetail(memberId)
                                }
                            )
                        )
                    },
                    containerColor = PrimaryColor,
                    contentColor = White,
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                }
            }

            if (selectedTab == "Workout") {
                FloatingActionButton(
                    onClick = { navigator?.push(AddExerciseScreen(memberId = memberDetail?.user?.id.toString())) },
                    containerColor = PrimaryColor,
                    contentColor = White,
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                }
            }

            if (selectedTab == "Diet") {
                FloatingActionButton(
                    onClick = {
                        navigator?.push(
                            CreateMealScreen(
                                memberGymUserId = memberDetail?.gymUserId ?: memberId,
                                onMealCreated = {
                                    screenModel.loadMemberDiet(memberDetail?.gymUserId ?: memberId)
                                }
                            )
                        )
                    },
                    containerColor = PrimaryColor,
                    contentColor = White,
                    shape = CircleShape
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { padding ->
        AppScrollableScreen(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            // Member Header Card
            StaggeredEntranceItem(index = 0) {
                val uriHandler = LocalUriHandler.current
                MemberHeaderCard(
                    imageUrl = memberDetail.memberAvatarUrl(),
                    name = memberDetail?.user?.fullName ?: "Guest",
                    membershipType = memberDetail?.subscription?.current_subscription?.plan_name
                        ?: "No Active Plan",
                    onCallClick = {
                        val telUri = memberDetail?.contact?.telUri?.takeIf { it.isNotBlank() }
                        if (!telUri.isNullOrBlank()) {
                            uriHandler.openUri(telUri)
                        } else {
                            memberDetail?.user?.phone?.let { phone ->
                                if (phone.isNotBlank()) {
                                    uriHandler.openUri("tel:$phone")
                                }
                            }
                        }
                    },
                    onMessageClick = {
                        val wa = memberDetail?.contact?.whatsappUrl?.takeIf { it.isNotBlank() }
                        if (!wa.isNullOrBlank()) {
                            uriHandler.openUri(wa)
                        } else {
                            memberDetail?.user?.phone?.let { phone ->
                                if (phone.isNotBlank()) {
                                    val cleanPhone = phone.filter { it.isDigit() }
                                    uriHandler.openUri("https://wa.me/$cleanPhone")
                                }
                            }
                        }
                    },
                    onEditClick = {
                        navigator?.push(
                            AddMemberScreen(
                                screenModel = screenModel,
                                memberId = memberId,
                                onMemberAdded = {
                                    screenModel.loadMemberDetail(memberId)
                                }
                            )
                        )
                    }
                )
            }

            // Tabs
            StaggeredEntranceItem(index = 1) {
                val tabScrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(tabScrollState)
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Spacer(modifier = Modifier.width(8.dp))
                    DetailTabChip(
                        label = "Profile",
                        icon = Res.drawable.ic_profile,
                        isSelected = selectedTab == "Profile",
                        onClick = { selectedTab = "Profile" }
                    )
                    DetailTabChip(
                        label = "Subscriptions",
                        icon = Res.drawable.ic_subscription,
                        isSelected = selectedTab == "Subscriptions",
                        onClick = { selectedTab = "Subscriptions" }
                    )
                    DetailTabChip(
                        label = "Attendance",
                        icon = Res.drawable.ic_cale,
                        isSelected = selectedTab == "Attendance",
                        onClick = { selectedTab = "Attendance" }
                    )
                    DetailTabChip(
                        label = "Payment",
                        icon = Res.drawable.ic_atm,
                        isSelected = selectedTab == "Payment",
                        onClick = { selectedTab = "Payment" }
                    )
                    DetailTabChip(
                        label = "Workout",
                        icon = Res.drawable.ic_workout,
                        isSelected = selectedTab == "Workout",
                        onClick = { selectedTab = "Workout" }
                    )
                    DetailTabChip(
                        label = "Diet",
                        icon = Res.drawable.ic_diet,
                        isSelected = selectedTab == "Diet",
                        onClick = { selectedTab = "Diet" }
                    )
                }
//                Card(
//                    shape = RoundedCornerShape(
//                        topStart = 50.dp,
//                        bottomStart = 50.dp,
//                        topEnd = 50.dp,
//                        bottomEnd = 50.dp
//                    ),
//                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
//                    colors = CardDefaults.cardColors(containerColor = White),
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .shadow(
//                            elevation = 4.dp,
//                            shape = RoundedCornerShape(
//                                topStart = 50.dp,
//                                bottomStart = 50.dp,
//                                topEnd = 50.dp,
//                                bottomEnd = 50.dp
//                            ),
//                            ambientColor = PrimaryColor,
//                            spotColor = PrimaryColor
//                        )
//                ) {
//                    Row(
//                        modifier = Modifier
//                            .fillMaxWidth()
//                            .horizontalScroll(tabScrollState)
//                            .padding(vertical = 8.dp),
//                        horizontalArrangement = Arrangement.spacedBy(12.dp)
//                    ) {
//                        Spacer(modifier = Modifier.width(8.dp))
//                        DetailTabChip(
//                            label = "Profile",
//                            icon = Res.drawable.ic_profile,
//                            isSelected = selectedTab == "Profile",
//                            onClick = { selectedTab = "Profile" }
//                        )
//                        DetailTabChip(
//                            label = "Subscriptions",
//                            icon = Res.drawable.ic_subscription,
//                            isSelected = selectedTab == "Subscriptions",
//                            onClick = { selectedTab = "Subscriptions" }
//                        )
//                        DetailTabChip(
//                            label = "Attendance",
//                            icon = Res.drawable.ic_cale,
//                            isSelected = selectedTab == "Attendance",
//                            onClick = { selectedTab = "Attendance" }
//                        )
//                        DetailTabChip(
//                            label = "Payment",
//                            icon = Res.drawable.ic_atm,
//                            isSelected = selectedTab == "Payment",
//                            onClick = { selectedTab = "Payment" }
//                        )
//                        DetailTabChip(
//                            label = "Workout",
//                            icon = Res.drawable.ic_workout,
//                            isSelected = selectedTab == "Workout",
//                            onClick = { selectedTab = "Workout" }
//                        )
//                        DetailTabChip(
//                            label = "Diet",
//                            icon = Res.drawable.ic_diet,
//                            isSelected = selectedTab == "Diet",
//                            onClick = { selectedTab = "Diet" }
//                        )
//                    }
//                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            StaggeredEntranceItem(index = 2) {
                when (selectedTab) {
                    "Profile" -> MemberDetailProfileContent(memberDetail)
                    "Subscriptions" -> MemberDetailSubscriptionsContent(
                        memberDetail = memberDetail,
                        plans = plans,
                        showReceivePayment = hasPendingPayment,
                        isUnfreezeSubmitting = unfreezeSubmitting,
                        onReceivePayment = { showReceivePayment = true },
                        onExtend = { targetSub ->
                            val sub = targetSub ?: memberDetail?.subscription?.current_subscription
                            val planId = sub?.let { resolveGymPlanForSubscription(it, plans) }
                            if (sub == null || planId.isNullOrBlank()) {
                                screenModel.userInfoToast(
                                    "Could not resolve gym plan. Wait for plans to load or check plan name."
                                )
                            } else {
                                selectedExtendSubscription = sub
                                showExtend = true
                            }
                        },
                        onFreeze = { showFreeze = true },
                        onUnfreeze = { targetSubscriptionId ->
                            val sub = memberDetail?.subscription?.current_subscription
                            val gymId = SessionManager.gymId
                            val memberSubscriptionId =
                                targetSubscriptionId?.takeIf { it.isNotBlank() }
                                    ?: sub?.member_subscription_id.orEmpty()
                            if (gymId.isBlank() || memberSubscriptionId.isBlank()) {
                                screenModel.userInfoToast("Could not resolve subscription for unfreeze.")
                                return@MemberDetailSubscriptionsContent
                            }
                            if (unfreezeSubmitting) return@MemberDetailSubscriptionsContent

                            unfreezeSubmitting = true
                            screenModel.unfreezeSubscription(
                                request = UnfreezeSubscriptionRequest(
                                    gymId = gymId,
                                    member_subscription_id = memberSubscriptionId,
                                ),
                                onSuccess = { screenModel.loadMemberDetail(memberId) },
                                showGlobalLoader = false,
                                onFinished = { unfreezeSubmitting = false },
                            )
                        },
                    )

                    "Attendance" -> MemberDetailAttendanceContent(
                        memberDetail = memberDetail,
                        attendanceSummary = attendanceSummary,
                        onDeleteBiometric = { showDeleteBiometricDialog = true },
                        selectedFilterDate = selectedAttendanceDate,
                        onFilterDateChange = { selectedAttendanceDate = it },
                    )

                    "Payment" -> MemberDetailPaymentsContent(memberDetail)
                    "Workout" -> MemberDetailWorkoutContent(
                        workouts = workouts,
                        memberId = memberDetail?.user?.id.toString(),
                        onDeleteWorkout = { workoutId ->
                            workoutToDelete = workoutId
                        }
                    )

                    "Diet" -> DietContent(
                        meals = memberDietMeals,
                        isLoading = dietMealsLoading,
                        mealAuthorName = null,
                        onMealClick = { meal ->
                            navigator?.push(
                                CreateMealScreen(
                                    memberGymUserId = memberDetail?.gymUserId ?: memberId,
                                    dietMeal = meal,
                                    readOnly = true
                                )
                            )
                        },
                        onMealEdit = { meal ->
                            navigator?.push(
                                CreateMealScreen(
                                    memberGymUserId = memberDetail?.gymUserId ?: memberId,
                                    dietMeal = meal,
                                    readOnly = false,
                                    onMealCreated = {
                                        screenModel.loadMemberDiet(
                                            memberDetail?.gymUserId ?: memberId
                                        )
                                    }
                                )
                            )
                        },
                        onMealDelete = { meal ->
                            mealToDelete = meal
                        }
                    )
                }
            }
        }
    }

        if (showReceivePayment) {
            ModalBottomSheet(
                onDismissRequest = {
                    collectPaymentSubmitting = false
                    showReceivePayment = false
                },
                sheetState = receivePaymentSheetState,
                containerColor = Color.White,
                dragHandle = null
            ) {
                ReceivePaymentSheet(
                    plans = plans,
                    plansLoading = false,
                    lockedContext = receivePaymentLockedContext,
                    isSubmittingPayment = collectPaymentSubmitting,
                    onDismiss = {
                        collectPaymentSubmitting = false
                        showReceivePayment = false
                    },
                    onCreatePlan = {
                        showReceivePayment = false
                        navigator?.push(AddPlanScreen(limitPlanTypes = true))
                    },
                    onReceivePayment = { memberSubscriptionId, gymPlanId, amountStr, mode, dateStored ->
                        val gymUserId = memberDetail?.gymUserId
                        if (gymUserId.isNullOrBlank()) return@ReceivePaymentSheet
                        val amountInt = amountStr.toIntOrNull()
                        if (amountInt == null || amountInt <= 0) {
                            screenModel.userInfoToast("Enter a valid amount.")
                            return@ReceivePaymentSheet
                        }
                        if (memberSubscriptionId.isBlank()) {
                            screenModel.userInfoToast("Missing member subscription. Cannot record payment.")
                            return@ReceivePaymentSheet
                        }
                        collectPaymentSubmitting = true
                        screenModel.receivePayment(
                            ReceivePaymentRequest(
                                type = "receive_payment",
                                member_id = gymUserId,
                                member_subscription_id = memberSubscriptionId,
                                gym_plan_id = gymPlanId,
                                amount = amountInt,
                                payment_mode = mode.lowercase(),
                                date = DateUtils.paymentStoredInstantToApiDate(dateStored),
                            ),
                            onSuccess = {
                                showReceivePayment = false
                                screenModel.loadMemberDetail(memberId)
                            },
                            showGlobalLoader = false,
                            onFinished = { collectPaymentSubmitting = false }
                        )
                    }
                )
            }
        }

        if (showExtend) {
            val currentSub = selectedExtendSubscription
                ?: memberDetail?.subscription?.current_subscription
                ?: memberDetail?.subscription?.activeSubscriptions()?.firstOrNull()
            val isRenewPlan = currentSub != null &&
                memberDetail?.subscription?.isCurrentSubscription(currentSub) != true
            // Resolve the matched PlanDTO so we can use its exact durationDays
            val resolvedPlan = currentSub?.let { sub ->
                val planId = resolveGymPlanForSubscription(sub, plans)
                plans.firstOrNull { it.id == planId }
            }
            ModalBottomSheet(
                onDismissRequest = {
                    if (!extendSubmitting) showExtend = false
                },
                sheetState = extendSheetState,
                containerColor = Color.White,
                dragHandle = null
            ) {
                ExtendSubscriptionSheet(
                    planTitle = currentSub?.plan_name ?: "",
                    dateRangeLabel = if (currentSub != null) {
                        subscriptionDateRangeLabel(currentSub.start_date, currentSub.expiry_date)
                    } else {
                        ""
                    },
                    displaySellingAmount = currentSub?.let { extendSheetDisplaySellingPrice(it, plans) } ?: 0,
                    isRenewPlan = isRenewPlan,
                    initialPaymentDateStored = if (isRenewPlan) DateUtils.getCurrentDateIso() else null,
                    planDurationDays = resolvedPlan?.durationDays,
                    isSubmitting = extendSubmitting,
                    onDismiss = {
                        if (!extendSubmitting) {
                            showExtend = false
                            selectedExtendSubscription = null
                        }
                    },
                    onConfirm = { mode, dateStored, sellingPrice, additionFee, startDateStored ->
                        val gymUser = memberDetail?.gymUserId
                        val gid = currentSub?.let { resolveGymPlanForSubscription(it, plans) }
                        if (gymUser.isNullOrBlank() || gid.isNullOrBlank()) {
                            screenModel.userInfoToast("Could not resolve member or plan for extension.")
                            return@ExtendSubscriptionSheet
                        }
                        if (isRenewPlan && sellingPrice <= 0) {
                            screenModel.userInfoToast("Selling price is not available for this plan.")
                            return@ExtendSubscriptionSheet
                        }
                        if (!isRenewPlan && additionFee <= 0) {
                            screenModel.userInfoToast("Enter a valid fee amount.")
                            return@ExtendSubscriptionSheet
                        }
                        extendSubmitting = true
                        screenModel.extendPlanPayment(
                            ExtendPlanPaymentRequest(
                                type = "extend_plan",
                                member_id = gymUser,
                                member_subscription_id = currentSub?.member_subscription_id,
                                gym_plan_id = gid,
                                addition_fee = additionFee.takeIf { it > 0 },
                                selling_price = sellingPrice.takeIf { it > 0 },
                                payment_mode = mode.lowercase(),
                                date = DateUtils.paymentStoredInstantToApiDate(dateStored),
                                start_date = startDateStored?.let { DateUtils.paymentStoredInstantToApiDate(it) },
                            ),
                            onSuccess = {
                                showExtend = false
                                selectedExtendSubscription = null
                                screenModel.loadMemberDetail(memberId)
                            },
                            showGlobalLoader = false,
                            onFinished = { extendSubmitting = false },
                        )
                    }
                )
            }
        }

        if (showFreeze) {
            val freezeSub = memberDetail?.subscription?.current_subscription
            ModalBottomSheet(
                onDismissRequest = {
                    if (!freezeSubmitting) showFreeze = false
                },
                sheetState = freezeSheetState,
                containerColor = Color.White,
                dragHandle = null
            ) {
                FreezeSubscriptionSheet(
                    planTitle = freezeSub?.plan_name ?: "",
                    dateRangeLabel = if (freezeSub != null) {
                        subscriptionDateRangeLabel(freezeSub.start_date, freezeSub.expiry_date)
                    } else {
                        ""
                    },
                    basePriceAmount = freezeSub?.let { defaultExtendPaymentAmount(it, plans) } ?: 0,
                    isSubmitting = freezeSubmitting,
                    onDismiss = {
                        if (!freezeSubmitting) showFreeze = false
                    },
                    onKeepCurrentPlan = {
                        if (!freezeSubmitting) showFreeze = false
                    },
                    onFreezeSubscription = { startDate, duration, freezeFee, reason ->
                        val gymId = SessionManager.gymId
                        val memberSubscriptionId = freezeSub?.member_subscription_id.orEmpty()
                        if (gymId.isBlank() || memberSubscriptionId.isBlank()) {
                            screenModel.userInfoToast("Could not resolve subscription for freeze.")
                            return@FreezeSubscriptionSheet
                        }

                        val startDateIso = freezeSheetDateToApiDate(startDate)
                        if (startDateIso == null) {
                            screenModel.userInfoToast("Please select a valid freeze start date.")
                            return@FreezeSubscriptionSheet
                        }

                        val durationDays = freezeDurationDaysFromInput(duration) ?: 0

                        val freezeFeeInt = freezeFee
                            .filter { it.isDigit() }
                            .toIntOrNull()
                            ?.coerceAtLeast(0)
                            ?: 0

                        freezeSubmitting = true
                        screenModel.freezeSubscription(
                            request = FreezeSubscriptionRequest(
                                gymId = gymId,
                                member_subscription_id = memberSubscriptionId,
                                freeze_start_date = startDateIso,
                                duration_days = durationDays,
                                freeze_fee = freezeFeeInt,
                                reason = reason.trim(),
                            ),
                            onSuccess = {
                                showFreeze = false
                                screenModel.loadMemberDetail(memberId)
                            },
                            showGlobalLoader = false,
                            onFinished = { freezeSubmitting = false },
                        )
                    },
                )
            }
        }
    }
}

private fun memberBirthDateLabel(detail: MemberDetailResponse?): String {
    val raw = detail?.dateOfBirth?.takeIf { it.isNotBlank() }
        ?: detail?.summary?.dob?.takeIf { it.isNotBlank() }
    return memberDetailDateLabel(raw).ifBlank { "Not available" }
}

private fun MemberDetailResponse?.memberAvatarUrl(): String {
    if (this == null) return ""
    return user.avatarUrl?.takeIf { it.isNotBlank() }
        ?: user.profile_image?.takeIf { it.isNotBlank() }
        ?: summary.profile_image?.takeIf { it.isNotBlank() }
        ?: ""
}

private fun resolveGymPlanForSubscription(sub: CurrentSubscriptionDTO, plans: List<PlanDTO>): String? {
    sub.gym_plan_id?.takeIf { it.isNotBlank() }?.let { return it }
    val name = sub.plan_name.takeIf { it.isNotBlank() } ?: return null
    return plans.firstOrNull { it.name == name }?.id?.takeIf { it.isNotBlank() }
}

/**
 * Selling price shown on extend/renew sheet (matches subscription card when set).
 */
private fun extendSheetDisplaySellingPrice(sub: CurrentSubscriptionDTO, plans: List<PlanDTO>): Int {
    sub.resolvedSellingPrice()?.let { return it }
    return defaultExtendPaymentAmount(sub, plans)
}

/**
 * Default contract total for extend/renew sheets (plan_price + selling_price when set).
 */
private fun defaultExtendPaymentAmount(sub: CurrentSubscriptionDTO, plans: List<PlanDTO>): Int {
    sub.resolvedSellingPrice()?.let { return it }
    val pending = sub.resolvedAmountPending()
    if (pending > 0) return pending
    val planId = resolveGymPlanForSubscription(sub, plans)?.takeIf { it.isNotBlank() }
        ?: return 0
    val plan = plans.firstOrNull { it.id == planId } ?: return 0
    return plan.priceCents.coerceAtLeast(0)
}

/** ISO8601 / `yyyy-MM-dd` / `dd/MM/yyyy` -> `MMM d, yyyy` (e.g. `Jun 5, 2026`). */
private fun memberDetailDateLabel(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val normalized = raw.trim()
    val localDate = run {
        val dateOnly = normalized.substringBefore("T").take(10)
        if (dateOnly.length == 10 && dateOnly[4] == '-' && dateOnly[7] == '-') {
            runCatching { LocalDate.parse(dateOnly) }.getOrNull()
        } else {
            val slash = normalized.replace(" ", "").split("/")
            if (slash.size == 3) {
                val day = slash[0].toIntOrNull()
                val month = slash[1].toIntOrNull()
                val year = slash[2].toIntOrNull()
                if (day != null && month != null && year != null) {
                    runCatching { LocalDate(year, month, day) }.getOrNull()
                } else null
            } else null
        }
    }
    return if (localDate != null) {
        "${memberDetailShortMonthName(localDate.monthNumber)} ${localDate.dayOfMonth}, ${localDate.year}"
    } else {
        normalized.substringBefore("T").ifBlank { normalized }
    }
}

private fun subscriptionDateRangeLabel(startIso: String, endIso: String) =
    "${memberDetailDateLabel(startIso)} to ${memberDetailDateLabel(endIso)}"

private fun freezeSheetDateToApiDate(value: String): String? {
    val normalized = value.replace(" ", "")
    val seg = normalized.split("/")
    if (seg.size == 3) {
        val day = seg[0].toIntOrNull() ?: return null
        val month = seg[1].toIntOrNull() ?: return null
        val year = seg[2].toIntOrNull() ?: return null
        return runCatching { LocalDate(year, month, day).toString() }.getOrNull()
    }
    return DateUtils.birthDateToIsoDateOnly(normalized)
}

private fun freezeDurationDaysFromInput(value: String): Int? {
    val digits = value.filter { it.isDigit() }
    return digits.toIntOrNull()
}

private fun memberDetailShortMonthName(month: Int): String = when (month) {
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

private fun dummyPastSubscriptions(): List<CurrentSubscriptionDTO> = listOf(
    CurrentSubscriptionDTO(
        member_subscription_id = "demo_past_1",
        plan_name = "Strength Builder",
        start_date = "2026-03-01T00:00:00.000Z",
        expiry_date = "2026-03-31T00:00:00.000Z",
        amount_paid = 3000,
        amount_pending = 0,
    ),
    CurrentSubscriptionDTO(
        member_subscription_id = "demo_past_2",
        plan_name = "Fat Loss Program",
        start_date = "2026-02-01T00:00:00.000Z",
        expiry_date = "2026-02-28T00:00:00.000Z",
        amount_paid = 1000,
        amount_pending = 2000,
    ),
)

private fun attendanceLogDateTimeLabel(displayRelative: String?, checkedInAt: String?): String {
    if (!displayRelative.isNullOrBlank()) return displayRelative
    val at = checkedInAt ?: return ""
    val datePart = memberDetailDateLabel(at)
    val timePart = DateUtils.formatChatTime(at)
    return when {
        datePart.isNotBlank() && timePart.isNotBlank() -> "$datePart $timePart"
        datePart.isNotBlank() -> datePart
        else -> DateUtils.formatShortDateTime(at)
    }
}

private fun attendanceFilterDateLabel(dateIso: String?): String {
    if (dateIso.isNullOrBlank()) return ""
    return DateUtils.formatBirthDateForDisplay(dateIso).ifBlank {
        memberDetailDateLabel(dateIso)
    }
}

private fun localDateToMillis(date: LocalDate): Long =
    date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()

private fun millisToLocalDate(millis: Long): LocalDate =
    Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.currentSystemDefault()).date

internal fun loadMemberAttendanceSummaryForDate(
    screenModel: OwnerDashboardScreenModel,
    memberId: String,
    dateIso: String?,
) {
    val query = resolveAttendanceSummaryQuery(dateIso)
    screenModel.loadMemberAttendanceSummary(
        memberId = memberId,
        month = query.month,
        year = query.year,
        date = query.date,
    )
}

private data class AttendanceSummaryQuery(
    val month: String? = null,
    val year: String? = null,
    val date: String? = null,
)

private fun resolveAttendanceSummaryQuery(dateIso: String?): AttendanceSummaryQuery {
    if (dateIso.isNullOrBlank()) return AttendanceSummaryQuery()
    val normalized = dateIso.trim().substringBefore("T").take(10)
    val parts = normalized.split("-")
    if (parts.size != 3) return AttendanceSummaryQuery(date = normalized)
    val month = parts[1].toIntOrNull()?.toString() ?: parts[1]
    return AttendanceSummaryQuery(
        month = month,
        year = parts[0],
        date = normalized,
    )
}

@Composable
fun MemberDetailProfileContent(memberDetail: MemberDetailResponse?) {
    val navigator = LocalNavigator.current
    val stats = memberDetail?.subscription?.stats

    Column {
        // Status Card
        CommonCard(
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Gray.copy(alpha = 0.02f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        MemberDetailStatusRow(
                            label = "Active Subscription",
                            value = "${stats?.active_subscription ?: 0}",
                            icon = Res.drawable.ic_active,
                            color = PrimaryDarkColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        MemberDetailStatusRow(
                            label = "Pending payment",
                            value = "${stats?.pending_payment ?: 0}",
                            icon = Res.drawable.ic_optional,
                            color = YellowColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        MemberDetailStatusRow(
                            label = "overdue",
                            value = "${stats?.overdue ?: 0}",
                            icon = Res.drawable.ic_overdue,
                            color = Color.Gray
                        )
                    }

                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        CommonCard(
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Gray.copy(alpha = 0.02f))
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GreenCardColor)
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "General Information",
                                    style = AppTextTheme.semiBold.copy(fontSize = 16.sp)
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            MemberDetailInfoItem(
                                label = "Phone Number",
                                value = memberDetail?.contact?.phone ?: "Not available",
                                icon = Res.drawable.ic_phone
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            MemberDetailInfoItem(
                                label = "Email",
                                value = memberDetail?.user?.email ?: "Not available",
                                icon = Res.drawable.ic_mail
                            )
//                            Spacer(modifier = Modifier.height(16.dp))
//                            MemberDetailInfoItem(
//                                label = "Address",
//                                value = memberDetail?.user?.address?.takeIf { it.isNotBlank() }
//                                    ?: memberDetail?.summary?.address?.takeIf { it.isNotBlank() }
//                                    ?: "Not available",
//                                icon = Res.drawable.ic_optional
//                            )
//                            Spacer(modifier = Modifier.height(16.dp))
//                            MemberDetailInfoItem(
//                                label = "Aadhaar",
//                                value = memberDetail?.user?.aadhaar_number?.takeIf { it.isNotBlank() }
//                                    ?: memberDetail?.summary?.aadhaar_number?.takeIf { it.isNotBlank() }
//                                    ?: "Not available",
//                                icon = Res.drawable.ic_optional
//                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            MemberDetailInfoItem(
                                label = "Birth Date",
                                value = memberBirthDateLabel(memberDetail),
                                icon = Res.drawable.ic_dob
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            MemberDetailInfoItem(
                                label = "Gender",
                                value = memberDetail?.gender ?: "Not available",
                                icon = Res.drawable.ic_gender
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            MemberDetailInfoItem(
                                label = "Joining Date",
                                value = memberDetailDateLabel(memberDetail?.joinedAt)
                                    .ifBlank { "Not available" },
                                icon = Res.drawable.ic_cale
                            )
                        }
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        CommonCard(
            content = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Gray.copy(alpha = 0.02f))
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GreenCardColor)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Physical & Emergency Details",
                                style = AppTextTheme.semiBold.copy(fontSize = 16.sp)
                            )
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    MemberDetailInfoItem(
                                        label = "Height",
                                        value = memberDetail?.user?.heightCm?.let { "$it cm" }
                                            ?: "N/A",
                                        icon = Res.drawable.ic_workout
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    MemberDetailInfoItem(
                                        label = "Weight",
                                        value = memberDetail?.user?.weightKg?.let { "$it kg" }
                                            ?: "N/A",
                                        icon = Res.drawable.ic_workout
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            MemberDetailInfoItem(
                                label = "Emergency Contact",
                                value = memberDetail?.emergencyContactName?.let { "$it (${memberDetail.emergencyContactPhone ?: ""})" }
                                    ?: "Not available",
                                icon = Res.drawable.ic_phone
                            )
                            val notesText = memberDetail?.notes?.takeIf { it.isNotBlank() }
                                ?: memberDetail?.summary?.notes?.takeIf { it.isNotBlank() }
                            if (!notesText.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                MemberDetailInfoItem(
                                    label = "Notes",
                                    value = notesText,
                                    icon = Res.drawable.ic_optional
                                )
                            }
                        }
                    }
                }
            }
        )


        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun MemberDetailSubscriptionsContent(
    memberDetail: MemberDetailResponse?,
    plans: List<PlanDTO> = emptyList(),
    showReceivePayment: Boolean = true,
    isUnfreezeSubmitting: Boolean = false,
    onReceivePayment: () -> Unit = {},
    onExtend: (CurrentSubscriptionDTO?) -> Unit = {},
    onFreeze: () -> Unit = {},
    onUnfreeze: (memberSubscriptionId: String?) -> Unit = {},
) {
    val subscription = memberDetail?.subscription
    val stats = subscription?.stats
    val activeSubscriptions = subscription?.activeSubscriptions().orEmpty()
    val primarySub = subscription?.current_subscription ?: activeSubscriptions.firstOrNull()
    val upcoming = subscription?.upcoming_subscriptions.orEmpty()
    val expired = subscription?.expired_subscriptions.orEmpty()
    val freeze = subscription?.freeze_subscriptions.orEmpty()

    fun isSubscriptionFrozen(sub: CurrentSubscriptionDTO): Boolean =
        sub.freeze_start_date != null ||
            freeze.any { it.member_subscription_id == sub.member_subscription_id }

    if (activeSubscriptions.isEmpty() && upcoming.isEmpty() && freeze.isEmpty() && expired.isEmpty()) {
        AppEmptyStateView(
            image = Res.drawable.img_no_member_enrolled,
            title = "No Subscription Records",
            subtitle = "This member has not selected any subscription yet."
        )
        Spacer(modifier = Modifier.height(80.dp))
        return
    }

    val activeCount = stats?.active_subscription?.takeIf { it > 0 }
        ?: activeSubscriptions.size
    val activePlanChip = when (activeCount) {
        0 -> "0 Plans"
        1 -> "1 Plan"
        else -> "$activeCount Plans"
    }

    Column {
        if (activeSubscriptions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Subscriptions",
                    style = AppTextTheme.medium.copy(fontSize = 14.sp)
                )
                Surface(
                    color = PrimaryColor,
                    shape = RoundedCornerShape(100.dp)
                ) {
                    Text(
                        text = activePlanChip,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = AppTextTheme.regular.copy(fontSize = 12.sp, color = White)
                    )
                }
            }

            activeSubscriptions.forEachIndexed { index, sub ->
                val isSubFrozen = isSubscriptionFrozen(sub)
                val isPrimary = sub.member_subscription_id != null &&
                    sub.member_subscription_id == primarySub?.member_subscription_id
                val showSubReceivePayment = showReceivePayment && sub.resolvedAmountPending() > 0

                MemberActiveSubscriptionCard(
                    sub = sub,
                    isFrozen = isSubFrozen,
                    isUnfreezeSubmitting = isUnfreezeSubmitting,
                    showReceivePayment = showSubReceivePayment,
                    showFreezeActions = isPrimary,
                    subscriptionActionLabel = subscription?.subscriptionActionLabel(sub) ?: "Extend",
                    onReceivePayment = onReceivePayment,
                    onSubscriptionAction = { onExtend(sub) },
                    onFreeze = onFreeze,
                    onUnfreeze = { onUnfreeze(sub.member_subscription_id) },
                )

                if (index < activeSubscriptions.lastIndex) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (upcoming.isNotEmpty()) {
            Text(
                text = "Upcoming Subscriptions",
                style = AppTextTheme.bold.copy(fontSize = 18.sp),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            upcoming.forEach { sub ->
                MemberSubscriptionSecondaryCard(
                    sub = sub,
                    statusLabel = "Upcoming",
                    statusAccent = PrimaryColor,
                    actionLabel = subscription?.subscriptionActionLabel(sub) ?: "Renew",
                    onActionClick = { onExtend(sub) },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (freeze.isNotEmpty()) {
            Text(
                text = "Freeze Subscriptions",
                style = AppTextTheme.bold.copy(fontSize = 18.sp),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            freeze.forEach { sub ->
                val freezeStart = memberDetailDateLabel(sub.freeze_start_date)
                val freezeEnd = memberDetailDateLabel(sub.freeze_end_date)
                val freezePeriod = when {
                    freezeStart.isNotBlank() && freezeEnd.isNotBlank() -> "Freeze: $freezeStart to $freezeEnd"
                    freezeStart.isNotBlank() -> "Freeze start: $freezeStart"
                    freezeEnd.isNotBlank() -> "Freeze end: $freezeEnd"
                    else -> null
                }
                val freezeDuration = sub.duration_days?.takeIf { it > 0 }?.let { "$it day freeze" }
                val freezeInfo = listOfNotNull(freezePeriod, freezeDuration).joinToString(" • ").ifBlank { null }

                MemberSubscriptionSecondaryCard(
                    sub = sub,
                    statusLabel = "Freeze",
                    statusAccent = PrimaryColor,
                    extraInfo = freezeInfo,
                    actionLabel = if (isUnfreezeSubmitting) "Unfreezing…" else "Unfreeze",
                    actionEnabled = !isUnfreezeSubmitting,
                    onActionClick = { onUnfreeze(sub.member_subscription_id) },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = "Past subscriptions",
            style = AppTextTheme.bold.copy(fontSize = 18.sp),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (expired.isEmpty()) {
            Text(
                text = "No past subscription records.",
                style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        } else {
            expired.forEach { sub ->
                val hasPendingAmount = sub.resolvedAmountPending() > 0
                val renewLabel = subscription?.subscriptionActionLabel(sub) ?: "Renew"
                MemberSubscriptionSecondaryCard(
                    sub = sub,
                    statusLabel = if (hasPendingAmount) "Expired" else "Completed",
                    statusAccent = if (hasPendingAmount) Gray else PrimaryDarkColor,
                    extraInfo = if (hasPendingAmount) {
                        "Pending ${Constants.RUPEE} ${sub.resolvedAmountPending()}"
                    } else {
                        "Fully paid"
                    },
                    actionLabel = if (hasPendingAmount) "Receive Payment" else renewLabel,
                    onActionClick = if (hasPendingAmount) onReceivePayment else { { onExtend(sub) } },
                    secondaryActionLabel = if (hasPendingAmount) renewLabel else null,
                    onSecondaryActionClick = if (hasPendingAmount) { { onExtend(sub) } } else null,
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(100.dp))
    }
}

@Composable
private fun MemberActiveSubscriptionCard(
    sub: CurrentSubscriptionDTO,
    catalogFallback: Int = 0,
    isFrozen: Boolean,
    isUnfreezeSubmitting: Boolean,
    showReceivePayment: Boolean,
    showFreezeActions: Boolean,
    subscriptionActionLabel: String,
    onReceivePayment: () -> Unit,
    onSubscriptionAction: () -> Unit,
    onFreeze: () -> Unit,
    onUnfreeze: () -> Unit,
) {
    CommonCard(
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Gray.copy(alpha = 0.02f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_king),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sub.plan_name,
                                style = AppTextTheme.medium.copy(fontSize = 14.sp)
                            )
                            Text(
                                text = subscriptionDateRangeLabel(sub.start_date, sub.expiry_date),
                                style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
                            )
                            val remainingDays = sub.remaining_days ?: 0
                            if (remainingDays > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$remainingDays days remaining",
                                    style = AppTextTheme.regular.copy(fontSize = 11.sp, color = Gray)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_active_status),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isFrozen) "Frozen" else "Active",
                                style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Black)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = GrayBorderColor)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SubscriptionStatColumn(
                            if (sub.hasSellingPrice()) "Selling Price" else "Total Contract",
                            "${Constants.RUPEE} ${sub.contractTotalAmount()}",
                            Black
                        )
                        SubscriptionStatColumn(
                            "Paid",
                            "${Constants.RUPEE} ${sub.resolvedAmountPaid()}",
                            PrimaryDarkColor
                        )
                        SubscriptionStatColumn(
                            "Pending",
                            "${Constants.RUPEE} ${sub.resolvedAmountPending()}",
                            YellowColor
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    if (showReceivePayment) {
                        CommonButton(
                            onClick = onReceivePayment,
                            text = "Receive Payment",
                            modifier = Modifier.fillMaxWidth(),
                            leftIcon = painterResource(Res.drawable.ic_wallet_filled)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
//                        if (showFreezeActions) {
                            CommonOutlineButton(
                                textColor = Black,
                                borderColor = Black.copy(alpha = 0.25f),
                                onClick = { if (isFrozen) onUnfreeze() else onFreeze() },
                                text = when {
                                    isFrozen && isUnfreezeSubmitting -> "Unfreezing…"
                                    isFrozen -> "Unfreeze"
                                    else -> "Freeze"
                                },
                                enabled = !isUnfreezeSubmitting,
                                modifier = Modifier.weight(1f)
                            )
//                        }
                        CommonOutlineButton(
                            textColor = Black,
                            borderColor = Black.copy(alpha = 0.25f),
                            onClick = onSubscriptionAction,
                            text = subscriptionActionLabel,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun MemberSubscriptionSecondaryCard(
    sub: CurrentSubscriptionDTO,
    catalogFallback: Int = 0,
    statusLabel: String,
    statusAccent: Color,
    extraInfo: String? = null,
    actionLabel: String? = null,
    actionEnabled: Boolean = true,
    onActionClick: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryActionClick: (() -> Unit)? = null,
) {
    CommonCard(
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Gray.copy(alpha = 0.02f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(36.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_king),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = sub.plan_name,
                                style = AppTextTheme.medium.copy(fontSize = 14.sp)
                            )
                            Text(
                                text = subscriptionDateRangeLabel(sub.start_date, sub.expiry_date),
                                style = AppTextTheme.regular.copy(
                                    fontSize = 12.sp,
                                    color = Gray
                                )
                            )
                            if (!extraInfo.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = extraInfo,
                                    style = AppTextTheme.regular.copy(
                                        fontSize = 11.sp,
                                        color = Gray
                                    )
                                )
                            }
                        }
                        Text(
                            text = statusLabel,
                            style = AppTextTheme.medium.copy(fontSize = 12.sp, color = statusAccent)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = GrayBorderColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SubscriptionStatColumn(
                            if (sub.hasSellingPrice()) "Selling Price" else "Total Contract",
                            "${Constants.RUPEE} ${sub.contractTotalAmount()}",
                            Black
                        )
                        SubscriptionStatColumn(
                            "Paid",
                            "${Constants.RUPEE} ${sub.resolvedAmountPaid()}",
                            PrimaryDarkColor
                        )
                        SubscriptionStatColumn(
                            "Pending",
                            "${Constants.RUPEE} ${sub.resolvedAmountPending()}",
                            YellowColor
                        )
                    }
                    if (!actionLabel.isNullOrBlank() && onActionClick != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        if (!secondaryActionLabel.isNullOrBlank() && onSecondaryActionClick != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CommonButton(
                                    onClick = onActionClick,
                                    text = actionLabel,
                                    enabled = actionEnabled,
                                    modifier = Modifier.weight(1f),
                                    leftIcon = painterResource(Res.drawable.ic_wallet_filled)
                                )
                                CommonOutlineButton(
                                    textColor = PrimaryColor,
                                    borderColor = PrimaryColor.copy(alpha = 0.35f),
                                    onClick = onSecondaryActionClick,
                                    text = secondaryActionLabel,
                                    enabled = actionEnabled,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        } else {
                            CommonOutlineButton(
                                textColor = PrimaryColor,
                                borderColor = PrimaryColor.copy(alpha = 0.35f),
                                onClick = onActionClick,
                                text = actionLabel,
                                enabled = actionEnabled,
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun SubscriptionStatColumn(label: String, value: String, valueColor: Color) {
    Column {
        Text(text = label, style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray))
        Text(text = value, style = AppTextTheme.semiBold.copy(fontSize = 16.sp, color = valueColor))
    }
}

@Composable
private fun AttendanceCalendarGrid(summary: MemberAttendanceSummaryResponse) {
    val filter = summary.filter
    val year = filter.year
    val month = filter.month
    val daysInMonth = summary.stats.days_in_month.takeIf { it > 0 }
        ?: summary.calendar.size.takeIf { it > 0 } ?: 31
    val statusByDate = remember(summary.calendar) {
        summary.calendar.associate { it.date to it.status.lowercase() }
    }
    val today = remember {
        Instant.fromEpochMilliseconds(getCurrentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val firstOfMonth = remember(year, month) { LocalDate(year, month, 1) }
    val leadingBlanks = firstOfMonth.dayOfWeek.ordinal
    val totalCells = leadingBlanks + daysInMonth
    val numRows = (totalCells + 6) / 7

    val weekdays = listOf("M", "T", "W", "T", "F", "S", "S")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        weekdays.forEach { label ->
            Text(
                text = label,
                style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray),
                modifier = Modifier.width(32.dp),
                textAlign = TextAlign.Center
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        var dayNum = 1
        repeat(numRows) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(7) { col ->
                    val idx = row * 7 + col
                    if (idx < leadingBlanks || dayNum > daysInMonth) {
                        Spacer(modifier = Modifier.size(32.dp))
                    } else {
                        val d = dayNum++
                        val dateStr =
                            "$year-${month.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
                        val status = statusByDate[dateStr].orEmpty()
                        val isPresent = status == "present"
                        val isAbsent = status == "absent"
                        val cellDate = LocalDate(year, month, d)
                        val isToday = cellDate == today
                        CalendarDay(d.toString(), isPresent, isToday, isAbsent)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberDetailAttendanceContent(
    memberDetail: MemberDetailResponse?,
    attendanceSummary: MemberAttendanceSummaryResponse?,
    onDeleteBiometric: () -> Unit = {},
    onMonthClick: (String, String) -> Unit = { _, _ -> },
    showViewAllHistoryButton: Boolean = false,
    onViewAllHistoryClick: () -> Unit = {},
    selectedFilterDate: String? = null,
    onFilterDateChange: (String?) -> Unit = {},
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val filteredLogs = remember(attendanceSummary?.recent_logs, selectedFilterDate) {
        attendanceSummary?.recent_logs.orEmpty().filteredByAttendanceDate(selectedFilterDate)
    }

    if (showDatePicker) {
        val initialPickerMillis = selectedFilterDate?.take(10)?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()?.let(::localDateToMillis)
        } ?: getCurrentTimeMillis()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialPickerMillis,
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onFilterDateChange(millisToLocalDate(millis).toString())
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = PrimaryColor)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = Gray)
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (attendanceSummary == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 440.dp)
                .padding(vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            AppEmptyStateView(
                image = Res.drawable.img_no_checkin,
                title = "No Check-ins Yet",
                subtitle = "This member has not marked attendance yet. Ask them to check in.",
                modifier = Modifier.fillMaxWidth(),
                useCardContainer = false
            )
        }
        return
    }

    Column {
        // Biometric Actions
        Text(
            text = "Biometric Actions",
            style = AppTextTheme.semiBold.copy(fontSize = 16.sp),
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BiometricActionItem(
                label = "BLOCK",
                icon = Icons.Default.Block,
                color = GrayBorderColor
            )
            BiometricActionItem(
                label = "ADD",
                icon = Icons.Default.Fingerprint,
                color = Color(0xFFE7F7F2),
                tint = PrimaryColor
            )
            BiometricActionItem(
                label = "RE-ADD",
                icon = Icons.Default.Refresh,
                color = GrayBorderColor
            )
            BiometricActionItem(
                label = "DELETE",
                icon = Icons.Default.Delete,
                color = GrayBorderColor,
                onClick = onDeleteBiometric
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Attendance Statistics
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Attendance Statistics",
                style = AppTextTheme.semiBold.copy(fontSize = 16.sp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!selectedFilterDate.isNullOrBlank()) {
                    Text(
                        text = "Clear",
                        style = AppTextTheme.medium.copy(fontSize = 13.sp, color = PrimaryColor),
                        modifier = Modifier.clickable { onFilterDateChange(null) },
                    )
                }
                Surface(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    color = White,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = attendanceFilterDateLabel(selectedFilterDate).ifBlank {
                                attendanceSummary.filter.month_label.ifBlank { "Select date" }
                            },
                            style = AppTextTheme.medium.copy(fontSize = 14.sp, color = PrimaryColor),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(Res.drawable.ic_cale),
                            contentDescription = "Select date",
                            modifier = Modifier.size(16.dp),
                            tint = PrimaryColor,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AttendanceStatCard(
                modifier = Modifier.weight(1f),
                value = (attendanceSummary?.stats?.days_present_month ?: 0).toString(),
                unit = "Days",
                label = "Days Present\n(Month)",
                progress = if ((attendanceSummary?.stats?.days_in_month ?: 1) > 0)
                    (attendanceSummary?.stats?.days_present_month
                        ?: 0).toFloat() / (attendanceSummary?.stats?.days_in_month ?: 30).toFloat()
                else 0f
            )
            AttendanceStatCard(
                modifier = Modifier.weight(1f),
                value = (attendanceSummary?.stats?.lifetime_check_ins ?: 0).toString(),
                unit = "Total",
                label = "Lifetime Attendance",
                progress = 1.0f // Or some other logic
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

//        CommonCard(
//            shape = RoundedCornerShape(24.dp),
//            elevation = 2.dp,
//            borderColor = Color(0xFFF3F4F6),
//            content = {
//                Column(modifier = Modifier.padding(16.dp)) {
//                    Row(
//                        modifier = Modifier.fillMaxWidth(),
//                        horizontalArrangement = Arrangement.SpaceBetween,
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(
//                            text = "Attendance",
//                            style = AppTextTheme.semiBold.copy(fontSize = 16.sp)
//                        )
//                        Surface(
//                            onClick = {
//                                onMonthClick(
//                                    attendanceSummary.filter.month.toString(),
//                                    attendanceSummary.filter.year.toString()
//                                )
//                            },
//                            shape = RoundedCornerShape(8.dp),
//                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
//                            color = White
//                        ) {
//                            Row(
//                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
//                                verticalAlignment = Alignment.CenterVertically
//                            ) {
//                                Text(
//                                    text = attendanceSummary.filter.month_label,
//                                    style = AppTextTheme.medium.copy(fontSize = 14.sp, color = PrimaryColor)
//                                )
//                                Spacer(modifier = Modifier.width(4.dp))
//                                Icon(
//                                    painter = painterResource(Res.drawable.ic_cale),
//                                    contentDescription = null,
//                                    modifier = Modifier.size(16.dp),
//                                    tint = PrimaryColor
//                                )
//                            }
//                        }
//                    }
//                    Spacer(modifier = Modifier.height(16.dp))
//                    AttendanceCalendarGrid(attendanceSummary)
//                }
//            }
//        )
//
//        Spacer(modifier = Modifier.height(24.dp))

        // Recent Logs
        Text(
            text = "Recent Logs",
            style = AppTextTheme.semiBold.copy(fontSize = 16.sp),
            modifier = Modifier.padding(bottom = 12.dp),
        )
        CommonCard(
            content = {
                Column {
                    if (filteredLogs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = 220.dp)
                                .padding(vertical = 16.dp, horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AppEmptyStateView(
                                image = Res.drawable.img_no_checkin,
                                title = "No recent activity",
                                subtitle = if (!selectedFilterDate.isNullOrBlank()) {
                                    "No attendance records for ${attendanceFilterDateLabel(selectedFilterDate)}."
                                } else {
                                    "Check-ins for ${attendanceSummary.filter.month_label} will appear here."
                                },
                                modifier = Modifier.fillMaxWidth(),
                                useCardContainer = false
                            )
                        }
                    } else {
                        val logsToShow = if (showViewAllHistoryButton) {
                            filteredLogs.take(6)
                        } else {
                            filteredLogs
                        }

                        logsToShow.forEachIndexed { index, log ->
                            val isCheckOut = !log.checked_out_at.isNullOrBlank()
                            AttendanceLogItem(
                                title = log.headline ?: if (isCheckOut) "Check-out" else "Check-in",
                                dateTime = attendanceLogDateTimeLabel(
                                    log.display_relative,
                                    if (isCheckOut) log.checked_out_at else log.checked_in_at,
                                ),
                                status = log.punctuality ?: "REGULAR",
                                statusColor = when (log.punctuality) {
                                    "EARLY", "ON TIME" -> PrimaryGreenColor
                                    "LATE" -> YellowColor
                                    else -> Gray
                                }
                            )
                            if (index < logsToShow.size - 1) {
                                HorizontalDivider(color = GrayBorderColor)
                            }
                        }
                    }

                    if (showViewAllHistoryButton && attendanceSummary.recent_logs.size > 6) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(onClick = onViewAllHistoryClick)
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "View All History",
                                style = AppTextTheme.bold.copy(fontSize = 16.sp, color = PrimaryColor)
                            )
                        }
                    }
                }

            }
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun BiometricActionItem(
    label: String,
    icon: ImageVector,
    color: Color,
    tint: Color = Gray,
    onClick: () -> Unit = {}
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(color, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = tint
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, style = AppTextTheme.semiBold.copy(fontSize = 12.sp, color = Gray))
    }
}

@Composable
fun AttendanceStatCard(
    modifier: Modifier,
    value: String,
    unit: String,
    label: String,
    progress: Float
) {
    CommonCard(
        modifier = modifier,
        content = {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth().height(160.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceAround
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(96.dp),
                        color = PrimaryColor,
                        strokeWidth = 8.dp,
                        trackColor = Color(0xFFF1F5F9),
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = value, style = AppTextTheme.semiBold.copy(fontSize = 24.sp))
                        Text(
                            text = unit,
                            style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = label,
                    style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray),
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}

@Composable
fun AttendanceLogItem(title: String, dateTime: String, status: String, statusColor: Color) {
    val isCheckOut = title.contains("check-out", ignoreCase = true) ||
        title.contains("checkout", ignoreCase = true) ||
        title.contains("check out", ignoreCase = true)
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(Color(0xFFE7F7F2), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isCheckOut) Icons.Default.Logout else Icons.AutoMirrored.Filled.Login,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = PrimaryColor
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = AppTextTheme.bold.copy(fontSize = 15.sp))
            Text(text = dateTime, style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Gray))
        }
        Text(
            text = status,
            style = AppTextTheme.bold.copy(fontSize = 13.sp, color = statusColor.copy(alpha = 0.8f))
        )
    }
}

@Composable
fun MemberDetailPaymentsContent(memberDetail: MemberDetailResponse?) {
    val navigator = LocalNavigator.current
    val currentSub = memberDetail?.subscription?.current_subscription
    val paymentSummary = memberDetail?.paymentSummary
    val historyEntries = memberDetail?.paymentHistory.orEmpty().filter {
        it.amount != null && !it.date.isNullOrBlank()
    }

    val hasPaymentData = paymentSummary != null ||
        historyEntries.isNotEmpty() ||
        (currentSub != null && (currentSub.resolvedAmountPaid() > 0 || currentSub.resolvedAmountPending() > 0))

    if (!hasPaymentData) {
        AppEmptyStateView(
            image = Res.drawable.img_no_payment,
            title = "No Payment Records Found",
            subtitle = "Choose a subscription and collect payment to see transaction history."
        )
        Spacer(modifier = Modifier.height(80.dp))
        return
    }

    val paidLabel: String
    val paidAmount: Int
    val outstandingLabel: String
    val outstandingAmount: Int
    if (paymentSummary != null) {
        paidLabel = "Paid (this year)"
        paidAmount = paymentSummary.paidThisYear
        outstandingLabel = "Outstanding"
        outstandingAmount = paymentSummary.outstandingAmount
    } else {
        paidLabel = "Paid (Current)"
        paidAmount = currentSub?.resolvedAmountPaid() ?: 0
        outstandingLabel = "Outstanding"
        outstandingAmount = currentSub?.resolvedAmountPending() ?: 0
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PaymentSummaryCard(
                modifier = Modifier.weight(1f),
                label = paidLabel,
                amount = "${Constants.RUPEE} $paidAmount",
                icon = Res.drawable.ic_payment,
                amountColor = PrimaryColor
            )
            PaymentSummaryCard(
                modifier = Modifier.weight(1f),
                label = outstandingLabel,
                amount = "${Constants.RUPEE} $outstandingAmount",
                icon = Res.drawable.ic_payment,
                amountColor = RedDarkColor
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Payment History", style = AppTextTheme.medium.copy(fontSize = 14.sp))
            Text(
                text = "View All",
                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = PrimaryColor),
                modifier = Modifier.clickable { navigator?.push(MemberPaymentHistoryScreen(
                    memberDetail?.gymUserId.toString()
                )) }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (historyEntries.isEmpty()) {
            Text(
                text = "No individual payment rows returned yet.",
                style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            val memberName = memberDetail?.user?.fullName.orEmpty()
            val memberPhone = memberDetail?.user?.phone
                ?: memberDetail?.contact?.phone
                ?: memberDetail?.summary?.phone
            historyEntries.forEach { entry ->
                PaymentHistoryCard(
                    model = entry.toPaymentHistoryCardModel(
                        memberName = memberName,
                        memberPhone = memberPhone,
                        dateFormatter = ::memberDetailDateLabel,
                    ),
                    onShare = { ShareService.shareMemberPaymentEntry(entry, memberName) },
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val memberName = memberDetail?.user?.fullName.orEmpty()
        CommonButton(
            onClick = {
                val lastEntry = historyEntries.firstOrNull()
                if (lastEntry != null) {
                    ShareService.shareMemberPaymentEntry(lastEntry, memberName)
                } else {
                    ShareService.shareText("No payment transactions available to share yet.")
                }
            },
            text = "Share Last Transaction",
            modifier = Modifier.fillMaxWidth(),
            leftIcon = painterResource(Res.drawable.ic_share),
            enabled = historyEntries.isNotEmpty(),
        )
        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun PaymentSummaryCard(
    modifier: Modifier,
    label: String,
    amount: String,
    icon: DrawableResource,
    amountColor: Color
) {
    CommonCard(
        modifier = modifier,
        content = {
            Column(modifier = Modifier.padding(16.dp)) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Gray)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = amount,
                    style = AppTextTheme.bold.copy(fontSize = 18.sp, color = amountColor)
                )
            }
        }
    )
}

@Composable
fun MemberDetailWorkoutContent(
    workouts: List<WorkoutDTO>,
    memberId: String = "",
    onDeleteWorkout: (String) -> Unit = {}
) {
    val navigator = LocalNavigator.current

    if (workouts.isEmpty()) {
        AppEmptyStateView(
            image = Res.drawable.img_no_plan,
            title = "No Workout Plans Found",
            subtitle = "No workout is assigned yet. Tap + to create the first workout plan."
        )
        Spacer(modifier = Modifier.height(80.dp))
    } else {
        Column {
            workouts.forEach { workout ->
                WorkoutPlanCard(
                    title = workout.title ?: "Workout",
                    count = "${workout.exercise_count ?: 0} Workout",
                    tag = workout.date?.let { memberDetailDateLabel(it) }?.takeIf { it.isNotBlank() } ?: "N/A",
                    createdBy = workout.createdByDisplayLabel(),
                    onCardClick = {
                        navigator?.push(
                            CreateWorkoutScreen(
                                memberId = memberId,
                                workoutId = workout.workoutId,
                                readOnly = true
                            )
                        )
                    },
                    onEditClick = {
                        navigator?.push(
                            CreateWorkoutScreen(
                                memberId = memberId,
                                workoutId = workout.workoutId,
                                readOnly = false
                            )
                        )
                    },
                    onDeleteClick = { onDeleteWorkout(workout.workoutId) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}


@Composable
fun DietContent(
    meals: List<DietMealDTO>,
    isLoading: Boolean,
    onMealClick: (DietMealDTO) -> Unit,
    onMealEdit: (DietMealDTO) -> Unit,
    mealAuthorName: String? = null,
    onMealDelete: ((DietMealDTO) -> Unit)? = null
) {
    fun mealVisual(mealType: String): Triple<ImageVector, Color, Color> =
        when (mealType.uppercase()) {
            "BREAKFAST" -> Triple(Icons.Default.WbSunny, Color(0xFFD1FAE5), Color(0xFF047857))
            "LUNCH" -> Triple(Icons.Default.Restaurant, Color(0xFFE0F2FE), Color(0xFF0369A1))
            "DINNER" -> Triple(Icons.Default.Restaurant, Color(0xFFEDE9FE), Color(0xFF5B21B6))
            "SNACK" -> Triple(Icons.Default.Fastfood, Color(0xFFFEF3C7), Color(0xFFB45309))
            else -> Triple(Icons.Default.WbSunny, Color(0xFFF1F5F9), Color(0xFF475569))
        }

    Column(modifier = Modifier.fillMaxWidth()) {
        when {
            isLoading && meals.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryColor)
                }
            }

            meals.isEmpty() -> {
                AppEmptyStateView(
                    image = Res.drawable.img_no_expenses,
                    title = "No Diet Plans Found",
                    subtitle = "No diet plan is scheduled yet. Tap + to add a meal plan."
                )
                Spacer(modifier = Modifier.height(80.dp))
            }

            else -> {
                Text(
                    text = "Scheduled meals",
                    style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                meals.forEach { meal ->
                    val totalKcal = meal.food_items.sumOf { it.calories * it.quantity }
                    val (icon, bg, tint) = mealVisual(meal.meal_type)
                    val foods = meal.food_items.map { fi ->
                        FoodItem(
                            name = fi.name,
                            quantity = formatDietMealFoodSummary(
                                weightKg = fi.weight_kg,
                                unitType = fi.unit_type,
                                calories = fi.calories,
                                quantity = fi.quantity,
                                protein = fi.protein,
                                carbs = fi.carbs,
                                fat = fi.fat,
                            ),
                        )
                    }
                    val scheduleLabel =
                        meal.time.takeIf { it.isNotBlank() }?.let { t -> "Meal time · $t" }
                    val byLabel = mealAuthorName?.takeIf { it.isNotBlank() }?.let { "By: $it" }
                    DietPlanCard(
                        mealName = meal.name,
                        caloriesLine = "$totalKcal kcal",
                        scheduleTime = scheduleLabel,
                        byLine = byLabel,
                        icon = icon,
                        iconBgColor = bg,
                        iconTintColor = tint,
                        foods = foods,
                        onCardClick = { onMealClick(meal) },
                        onEditClick = { onMealEdit(meal) },
                        onDeleteClick = { onMealDelete?.invoke(meal) },
                        showEditButton = true,
                        showDeleteButton = onMealDelete != null
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun MemberDetailStatusRow(label: String, value: String, icon: DrawableResource, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = AppTextTheme.medium.copy(fontSize = 15.sp, color = color),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = AppTextTheme.bold.copy(fontSize = 16.sp)
        )
    }
}

@Composable
fun MemberDetailInfoItem(label: String, value: String, icon: DrawableResource) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.LightGray))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
            )
            Text(
                text = value,
                style = AppTextTheme.medium.copy(fontSize = 14.sp)
            )
        }
    }
}
