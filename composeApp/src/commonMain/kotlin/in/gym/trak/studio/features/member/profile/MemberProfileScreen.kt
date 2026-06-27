package `in`.gym.trak.studio.features.member

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.BorderStroke

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.rememberAsyncImagePainter
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.CommonOutlineButton
import `in`.gym.trak.studio.components.ConfirmationDialog
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.components.ManagementCard
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.features.auth.LoginScreen
import `in`.gym.trak.studio.features.dashboard.OwnerDashboardScreen
import `in`.gym.trak.studio.features.dashboard.LeaderboardScreen
import `in`.gym.trak.studio.features.dashboard.BroadcastChannelsScreen
import `in`.gym.trak.studio.features.members.MemberAttendanceScreen
import `in`.gym.trak.studio.features.members.MemberDetailScreen
import `in`.gym.trak.studio.features.members.MemberPaymentHistoryScreen
import `in`.gym.trak.studio.features.member.profile.MemberProfileSubscriptionsScreen
import `in`.gym.trak.studio.features.member.workout.MemberWorkoutHistoryScreen
import `in`.gym.trak.studio.features.members.MemberSubscriptionScreen
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.theme.YellowColor
import `in`.gym.trak.studio.viewmodel.member.MemberProfileScreenModel
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.gym_boy
import gym.composeapp.generated.resources.ic_atm
import gym.composeapp.generated.resources.ic_attendance
import gym.composeapp.generated.resources.ic_broadcast
import gym.composeapp.generated.resources.ic_delete_outline
import gym.composeapp.generated.resources.ic_diet
import gym.composeapp.generated.resources.ic_gold_pro
import gym.composeapp.generated.resources.ic_gym_subscription
import gym.composeapp.generated.resources.ic_leaderboard
import gym.composeapp.generated.resources.ic_logout
import gym.composeapp.generated.resources.ic_my_shop
import gym.composeapp.generated.resources.ic_physics_metrics
import gym.composeapp.generated.resources.ic_wallet_filled
import gym.composeapp.generated.resources.ic_workout
import `in`.gym.trak.studio.data.model.MemberProfileDetailResponse
import `in`.gym.trak.studio.data.model.activeSubscriptions
import `in`.gym.trak.studio.data.model.contractTotalAmount
import `in`.gym.trak.studio.data.model.primarySubscription
import `in`.gym.trak.studio.data.model.resolvedAmountPaid
import `in`.gym.trak.studio.data.model.resolvedAmountPending
import `in`.gym.trak.studio.base.Constants
import `in`.gym.trak.studio.features.shop.ShopScreen
import `in`.gym.trak.studio.utils.DateUtils
import org.jetbrains.compose.resources.painterResource

/**
 * Member profile tab backed by /members/{memberId}.
 */
class MemberProfileScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { MemberProfileScreenModel() }
        val profile by screenModel.memberDetail.collectAsState()
        val isRefreshing by screenModel.isRefreshing.collectAsState()
        var showLogoutDialog by remember { mutableStateOf(false) }
        var showDeleteDialog by remember { mutableStateOf(false) }

        if (showLogoutDialog) {
            ConfirmationDialog(
                onDismissRequest = { showLogoutDialog = false },
                onConfirm = {
                    showLogoutDialog = false
                    screenModel.logout {
                        navigator?.replaceAll(LoginScreen())
                    }
                },
                title = "Log Out",
                message = "Are you sure you want to log out of your account?",
                confirmText = "Log Out"
            )
        }

        if (showDeleteDialog) {
            ConfirmationDialog(
                onDismissRequest = { showDeleteDialog = false },
                onConfirm = {
                    SessionManager.clearSession()
                    navigator?.replaceAll(LoginScreen())
                },
                title = "Delete Account",
                message = "Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently removed.",
                confirmText = "Delete",
                isDangerAction = true
            )
        }

        LaunchedEffect(Unit) {
            if (profile == null) {
                screenModel.loadProfile(showFullLoader = true)
            }
        }

        LoadingScreenHandler(screenModel = screenModel) {
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { screenModel.refresh() },
                state = rememberPullToRefreshState(),
                indicator = {}
            ) {
                MemberProfileContent(
                    profile = profile,
                    onCollectPaymentClick = {
                        val id = SessionManager.effectiveMemberListingIdForApi(profile?.id.orEmpty())
                        if (id.isNotBlank()) {
                            navigator?.push(
                                MemberDetailScreen(
                                    memberId = id,
                                    initialTab = "Subscriptions",
                                    initialShowReceivePayment = true,
                                ),
                            )
                        }
                    },
                    onEditProfileClick = {
                        navigator?.push(
                            EditMemberProfileScreen(
                                initialProfile = profile,
                                onUpdateSuccess = { screenModel.refresh() },
                            ),
                        )
                    },
                    onSwitchRoleClick = {
                        screenModel.switchToOwner {
                            navigator?.replaceAll(OwnerDashboardScreen())
                        }
                    },
                    onLogoutClick = { showLogoutDialog = true },
                    onDeleteClick = { showDeleteDialog = true },
                    onPhysicalMetricsClick = {
                        navigator?.push(
                            PhysicalMetricsScreen(
                                onPhysicalMetricsUpdated = { screenModel.refresh() },
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun MemberProfileContent(
    profile: MemberProfileDetailResponse?,
    onCollectPaymentClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onSwitchRoleClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onPhysicalMetricsClick: () -> Unit,
) {
    val navigator = LocalNavigator.current
    val avatarUrl = profile?.profile_image
    val memberName = profile?.name ?: "Member"
    val gymName = profile?.gym?.name?.takeIf { it.isNotBlank() }
    val joinFormatted = DateUtils.formatBirthDateForDisplay(profile?.join_date)
    val joinedLabel =
        if (joinFormatted.isNotBlank()) "Member Since $joinFormatted" else "Member"
    val profileSubtitle = buildList {
        gymName?.let { add(it) }
        add(joinedLabel)
    }.joinToString("  •  ")

    AppScrollableScreen(
        contentPadding = PaddingValues(0.dp),
    ) {
        StaggeredEntranceItem(index = 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                CommonCard(
                    backgroundColor = Color.Transparent,
                    elevation = 0.dp,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(PrimaryColor)
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                            Image(
                                painter = if (!avatarUrl.isNullOrBlank()) {
                                    rememberAsyncImagePainter(avatarUrl)
                                } else {
                                    painterResource(Res.drawable.gym_boy)
                                },
                                contentDescription = "Profile Pic",
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(White),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    memberName,
                                    style = AppTextTheme.semiBold.copy(fontSize = 18.sp, color = White)
                                )
                                Text(
                                    profileSubtitle,
                                    style = AppTextTheme.regular.copy(
                                        fontSize = 12.sp,
                                        color = White.copy(alpha = 0.7f)
                                    )
                                )
                                profile?.status?.takeIf { it.isNotBlank() }?.let { status ->
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(100.dp),
                                        color = White.copy(alpha = 0.2f),
                                    ) {
                                        Text(
                                            text = status.replaceFirstChar { it.uppercase() },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = AppTextTheme.medium.copy(
                                                fontSize = 10.sp,
                                                color = White,
                                            ),
                                        )
                                    }
                                }
                            }
                            Column(
                                horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Surface(
                                    onClick = onEditProfileClick,
                                    shape = RoundedCornerShape(100.dp),
                                    color = White.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, White.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit profile",
                                            tint = White,
                                            modifier = Modifier.size(14.dp),
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Edit",
                                            style = AppTextTheme.medium.copy(
                                                fontSize = 12.sp,
                                                color = White
                                            ),
                                        )
                                    }
                                }

                                if(!SessionManager.gymId.isBlank())
                                Surface(
                                    onClick = onSwitchRoleClick,
                                    shape = RoundedCornerShape(100.dp),
                                    color = White.copy(alpha = 0.2f),
                                    border = BorderStroke(1.dp, White.copy(alpha = 0.3f))
                                ) {
                                    Text(
                                        text = "Switch to Owner",
                                        style = AppTextTheme.medium.copy(
                                            fontSize = 12.sp,
                                            color = White
                                        ),
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                            Spacer(modifier = Modifier.height(22.dp))
                            Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatItem(
                                value = profile?.stats?.active_subscription?.toString() ?: "0",
                                label = "Active"
                            )
                            VerticalDivider(
                                modifier = Modifier.height(30.dp),
                                color = White.copy(alpha = 0.3f)
                            )
                            StatItem(
                                value = profile?.stats?.pending_payment?.toString() ?: "0",
                                label = "Pending"
                            )
                            VerticalDivider(
                                modifier = Modifier.height(30.dp),
                                color = White.copy(alpha = 0.3f)
                            )
                            StatItem(
                                value = profile?.stats?.overdue?.toString() ?: "0",
                                label = "Overdue"
                            )
                            }
                        }
                    }
                }
            }
        }

        StaggeredEntranceItem(index = 1) {
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                MembershipBanner(
                    profile = profile,
                    onClick = {
                        navigator?.push(
                            MemberProfileSubscriptionsScreen(
                                currentSubscriptions = profile?.activeSubscriptions().orEmpty(),
                                upcomingSubscriptions = profile?.upcoming_subscriptions.orEmpty(),
                                expiredSubscriptions = profile?.expired_subscriptions.orEmpty(),
                                pastSubscriptions = profile?.past_subscriptions.orEmpty(),
                                freezeSubscriptions = profile?.freeze_subscriptions.orEmpty(),
                            ),
                        )
                    },
                )
                val primarySub = profile?.primarySubscription()
                val showCollectPayment =
                    (profile?.stats?.pending_payment ?: 0) > 0 ||
                        (primarySub?.resolvedAmountPending() ?: 0) > 0
//                if (showCollectPayment) {
//                    Spacer(modifier = Modifier.height(16.dp))
//                    CommonButton(
//                        text = "Collect Payment",
//                        modifier = Modifier.fillMaxWidth(),
//                        leftIcon = painterResource(Res.drawable.ic_wallet_filled),
//                        onClick = onCollectPaymentClick,
//                    )
//                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("general Information", style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Black))
                Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ManagementCard(
                    icon = Res.drawable.ic_attendance,
                    title = "Attendance",
                    subtitle = "",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val id = SessionManager.effectiveMemberListingIdForApi(profile?.id.orEmpty())
                        if (id.isNotBlank()) {
                            navigator?.push(MemberAttendanceScreen(memberId = id))
                        }
                    }
                )
                ManagementCard(
                    icon = Res.drawable.ic_atm,
                    title = "Payment",
                    subtitle = "",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val id = SessionManager.effectiveMemberListingIdForApi(profile?.id.orEmpty())
                        if (id.isNotBlank()) {
                            navigator?.push(MemberPaymentHistoryScreen(memberId = id))
                        }
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ManagementCard(
                    icon = Res.drawable.ic_leaderboard,
                    title = "Leaderboard",
                    subtitle = "",
                    modifier = Modifier.weight(1f),
                    onClick = { navigator?.push(LeaderboardScreen()) }
                )
                ManagementCard(
                    icon = Res.drawable.ic_workout,
                    title = "Workout History",
                    subtitle = "",
                    modifier = Modifier.weight(1f),
                    onClick = { navigator?.push(MemberWorkoutHistoryScreen()) }
                )

//                ManagementCard(
//                    icon = Res.drawable.ic_gym_subscription,
//                    title = "Subscription",
//                    subtitle = "",
//                    modifier = Modifier.weight(1f),
//                    onClick = { navigator?.push(MemberSubscriptionScreen()) }
//                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ManagementCard(
                    icon = Res.drawable.ic_diet,
                    title = "Diet History",
                    subtitle = "",
                    modifier = Modifier.weight(1f),
                    onClick = { navigator?.push(DietHistoryScreen()) }
                )
                ManagementCard(
                    icon = Res.drawable.ic_my_shop,
                    title = "Gym Shop",
                    subtitle = "",
                    modifier = Modifier.weight(1f),
                    onClick = {
                        navigator?.push(ShopScreen(fromMemberProfile = true))
                    }
                )

            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ManagementCard(
                    icon = Res.drawable.ic_broadcast,
                    title = "Broadcast",
                    subtitle = "",
                    modifier = Modifier.weight(1f),
                    onClick = { navigator?.push(BroadcastChannelsScreen(isReadOnly = true)) }
                )
                ManagementCard(
                    icon = Res.drawable.ic_physics_metrics,
                    title = "Physical Metrics",
                    subtitle = "",
                    modifier = Modifier.weight(1f),
                    onClick = onPhysicalMetricsClick
                )

            }

//            Row(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(vertical = 4.dp),
//                horizontalArrangement = Arrangement.spacedBy(8.dp)
//            ) {
//                Spacer(modifier = Modifier.weight(1f))
//            }

                Spacer(modifier = Modifier.height(40.dp))

                CommonButton(
                    text = "Delete Account",
                    onClick = onDeleteClick,
                    color = Color(0xFFEF4444),
                    leftIcon = painterResource(Res.drawable.ic_delete_outline)
                )
                Spacer(modifier = Modifier.height(16.dp))
                CommonOutlineButton(
                    text = "Log Out",
                    onClick = onLogoutClick,
                    borderColor = Color(0xFFE2E8F0),
                    textColor = Color(0xFFEF4444),
                    leftIcon = painterResource(Res.drawable.ic_logout)
                )
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun MembershipBanner(
    profile: MemberProfileDetailResponse?,
    onClick: () -> Unit,
) {
    val activeSubscriptions = profile?.activeSubscriptions().orEmpty()
    val current = profile?.primarySubscription()
    val planName = when {
        activeSubscriptions.size > 1 -> "${activeSubscriptions.size} Active Plans"
        current?.plan_name?.isNotBlank() == true -> current.plan_name
        else -> "No Active Plan"
    }
    val expiryFormatted = current?.expiry_date?.takeIf { it.isNotBlank() }?.let {
        DateUtils.formatBirthDateForDisplay(it)
    }.orEmpty()
    val expiry = expiryFormatted.ifBlank { "--" }
    val remainingDays = current?.remaining_days ?: 0
    val paid = current?.resolvedAmountPaid() ?: 0
    val pending = current?.resolvedAmountPending() ?: 0
    val contractTotal = current?.contractTotalAmount() ?: 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(YellowColor.copy(alpha = 0.2f), shape = RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(Res.drawable.ic_gold_pro),
                null,
                tint = Color.Unspecified,
                modifier = Modifier.size(44.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(planName, style = AppTextTheme.medium.copy(fontSize = 14.sp))
                val expiryLine = buildString {
                    append("Expires $expiry")
                    if (remainingDays > 0) append("  •  $remainingDays days left")
                }
                Text(
                    expiryLine,
                    style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Black.copy(alpha = 0.6f))
                )
                if (current != null) {
                    Text(
                        "${Constants.RUPEE} $contractTotal  •  Paid $paid  •  Pending $pending",
                        style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Black.copy(alpha = 0.6f))
                    )
                }
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Gray)
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color = White) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = AppTextTheme.bold.copy(fontSize = 18.sp, color = color))
        Text(
            label,
            style = AppTextTheme.regular.copy(fontSize = 12.sp, color = color.copy(alpha = 0.7f))
        )
    }
}
