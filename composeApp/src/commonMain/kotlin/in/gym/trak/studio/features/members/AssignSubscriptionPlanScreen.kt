package `in`.gym.trak.studio.features.members

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.data.model.AssignMemberPlanRequest
import `in`.gym.trak.studio.features.plans.AddPlanScreen
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import kotlin.jvm.Transient

/**
 * Assign subscription to a member — same navigation entry as before; UI lives in
 * [AssignSubscriptionContent].
 */
class AssignSubscriptionPlanScreen(
    private val memberGymUserId: String,
    private val memberName: String = "",
    private val memberImageUrl: String = "",
    private val membershipType: String = "",
    private val memberPhone: String = "",
    private val telUri: String? = null,
    private val whatsappUrl: String? = null,
    @Transient private val onSubscriptionCreated: (() -> Unit)? = null,
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val plans by screenModel.plans.collectAsState()
        val plansLoading by screenModel.plansLoading.collectAsState()
        var assignSubmitting by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            screenModel.loadPlans(showGlobalLoader = false)
        }

        LoadingScreenHandler(screenModel = screenModel) {
            AssignSubscriptionContent(
                onBackClick = { if (!assignSubmitting) navigator?.pop() },
                memberName = memberName,
                memberImageUrl = memberImageUrl,
                membershipType = membershipType,
                memberPhone = memberPhone,
                telUri = telUri,
                whatsappUrl = whatsappUrl,
                plans = plans,
                plansLoading = plansLoading,
                isSubmitting = assignSubmitting,
                onSubmit = { planId, startDateIso ->
                    if (memberGymUserId.isBlank()) return@AssignSubscriptionContent
                    assignSubmitting = true
                    screenModel.assignMemberPlan(
                        request = AssignMemberPlanRequest(
                            member_id = memberGymUserId,
                            gym_plan_id = planId,
                            start_date = startDateIso,
                            discount = 0,
                        ),
                        onSuccess = {
                            assignSubmitting = false
                            onSubscriptionCreated?.invoke()
                            navigator?.pop()
                        },
                        showGlobalLoader = false,
                        onAfterFailure = { assignSubmitting = false },
                    )
                },
                onCreatePlanClick = {
                    navigator?.push(
                        AddPlanScreen(
                            limitPlanTypes = false,
                            onPlanSaved = { screenModel.loadPlans(showGlobalLoader = false) }
                        )
                    )
                },
            )
        }
    }
}
