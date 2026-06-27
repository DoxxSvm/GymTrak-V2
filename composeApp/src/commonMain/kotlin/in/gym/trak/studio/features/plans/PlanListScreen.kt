package `in`.gym.trak.studio.features.plans

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import `in`.gym.trak.studio.components.AppEmptyStateView
import `in`.gym.trak.studio.components.ConfirmationDialog
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.data.model.PlanDTO
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import `in`.gym.trak.studio.components.SummaryStatCard
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.data.repository.SessionManager.hasPermission
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_profile
import gym.composeapp.generated.resources.ic_total_active_plan
import gym.composeapp.generated.resources.img_no_plan

class PlanListScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val plans by screenModel.plans.collectAsState()
        val subscriptionCount by screenModel.planSubscriptionCount.collectAsState()
        var planToDelete by remember { mutableStateOf<PlanDTO?>(null) }
        val canDeletePlan = hasPermission(SessionManager.PermissionKeys.KEY_PLAN_DELETE)

        LaunchedEffect(Unit) {
            screenModel.loadPlans()
        }

        planToDelete?.let { plan ->
            ConfirmationDialog(
                onDismissRequest = { planToDelete = null },
                onConfirm = {
                    val planId = plan.id
                    screenModel.deletePlan(planId) {
                        screenModel.loadPlans(showGlobalLoader = false)
                    }
                },
                title = "Delete Plan",
                message = "Are you sure you want to archive \"${plan.name}\"? The plan will be marked inactive.",
                confirmText = "Delete",
                isDangerAction = true
            )
        }

        LoadingScreenHandler(screenModel = screenModel) {
            Scaffold(
                topBar = {
                    GymAppBar(
                        title = "Gym Packages",
                        onBackClick = { navigator?.pop() }
                    )
                },
                floatingActionButton = {
                    if (hasPermission(
                            SessionManager.PermissionKeys.KEY_PLAN_CREATE
                        )
                    )
                    FloatingActionButton(
                        onClick = { navigator?.push(AddPlanScreen()) },
                        containerColor = PrimaryColor,
                        contentColor = White,
                        shape = CircleShape
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Plan")
                    }
                },
                containerColor = Color.Transparent
            ) { padding ->
                AppScrollableScreen(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
                ) {
                    StaggeredEntranceItem(index = 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SummaryStatCard(
                                label = "Total Active",
                                value = "${plans.size} Plans",
                                icon = Res.drawable.ic_total_active_plan,
                                isSelected = true,
                                modifier = Modifier.weight(1f)
                            )
                            SummaryStatCard(
                                label = "Subscribers",
                                value = "$subscriptionCount",
                                icon = Res.drawable.ic_profile,
                                isSelected = false,
                                modifier = Modifier.weight(1f),
                                containerColor = White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (plans.isEmpty()) {
                        AppEmptyStateView(
                            image = Res.drawable.img_no_plan,
                            title = "No Plans Found",
                            subtitle = "Create your first gym package to start enrolling members.",
                            modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                        )
                    } else {
                        plans.forEachIndexed { index, plan ->
                            StaggeredEntranceItem(index = index + 1) {
                                PlanListRowFromDto(
                                    plan = plan,
                                    onPrimaryClick = { navigator?.push(PlanDetailsScreen(planId = plan.id)) },
                                    onDeleteClick = if (canDeletePlan) {
                                        { planToDelete = plan }
                                    } else {
                                        null
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                }
            }
        }
    }
}
