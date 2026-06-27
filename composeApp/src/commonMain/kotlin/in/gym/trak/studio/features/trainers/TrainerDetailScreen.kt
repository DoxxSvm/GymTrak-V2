package `in`.gym.trak.studio.features.trainers

import `in`.gym.trak.studio.components.AppScrollableScreen


/**
 * Detailed profile for a trainer.
 * Includes tabs for profile, plans, and attendance, quick action buttons,
 * contact information, and professional details.
 */


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.SubcomposeAsyncImage
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.gym_boy
import gym.composeapp.generated.resources.ic_add_plan
import gym.composeapp.generated.resources.ic_cale
import gym.composeapp.generated.resources.ic_currency
import gym.composeapp.generated.resources.ic_delete_trainer
import gym.composeapp.generated.resources.ic_experience
import gym.composeapp.generated.resources.ic_location
import gym.composeapp.generated.resources.ic_make_payment
import gym.composeapp.generated.resources.ic_money
import gym.composeapp.generated.resources.ic_phone
import gym.composeapp.generated.resources.ic_plans
import gym.composeapp.generated.resources.ic_profile
import gym.composeapp.generated.resources.ic_specialization
import gym.composeapp.generated.resources.ic_total_active_plan
import gym.composeapp.generated.resources.ic_trainer_edit
import gym.composeapp.generated.resources.ic_trainer_salary
import gym.composeapp.generated.resources.ic_wallet_filled
import gym.composeapp.generated.resources.ic_workout
import gym.composeapp.generated.resources.img_no_checkin
import gym.composeapp.generated.resources.img_no_expenses
import gym.composeapp.generated.resources.img_no_payment
import gym.composeapp.generated.resources.img_no_plan
import `in`.gym.trak.studio.base.Constants
import `in`.gym.trak.studio.components.AppEmptyStateView
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.ConfirmationDialog
import `in`.gym.trak.studio.components.DetailTabChip
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.components.SearchBar
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import `in`.gym.trak.studio.components.SummaryStatCard
import `in`.gym.trak.studio.data.model.GymStaffListRole
import `in`.gym.trak.studio.data.model.TrainerAttendanceDTO
import `in`.gym.trak.studio.data.model.TrainerAttendanceLogDTO
import `in`.gym.trak.studio.data.model.TrainerPaymentItemDTO
import `in`.gym.trak.studio.data.model.TrainerPaymentsDTO
import `in`.gym.trak.studio.data.model.TrainerSalaryDetailsDTO
import `in`.gym.trak.studio.data.model.TrainerSalaryPaymentItemDTO
import `in`.gym.trak.studio.data.model.UpdateTrainerRequest
import `in`.gym.trak.studio.data.repository.SessionManager.PermissionKeys
import `in`.gym.trak.studio.data.repository.SessionManager.hasPermission
import `in`.gym.trak.studio.features.management.LeaveHistoryScreen
import `in`.gym.trak.studio.features.members.AttendanceLogItem
import `in`.gym.trak.studio.features.members.AttendanceStatCard
import `in`.gym.trak.studio.features.members.BiometricActionItem
import `in`.gym.trak.studio.features.payments.RevenueBarChart
import `in`.gym.trak.studio.features.payments.formatRevenueTotalDisplay
import `in`.gym.trak.studio.features.plans.AddPlanScreen
import `in`.gym.trak.studio.features.plans.PlanDetailsScreen
import `in`.gym.trak.studio.features.plans.PlanListItem
import `in`.gym.trak.studio.getCurrentTimeMillis
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.GrayBorderColor
import `in`.gym.trak.studio.theme.OffGreenColor
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.PrimaryDarkColor
import `in`.gym.trak.studio.theme.PrimaryGreenColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.theme.YellowColor
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

class TrainerDetailScreen(
    val trainerId: String,
    private val onRefresh: (() -> Unit)? = null,
    private val listRole: String = GymStaffListRole.TRAINER
) : Screen {
    private val roleLabel: String =
        if (listRole == GymStaffListRole.STAFF) "Staff" else "Trainer"

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val detail by screenModel.trainerDetail.collectAsState()

        LaunchedEffect(trainerId, listRole) {
            screenModel.clearError()
            screenModel.getTrainerDetail(trainerId, listRole)
        }

        var selectedTab by rememberSaveable { mutableStateOf("Profile") }
        var showPasswordSheet by remember { mutableStateOf(false) }
        var showSalaryPaymentSheet by remember { mutableStateOf(false) }
        var showPermissionsSheet by remember { mutableStateOf(false) }
        val passwordSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val permissionsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        /** Same pattern as owner dashboard: any of the keys in [PermissionKeys] grants access (OR). While [detail] is null, UI stays usable. */


        fun tabAllowed(label: String): Boolean = when (label) {
            "Profile" -> true
            "Plans" -> listRole != GymStaffListRole.STAFF && hasPermission(
                PermissionKeys.KEY_PLAN_READ,
                PermissionKeys.KEY_PLAN_CREATE,
                PermissionKeys.KEY_PLAN_UPDATE,
                PermissionKeys.KEY_PLAN_DELETE
            )

            "Attendance" -> hasPermission(
                PermissionKeys.KEY_ATTENDANCE_READ,
                PermissionKeys.KEY_BIOMETRIC_CREATE,
                PermissionKeys.KEY_BIOMETRIC_DELETE,
                PermissionKeys.KEY_BIOMETRIC_BLOCK
            )

            "Payments" -> hasPermission(
                PermissionKeys.KEY_PAYMENTS,
                PermissionKeys.KEY_PAYMENT_READ,
                PermissionKeys.KEY_PAYMENT_CREATE
            )

            "Salary" -> hasPermission(
                PermissionKeys.KEY_SALARY_READ,
                PermissionKeys.KEY_SALARY_CREATE
            )

            else -> true
        }

        LaunchedEffect(detail?.permissions, selectedTab, listRole) {
            if (!tabAllowed(selectedTab)) {
                val order = listOf("Profile", "Plans", "Attendance", "Payments", "Salary")
                selectedTab = order.firstOrNull { tabAllowed(it) } ?: "Profile"
            }
        }
        var showDeleteDialog by remember { mutableStateOf(false) }
        var showDeleteBiometricDialog by remember { mutableStateOf(false) }

        if (showDeleteDialog) {
            ConfirmationDialog(
                onDismissRequest = { showDeleteDialog = false },
                onConfirm = {
                    screenModel.deleteTrainer(
                        trainerId,
                        onSuccess = {
                            showDeleteDialog = false
                            onRefresh?.invoke()
                            navigator?.pop()
                        },
                        role = listRole
                    )
                },
                title = "Delete $roleLabel",
                message = "Are you sure you want to delete this ${roleLabel.lowercase()}? This action cannot be undone.",
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
                message = "Are you sure you want to delete this ${roleLabel.lowercase()}'s biometric data?",
                confirmText = "Delete",
                isDangerAction = true
            )
        }

        LoadingScreenHandler(screenModel = screenModel) {
            Scaffold(
                topBar = {
                    GymAppBar(
                        title = when (selectedTab) {
                            "Profile" -> "$roleLabel Information"
                            "Plans" -> "$roleLabel Plans"
                            "Attendance" -> "Attendance History"
                            "Payments" -> "Payment Details"
                            "Salary" -> "Salary Details"
                            else -> "$roleLabel Detail"
                        },
                        onBackClick = { navigator?.pop() }
                    )
                },
                floatingActionButton = {
                    if (selectedTab == "Plans" && hasPermission(
                            PermissionKeys.KEY_PLAN_CREATE,
                            PermissionKeys.KEY_PLAN_UPDATE
                        )
                    ) {
                        FloatingActionButton(
                            onClick = {
                                navigator?.push(
                                    AddPlanScreen(
                                        preselectedTrainerId = trainerId,
                                        limitPlanTypes = true
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
                },
                containerColor = Color.Transparent
            ) { padding ->
                AppScrollableScreen(
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {

                    // Main Navigation Tabs (Scrollable Side-by-Side as requested)
                    StaggeredEntranceItem(index = 0) {
                        val tabScrollState = rememberScrollState()
                        Spacer(modifier = Modifier.height(6.dp))
                        Card(
                            shape = RoundedCornerShape(
                                topStart = 50.dp,
                                bottomStart = 50.dp,
                                topEnd = 50.dp,
                                bottomEnd = 50.dp
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = White),
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 4.dp,
                                    shape = RoundedCornerShape(
                                        topStart = 50.dp,
                                        bottomStart = 50.dp,
                                        topEnd = 50.dp,
                                        bottomEnd = 50.dp
                                    ),
                                    ambientColor = PrimaryColor,
                                    spotColor = PrimaryColor
                                )
                        ) {
                            Row(
                                modifier = Modifier
                                    .horizontalScroll(rememberScrollState())
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {

                                DetailTabChip(
                                    label = "Profile",
                                    isSelected = selectedTab == "Profile",
                                    icon = Res.drawable.ic_profile,
                                    onClick = { selectedTab = "Profile" }
                                )

                                if (tabAllowed("Plans")) {
                                    DetailTabChip(
                                        label = "Plans",
                                        isSelected = selectedTab == "Plans",
                                        icon = Res.drawable.ic_plans,
                                        onClick = { selectedTab = "Plans" }
                                    )
                                }

                                if (tabAllowed("Attendance")) {
                                    DetailTabChip(
                                        label = "Attendance",
                                        isSelected = selectedTab == "Attendance",
                                        icon = Res.drawable.ic_cale,
                                        onClick = { selectedTab = "Attendance" }
                                    )
                                }

                                if (tabAllowed("Payments")) {
                                    DetailTabChip(
                                        label = "Payments",
                                        isSelected = selectedTab == "Payments",
                                        icon = Res.drawable.ic_money,
                                        onClick = { selectedTab = "Payments" }
                                    )
                                }

                                if (tabAllowed("Salary")) {
                                    DetailTabChip(
                                        label = "Salary",
                                        isSelected = selectedTab == "Salary",
                                        icon = Res.drawable.ic_workout,
                                        onClick = { selectedTab = "Salary" }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    StaggeredEntranceItem(index = 1) {
                        when (selectedTab) {
                            "Profile" -> {
                                // Avatar and Name
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(100.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFF1F5F9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        SubcomposeAsyncImage(
                                            model = detail?.user?.avatarUrl,
                                            contentDescription = "Profile Picture",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                            loading = {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp),
                                                    color = PrimaryColor,
                                                    strokeWidth = 2.dp
                                                )
                                            },
                                            error = {
                                                Image(
                                                    painter = painterResource(Res.drawable.gym_boy),
                                                    contentDescription = "Placeholder",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = detail?.user?.fullName ?: "Loading...",
                                        style = AppTextTheme.bold.copy(
                                            fontSize = 18.sp,
                                            color = Black
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Quick Actions
                                    Text(
                                        text = "Quick actions",
                                        style = AppTextTheme.regular.copy(
                                            fontSize = 16.sp,
                                            color = Black
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        item {
                                            Spacer(modifier = Modifier.width(1.dp))
                                        }
                                        if (listRole != GymStaffListRole.STAFF && hasPermission(
                                                PermissionKeys.KEY_PLAN_CREATE,
                                                PermissionKeys.KEY_PLAN_UPDATE
                                            )
                                        ) {
                                            item {
                                                ActionItem(
                                                    "Add Plan",
                                                    Res.drawable.ic_add_plan,
                                                    onClick = {
                                                        navigator?.push(
                                                            AddPlanScreen(
                                                                preselectedTrainerId = trainerId,
                                                                limitPlanTypes = true
                                                            )
                                                        )
                                                    })
                                            }
                                        }
                                        if (hasPermission(PermissionKeys.KEY_TRAINER_DELETE)) {
                                            item {
                                                ActionItem(
                                                    "Delete",
                                                    Res.drawable.ic_delete_trainer,
                                                    onClick = { showDeleteDialog = true })
                                            }
                                        }
                                        if (hasPermission(
                                                PermissionKeys.KEY_SALARY_READ,
                                                PermissionKeys.KEY_SALARY_CREATE
                                            )
                                        ) {
                                            item {
                                                ActionItem(
                                                    "Salary Pay",
                                                    Res.drawable.ic_trainer_salary,
                                                    onClick = { showSalaryPaymentSheet = true })
                                            }
                                        }
                                        if (hasPermission(
                                                PermissionKeys.KEY_TRAINER_UPDATE,
                                                PermissionKeys.KEY_TRAINER_PERMISSIONS_ASSIGN
                                            )
                                        ) {
                                            item {
                                                ActionItem(
                                                    "Edit",
                                                    Res.drawable.ic_trainer_edit,
                                                    onClick = {
                                                        detail?.let { data ->
                                                            navigator?.push(
                                                                EditTrainerScreen(
                                                                    trainerId = trainerId,
                                                                    initialData = data,
                                                                    onRefresh = {
                                                                        screenModel.getTrainerDetail(
                                                                            trainerId,
                                                                            listRole
                                                                        )
                                                                        onRefresh?.invoke()
                                                                    },
                                                                    listRole = listRole
                                                                )
                                                            )
                                                        }
                                                    })
                                            }
                                        }
                                        item {
                                            Spacer(modifier = Modifier.width(1.dp))
                                        }
                                    }

//                                    Spacer(modifier = Modifier.height(24.dp))
//
//                                    // Contact Information
//                                    CommonCard(
//                                        content = {
//                                            Column(modifier = Modifier.padding(16.dp)) {
//                                                InfoRow(
//                                                    label = "Phone Number",
//                                                    value = detail?.user?.phone ?: "N/A",
//                                                    icon = Res.drawable.ic_phone
//                                                )
//                                                Spacer(modifier = Modifier.height(16.dp))
//                                                InfoRow(
//                                                    label = "Address",
//                                                    value = detail?.profile?.address ?: "N/A",
//                                                    icon = Res.drawable.ic_location
//                                                )
//                                            }
//                                        }
//
//                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // User account (login / staff)
                                    CommonCard(
                                        content = {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text(
                                                    text = "User Info",
                                                    style = AppTextTheme.semiBold.copy(
                                                        fontSize = 16.sp,
                                                        color = Black
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                                InfoRow(
                                                    label = "Username",
                                                    value = detail?.user?.username?.takeIf { it.isNotBlank() }
                                                        ?: "—",
                                                    icon = Res.drawable.ic_profile
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))

                                                InfoRow(
                                                    label = "Phone Number",
                                                    value = detail?.user?.phone ?: "N/A",
                                                    icon = Res.drawable.ic_phone
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))
                                                InfoRow(
                                                    label = "Address",
                                                    value = detail?.profile?.address ?: "N/A",
                                                    icon = Res.drawable.ic_location
                                                )

                                                Spacer(modifier = Modifier.height(16.dp))
                                                if (hasPermission(
                                                        PermissionKeys.KEY_TRAINER_CREDENTIALS_MANAGE,
                                                        PermissionKeys.KEY_TRAINER_UPDATE
                                                    )
                                                ) {
                                                    HorizontalDivider(color = GrayBorderColor)
                                                    Spacer(modifier = Modifier.height(12.dp))
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clip(RoundedCornerShape(12.dp))
                                                            .clickable { showPasswordSheet = true }
                                                            .padding(vertical = 8.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "Update Password",
                                                            style = AppTextTheme.semiBold.copy(
                                                                fontSize = 15.sp,
                                                                color = PrimaryColor
                                                            )
                                                        )
                                                        Icon(
                                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                            contentDescription = null,
                                                            tint = Gray
                                                        )
                                                    }

                                                    if (hasPermission(PermissionKeys.KEY_TRAINER_PERMISSIONS_ASSIGN)) {
                                                        Spacer(modifier = Modifier.height(12.dp))
                                                        Row(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clip(RoundedCornerShape(12.dp))
                                                                .clickable {
                                                                    showPermissionsSheet = true
                                                                }
                                                                .padding(vertical = 8.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Text(
                                                                text = "Update Permission",
                                                                style = AppTextTheme.semiBold.copy(
                                                                    fontSize = 15.sp,
                                                                    color = PrimaryColor
                                                                )
                                                            )
                                                            Icon(
                                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                                contentDescription = null,
                                                                tint = Gray
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Professional Details
                                    CommonCard(
                                        content = {
                                            Column(modifier = Modifier.padding(16.dp)) {
                                                Text(
                                                    text = "Professional Details",
                                                    style = AppTextTheme.semiBold.copy(
                                                        fontSize = 16.sp,
                                                        color = Black
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(16.dp))

                                                DetailItem(
                                                    label = "Experience",
                                                    value = detail?.profile?.experience ?: "N/A",
                                                    icon = Res.drawable.ic_experience
                                                )
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = 12.dp),
                                                    color = GrayBorderColor
                                                )
                                                DetailItem(
                                                    label = "Base Salary",
                                                    value = "${Constants.RUPEE} ${detail?.profile?.salaryCents ?: 0}/${
                                                        detail?.profile?.salaryPeriod ?: "month"
                                                    }",
                                                    icon = Res.drawable.ic_currency
                                                )
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(vertical = 12.dp),
                                                    color = GrayBorderColor
                                                )

                                                Column {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            painter = painterResource(Res.drawable.ic_specialization),
                                                            contentDescription = null,
                                                            tint = Color.Unspecified,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Text(
                                                            text = "Specialization",
                                                            style = AppTextTheme.regular.copy(
                                                                fontSize = 12.sp,
                                                                color = Gray
                                                            )
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(10.dp))
                                                    SpecializationChips(
                                                        expertise = detail?.expertise ?: emptyList()
                                                    )
                                                }
                                            }
                                        }
                                    )
                                }
                            }

                            "Plans" -> {
                                // Summary Stats
                                Column(
                                    modifier = Modifier.fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 24.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        SummaryStatCard(
                                            label = "Total Active",
                                            value = "${detail?.plans?.totalActivePlans ?: 0} Plans",
                                            icon = Res.drawable.ic_total_active_plan,
                                            isSelected = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        SummaryStatCard(
                                            label = "Subscribers",
                                            value = "${detail?.plans?.totalSubscribers ?: 0}",
                                            icon = Res.drawable.ic_profile,
                                            isSelected = false,
                                            modifier = Modifier.weight(1f),
                                            containerColor = White
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Plan List
                                    if (detail?.plans?.items.isNullOrEmpty()) {
                                        AppEmptyStateView(
                                            image = Res.drawable.img_no_plan,
                                            title = "No Plans Found",
                                            subtitle = "This trainer has no assigned plans yet. Please create a plan.",
                                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                                        )
                                    } else {
                                        detail?.plans?.items?.forEach { plan ->
                                            PlanListItem(
                                                planType = plan.type,
                                                name = plan.name,
                                                duration = plan.durationLabel,
                                                clients = "${plan.activeClients}",
                                                price = "${Constants.RUPEE} ${plan.price}",
                                                onViewDetails = {
                                                    navigator?.push(
                                                        PlanDetailsScreen(
                                                            planId = plan.id
                                                        )
                                                    )
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(24.dp))
                                        }
                                    }
                                }
                            }

                            "Attendance" -> {
                                // Attendance content
                                TrainerAttendanceContent(
                                    attendance = detail?.attendance,
                                    onDeleteBiometric = { showDeleteBiometricDialog = true }
                                )
                            }

                            "Payments" -> {
                                PaymentDetailsContent(payments = detail?.payments)
                            }

                            "Salary" -> {
                                SalaryDetailsTab(
                                    applicantId = detail?.gymUserId,
                                    salary = detail?.salary,
                                    trainerName = detail?.user?.fullName,
                                    trainerImageUrl = detail?.user?.avatarUrl,
                                    onOpenMakePaymentSheet = { showSalaryPaymentSheet = true }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }

            }

            if (showPasswordSheet) {
                val isLoading by screenModel.isLoading.collectAsState()
                var newPassword by remember(showPasswordSheet) { mutableStateOf("") }
                var confirmPassword by remember(showPasswordSheet) { mutableStateOf("") }
                var localError by remember(showPasswordSheet) { mutableStateOf<String?>(null) }

                ModalBottomSheet(
                    onDismissRequest = { showPasswordSheet = false },
                    sheetState = passwordSheetState,
                    containerColor = White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp)
                    ) {
                        Text(
                            text = "Update password",
                            style = AppTextTheme.bold.copy(fontSize = 18.sp, color = Black)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Set a new login password for this trainer.",
                            style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Gray)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        CommonTextField(
                            value = newPassword,
                            onValueChange = {
                                newPassword = it
                                localError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "New password",
                            isPassword = true,
                            enabled = !isLoading,
                            borderRadius = 12.dp,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Next
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        CommonTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                localError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "Confirm password",
                            isPassword = true,
                            enabled = !isLoading,
                            borderRadius = 12.dp,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            )
                        )
                        localError?.let { msg ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = msg, color = Color(0xFFB91C1C), fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        CommonButton(
                            text = if (isLoading) "Saving…" else "Save password",
                            onClick = {
                                when {
                                    newPassword.length < 8 -> {
                                        localError = "Password must be at least 8 characters."
                                    }

                                    newPassword != confirmPassword -> {
                                        localError = "Passwords do not match."
                                    }

                                    else -> {
                                        screenModel.setTrainerPassword(
                                            trainerId = trainerId,
                                            newPassword = newPassword,
                                            onSuccess = {

                                                showPasswordSheet = false
                                                newPassword = ""
                                                confirmPassword = ""
                                                screenModel.getTrainerDetail(trainerId, listRole)
                                                onRefresh?.invoke()
                                            },
                                            onError = { localError = it }
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isLoading
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(
                            onClick = { showPasswordSheet = false },
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            enabled = !isLoading
                        ) {
                            Text("Cancel", color = Gray)
                        }
                    }
                }
            }

            if (showSalaryPaymentSheet) {
                MakeSalaryPaymentDialog(
                    onDismiss = { showSalaryPaymentSheet = false },
                    onConfirm = { amount, paymentMode, date, onSuccess ->
                        screenModel.createTrainerSalaryPayment(
                            trainerId = trainerId,
                            amount = amount,
                            paymentMode = paymentMode,
                            date = date
                        ) {
                            showSalaryPaymentSheet = false
                            screenModel.getTrainerDetail(trainerId, listRole)
                            onRefresh?.invoke()
                            onSuccess()
                        }
                    }
                )
            }

            if (showPermissionsSheet && detail != null) {
                ModalBottomSheet(
                    onDismissRequest = { showPermissionsSheet = false },
                    sheetState = permissionsSheetState,
                    containerColor = Color.White,
                    dragHandle = null
                ) {
                    TrainerPermissionsSheet(
                        onDismiss = { showPermissionsSheet = false },
                        sheetTitle = if (listRole == GymStaffListRole.STAFF) {
                            "Staff permissions"
                        } else {
                            "Trainer permissions"
                        },
                        modules = if (listRole == GymStaffListRole.STAFF) {
                            StaffPermissionCatalog.modules
                        } else {
                            TrainerPermissionCatalog.modules
                        },
                        initialPermissionKeys = detail?.permissions,
                        onComplete = { permissionKeys ->
                            showPermissionsSheet = false
                            val request = UpdateTrainerRequest(
                                permissions = permissionKeys,
                                isActive = detail?.isActive ?: true,
                            )
                            screenModel.updateTrainer(
                                trainerId,
                                request,
                                onSuccess = {
                                    screenModel.getTrainerDetail(trainerId, listRole)
                                    onRefresh?.invoke()
                                },
                                role = listRole
                            )
                        }
                    )
                }
            }
        }
    }


    @Composable
    fun PaymentDetailsContent(payments: TrainerPaymentsDTO? = null) {
        var searchQuery by remember { mutableStateOf("") }
        val allItems = payments?.items.orEmpty()
        val filteredItems = remember(allItems, searchQuery) {
            val query = searchQuery.trim()
            if (query.isBlank()) {
                allItems
            } else {
                allItems.filter { item ->
                    item.memberName.contains(query, ignoreCase = true) ||
                        item.method.contains(query, ignoreCase = true) ||
                        item.subtitle?.contains(query, ignoreCase = true) == true ||
                        item.planName.contains(query, ignoreCase = true)
                }
            }
        }

        val isEmptyPaymentSection = payments == null ||
            (allItems.isEmpty() && payments.chart.isEmpty() && payments.totalRevenue <= 0)
        if (isEmptyPaymentSection) {
            AppEmptyStateView(
                image = Res.drawable.img_no_payment,
                title = "No Payment Records Found",
                subtitle = "You haven't received any trainer payments yet.",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)
            )
            return
        }

        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            // Attendance, Payments, Salary sub-tabs (Image 5)

            Spacer(modifier = Modifier.height(16.dp))

            val chartLabels = payments?.chart.orEmpty().map { "W${it.week}" }
            val chartValues = payments?.chart.orEmpty().map { it.revenue.toFloat().coerceAtLeast(0f) }
            val peakIndex = if (chartValues.isNotEmpty()) {
                chartValues.indices.maxByOrNull { chartValues[it] } ?: -1
            } else {
                -1
            }
            val maxValue = chartValues.maxOrNull() ?: 0f
            val totalRevenue = (payments?.totalRevenue ?: 0).toDouble().let { total ->
                if (total > 0.0) total else chartValues.sum().toDouble()
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Revenue Overview",
                                style = AppTextTheme.bold.copy(fontSize = 18.sp),
                            )
                            Text(
                                text = buildString {
                                    append("Total: ${Constants.RUPEE} ")
                                    append(formatRevenueTotalDisplay(totalRevenue))
                                    val txnCount = payments?.items.orEmpty().size
                                    if (txnCount > 0) append(" · $txnCount payments")
                                },
                                style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray),
                            )
                        }
                        payments?.revenueChangePercent?.let { changePercent ->
                            Row(
                                modifier = Modifier
                                    .background(Color(0xFFE7F7F2), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = if (changePercent >= 0) {
                                        Icons.AutoMirrored.Filled.TrendingUp
                                    } else {
                                        Icons.AutoMirrored.Filled.TrendingDown
                                    },
                                    contentDescription = null,
                                    tint = if (changePercent >= 0) PrimaryDarkColor else Color.Red,
                                    modifier = Modifier.size(12.dp),
                                )
                                Text(
                                    text = " ${if (changePercent >= 0) "+" else ""}${changePercent}%",
                                    style = AppTextTheme.bold.copy(
                                        fontSize = 11.sp,
                                        color = if (changePercent >= 0) PrimaryDarkColor else Color.Red,
                                    ),
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    RevenueBarChart(
                        months = chartLabels,
                        values = chartValues,
                        peakIndex = peakIndex,
                        maxValue = maxValue,
                        selectedPeriod = "weekly",
                        isLoading = false,
                        errorMessage = null,
                        onRetry = {},
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Payment History",
                style = AppTextTheme.bold.copy(fontSize = 20.sp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                placeholder = "Search by name or payment mode",
            )

            Spacer(modifier = Modifier.height(12.dp))

            when {
                allItems.isEmpty() -> {
                    AppEmptyStateView(
                        image = Res.drawable.img_no_payment,
                        title = "No Payment History",
                        subtitle = "No payment transactions are available for this trainer.",
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                }
                filteredItems.isEmpty() -> {
                    AppEmptyStateView(
                        image = Res.drawable.img_no_payment,
                        title = "No results",
                        subtitle = "No payments match your search.",
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        useCardContainer = false,
                    )
                }
                else -> {
                    filteredItems.forEach { payment ->
                        PaymentHistoryItem(payment)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    @Composable
    fun SubTabItem(
        label: String,
        isSelected: Boolean,
        icon: DrawableResource,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Surface(
            onClick = onClick,
            modifier = modifier.height(40.dp),
            shape = RoundedCornerShape(100.dp),
            color = if (isSelected) PrimaryColor else Color.Transparent
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = if (isSelected) White else Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = AppTextTheme.bold.copy(
                        fontSize = 12.sp,
                        color = if (isSelected) White else Gray
                    )
                )
            }
        }
    }

    @Composable
    fun PaymentHistoryItem(payment: TrainerPaymentItemDTO) {
        val subtitle = payment.subtitle?.takeIf { it.isNotBlank() }
            ?: payment.planName.takeIf { it.isNotBlank() }
            ?: "Membership payment"
        val methodLabel = payment.method.trim().replaceFirstChar { it.uppercase() }
        val dateLabel = payment.createdAt
            .substringBefore("T")
            .takeIf { it.isNotBlank() }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color(0xFFF1F5F9), CircleShape),
                ) {
                    if (!payment.memberAvatarUrl.isNullOrBlank()) {
                        SubcomposeAsyncImage(
                            model = payment.memberAvatarUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            loading = { TrainerPaymentAvatarPlaceholder() },
                            error = { TrainerPaymentAvatarPlaceholder() },
                        )
                    } else {
                        TrainerPaymentAvatarPlaceholder()
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = payment.memberName.ifBlank { "Member" },
                        style = AppTextTheme.bold.copy(fontSize = 15.sp),
                    )
                    Text(
                        text = subtitle,
                        style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(30.dp))
                                .background(PrimaryColor.copy(alpha = 0.20f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = methodLabel.ifBlank { "Cash" },
                                style = AppTextTheme.medium.copy(
                                    fontSize = 11.sp,
                                    color = Black.copy(alpha = 0.80f),
                                ),
                            )
                        }
                        if (!dateLabel.isNullOrBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• $dateLabel",
                                style = AppTextTheme.regular.copy(
                                    fontSize = 11.sp,
                                    color = Black.copy(alpha = 0.80f),
                                ),
                            )
                        }
                    }
                }

                Text(
                    text = "${Constants.RUPEE}${payment.amount}",
                    style = AppTextTheme.bold.copy(fontSize = 16.sp, color = PrimaryColor),
                )
            }
        }
    }

    @Composable
    private fun TrainerPaymentAvatarPlaceholder() {
        Image(
            painter = painterResource(Res.drawable.gym_boy),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
        )
    }


    @Composable
    fun TabItem(
        label: String,
        isSelected: Boolean,
        icon: DrawableResource,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Surface(
            onClick = onClick,
            modifier = modifier.height(44.dp),
            shape = RoundedCornerShape(100.dp),
            color = if (isSelected) PrimaryColor else Color.Transparent
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = if (isSelected) White else Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    style = AppTextTheme.bold.copy(
                        fontSize = 13.sp,
                        color = if (isSelected) White else Gray
                    )
                )
            }
        }
    }

    @Composable
    fun ActionItem(label: String, icon: DrawableResource, onClick: () -> Unit = {}) {


        CommonCard(
            onClick = onClick,
            content = {

                Column(
                    modifier = Modifier.height(90.dp).width(100.dp).background(
                        color = Black.copy(alpha = 0.01f)
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = label,
                        style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Black)
                    )
                }
            }
        )

    }

    @Composable
    fun InfoRow(label: String, value: String, icon: DrawableResource) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray)
                )
                Text(
                    text = value,
                    style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black)
                )
            }
        }
    }

    @Composable
    fun DetailItem(label: String, value: String, icon: DrawableResource) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray),
                modifier = Modifier.weight(1f)
            )
            Text(text = value, style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black))
        }
    }


    @Composable
    fun SalaryDetailsTab(

        salary: TrainerSalaryDetailsDTO? = null,
        trainerName: String? = null,
        trainerImageUrl: String? = null,
        applicantId: String? = null,
        onOpenMakePaymentSheet: () -> Unit = {}
    ) {
        val navigator = LocalNavigator.current
        val isEmptySalarySection = salary == null ||
                (salary.paymentHistory.isEmpty() && salary.monthlySalary == 0 && salary.paidAmount == 0 && salary.pendingAmount == 0)
        if (isEmptySalarySection) {
            Column(modifier = Modifier.padding(16.dp)) {
                AppEmptyStateView(
                    image = Res.drawable.img_no_expenses,
                    title = "No Salary Records Found",
                    subtitle = "No salary data is available for this trainer yet.",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                CommonButton(
                    leftIcon = painterResource(Res.drawable.ic_make_payment),
                    onClick = onOpenMakePaymentSheet,
                    text = "Make Payment",
                    modifier = Modifier.fillMaxWidth(),
                    color = PrimaryColor,
                    textColor = White
                )
            }
            return
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // Leave History Quick Link
            Card(
                modifier = Modifier.fillMaxWidth()
                    .clickable {
                        navigator?.push(
                            LeaveHistoryScreen(
                                applicantId = applicantId,
                                applicantName = trainerName,
                                applicantImageUrl = trainerImageUrl
                            )
                        )
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE7F7F2))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_cale),
                        contentDescription = null,
                        tint = PrimaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Leave History",
                            style = AppTextTheme.bold.copy(fontSize = 15.sp, color = Black)
                        )
                        Text(
                            text = "View remaining balance & history",
                            style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray)
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            // Salary Summary Card (Image 1)
            CommonCard(
                backgroundColor = OffGreenColor,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Monthly Salary",
                                style = AppTextTheme.regular.copy(
                                    fontSize = 12.sp,
                                    color = Black.copy(alpha = 0.60f)
                                )
                            )
                            Text(
                                text = "${Constants.RUPEE} ${salary?.monthlySalary ?: 0}",
                                style = AppTextTheme.medium.copy(
                                    fontSize = 14.sp,
                                    color = PrimaryColor
                                )
                            )
                        }
                        Column(horizontalAlignment = Alignment.Start) {
                            Text(
                                text = "Paid Amount",
                                style = AppTextTheme.regular.copy(
                                    fontSize = 12.sp,
                                    color = Black.copy(alpha = 0.60f)
                                )
                            )
                            Text(
                                text = "${Constants.RUPEE} ${salary?.paidAmount ?: 0}",
                                style = AppTextTheme.medium.copy(
                                    fontSize = 14.sp,
                                    color = PrimaryColor
                                )
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Pending",
                                style = AppTextTheme.regular.copy(
                                    fontSize = 12.sp,
                                    color = Black.copy(alpha = 0.60f)
                                )
                            )
                            Text(
                                text = "${Constants.RUPEE} ${salary?.pendingAmount ?: 0}",
                                style = AppTextTheme.medium.copy(
                                    fontSize = 14.sp,
                                    color = PrimaryColor
                                )
                            )
                        }
                    }
                })

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Payment History",
                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black)
            )
            Spacer(modifier = Modifier.height(12.dp))
            // Payment History
            if (salary?.paymentHistory.isNullOrEmpty()) {
                AppEmptyStateView(
                    image = Res.drawable.img_no_expenses,
                    title = "No Salary Payments",
                    subtitle = "Salary payment history will appear here after first payment.",
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                )
            } else {
                salary.paymentHistory?.forEach { payment ->
                    SalaryPaymentItem(payment)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            CommonButton(
                leftIcon = painterResource(Res.drawable.ic_make_payment),
                onClick = onOpenMakePaymentSheet,
                text = "Make Payment",
                modifier = Modifier.fillMaxWidth(),
                color = PrimaryColor,
                textColor = White
            )
        }
    }

    @Composable
    fun SalaryPaymentItem(payment: TrainerSalaryPaymentItemDTO) {
        CommonCard(
            content = {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = payment.title,
                            style = AppTextTheme.semiBold.copy(fontSize = 14.sp, color = Black)
                        )
                        Text(
                            text = payment.date,
                            style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${Constants.RUPEE} ${payment.amount}",
                            style = AppTextTheme.medium.copy(
                                fontSize = 14.sp,
                                color = PrimaryDarkColor
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            color = Color(0xFFD48B45).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(100.dp)
                        ) {
                            Text(
                                text = payment.method,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                                style = AppTextTheme.regular.copy(
                                    fontSize = 12.sp,
                                    color = Color(0xFFD48B45)
                                )
                            )
                        }
                    }
                }
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MakeSalaryPaymentDialog(
        onDismiss: () -> Unit,
        onConfirm: (amount: Int, paymentMode: String, date: String, onSuccess: () -> Unit) -> Unit
    ) {
        fun formatIsoDate(millis: Long): String {
            val localDate = Instant.fromEpochMilliseconds(millis).toLocalDateTime(TimeZone.UTC).date
            return "${localDate.year}-${
                localDate.monthNumber.toString().padStart(2, '0')
            }-${localDate.dayOfMonth.toString().padStart(2, '0')}"
        }

        var amount by rememberSaveable { mutableStateOf("") }
        var selectedMode by rememberSaveable { mutableStateOf("cash") }
        var paymentDate by rememberSaveable { mutableStateOf(formatIsoDate(getCurrentTimeMillis())) }
        var showDatePicker by remember { mutableStateOf(false) }
        var validationError by remember { mutableStateOf<String?>(null) }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                paymentDate = formatIsoDate(millis)
                            }
                            showDatePicker = false
                        }
                    ) { Text("OK", color = PrimaryColor) }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel", color = Gray)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            dragHandle = null
        ) {
            Column(
                modifier = Modifier.padding(24.dp).padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(24.dp))
                    Text(
                        text = "Make Salary Payment",
                        style = AppTextTheme.bold.copy(fontSize = 18.sp, color = Black)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Amount",
                        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CommonTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = "${Constants.RUPEE} Amount"
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Payment Mode",
                        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            onClick = { selectedMode = "cash" },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = if (selectedMode == "cash") PrimaryColor else Color(0xFFF7F8F9),
                            border = if (selectedMode != "cash") androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color(0xFFF1F5F9)
                            ) else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "Cash",
                                    style = AppTextTheme.bold.copy(
                                        fontSize = 14.sp,
                                        color = if (selectedMode == "cash") White else Gray
                                    )
                                )
                            }
                        }
                        Surface(
                            onClick = { selectedMode = "upi" },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = if (selectedMode == "upi") PrimaryColor else Color(0xFFF7F8F9),
                            border = if (selectedMode != "upi") androidx.compose.foundation.BorderStroke(
                                1.dp,
                                Color(0xFFF1F5F9)
                            ) else null
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "UPI",
                                    style = AppTextTheme.bold.copy(
                                        fontSize = 14.sp,
                                        color = if (selectedMode == "upi") White else Gray
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Date",
                        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    ) {
                        CommonTextField(
                            value = paymentDate,
                            onValueChange = {},
                            placeholder = "YYYY-MM-DD",
                            readOnly = true,
                            leadingIconDrawable = null,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                validationError?.let {
                    Text(
                        text = it,
                        style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Color.Red),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                CommonButton(
                    onClick = {
                        val amountInt = amount.toIntOrNull() ?: 0
                        when {
                            amountInt <= 0 -> validationError = "Enter a valid amount"
                            paymentDate.isBlank() -> validationError = "Please select a date"
                            else -> {
                                validationError = null
                                onConfirm(amountInt, selectedMode, paymentDate) {
                                    onDismiss()
                                }
                            }
                        }
                    },
                    text = "Confirm Payment",
                    leftIcon = painterResource(Res.drawable.ic_wallet_filled),
                )
            }
        }
    }

    @Composable
    fun TrainerAttendanceContent(
        attendance: TrainerAttendanceDTO? = null,
        onDeleteBiometric: () -> Unit = {}
    ) {
        fun resolveStatusLabel(log: TrainerAttendanceLogDTO): String {
            val raw = log.statusLabel.ifBlank { log.status }.ifBlank { "Unknown" }
            return raw.replace("_", " ").uppercase()
        }

        fun resolveTimestamp(log: TrainerAttendanceLogDTO): String {
            return log.timestamp.ifBlank { log.checkedInAt }
        }

        fun formatAttendanceDateTime(raw: String): String {
            if (raw.isBlank()) return "No check-in time"
            return try {
                val instant = Instant.parse(raw)
                val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                val date = "${local.dayOfMonth.toString().padStart(2, '0')}/${
                    local.monthNumber.toString().padStart(2, '0')
                }/${local.year}"
                val hour24 = local.hour
                val minute = local.minute.toString().padStart(2, '0')
                val amPm = if (hour24 >= 12) "PM" else "AM"
                val hour12 = when {
                    hour24 == 0 -> 12
                    hour24 > 12 -> hour24 - 12
                    else -> hour24
                }
                "$date, ${hour12.toString().padStart(2, '0')}:$minute $amPm"
            } catch (_: Throwable) {
                raw
            }
        }

        fun formatYearMonthLabel(value: String?): String {
            val raw = value?.trim().orEmpty()
            if (raw.isBlank()) return "Current Month"
            val parts = raw.split("-")
            if (parts.size != 2) return raw
            val year = parts[0]
            val month = parts[1].toIntOrNull() ?: return raw
            val monthName = when (month) {
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
                else -> return raw
            }
            return "$monthName $year"
        }

        val isEmptyAttendanceSection = attendance == null ||
                (attendance.recentLogs.isEmpty() && attendance.summary == null)
        if (isEmptyAttendanceSection) {
            AppEmptyStateView(
                image = Res.drawable.img_no_checkin,
                title = "No Check-ins Yet",
                subtitle = "This trainer hasn't marked attendance yet. Tap below to check in.",
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp)
            )
            return
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Attendance Statistics",
                    style = AppTextTheme.semiBold.copy(fontSize = 16.sp)
                )
                Surface(
                    onClick = { },
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9)),
                    color = White
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatYearMonthLabel(attendance?.summary?.yearMonth),
                            style = AppTextTheme.medium.copy(fontSize = 14.sp, color = PrimaryColor)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(Res.drawable.ic_cale),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = PrimaryColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AttendanceStatCard(
                    modifier = Modifier.weight(1f),
                    value = "${attendance?.summary?.daysPresentThisMonth ?: 0}",
                    unit = "Days",
                    label = "Days Present\n(Month)",
                    progress = if ((attendance?.summary?.daysInMonth
                            ?: 0) > 0
                    ) (attendance?.summary?.daysPresentThisMonth
                        ?: 0).toFloat() / attendance?.summary?.daysInMonth!!.toFloat() else 0f
                )
                AttendanceStatCard(
                    modifier = Modifier.weight(1f),
                    value = "${attendance?.summary?.lifetimeCheckIns ?: 0}",
                    unit = "Total",
                    label = "Lifetime Attendance",
                    progress = 1f
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Recent Logs
            Text(
                text = "Recent Logs",
                style = AppTextTheme.semiBold.copy(fontSize = 16.sp),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            CommonCard(
                content = {
                    Column {
                        if (attendance?.recentLogs.isNullOrEmpty()) {
                            AppEmptyStateView(
                                image = Res.drawable.img_no_checkin,
                                title = "No Recent Logs",
                                subtitle = "Recent attendance logs will appear here once check-ins start.",
                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                            )
                        } else {
                            attendance?.recentLogs?.forEachIndexed { index, log ->
                                val resolvedStatus = resolveStatusLabel(log)
                                AttendanceLogItem(
                                    title = log.title,
                                    dateTime = formatAttendanceDateTime(resolveTimestamp(log)),
                                    status = resolvedStatus,
                                    statusColor = try {
                                        if (log.statusColor != null) Color(
                                            log.statusColor.removePrefix(
                                                "#"
                                            ).toLong(16) or 0x00000000FF000000
                                        ) else Gray
                                    } catch (e: Exception) {
                                        when (resolvedStatus.uppercase()) {
                                            "ON TIME", "EARLY" -> PrimaryGreenColor
                                            "LATE" -> YellowColor
                                            else -> Gray
                                        }
                                    }
                                )
                                if (index < attendance.recentLogs.size - 1) {
                                    HorizontalDivider(color = GrayBorderColor)
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { }
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "View All History",
                                style = AppTextTheme.bold.copy(
                                    fontSize = 16.sp,
                                    color = PrimaryColor
                                )
                            )
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    @Composable
    fun SpecializationChip(text: String) {
        Surface(
            color = Color(0xFFF7F8F9),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black)
            )
        }
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun SpecializationChips(expertise: List<String>) {
        if (expertise.isEmpty()) {
            Text(
                text = "N/A",
                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black)
            )
            return
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            expertise.forEach { item ->
                SpecializationChip(item)
            }
        }
    }
}
