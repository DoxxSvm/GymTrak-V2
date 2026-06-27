package `in`.gym.trak.studio.features.dashboard

import `in`.gym.trak.studio.base.Constants

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import androidx.compose.foundation.layout.navigationBarsPadding
import coil3.compose.rememberAsyncImagePainter
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import `in`.gym.trak.studio.components.BackHandler
import `in`.gym.trak.studio.components.CommonProgressOverlay
import `in`.gym.trak.studio.components.ConfirmationDialog
import `in`.gym.trak.studio.components.CustomContainer
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import `in`.gym.trak.studio.components.rememberExitApplication
import `in`.gym.trak.studio.data.model.AttendanceStats
import `in`.gym.trak.studio.data.model.DashboardStats
import `in`.gym.trak.studio.data.model.EnquiryStatsResponse
import `in`.gym.trak.studio.data.model.ExpiryAlerts
import `in`.gym.trak.studio.data.model.HealthFactors
import `in`.gym.trak.studio.data.model.HealthStats
import `in`.gym.trak.studio.data.model.MemberStatsResponse
import `in`.gym.trak.studio.data.model.NotificationStats
import `in`.gym.trak.studio.data.model.OwnerDashboardNewResponse
import `in`.gym.trak.studio.data.model.PaymentStats
import `in`.gym.trak.studio.data.model.RecentPaymentMemberUserDTO
import `in`.gym.trak.studio.data.model.RecentPaymentNewDTO
import `in`.gym.trak.studio.data.model.TrafficTrendHourlyDTO
import `in`.gym.trak.studio.data.model.TrafficTrendStats
import `in`.gym.trak.studio.data.model.TrafficTrendWeeklyDTO
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.data.repository.SessionManager.PermissionKeys
import `in`.gym.trak.studio.data.repository.SessionManager.hasPermission
import `in`.gym.trak.studio.utils.NotificationManager
import `in`.gym.trak.studio.features.enquiries.EnquiryScreen
import `in`.gym.trak.studio.features.members.MemberScreen
import `in`.gym.trak.studio.features.payments.PaymentsScreen
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.BlueLightColor
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.GreenColor
import `in`.gym.trak.studio.theme.GreenLight2Color
import `in`.gym.trak.studio.theme.GreenLightColor
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.PrimaryDarkColor
import `in`.gym.trak.studio.theme.PrimaryLightColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.theme.YellowColor
import `in`.gym.trak.studio.theme.YellowLightColor
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.gym_boy
import gym.composeapp.generated.resources.ic_active
import gym.composeapp.generated.resources.ic_bottom_home
import gym.composeapp.generated.resources.ic_bottom_members
import gym.composeapp.generated.resources.ic_bottom_wallet
import gym.composeapp.generated.resources.ic_expired
import gym.composeapp.generated.resources.ic_gym
import gym.composeapp.generated.resources.ic_inactive
import gym.composeapp.generated.resources.ic_gym_subscription
import gym.composeapp.generated.resources.ic_location
import gym.composeapp.generated.resources.ic_members
import gym.composeapp.generated.resources.ic_money
import gym.composeapp.generated.resources.ic_notification
import gym.composeapp.generated.resources.ic_warning
import gym.composeapp.generated.resources.userIcon
import `in`.gym.trak.studio.data.model.GymDTO
import `in`.gym.trak.studio.data.model.UserOwnedGymDTO
import `in`.gym.trak.studio.utils.DateUtils
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.max

private fun formatDashboardAmountFromCents(cents: Long, currencyCode: String?): String {
    val negative = cents < 0
    val abs = kotlin.math.abs(cents)
    val whole = abs
    val minor = abs
    val amountText =
        if (minor == 0L) "$whole" else "$whole"

    val body = "${Constants.RUPEE}$amountText"
    return if (negative) "-$body" else body
}

private fun recentPaymentMemberLabel(payment: RecentPaymentNewDTO): String =
    payment.memberUser?.fullName?.takeIf { it.isNotBlank() }
        ?: payment.memberName?.takeIf { it.isNotBlank() }
        ?: "Unknown"

class OwnerDashboardScreen : Screen {
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val dashboardData by screenModel.dashboardData.collectAsState()
        val isLoading by screenModel.isLoading.collectAsState()
        val fcmToken by NotificationManager.fcmToken.collectAsState(
            SessionManager.fcmDeviceToken.takeIf { it.isNotEmpty() }
        )
        val permissionsVersion by screenModel.permissionsVersion.collectAsState()
        val userGyms by screenModel.userGyms.collectAsState()
        val activeGymId by screenModel.activeGymId.collectAsState()

        val selectedTab by screenModel.selectedTab.collectAsState()
        var showExitDialog by remember { mutableStateOf(false) }
        var isPullRefreshRequested by remember { mutableStateOf(false) }
        val exitApp = rememberExitApplication()
//        val apiPermissions = dashboardData?.let { data ->
//            if (data.effectivePermissions.isNotEmpty()) data.effectivePermissions else data.permissions.effective
//        }.orEmpty()
//        val hasSessionPermissions = remember(dashboardData) {
//            SessionManager.getOwnerDashboardPermissions() != null
//        }
//        fun hasPermission(vararg keys: String): Boolean {
//            if (apiPermissions.isNotEmpty()) {
//                return keys.any { key -> apiPermissions[key] == true }
//            }
//            if (hasSessionPermissions) {
//                return keys.any { key -> SessionManager.getPermission(key) }
//            }
//            return true
//        }

        // Load gyms first so [SessionManager.gymId] is valid (persisted id or first gym), then dashboard.
        LaunchedEffect(Unit) {
            screenModel.bootstrapDashboardEntry()
            screenModel.registerDeviceTokenWithBackend()
        }
        var previousTab by remember { mutableStateOf<String?>(null) }
        LaunchedEffect(selectedTab, permissionsVersion) {
            if (permissionsVersion == 0) return@LaunchedEffect
            if (selectedTab == "Home") {
                if (previousTab != null && previousTab != "Home") {
                    screenModel.loadDashboardData()
                }
                previousTab = selectedTab
            } else {
                previousTab = selectedTab
            }
        }
        LaunchedEffect(isLoading) {
            if (!isLoading) isPullRefreshRequested = false
        }

        // When FCM token arrives after splash (or refreshes), register with backend.
        LaunchedEffect(fcmToken) {
            if (!fcmToken.isNullOrEmpty()) {
                screenModel.registerDeviceTokenWithBackend()
            }
        }


        val navItems = remember(permissionsVersion) {
            buildList {
//                if (hasPermission(PermissionKeys.KEY_DASHBOARD, PermissionKeys.KEY_DASHBOARD_VIEW)) {
                    add(DashboardTabItem("Home", Res.drawable.ic_bottom_home, "Home"))
//                }
                if (hasPermission(
                        PermissionKeys.KEY_MEMBERS,
                        PermissionKeys.KEY_CLIENT_READ,
                        PermissionKeys.KEY_CLIENT_DETAILS_READ
                    )
                ) {
                    add(DashboardTabItem("Members", Res.drawable.ic_bottom_members, "Members"))
                }
                if (hasPermission(
                        PermissionKeys.KEY_PAYMENTS,
                        PermissionKeys.KEY_PAYMENT_READ,
                        PermissionKeys.KEY_DASHBOARD_PAYMENTS_WIDGET
                    )
                ) {
                    add(DashboardTabItem("Payments", Res.drawable.ic_bottom_wallet, "Payments"))
                }
                add(DashboardTabItem("Profile", Res.drawable.userIcon, "Profile"))
            }
        }
        val defaultTab = navItems.first().value

        LaunchedEffect(navItems, selectedTab) {
            if (selectedTab.isBlank() || navItems.none { it.value == selectedTab }) {
                screenModel.onTabSelected(defaultTab)
            }
        }

        BackHandler {
            when {
                showExitDialog -> showExitDialog = false
                selectedTab != defaultTab -> screenModel.onTabSelected(defaultTab)
                else -> showExitDialog = true
            }
        }

        if (showExitDialog) {
            ConfirmationDialog(
                onDismissRequest = { showExitDialog = false },
                onConfirm = { exitApp() },
                title = "Exit app?",
                message = "Are you sure you want to close the application?",
                confirmText = "Exit",
                dismissText = "Cancel",
            )
        }

        // Block the entire view until the dashboard API completes and permissions are persisted.
        // Only a clean spinner is shown — no nav bar, no tab content, no dim overlays.
        if (permissionsVersion == 0) {
            CommonProgressOverlay()
        } else {
            // Permissions are ready — show full dashboard UI with error/toast handling.
            LoadingScreenHandler(
                screenModel = screenModel,
                showLoadingOverlay = !isPullRefreshRequested
            ) {
                Scaffold(
                    bottomBar = {
                        BottomNavigationBar(
                            selectedItem = selectedTab,
                            items = navItems,
                            onItemSelected = { selected ->
                                if (navItems.any { it.value == selected }) {
                                    screenModel.onTabSelected(selected)
                                }
                            }
                        )
                    }
                ) { paddingValues ->
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                        AnimatedContent(
                            targetState = selectedTab,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(400)))
                                    .togetherWith(fadeOut(animationSpec = tween(400)))
                            },
                            modifier = Modifier.fillMaxSize()
                        ) { targetTab ->
                            when (targetTab) {
                                "Home" -> {
                                    PullToRefreshBox(
                                        isRefreshing = isPullRefreshRequested && isLoading,
                                        onRefresh = {
                                            isPullRefreshRequested = true
                                            screenModel.refresh()
                                        },
                                        state = rememberPullToRefreshState(),
                                        indicator = {}
                                    ) {
                                        HomeContent(
                                            data = dashboardData,
                                            ownedGyms = userGyms,
                                            activeGymId = activeGymId,
                                            onSelectOwnerGym = { screenModel.selectOwnerGym(it) },
                                            onMemberStatNavigate = { filter ->
                                                screenModel.openMembersWithFilter(filter)
                                            },
                                            onViewAllTransactions = {
                                                screenModel.onTabSelected("Payments")
                                            }
                                        )
                                    }
                                }
                                "Members" -> MemberScreen(screenModel)
                                "Payments" -> PaymentsScreen()
                                "Profile" -> ProfilePage(screenModel)
                                else -> Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "$targetTab Screen Coming Soon")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeContent(
    data: OwnerDashboardNewResponse?,
    ownedGyms: List<UserOwnedGymDTO> = emptyList(),
    activeGymId: String = "",
    onSelectOwnerGym: (gymId: String) -> Unit = {},
    onMemberStatNavigate: (memberListFilter: String) -> Unit = {},
    onViewAllTransactions: () -> Unit = {}
) {
//    val apiPermissions = data?.let { payload ->
//        payload.effectivePermissions.ifEmpty { payload.permissions.effective }
//    }.orEmpty()
//    val hasSessionPermissions = SessionManager.getOwnerDashboardPermissions() != null
//    fun hasPermission(vararg keys: String): Boolean {
//        if (apiPermissions.isNotEmpty()) {
//            return keys.any { key -> apiPermissions[key] == true }
//        }
//        if (hasSessionPermissions) {
//            return keys.any { key -> SessionManager.getPermission(key) }
//        }
//        return true
//    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header Section
        item {
            val navigator = LocalNavigator.current
            StaggeredEntranceItem(index = 0) {
                HeaderSection(
                    greeting = "Dashboard",
                    userName = data?.owner_name ?: "Owner",
                    fallbackGymName = data?.gym?.name,
                    ownedGyms = ownedGyms,
                    activeGymId = activeGymId,
                    allowGymSelection = SessionManager.userRole.equals("gym_owner", ignoreCase = true),
                    onSelectOwnerGym = onSelectOwnerGym,
                    canViewNotifications = hasPermission(PermissionKeys.KEY_DASHBOARD_NOTIFICATIONS),
                    onNotificationClick = { navigator?.push(NotificationScreen()) }
                )
            }
        }

        // Analytics Card
        if (hasPermission(PermissionKeys.KEY_DASHBOARD_ANALYTICS)) {
            item {
                StaggeredEntranceItem(index = 1) {
                    AnalyticsCard(data?.members?.active ?: 0, data?.members?.total ?: 0)
                }
            }
        }

        // Payments Section
        if (hasPermission(
                PermissionKeys.KEY_PAYMENTS,
                PermissionKeys.KEY_PAYMENT_READ,
                PermissionKeys.KEY_DASHBOARD_PAYMENTS_WIDGET
            )
        ) {
            item {
                StaggeredEntranceItem(index = 2) {
                    PaymentsSection(
                        receivedLast30DaysCents = data?.payments?.receivedLast30DaysCents ?: 0L,
                        pendingCents = data?.payments?.pendingCents ?: 0L,
                        summaryCurrency = data?.payments?.currency,
                        recentPayments = data?.payments?.recent ?: emptyList(),
                        onViewAllTransactions = onViewAllTransactions
                    )
                }
            }
        }

        // Traffic Trend
        if (hasPermission(
                PermissionKeys.KEY_ATTENDANCE_READ,
                PermissionKeys.KEY_DASHBOARD_ANALYTICS
            )
        ) {
            item {
                StaggeredEntranceItem(index = 3) {
                    TrafficTrendSection(trafficTrend = data?.trafficTrend)
                }
            }
        }

        // Stats Grid
        if (hasPermission(
                PermissionKeys.KEY_MEMBERS,
                PermissionKeys.KEY_CLIENT_READ,
                PermissionKeys.KEY_CLIENT_DETAILS_READ
            )
        ) {
            item {
                StaggeredEntranceItem(index = 4) {
                    val stats = data?.members?.let {
                        DashboardStats(
                            active_members = it.active,
                            inactive_members = it.inactive,
                            expired_members = it.expired,
                            total_members = it.total
                        )
                    }
                    StatsGrid(stats = stats, onStatClick = onMemberStatNavigate)
                }
            }
        }

        // Enquiry Overview
        if (hasPermission(PermissionKeys.KEY_LEAD_READ)) {
            item {
                StaggeredEntranceItem(index = 5) {
                    EnquiryOverview(data?.enquiries)
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeaderSection(
    greeting: String,
    userName: String,
    fallbackGymName: String?,
    ownedGyms: List<UserOwnedGymDTO> = emptyList(),
    activeGymId: String = "",
    /** Gym picker is available only for gym owners (not trainers or staff). */
    allowGymSelection: Boolean = false,
    onSelectOwnerGym: (gymId: String) -> Unit = {},
    canViewNotifications: Boolean = true,
    onNotificationClick: () -> Unit = {}
) {
    var showGymSelector by remember { mutableStateOf(false) }
    val gymPickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val displayGymName = remember(ownedGyms, activeGymId, fallbackGymName) {
        ownedGyms.find { it.id == activeGymId }?.name?.takeIf { it.isNotBlank() }
            ?: fallbackGymName?.takeIf { it.isNotBlank() }
            ?: "Gym"
    }

    val canOpenGymPicker = allowGymSelection && ownedGyms.size > 1

    LaunchedEffect(allowGymSelection) {
        if (!allowGymSelection) showGymSelector = false
    }

    if (showGymSelector && canOpenGymPicker) {
        ModalBottomSheet(
            onDismissRequest = { showGymSelector = false },
            sheetState = gymPickerSheetState,
            containerColor = White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            dragHandle = null,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Text(
                    text = "Select gym",
                    style = AppTextTheme.bold.copy(fontSize = 18.sp, color = Black),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
                HorizontalDivider(color = Color(0xFFE5E7EB))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ownedGyms, key = { it.id }) { gym ->
                        val selected = gym.id == activeGymId
                        Surface(
                            onClick = {
                                onSelectOwnerGym(gym.id)
                                showGymSelector = false
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) PrimaryColor.copy(alpha = 0.12f) else Color(0xFFF9FAFB),
                            border = if (selected) {
                                BorderStroke(1.dp, PrimaryColor.copy(alpha = 0.45f))
                            } else {
                                null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFE8ECF0)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val thumb = gym.logoUrl
                                    if (!thumb.isNullOrBlank()) {
                                        Image(
                                            painter = rememberAsyncImagePainter(thumb),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_gym_subscription),
                                            contentDescription = null,
                                            tint = PrimaryColor,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = gym.name.ifBlank { "Gym" },
                                        style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black),
                                        maxLines = 2
                                    )
                                    val addr = gym.address
                                    if (!addr.isNullOrBlank()) {
                                        Text(
                                            text = addr,
                                            style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray),
                                            maxLines = 2
                                        )
                                    }
                                }
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = PrimaryColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(GreenColor, GreenLightColor)
                )
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.gym_boy),
                    contentDescription = "Profile Image",
                    modifier = Modifier.size(45.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = greeting,
                    color = White.copy(alpha = 0.8f),
                    style = AppTextTheme.regular.copy(fontSize = 14.sp)
                )
                Text(
                    text = userName,
                    color = White,
                    style = AppTextTheme.bold.copy(fontSize = 18.sp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            if (canViewNotifications) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(White)
                        .clickable { onNotificationClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_notification),
                        contentDescription = "Notification",
                        tint = PrimaryDarkColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(White)
                .clickable(enabled = canOpenGymPicker) {
                    if (canOpenGymPicker) {
                        showGymSelector = true
                    }
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_location),
                contentDescription = "Location",
                tint = PrimaryDarkColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = displayGymName,
                color = Gray,
                style = AppTextTheme.medium.copy(fontSize = 14.sp),
                modifier = Modifier.weight(1f)
            )
            if (canOpenGymPicker) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Dropdown",
                    tint = Gray
                )
            }
        }
    }
}

@Composable
fun AnalyticsCard(active: Int, total: Int) {
    val percentage =
        if (total > 0) (active.toFloat() / total.toFloat()) else 0.85f // Default 85% fallback
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B2544))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(
                        text = "Training",
                        color = White,
                        style = AppTextTheme.bold.copy(fontSize = 18.sp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Analytics",
                        color = Color(0xFFFFC107),
                        style = AppTextTheme.bold.copy(fontSize = 18.sp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "High-Impact Performance\nThis Month",
                    color = White.copy(alpha = 0.7f),
                    style = AppTextTheme.regular.copy(fontSize = 12.sp)
                )
            }

            Box(contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.size(80.dp)) {
                    drawArc(
                        color = Color.Gray.copy(alpha = 0.2f),
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = Color(0xFFFFC107),
                        startAngle = 135f,
                        sweepAngle = 270f * percentage.coerceIn(0f, 1f),
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_gym),
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${(percentage * 100).toInt()}%",
                        color = White,
                        style = AppTextTheme.bold.copy(fontSize = 16.sp)
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentsSection(
    receivedLast30DaysCents: Long,
    pendingCents: Long,
    summaryCurrency: String?,
    recentPayments: List<RecentPaymentNewDTO>,
    onViewAllTransactions: () -> Unit
) {
    Column {
        Text(
            text = "Payments",
            style = AppTextTheme.bold.copy(fontSize = 18.sp),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        )

        {
            Column {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_money),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Received",
                                    color = Gray,
                                    style = AppTextTheme.regular.copy(fontSize = 12.sp)
                                )
                                Text(
                                    text = formatDashboardAmountFromCents(
                                        receivedLast30DaysCents,
                                        summaryCurrency
                                    ),
                                    color = PrimaryColor,
                                    style = AppTextTheme.bold.copy(fontSize = 16.sp)
                                )
                            }
                        }
                        VerticalDivider(
                            modifier = Modifier.height(40.dp).width(1.dp),
                            color = Color.LightGray
                        )
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_warning),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Pending",
                                    color = Gray,
                                    style = AppTextTheme.regular.copy(fontSize = 12.sp)
                                )
                                Text(
                                    text = formatDashboardAmountFromCents(pendingCents, summaryCurrency),
                                    color = YellowColor,
                                    style = AppTextTheme.bold.copy(fontSize = 16.sp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    if (recentPayments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Image(
                                    painter = painterResource(Res.drawable.ic_warning), // A suitable placeholder/icon
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp).padding(bottom = 8.dp),
                                    colorFilter = tint(Color.LightGray)
                                )
                                Text(
                                    text = "No recent payments",
                                    color = Gray,
                                    style = AppTextTheme.medium.copy(fontSize = 14.sp)
                                )
                            }
                        }
                    } else {
                        val displayPayments = recentPayments.take(3)
                        displayPayments.forEachIndexed { index, payment ->
                            TransactionItem(
                                name = recentPaymentMemberLabel(payment),
                                time = DateUtils.formatDashboardTransactionDateTime(payment.createdAt),
                                statusLabel = payment.status ?: "",
                                amount = formatDashboardAmountFromCents(
                                    payment.amountCents ?: 0L,
                                    payment.currency ?: summaryCurrency
                                ),
                                image = Res.drawable.gym_boy
                            )
                            if (index < displayPayments.size - 1) {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                }
// ...

                Button(
                    onClick = onViewAllTransactions,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryColor.copy(alpha = 0.16f)
                    ),
                    shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        text = "View All Transactions",
                        color = Black,
                        style = AppTextTheme.medium,
                        modifier = Modifier.padding(vertical = 14.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItem(
    name: String,
    time: String,
    statusLabel: String,
    amount: String,
    image: DrawableResource
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(image),
            contentDescription = null,
            modifier = Modifier.size(35.dp).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = AppTextTheme.bold.copy(fontSize = 14.sp))
            if (time.isNotBlank()) {
                Text(
                    text = time,
                    color = Gray,
                    style = AppTextTheme.regular.copy(fontSize = 11.sp)
                )
            }
            if (statusLabel.isNotBlank()) {
                Text(
                    text = statusLabel,
                    color = Gray.copy(alpha = 0.85f),
                    style = AppTextTheme.regular.copy(fontSize = 10.sp)
                )
            }
        }
        Text(text = amount, color = PrimaryColor, style = AppTextTheme.bold.copy(fontSize = 14.sp))
    }
}

private fun trafficTrendDayMatches(selected: String, candidate: String): Boolean {
    if (selected.equals(candidate, ignoreCase = true)) return true
    val s = selected.take(3)
    val c = candidate.take(3)
    return s.isNotEmpty() && s.equals(c, ignoreCase = true)
}

private data class TrafficHourSlot(val start: String, val end: String?)

/** Parses API labels like `6 AM to 9 AM` into start/end times. */
private fun parseTrafficHourSlot(label: String): TrafficHourSlot {
    val trimmed = label.trim()
    if (trimmed.isEmpty()) return TrafficHourSlot("", null)
    val parts = trimmed.split(Regex("\\s+to\\s+", RegexOption.IGNORE_CASE), limit = 2)
    return if (parts.size == 2) {
        TrafficHourSlot(start = parts[0].trim(), end = parts[1].trim())
    } else {
        TrafficHourSlot(start = trimmed, end = null)
    }
}

/** Compact range when both times share AM/PM, e.g. `6–9 AM`. */
private fun formatTrafficHourSlotCompact(label: String): String {
    val slot = parseTrafficHourSlot(label)
    val end = slot.end ?: return slot.start
    val startMeridiem = trafficHourMeridiem(slot.start)
    val endMeridiem = trafficHourMeridiem(end)
    if (startMeridiem != null && startMeridiem == endMeridiem) {
        val startTime = slot.start.removeSuffix(" $startMeridiem").removeSuffix(startMeridiem).trim()
        val endTime = end.removeSuffix(" $endMeridiem").removeSuffix(endMeridiem).trim()
        return "$startTime–$endTime $startMeridiem"
    }
    return "${slot.start}–$end"
}

private fun trafficHourMeridiem(time: String): String? = when {
    time.endsWith(" AM", ignoreCase = true) -> "AM"
    time.endsWith(" PM", ignoreCase = true) -> "PM"
    else -> null
}

private fun trafficTrendPeakSlotLabel(hourlyBars: List<TrafficTrendHourlyDTO>, peakCount: Int): String? {
    if (peakCount <= 0) return null
    val peakSlot = hourlyBars.firstOrNull { it.count == peakCount } ?: return null
    return formatTrafficHourSlotCompact(peakSlot.label)
}

private fun trafficTrendDayLabel(day: String, todayShortName: String): String =
    if (trafficTrendDayMatches(day, todayShortName)) "Today" else day

@Composable
fun TrafficTrendSection(
    trafficTrend: TrafficTrendStats?
) {
    val weeklyDays = trafficTrend?.weekly.orEmpty().ifEmpty {
        listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").map { TrafficTrendWeeklyDTO(day = it, count = 0) }
    }
    var selectedDay by remember(trafficTrend?.selectedDay) {
        mutableStateOf(
            trafficTrend?.selectedDay?.takeIf { it.isNotBlank() }
                ?: weeklyDays.firstOrNull()?.day.orEmpty().ifBlank { "Mon" }
        )
    }
    LaunchedEffect(trafficTrend?.selectedDay) {
        trafficTrend?.selectedDay?.takeIf { it.isNotBlank() }?.let { selectedDay = it }
    }

    val todayShortName = remember { DateUtils.getCurrentWeekdayShortName() }
    val todayCount = trafficTrend?.todayCount ?: 0
    val apiCapacity = trafficTrend?.capacity ?: 0
    val selectedWeekDay = weeklyDays.find { trafficTrendDayMatches(selectedDay, it.day) }
    val hourlyBars = selectedWeekDay?.hourly.orEmpty()
    val maxBarCount = hourlyBars.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    val peakCount = hourlyBars.maxOfOrNull { it.count } ?: 0
    val selectedDayLabel = trafficTrendDayLabel(selectedDay, todayShortName)
    val dayTotal = selectedWeekDay?.count ?: 0
    val isSelectedToday = trafficTrendDayMatches(selectedDay, todayShortName)
    val currentCount = if (isSelectedToday) todayCount else dayTotal
    val progressFraction = if (apiCapacity > 0) {
        (currentCount.toFloat() / apiCapacity.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val progressPercent = (progressFraction * 100f).toInt()
    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 400),
        label = "trafficCapacityProgress"
    )
    val hasHourlyData = hourlyBars.isNotEmpty()
    val chartHeight = 168.dp
    val peakSlotLabel = remember(hourlyBars, peakCount) {
        trafficTrendPeakSlotLabel(hourlyBars, peakCount)
    }
    val gridColor = Color(0xFFE8ECF4)
    val chartBackground = Color(0xFFF8FAFC)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Traffic Trend",
                        style = AppTextTheme.bold.copy(fontSize = 17.sp, color = Black)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Hourly breakdown · $selectedDayLabel",
                        style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$currentCount",
                            style = AppTextTheme.bold.copy(fontSize = 22.sp, color = PrimaryColor)
                        )
                        Text(
                            text = if (apiCapacity > 0) "/$apiCapacity" else "",
                            style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray)
                        )
                    }
                    Text(
                        text = if (apiCapacity > 0) "$progressPercent% capacity" else selectedDayLabel,
                        style = AppTextTheme.regular.copy(fontSize = 11.sp, color = Gray)
                    )
                }
            }

//            if (apiCapacity > 0) {
//                Spacer(modifier = Modifier.height(12.dp))
//                Row(
//                    modifier = Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.SpaceBetween,
//                    verticalAlignment = Alignment.CenterVertically
//                ) {
//                    Text(
//                        text = "$currentCount of $apiCapacity visits",
//                        style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Black)
//                    )
//                    Text(
//                        text = "$progressPercent%",
//                        style = AppTextTheme.bold.copy(fontSize = 12.sp, color = PrimaryColor)
//                    )
//                }
//                Spacer(modifier = Modifier.height(6.dp))
//                LinearProgressIndicator(
//                    progress = { animatedProgress },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(8.dp)
//                        .clip(RoundedCornerShape(4.dp)),
//                    color = PrimaryColor,
//                    trackColor = Color(0xFFF1F5F9)
//                )
//            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                weeklyDays.forEach { weekDay ->
                    val day = weekDay.day
                    val isSelected = trafficTrendDayMatches(selectedDay, day)
                    val dayLabel = trafficTrendDayLabel(day, todayShortName)
                    val chipCount = weekDay.count
                    Surface(
                        onClick = { if (!isSelected) selectedDay = day },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) PrimaryColor else Color(0xFFF1F5F9),
                        border = if (!isSelected) BorderStroke(1.dp, Color(0xFFE8ECF4)) else null
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayLabel,
                                style = AppTextTheme.semiBold.copy(
                                    fontSize = 12.sp,
                                    color = if (isSelected) White else Gray
                                )
                            )
                            if (chipCount > 0) {
                                Text(
                                    text = "$chipCount",
                                    style = AppTextTheme.medium.copy(
                                        fontSize = 10.sp,
                                        color = if (isSelected) White.copy(alpha = 0.9f) else PrimaryColor
                                    )
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$dayTotal visits on $selectedDayLabel",
                    style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray)
                )
                if (peakCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(PrimaryColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = buildString {
                                append("Peak $peakCount")
                                peakSlotLabel?.let { append(" · $it") }
                            },
                            style = AppTextTheme.medium.copy(fontSize = 11.sp, color = PrimaryColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .clip(RoundedCornerShape(14.dp))
                    .background(chartBackground)
            ) {
                if (!hasHourlyData) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No traffic data for $selectedDayLabel",
                            style = AppTextTheme.medium.copy(fontSize = 13.sp, color = Gray),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                } else {
                    val density = LocalDensity.current
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val dash = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        listOf(0.25f, 0.5f, 0.75f).forEach { step ->
                            val y = size.height * (1f - step)
                            drawLine(
                                color = gridColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1f,
                                pathEffect = dash
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        hourlyBars.forEach { item ->
                            val isPeak = item.count > 0 && item.count == peakCount
                            val heightFraction = item.count.toFloat() / maxBarCount
                            val barAreaMaxPx = with(density) { 72.dp.toPx() }
                            val barHeight = max(4f, heightFraction * barAreaMaxPx)
                            TrafficTrendBar(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                barHeight = with(density) { barHeight.toDp() },
                                count = item.count,
                                hourSlot = parseTrafficHourSlot(item.label),
                                isPeak = isPeak
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrafficTrendBar(
    modifier: Modifier = Modifier,
    barHeight: Dp,
    count: Int,
    hourSlot: TrafficHourSlot,
    isPeak: Boolean,
) {
    Column(
        modifier = modifier.padding(horizontal = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$count",
                style = AppTextTheme.bold.copy(
                    fontSize = 9.sp,
                    color = when {
                        isPeak -> PrimaryColor
                        count > 0 -> Gray
                        else -> Gray.copy(alpha = 0.45f)
                    }
                ),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
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
                    .width(24.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                    .background(
                        if (isPeak) {
                            Brush.verticalGradient(
                                colors = listOf(PrimaryColor, PrimaryLightColor.copy(alpha = 0.85f))
                            )
                        } else {
                            Brush.verticalGradient(
                                colors = listOf(
                                    PrimaryLightColor.copy(alpha = 0.55f),
                                    PrimaryLightColor.copy(alpha = 0.25f)
                                )
                            )
                        }
                    )
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = hourSlot.start.ifBlank { "—" },
                style = AppTextTheme.semiBold.copy(
                    fontSize = 8.sp,
                    color = if (isPeak) PrimaryColor else Black
                ),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            if (!hourSlot.end.isNullOrBlank()) {
                Text(
                    text = hourSlot.end,
                    style = AppTextTheme.regular.copy(
                        fontSize = 7.sp,
                        color = if (isPeak) PrimaryColor.copy(alpha = 0.85f) else Gray
                    ),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            } else {
                Spacer(modifier = Modifier.height(9.dp))
            }
        }
    }
}

@Composable
fun StatsGrid(
    stats: DashboardStats?,
    onStatClick: (memberListFilter: String) -> Unit = {}
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Active",
                count = "${stats?.active_members ?: 0}",
                icon = Res.drawable.ic_active,
                tint = PrimaryDarkColor,
                onClick = { onStatClick("Active") }
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Inactive",
                count = "${stats?.inactive_members ?: 0}",
                icon = Res.drawable.ic_inactive,
                tint = Gray,
                onClick = { onStatClick("Inactive") }
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Expired",
                count = "${stats?.expired_members ?: 0}",
                icon = Res.drawable.ic_expired,
                tint = Color.Red,
                onClick = { onStatClick("Expired") }
            )
            StatCard(
                modifier = Modifier.weight(1f),
                title = "Total Members",
                count = "${stats?.total_members ?: 0}",
                icon = Res.drawable.ic_members,
                tint = Color(0xFF3F51B5),
                onClick = { onStatClick("All Member") }
            )
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier,
    title: String,
    count: String,
    icon: DrawableResource,
    tint: Color,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    )
    {
        Column(

            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Gray,
                    style = AppTextTheme.regular.copy(fontSize = 12.sp)
                )
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = count, style = AppTextTheme.bold.copy(fontSize = 18.sp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun GymManagementSection() {
    val navigator = LocalNavigator.current
    Column {
        Text(
            text = "Gym Management",
            style = AppTextTheme.bold.copy(fontSize = 18.sp),
            modifier = Modifier.padding(bottom = 12.dp)
        )
//        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
//            ManagementCard(
//                modifier = Modifier.weight(1f),
//                title = "Our Trainer",
//                icon = Res.drawable.ic_trainer,
//                onClick = { navigator?.push(TrainerScreen()) }
//            )
//            ManagementCard(
//                modifier = Modifier.weight(1f),
//                title = "Expenses",
//                icon = Res.drawable.ic_expenses,
//                onClick = { navigator?.push(ExpensesScreen()) }
//            )
//        }
//        Spacer(modifier = Modifier.height(16.dp))
//        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
//            ManagementCard(
//                modifier = Modifier.weight(1f),
//                title = "Leave Management",
//                icon = Res.drawable.ic_cale,
//                onClick = { navigator?.push(LeaveManagementScreen()) }
//            )
//            ManagementCard(
//                modifier = Modifier.weight(1f),
//                title = "Whatsapp Automation",
//                icon = Res.drawable.ic_message,
//                onClick = { navigator?.push(WhatsappAutomationScreen()) }
//            )
//        }
    }
}

@Composable
fun ManagementCard(modifier: Modifier, title: String, icon: DrawableResource, onClick: () -> Unit) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(PrimaryColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = PrimaryColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = AppTextTheme.bold.copy(fontSize = 14.sp, color = Black)
            )
        }
    }
}

@Composable
fun EnquiryOverview(enquiry: EnquiryStatsResponse?) {
    val navigator = LocalNavigator.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable { navigator?.push(EnquiryScreen()) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lead/Enquiry Summary",
                    style = AppTextTheme.bold.copy(fontSize = 18.sp),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Black,
                    modifier = Modifier.size(16.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EnquiryCard(
                    modifier = Modifier.weight(1f),
                    title = "Total Enquiry",
                    count = "${enquiry?.total ?: 0}",
                    color = Black,
                    cardColor = BlueLightColor
                )

                EnquiryCard(
                    modifier = Modifier.weight(1f),
                    title = "Converted",
                    count = "${enquiry?.converted ?: 0}",
                    color = PrimaryDarkColor,
                    cardColor = GreenLight2Color
                )

                EnquiryCard(
                    modifier = Modifier.weight(1f),
                    title = "Pending",
                    count = "${enquiry?.pending ?: 0}",
                    color = YellowColor,
                    cardColor = YellowLightColor
                )
            }
        }
    }
}

@Composable
fun EnquiryCard(modifier: Modifier, title: String, count: String, color: Color, cardColor: Color) {
    CustomContainer(
        modifier = modifier,
        backgroundColor = cardColor,
        shape = RoundedCornerShape(12.dp),
        padding = PaddingValues(12.dp)
    ) {
        Column {
            Text(text = title, color = Gray, style = AppTextTheme.regular.copy(fontSize = 12.sp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = count, color = color, style = AppTextTheme.bold.copy(fontSize = 16.sp))

        }
    }
}

private data class DashboardTabItem(
    val label: String,
    val icon: DrawableResource,
    val value: String
)

@Composable
private fun BottomNavigationBar(
    selectedItem: String,
    items: List<DashboardTabItem>,
    onItemSelected: (String) -> Unit
) {

    NavigationBar(
        containerColor = Color.White,
//        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        items.forEach { item ->
            val label = item.label
            val icon = item.icon
            val value = item.value

            val isSelected = selectedItem == value

            NavigationBarItem(
                selected = isSelected,
                modifier = Modifier.padding(0.dp),
                onClick = { onItemSelected(value) },

                icon = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Icon(
                            painter = painterResource(icon),
                            contentDescription = label,
                            modifier = Modifier.size(24.dp),
                            tint = if (isSelected) PrimaryDarkColor else Gray
                        )

                    }
                },

                label = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) PrimaryDarkColor else Gray,
                            style = AppTextTheme.medium.copy(fontSize = 12.sp)
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .height(6.dp)
                                    .width(60.dp)
                                    .background(
                                        PrimaryColor,
                                        shape = RoundedCornerShape(
                                            topStart = 50.dp,
                                            topEnd = 50.dp
                                        ),
                                    )
                            )
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                },

                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}
