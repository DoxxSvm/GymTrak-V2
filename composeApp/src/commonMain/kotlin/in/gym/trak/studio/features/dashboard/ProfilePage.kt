package `in`.gym.trak.studio.features.dashboard

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.rememberAsyncImagePainter
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonOutlineButton
import `in`.gym.trak.studio.components.ConfirmationDialog
import `in`.gym.trak.studio.components.ManagementCard
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import `in`.gym.trak.studio.data.model.GymStaffListRole
import `in`.gym.trak.studio.data.model.TrainerDetailResponse
import `in`.gym.trak.studio.data.model.TrainerProfileDetailDTO
import `in`.gym.trak.studio.data.model.TrainerUserDetailDTO
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.data.repository.SessionManager.hasPermission
import `in`.gym.trak.studio.features.auth.LoginScreen
import `in`.gym.trak.studio.features.expenses.ExpensesScreen
import `in`.gym.trak.studio.features.management.GymSubscriptionScreen
import `in`.gym.trak.studio.features.management.LeaveManagementScreen
import `in`.gym.trak.studio.features.member.MemberDashboardScreen
import `in`.gym.trak.studio.features.plans.PlanListScreen
import `in`.gym.trak.studio.features.shop.ShopScreen
import `in`.gym.trak.studio.features.trainers.EditTrainerScreen
import `in`.gym.trak.studio.features.trainers.TrainerScreen
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.PrimaryDarkColor
import `in`.gym.trak.studio.theme.RedColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.gym_boy
import gym.composeapp.generated.resources.ic_delete
import gym.composeapp.generated.resources.ic_expenses
import gym.composeapp.generated.resources.ic_gym_subscription
import gym.composeapp.generated.resources.ic_leaderboard
import gym.composeapp.generated.resources.ic_leave_managment
import gym.composeapp.generated.resources.ic_logout
import gym.composeapp.generated.resources.ic_my_shop
import gym.composeapp.generated.resources.ic_plans
import gym.composeapp.generated.resources.ic_profile
import gym.composeapp.generated.resources.ic_qr_scanner
import gym.composeapp.generated.resources.ic_trainer
import gym.composeapp.generated.resources.ic_broadcast
import gym.composeapp.generated.resources.ic_staff
import gym.composeapp.generated.resources.ic_whatsapp
import gym.composeapp.generated.resources.ic_workout
import `in`.gym.trak.studio.features.auth.OnboardingScreen
import `in`.gym.trak.studio.features.management.WhatsappAutomationScreen
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Content for the Profile tab in the Owner Dashboard.
 * Displays profile information, management grid, and account actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePage(screenModel: OwnerDashboardScreenModel) {
    val navigator = LocalNavigator.currentOrThrow
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val managementList = listOf(
        if (hasPermission(SessionManager.PermissionKeys.KEY_TRAINER_READ))
            ManagementItem(
                title = "Trainers",
                subtitle = "Manage Profiles",
                icon = Res.drawable.ic_workout,
                onClick = { navigator.push(TrainerScreen(listRole = GymStaffListRole.TRAINER)) }
            )
        else null,
        if (hasPermission(SessionManager.PermissionKeys.KEY_TRAINER_READ))
            ManagementItem(
                title = "Staff",
                subtitle = "Manage Staff",
                icon = Res.drawable.ic_staff,
                onClick = { navigator.push(TrainerScreen(listRole = GymStaffListRole.STAFF)) }
            )
        else null,
        if (hasPermission(SessionManager.PermissionKeys.KEY_GYM_READ))
            ManagementItem(
                title = "My Gym",
                subtitle = "Your gyms",
                icon = Res.drawable.ic_gym_subscription,
                onClick = { navigator.push(MyGymsScreen(sharedDashboardScreenModel = screenModel)) }
            )
        else null,
        if (hasPermission(SessionManager.PermissionKeys.KEY_EXPENSE_READ))

            ManagementItem(
                title = "Expenses",
                subtitle = "Track Spending",
                icon = Res.drawable.ic_expenses,
                onClick = { navigator.push(ExpensesScreen()) }
            )
        else null,

        if (hasPermission(SessionManager.PermissionKeys.KEY_PLAN_READ))
            ManagementItem(
                title = "Plans",
                subtitle = "Gym Packages",
                icon = Res.drawable.ic_plans,
                onClick = { navigator.push(PlanListScreen()) }
            )
        else null,

        if (hasPermission(SessionManager.PermissionKeys.KEY_SUBSCRIPTION_READ))
            ManagementItem(
                title = "GymTrak Subscriptions",
                subtitle = "Member Status",
                icon = Res.drawable.ic_gym_subscription,
                onClick = { navigator.push(GymSubscriptionScreen()) }
            )
        else null,

        if (hasPermission(SessionManager.PermissionKeys.KEY_QR_VIEW))
            ManagementItem(
                title = "QR Scanner",
                subtitle = "Scan Entry",
                icon = Res.drawable.ic_qr_scanner,
                onClick = { navigator.push(AttendanceScannerScreen()) }
            )
        else null,

        if (hasPermission(SessionManager.PermissionKeys.KEY_LEAVE_READ))
            ManagementItem(
                title = "Leave Management",
                subtitle = "Approve Request",
                icon = Res.drawable.ic_leave_managment,
                onClick = { navigator.push(LeaveManagementScreen()) }
            )
        else null,

        if (hasPermission(SessionManager.PermissionKeys.KEY_BROADCAST_WHATSAPP))
            ManagementItem(
                title = "Broadcast",
                subtitle = "Channel Messaging",
                icon = Res.drawable.ic_broadcast,
                onClick = { navigator.push(BroadcastChannelsScreen()) }
            )
        else null,

        if (hasPermission(SessionManager.PermissionKeys.KEY_PRODUCT_READ))
            ManagementItem(
                title = "My Shop",
                subtitle = "Gym Product Shop",
                icon = Res.drawable.ic_my_shop,
                onClick = { navigator.push(ShopScreen()) }
            )
        else null,

        if (hasPermission(SessionManager.PermissionKeys.KEY_LEAD_READ))

            ManagementItem(
                title = "Leaderboard",
                subtitle = "Gym People Score",
                icon = Res.drawable.ic_leaderboard,
                onClick = { navigator.push(LeaderboardScreen()) }
            )
        else null,

        ManagementItem(
            title = "Whatsapp Automation",
            subtitle = "Automation",
            icon = Res.drawable.ic_whatsapp,
            onClick = { navigator.push(WhatsappAutomationScreen()) }

        )
    )
    if (showLogoutDialog) {
        ConfirmationDialog(
            onDismissRequest = { showLogoutDialog = false },
            onConfirm = {
                screenModel.logout {
                    navigator.replaceAll(LoginScreen())
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
                screenModel.deleteAccount {
                    navigator.replaceAll(LoginScreen())
                }
            },
            title = "Delete Account",
            message = "Are you sure you want to delete your account? This action cannot be undone and all your data will be permanently removed.",
            confirmText = "Delete",
            isDangerAction = true
        )
    }

    val profile by screenModel.profileData.collectAsState()
    val isLoading by screenModel.isLoading.collectAsState()
    val isUploadingImage by screenModel.isUploadingImage.collectAsState()
    val imageUploadTarget by screenModel.activeImageUploadTarget.collectAsState()
    val showProfilePhotoUploadOverlay =
        isUploadingImage && imageUploadTarget == OwnerDashboardScreenModel.ActiveImageUploadTarget.ProfilePhoto
    var isPullRefreshRequested by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (profile == null) {
            screenModel.loadProfile()
        }
        screenModel.loadUserGyms()
    }
    LaunchedEffect(isLoading) {
        if (!isLoading) isPullRefreshRequested = false
    }

    PullToRefreshBox(
        isRefreshing = isPullRefreshRequested && isLoading,
        onRefresh = {
            isPullRefreshRequested = true
            screenModel.loadProfile()
            screenModel.loadUserGyms()
        },
        state = rememberPullToRefreshState(),
        indicator = {}
    ) {

        val visible = remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            visible.value = true
        }

        AppScrollableScreen(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StaggeredEntranceItem(index = 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profile",
                        style = AppTextTheme.bold.copy(fontSize = 20.sp)
                    )
                    Surface(
                        onClick = {
                            screenModel.switchToMember(
                                onSuccess = {
                                    navigator.replaceAll(MemberDashboardScreen())
                                },
                                onNavigateToOnboarding = {
                                    navigator.push(
                                        OnboardingScreen(
                                            isOwner = false,
                                            isSwitchingFromOwner = true,
                                            initialName = profile?.personalInfo?.fullName
                                        )
                                    )
                                }
                            )
                        },
                        shape = RoundedCornerShape(100.dp),
                        color = Color(0xFFE7F7F2),
                        border = BorderStroke(1.dp, PrimaryColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "Switch to Member",
                            style = AppTextTheme.medium.copy(
                                fontSize = 12.sp,
                                color = PrimaryColor
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Profile Avatar and Name
            StaggeredEntranceItem(index = 1) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(White)
                            .border(2.dp, PrimaryColor.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        val profileImage = profile?.personalInfo?.profileImage
                        val painter = if (!profileImage.isNullOrEmpty())
                            rememberAsyncImagePainter(profileImage)
                        else
                            painterResource(Res.drawable.gym_boy)

                        Image(
                            painter = painter,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(92.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                        if (showProfilePhotoUploadOverlay) {
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(CircleShape)
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = White,
                                    strokeWidth = 2.dp
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = profile?.personalInfo?.fullName ?: "",
                        style = AppTextTheme.bold.copy(fontSize = 18.sp),
                        color = Black
                    )

                    val gymName = profile?.gymDetails?.gymName
                    if (!gymName.isNullOrEmpty()) {
                        Text(
                            text = gymName,
                            style = AppTextTheme.medium.copy(fontSize = 14.sp),
                            color = Gray
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            // Edit Profile Button
            StaggeredEntranceItem(index = 2) {
                Surface(
                    onClick = {
                        val userRole = SessionManager.userRole
                        print("user role ==>${userRole}")
                        if (userRole.equals("trainer", ignoreCase = true)) {
                            profile?.let { data ->
                                // Construct an initialData object from ProfileResponse for EditTrainerScreen
                                val initialData = TrainerDetailResponse(
                                    gymUserId = data.id ?: "",
                                    user = TrainerUserDetailDTO(
                                        fullName = data.personalInfo?.fullName ?: "",
                                        avatarUrl = data.personalInfo?.profileImage
                                    ),
                                    profile = TrainerProfileDetailDTO(
                                        dateOfBirth = data.personalInfo?.dateOfBirth,
                                        gender = data.personalInfo?.gender ?: "male",
                                        address = data.personalInfo?.address,
                                        experience = data.trainerDetails?.experience,
                                        salaryCents = data.trainerDetails?.salary ?: 0,
                                        salaryPeriod = data.trainerDetails?.salaryDuration
                                    ),
                                    expertise = data.trainerDetails?.expertise ?: emptyList()
                                )
                                navigator.push(
                                    EditTrainerScreen(
                                        trainerId = data.id ?: "",
                                        initialData = initialData,
                                        onRefresh = { screenModel.loadProfile() },
                                        isSelfEdit = true
                                    )
                                )
                            }
                        } else {
                            navigator.push(
                                EditProfileScreen(
                                    sharedDashboardScreenModel = screenModel,
                                    onUpdateSuccess = { screenModel.loadProfile() }
                                )
                            )
                        }
                    },
                    modifier = Modifier.height(44.dp),
                    shape = RoundedCornerShape(100.dp),
                    color = Color(0xFFE7F7F2),
                    border = BorderStroke(
                        1.dp,
                        PrimaryColor.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_profile),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = PrimaryColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit profile",
                            style = AppTextTheme.bold.copy(fontSize = 15.sp, color = Black)
                        )
                    }
                }
            }

//                Spacer(modifier = Modifier.height(24.dp))
//
//                val userGyms by screenModel.userGyms.collectAsState()
//                val userGymsLoading by screenModel.userGymsLoading.collectAsState()
//                val currentGymId = SessionManager.gymId
//
//                StaggeredEntranceItem(index = 3) {
//                    Column(modifier = Modifier.fillMaxWidth()) {
//                        Row(
//                            modifier = Modifier.fillMaxWidth(),
//                            horizontalArrangement = Arrangement.SpaceBetween,
//                            verticalAlignment = Alignment.CenterVertically
//                        ) {
//                            Column(modifier = Modifier.weight(1f)) {
//                                Text(
//                                    text = "My gyms",
//                                    style = AppTextTheme.bold.copy(fontSize = 18.sp),
//                                    color = Black
//                                )
//                                Text(
//                                    text = "Gyms linked to your account",
//                                    style = AppTextTheme.medium.copy(
//                                        fontSize = 12.sp,
//                                        color = PrimaryDarkColor
//                                    )
//                                )
//                            }
//                            if (!SessionManager.userRole.equals("trainer", ignoreCase = true)) {
//                                Surface(
//                                    onClick = {
//                                        navigator.push(
//                                            AddGymScreen(sharedDashboardScreenModel = screenModel)
//                                        )
//                                    },
//                                    shape = RoundedCornerShape(100.dp),
//                                    color = Color(0xFFE7F7F2),
//                                    border = BorderStroke(1.dp, PrimaryColor.copy(alpha = 0.3f))
//                                ) {
//                                    Text(
//                                        text = "Add gym",
//                                        style = AppTextTheme.medium.copy(
//                                            fontSize = 12.sp,
//                                            color = PrimaryColor
//                                        ),
//                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
//                                    )
//                                }
//                            }
//                        }
//
//                        Spacer(modifier = Modifier.height(12.dp))
//
//                        if (userGymsLoading) {
//                            LinearProgressIndicator(
//                                modifier = Modifier.fillMaxWidth(),
//                                color = PrimaryColor
//                            )
//                            Spacer(modifier = Modifier.height(8.dp))
//                        }
//
//                        if (!userGymsLoading && userGyms.isEmpty()) {
//                            Text(
//                                text = "No gyms found for your account.",
//                                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray),
//                                modifier = Modifier.padding(vertical = 8.dp)
//                            )
//                        }
//
//                        userGyms.forEach { gym ->
//                            val isCurrent = gym.id.isNotEmpty() && gym.id == currentGymId
//                            Surface(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(vertical = 4.dp),
//                                shape = RoundedCornerShape(12.dp),
//                                color = White,
//                                border = BorderStroke(1.dp, PrimaryColor.copy(alpha = 0.12f))
//                            ) {
//                                Row(
//                                    modifier = Modifier.padding(12.dp),
//                                    verticalAlignment = Alignment.CenterVertically
//                                ) {
//                                    Box(
//                                        modifier = Modifier
//                                            .size(48.dp)
//                                            .clip(RoundedCornerShape(8.dp))
//                                            .background(Color(0xFFF3F4F6)),
//                                        contentAlignment = Alignment.Center
//                                    ) {
//                                        val thumb = gym.logoUrl
//                                        if (!thumb.isNullOrBlank()) {
//                                            Image(
//                                                painter = rememberAsyncImagePainter(thumb),
//                                                contentDescription = null,
//                                                modifier = Modifier.fillMaxSize(),
//                                                contentScale = ContentScale.Crop
//                                            )
//                                        } else {
//                                            Icon(
//                                                painter = painterResource(Res.drawable.ic_gym_subscription),
//                                                contentDescription = null,
//                                                tint = PrimaryColor,
//                                                modifier = Modifier.size(24.dp)
//                                            )
//                                        }
//                                    }
//                                    Spacer(modifier = Modifier.width(12.dp))
//                                    Column(modifier = Modifier.weight(1f)) {
//                                        Row(verticalAlignment = Alignment.CenterVertically) {
//                                            Text(
//                                                text = gym.name.ifBlank { "Gym" },
//                                                style = AppTextTheme.bold.copy(fontSize = 15.sp, color = Black),
//                                                maxLines = 1
//                                            )
//                                            if (isCurrent) {
//                                                Spacer(modifier = Modifier.width(8.dp))
//                                                Surface(
//                                                    shape = RoundedCornerShape(100.dp),
//                                                    color = PrimaryColor.copy(alpha = 0.12f)
//                                                ) {
//                                                    Text(
//                                                        text = "Active",
//                                                        style = AppTextTheme.medium.copy(
//                                                            fontSize = 10.sp,
//                                                            color = PrimaryColor
//                                                        ),
//                                                        modifier = Modifier.padding(
//                                                            horizontal = 8.dp,
//                                                            vertical = 2.dp
//                                                        )
//                                                    )
//                                                }
//                                            }
//                                        }
//                                        val addr = gym.address
//                                        if (!addr.isNullOrBlank()) {
//                                            Text(
//                                                text = addr,
//                                                style = AppTextTheme.medium.copy(
//                                                    fontSize = 12.sp,
//                                                    color = Gray
//                                                ),
//                                                maxLines = 2
//                                            )
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }

            Spacer(modifier = Modifier.height(32.dp))

            // Management Grid Header
            StaggeredEntranceItem(index = 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Management",
                        style = AppTextTheme.bold.copy(fontSize = 18.sp),
                        color = Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "(Quick Access)",
                        style = AppTextTheme.medium.copy(
                            fontSize = 12.sp,
                            color = PrimaryDarkColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Grid
//            Column(
//                modifier = Modifier.fillMaxWidth()
//            ) {
//                // Row 1
////                StaggeredEntranceItem(index = 4) {
////                    Row(
////                        modifier = Modifier
////                            .fillMaxWidth()
////                            .padding(vertical = 4.dp),
////                        horizontalArrangement = Arrangement.spacedBy(8.dp)
////                    ) {
////                        ManagementCard(
////                            icon = Res.drawable.ic_trainer,
////                            title = "Trainers",
////                            subtitle = "Manage Profiles",
////                            modifier = Modifier.weight(1f),
////                            onClick = { navigator.push(TrainerScreen()) }
////                        )
////                        if (hasPermission(SessionManager.PermissionKeys.KEY_TRAINER_READ))
////                        ManagementCard(
////                            icon = Res.drawable.ic_expenses,
////                            title = "Expenses",
////                            subtitle = "Track Spending",
////                            modifier = Modifier.weight(1f),
////                            onClick = { navigator.push(ExpensesScreen()) }
////                        )
////                    }
////                }
////
////                // Row 2
////                StaggeredEntranceItem(index = 5) {
////                    Row(
////                        modifier = Modifier
////                            .fillMaxWidth()
////                            .padding(vertical = 4.dp),
////                        horizontalArrangement = Arrangement.spacedBy(8.dp)
////                    ) {
////                        ManagementCard(
////                            icon = Res.drawable.ic_plans,
////                            title = "Plans",
////                            subtitle = "Gym Packages",
////                            modifier = Modifier.weight(1f),
////                            onClick = { navigator.push(PlanListScreen()) }
////                        )
////                        ManagementCard(
////                            icon = Res.drawable.ic_gym_subscription,
////                            title = "GymTrak Subscriptions",
////                            subtitle = "Member Status",
////                            modifier = Modifier.weight(1f),
////                            onClick = { navigator.push(GymSubscriptionScreen()) }
////                        )
////                    }
////                }
////
////                // Row 3
////                StaggeredEntranceItem(index = 6) {
////                    Row(
////                        modifier = Modifier
////                            .fillMaxWidth()
////                            .padding(vertical = 4.dp),
////                        horizontalArrangement = Arrangement.spacedBy(8.dp)
////                    ) {
////                        ManagementCard(
////                            icon = Res.drawable.ic_qr_scanner,
////                            title = "QR Scanner",
////                            subtitle = "Scan Entry",
////                            modifier = Modifier.weight(1f),
////                            onClick = { navigator.push(AttendanceScannerScreen()) }
////                        )
////                        ManagementCard(
////                            icon = Res.drawable.ic_leave_managment,
////                            title = "Leave Management",
////                            subtitle = "Approve Request",
////                            modifier = Modifier.weight(1f),
////                            onClick = { navigator.push(LeaveManagementScreen()) }
////                        )
////                    }
////                }
////
////                // Row 4
////                StaggeredEntranceItem(index = 7) {
////                    Row(
////                        modifier = Modifier
////                            .fillMaxWidth()
////                            .padding(vertical = 4.dp),
////                        horizontalArrangement = Arrangement.spacedBy(8.dp)
////                    ) {
////                        ManagementCard(
////                            icon = Res.drawable.ic_whatsapp,
////                            title = "Whatsapp Automation",
////                            subtitle = "Automation",
////                            modifier = Modifier.weight(1f),
////                            onClick = { navigator.push(WhatsappAutomationScreen()) }
////                        )
////                        ManagementCard(
////                            icon = Res.drawable.ic_my_shop,
////                            title = "My Shop",
////                            subtitle = "Gym Product Shop",
////                            modifier = Modifier.weight(1f),
////                            onClick = { navigator.push(ShopScreen()) }
////                        )
////                    }
////                }
////
////                // Row 5
////                StaggeredEntranceItem(index = 8) {
////                    Row(
////                        modifier = Modifier
////                            .fillMaxWidth()
////                            .padding(vertical = 4.dp),
////                        horizontalArrangement = Arrangement.spacedBy(8.dp)
////                    ) {
////                        ManagementCard(
////                            icon = Res.drawable.ic_leaderboard,
////                            title = "Leaderboard",
////                            subtitle = "Gym People Score",
////                            modifier = Modifier.weight(1f),
////                            onClick = { navigator.push(LeaderboardScreen()) }
////                        )
////                        Spacer(modifier = Modifier.weight(1f))
////                    }
////                }
//            }
            Column {
                managementList.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        rowItems.forEach { item ->
                            item?.let {
                                ManagementCard(
                                    icon = it.icon,
                                    title = item.title,
                                    subtitle = item.subtitle,
                                    modifier = Modifier.weight(1f),
                                    onClick = item.onClick
                                )
                            }
                        }

                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            // Account buttons
            StaggeredEntranceItem(index = 9) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    CommonButton(
                        onClick = { showDeleteDialog = true },
                        text = "Delete Account",
                        color = Color(0xFFE63946),
                        modifier = Modifier.fillMaxWidth(),
                        leftIcon = painterResource(Res.drawable.ic_delete),
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    CommonOutlineButton(
                        onClick = { showLogoutDialog = true },
                        text = "Log Out",
                        borderColor = Color.Transparent,
                        enabled = true,
                        textColor = RedColor,
                        modifier = Modifier.fillMaxWidth(),
                        leftIcon = painterResource(Res.drawable.ic_logout),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

data class ManagementItem(
    val title: String,
    val subtitle: String,
    val icon: DrawableResource,
    val onClick: () -> Unit
)
