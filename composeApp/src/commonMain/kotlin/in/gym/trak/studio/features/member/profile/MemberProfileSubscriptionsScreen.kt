package `in`.gym.trak.studio.features.member.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.base.Constants
import `in`.gym.trak.studio.components.AppEmptyStateView
import `in`.gym.trak.studio.components.AppScrollableScreen
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import `in`.gym.trak.studio.data.model.CurrentSubscriptionDTO
import `in`.gym.trak.studio.data.model.contractTotalAmount
import `in`.gym.trak.studio.data.model.hasSellingPrice
import `in`.gym.trak.studio.data.model.resolvedAmountPaid
import `in`.gym.trak.studio.data.model.resolvedAmountPending
import `in`.gym.trak.studio.features.members.SubscriptionStatColumn
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.GrayBorderColor
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.PrimaryDarkColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.theme.YellowColor
import `in`.gym.trak.studio.utils.DateUtils
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_active_status
import gym.composeapp.generated.resources.ic_king
import gym.composeapp.generated.resources.img_no_member_enrolled
import org.jetbrains.compose.resources.painterResource

/**
 * Member self-service view of active and upcoming gym subscriptions from profile API.
 */
class MemberProfileSubscriptionsScreen(
    private val currentSubscriptions: List<CurrentSubscriptionDTO> = emptyList(),
    private val currentSubscription: CurrentSubscriptionDTO? = null,
    private val upcomingSubscriptions: List<CurrentSubscriptionDTO> = emptyList(),
    private val expiredSubscriptions: List<CurrentSubscriptionDTO> = emptyList(),
    private val pastSubscriptions: List<CurrentSubscriptionDTO> = emptyList(),
    private val freezeSubscriptions: List<CurrentSubscriptionDTO> = emptyList(),
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val activePlans = currentSubscriptions.ifEmpty {
            listOfNotNull(currentSubscription)
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                GymAppBar(
                    title = "My Subscriptions",
                    onBackClick = { navigator.pop() },
                )
            },
        ) { padding ->
            AppScrollableScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            ) {
                if (activePlans.isEmpty() &&
                    upcomingSubscriptions.isEmpty() &&
                    expiredSubscriptions.isEmpty() &&
                    pastSubscriptions.isEmpty() &&
                    freezeSubscriptions.isEmpty()
                ) {
                    AppEmptyStateView(
                        image = Res.drawable.img_no_member_enrolled,
                        title = "No Subscription Records",
                        subtitle = "You do not have any active or upcoming subscriptions yet.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                    )
                    return@AppScrollableScreen
                }

                if (activePlans.isNotEmpty()) {
                    StaggeredEntranceItem(index = 0) {
                        SectionHeader(
                            title = if (activePlans.size == 1) "Current Subscription" else "Current Subscriptions",
                            chipText = "${activePlans.size} Active",
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    activePlans.forEachIndexed { index, subscription ->
                        StaggeredEntranceItem(index = index + 1) {
                            ProfileSubscriptionCard(
                                subscription = subscription,
                                statusLabel = "Active",
                                statusAccent = Black,
                                showActiveBadge = true,
                            )
                        }
                        if (index < activePlans.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (freezeSubscriptions.isNotEmpty()) {
                    StaggeredEntranceItem(index = activePlans.size + 1) {
                        SectionHeader(
                            title = "Frozen Subscriptions",
                            chipText = "${freezeSubscriptions.size} Plan${if (freezeSubscriptions.size == 1) "" else "s"}",
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    freezeSubscriptions.forEachIndexed { index, subscription ->
                        StaggeredEntranceItem(index = activePlans.size + index + 2) {
                            ProfileSubscriptionCard(
                                subscription = subscription,
                                statusLabel = "Frozen",
                                statusAccent = Gray,
                            )
                        }
                        if (index < freezeSubscriptions.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (upcomingSubscriptions.isNotEmpty()) {
                    StaggeredEntranceItem(index = 2) {
                        SectionHeader(
                            title = "Upcoming Subscriptions",
                            chipText = "${upcomingSubscriptions.size} Plan${if (upcomingSubscriptions.size == 1) "" else "s"}",
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    upcomingSubscriptions.forEachIndexed { index, subscription ->
                        StaggeredEntranceItem(index = index + 3) {
                            ProfileSubscriptionCard(
                                subscription = subscription,
                                statusLabel = "Upcoming",
                                statusAccent = PrimaryColor,
                            )
                        }
                        if (index < upcomingSubscriptions.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                val expiredOrPast = (expiredSubscriptions + pastSubscriptions).distinctBy {
                    it.member_subscription_id ?: "${it.plan_name}-${it.start_date}"
                }
                if (expiredOrPast.isNotEmpty()) {
                    StaggeredEntranceItem(index = activePlans.size + upcomingSubscriptions.size + 2) {
                        SectionHeader(
                            title = "Past Subscriptions",
                            chipText = "${expiredOrPast.size} Plan${if (expiredOrPast.size == 1) "" else "s"}",
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    expiredOrPast.forEachIndexed { index, subscription ->
                        StaggeredEntranceItem(index = activePlans.size + upcomingSubscriptions.size + index + 3) {
                            ProfileSubscriptionCard(
                                subscription = subscription,
                                statusLabel = "Expired",
                                statusAccent = Gray,
                            )
                        }
                        if (index < expiredOrPast.lastIndex) {
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    chipText: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = AppTextTheme.bold.copy(fontSize = 18.sp, color = Black),
        )
        Surface(
            color = PrimaryColor,
            shape = RoundedCornerShape(100.dp),
        ) {
            Text(
                text = chipText,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = AppTextTheme.regular.copy(fontSize = 12.sp, color = White),
            )
        }
    }
}

@Composable
private fun ProfileSubscriptionCard(
    subscription: CurrentSubscriptionDTO,
    statusLabel: String,
    statusAccent: Color,
    showActiveBadge: Boolean = false,
) {
    CommonCard(
        content = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Gray.copy(alpha = 0.02f)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_king),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = subscription.plan_name.ifBlank { "Subscription" },
                                style = AppTextTheme.medium.copy(fontSize = 14.sp),
                            )
                            Text(
                                text = profileSubscriptionDateRange(
                                    subscription.start_date,
                                    subscription.expiry_date,
                                ),
                                style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray),
                            )
                            val remainingDays = subscription.remaining_days ?: 0
                            if (remainingDays > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$remainingDays days remaining",
                                    style = AppTextTheme.regular.copy(fontSize = 11.sp, color = Gray),
                                )
                            }
                        }
                        if (showActiveBadge) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_active_status),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(10.dp),
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = statusLabel,
                                    style = AppTextTheme.medium.copy(fontSize = 12.sp, color = statusAccent),
                                )
                            }
                        } else {
                            Text(
                                text = statusLabel,
                                style = AppTextTheme.medium.copy(fontSize = 12.sp, color = statusAccent),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = GrayBorderColor)
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        SubscriptionStatColumn(
                            label = if (subscription.hasSellingPrice()) "Selling Price" else "Total Contract",
                            value = "${Constants.RUPEE} ${subscription.contractTotalAmount()}",
                            valueColor = Black,
                        )
                        SubscriptionStatColumn(
                            label = "Paid",
                            value = "${Constants.RUPEE} ${subscription.resolvedAmountPaid()}",
                            valueColor = PrimaryDarkColor,
                        )
                        SubscriptionStatColumn(
                            label = "Pending",
                            value = "${Constants.RUPEE} ${subscription.resolvedAmountPending()}",
                            valueColor = YellowColor,
                        )
                    }
                }
            }
        },
    )
}

private fun profileSubscriptionDateRange(startIso: String, endIso: String): String {
    val start = DateUtils.formatBirthDateForDisplay(startIso)
    val end = DateUtils.formatBirthDateForDisplay(endIso)
    return when {
        start.isNotBlank() && end.isNotBlank() -> "$start to $end"
        start.isNotBlank() -> start
        end.isNotBlank() -> end
        else -> "--"
    }
}
