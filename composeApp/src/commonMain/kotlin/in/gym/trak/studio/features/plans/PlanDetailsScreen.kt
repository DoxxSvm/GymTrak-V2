package `in`.gym.trak.studio.features.plans

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.CommonOutlineButton
import `in`.gym.trak.studio.components.ConfirmationDialog
import `in`.gym.trak.studio.components.DaySelector
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.BlueLightColor
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.GrayBorderColor
import `in`.gym.trak.studio.theme.OffGreenColor
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.PrimaryDarkColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.base.Constants
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.gym_boy
import gym.composeapp.generated.resources.ic_active_client
import gym.composeapp.generated.resources.ic_clock
import gym.composeapp.generated.resources.ic_delete
import gym.composeapp.generated.resources.ic_edit
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.data.model.PlanDTO
import org.jetbrains.compose.resources.painterResource

/**
 * Screen for viewing the details of a specific gym plan.
 * Portrayed in Image 1 and 2.
 */
//fun PlanDTO.planTypeDisplayLabel(): String = when (type.uppercase()) {
//    "GYM_MEMBERSHIP" -> "Gym Membership"
//    "PT_PLAN" -> "Pt Plan"
//    "BATCH_PLAN" -> "Batch Plan"
//    else -> type.replace('_', ' ').trim()
//        .lowercase()
//        .replaceFirstChar { it.uppercaseChar() }
//}
class PlanDetailsScreen(val planId: String) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val plan by screenModel.planDetail.collectAsState()
        val enrolledMembers by screenModel.enrolledMembers.collectAsState()
        val isLoading by screenModel.isLoading.collectAsState()
        var isPullRefreshRequested by remember { mutableStateOf(false) }

        var selectedTab by remember { mutableStateOf("Basic Information") }
        var showDeleteDialog by remember { mutableStateOf(false) }

        LaunchedEffect(planId) {
            screenModel.getPlanDetail(planId)
            screenModel.loadEnrolledMembers(planId)
        }
        LaunchedEffect(isLoading) {
            if (!isLoading) isPullRefreshRequested = false
        }

        if (showDeleteDialog) {
            ConfirmationDialog(
                onDismissRequest = { showDeleteDialog = false },
                onConfirm = {
                    screenModel.deletePlan(planId) {
                        navigator?.pop()
                    }
                },
                title = "Delete Plan",
                message = "Are you sure you want to archive this plan? It will be marked inactive.",
                confirmText = "Delete",
                isDangerAction = true
            )
        }

        LoadingScreenHandler(
            screenModel = screenModel,
            showLoadingOverlay = !isPullRefreshRequested
        ) {
            Scaffold(
                topBar = {
                    GymAppBar(
                        title = "Plan Details",
                        onBackClick = { navigator?.pop() }
                    )
                },
                containerColor = Color.Transparent
            ) { padding ->
                PullToRefreshBox(
                    isRefreshing = isPullRefreshRequested && isLoading,
                    onRefresh = {
                        isPullRefreshRequested = true
                        screenModel.getPlanDetail(planId)
                        screenModel.loadEnrolledMembers(planId)
                    },
                    state = rememberPullToRefreshState(),
                    indicator = {},
                    modifier = Modifier.fillMaxSize().padding(padding)
                ) {
                    AppScrollableScreen(
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Spacer(modifier = Modifier.height(2.dp))

                        // Tabs

                        CommonCard(
                            backgroundColor = White,
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier.fillMaxWidth(),
                            content = {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    TabItem(
                                        label = "Basic Information",
                                        isSelected = selectedTab == "Basic Information",
                                        onClick = { selectedTab = "Basic Information" },
                                        modifier = Modifier.weight(1f)
                                    )
                                    TabItem(
                                        label = "Clients Enrolled",
                                        isSelected = selectedTab == "Clients Enrolled",
                                        onClick = { selectedTab = "Clients Enrolled" },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            })


                        Spacer(modifier = Modifier.height(16.dp))

                        if (selectedTab == "Basic Information") {
                            // Plan Type Header Card
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = OffGreenColor)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_active_client),
                                            contentDescription = null,
                                            tint = White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = "Plan Type",
                                            style = AppTextTheme.regular.copy(
                                                fontSize = 14.sp,
                                                color = PrimaryColor
                                            )
                                        )
                                        Text(
                                            text = when (plan?.type) {
                                                "GYM_MEMBERSHIP" -> "Gym Membership"
                                                "PT_PLAN" -> "PT Plan"
                                                "BATCH_PLAN" -> "Batch Plan"
                                                else -> "Gym Membership"
                                            },
                                            style = AppTextTheme.semiBold.copy(
                                                fontSize = 16.sp,
                                                color = Black
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Plan Information",
                                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            CommonCard(
                                content = {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        InfoRow(
                                            label = "Plan Name",
                                            value = plan?.name ?: "Gold Membership"
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            color = GrayBorderColor
                                        )
                                        InfoRow(
                                            label = "Duration",
                                            value = "${plan?.durationDays} Days"
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 12.dp),
                                            color = GrayBorderColor
                                        )
                                        InfoRow(
                                            label = "Price",
                                            value = "${Constants.RUPEE} ${(plan?.priceCents ?: 0) / 100}",
                                            valueColor = PrimaryDarkColor
                                        )
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Trainer & Batch Details",
                                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            CommonCard(
                                content = {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Assigned Trainer",
                                                    style = AppTextTheme.regular.copy(
                                                        fontSize = 12.sp,
                                                        color = Gray
                                                    )
                                                )
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Image(
                                                    painter = painterResource(Res.drawable.gym_boy),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(24.dp)
                                                        .clip(CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = plan?.trainer?.name ?: "No Trainer",
                                                    style = AppTextTheme.medium.copy(
                                                        fontSize = 14.sp,
                                                        color = Black
                                                    )
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Weekly Schedule",
                                            style = AppTextTheme.regular.copy(
                                                fontSize = 12.sp,
                                                color = Gray
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        val daysMap =
                                            listOf("M0", "T1", "W2", "T3", "F4", "S5", "S6")
                                        val selectedDays = plan?.batch?.daysOfWeek?.mapNotNull {
                                            if (it in 0..6) daysMap[it] else null
                                        }?.toSet() ?: setOf("M0", "T1", "W2", "T3", "F4")

                                        DaySelector(
                                            selectedDays = selectedDays,
                                            onDayToggle = {})
                                    }
                                }
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            if (plan?.batch != null) {
                                Text(
                                    text = "Batch Details",
                                    style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Allowed Gender: ${plan?.batch?.gender ?: "Both"}",
                                    style = AppTextTheme.regular.copy(
                                        fontSize = 12.sp,
                                        color = Gray
                                    )
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            plan?.batch?.shifts?.forEachIndexed { index, shift ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = White),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        Color(0xFFF1F5F9)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(PrimaryColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(Res.drawable.ic_clock),
                                                contentDescription = null,
                                                tint = White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = "Batch ${index + 1}",
                                                style = AppTextTheme.regular.copy(
                                                    fontSize = 12.sp,
                                                    color = Gray
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Surface(
                                                color = BlueLightColor,
                                                shape = RoundedCornerShape(100.dp)
                                            ) {
                                                Text(
                                                    text = "${shift.startTime} - ${shift.endTime}",
                                                    modifier = Modifier.padding(
                                                        horizontal = 12.dp,
                                                        vertical = 4.dp
                                                    ),
                                                    style = AppTextTheme.regular.copy(
                                                        fontSize = 12.sp,
                                                        color = Black
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CommonOutlineButton(
                                    onClick = {
                                        navigator?.push(
                                            AddPlanScreen(
                                                existingPlanId = planId,
                                                onPlanSaved = {
                                                    screenModel.getPlanDetail(planId)
                                                    screenModel.loadEnrolledMembers(planId)
                                                }
                                            )
                                        )
                                    },
                                    text = "Edit plan",
                                    textColor = Black,
                                    modifier = Modifier.weight(1f),
                                    leftIcon = painterResource(Res.drawable.ic_edit),


                                    )
                                CommonButton(
                                    onClick = { showDeleteDialog = true },
                                    text = "Delete plan",
                                    color = PrimaryColor,
                                    leftIcon = painterResource(Res.drawable.ic_delete),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        } else {
                            // Clients Enrolled (Image 2)
                            if (enrolledMembers.isEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No clients enrolled in this plan",
                                        style = AppTextTheme.medium.copy(color = Gray)
                                    )
                                }
                            } else {
                                enrolledMembers.forEach { enrolled ->
                                    ClientListItem(
                                        name = enrolled.member.name,
                                        endsAt = enrolled.endsAt
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    @Composable
    fun TabItem(
        label: String,
        isSelected: Boolean,
        onClick: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        Surface(
            onClick = onClick,
            modifier = modifier.height(50.dp),
            shape = RoundedCornerShape(100.dp),
            color = if (isSelected) PrimaryColor else Color.Transparent
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = label,
                    style = AppTextTheme.medium.copy(
                        fontSize = 14.sp,
                        color = if (isSelected) White else Gray
                    )
                )
            }
        }
    }

    @Composable
    fun InfoRow(label: String, value: String, valueColor: Color = Black) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
            )
            Text(
                text = value,
                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = valueColor)
            )
        }
    }

    @Composable
    fun ClientListItem(name: String, endsAt: String) {
        CommonCard(
            content = {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(Res.drawable.gym_boy),
                        contentDescription = null,
                        modifier = Modifier.size(50.dp).clip(CircleShape)
                            .border(1.dp, Color(0xFFF1F5F9), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            style = AppTextTheme.semiBold.copy(fontSize = 14.sp, color = Black)
                        )
                        Text(
                            text = "Plan ends at: ${endsAt.split("T")[0]}",
                            style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
                        )
                    }
                }
            }
        )
    }

}
