package `in`.gym.trak.studio.features.members

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import `in`.gym.trak.studio.components.AuthHeader
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.PlanCard
import `in`.gym.trak.studio.theme.*
import gym.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import `in`.gym.trak.studio.base.Constants
import `in`.gym.trak.studio.components.GymAppBar

/**
 * Hub for managing both Gym Membership Plans and Gym Shop Subscriptions.
 */
class MemberSubscriptionScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        var isAnnual by remember { mutableStateOf(true) }
        var selectedPlanIndex by remember { mutableStateOf(0) }

        val plans = listOf(
            SubscriptionPlan(
                tier = "Silver",
                price = if (isAnnual) "${Constants.RUPEE} 19" else "${Constants.RUPEE} 25",
                period = "month",
                description = "AI-powered workouts, nutrition insights, and complete progress tracking.",
                savingsLabel = "15%",
                bgColor = DarkBlue,
                edgeColor = Color(0xFF475569),
                icon = Res.drawable.ic_silver,
                features = listOf("Standard Gym Access", "Basic workout plans", "Nutrition guidance")
            ),
            SubscriptionPlan(
                tier = "Gold",
                price = if (isAnnual) "${Constants.RUPEE} 29" else "${Constants.RUPEE} 39",
                period = "month",
                description = "AI-powered workouts, nutrition insights, and complete progress tracking.",
                savingsLabel = "20%",
                bgColor = Color(0xFF1E293B),
                edgeColor = YellowColor,
                icon = Res.drawable.ic_gold,
                features = listOf("Unlimited Gym Access", "Premium workout plans", "Personal Trainer")
            )
        )

        Scaffold(
            topBar = {
                GymAppBar(
                    title = "",
                    onBackClick = { navigator?.pop() }
                )
//                CenterAlignedTopAppBar(
//                    title = { },
//                    navigationIcon = {
//                        IconButton(onClick = { navigator?.pop() }) {
//                            Row(verticalAlignment = Alignment.CenterVertically) {
//                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
//                            }
//                        }
//                    },
//                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
//                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            AppScrollableScreen(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "Choose Your Path",
                        style = AppTextTheme.bold.copy(fontSize = 24.sp, color = DarkBlue)
                    )
                    Text(
                        text = "select a plan that fits your fitness jouney and goals.",
                        style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Pricing Toggle Header
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pricing",
                        style = AppTextTheme.bold.copy(fontSize = 20.sp, color = DarkBlue)
                    )

                    // Annual/Monthly Toggle
                    Surface(
                        modifier = Modifier
                            .height(40.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xFFF1F5F9)),
                        color = Color(0xFFF1F5F9),
                        shape = RoundedCornerShape(100.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PricingTab(
                                text = "Annual",
                                isSelected = isAnnual,
                                onClick = { isAnnual = true }
                            )
                            PricingTab(
                                text = "Monthly",
                                isSelected = !isAnnual,
                                onClick = { isAnnual = false }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Carousel of Plans
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    plans.forEachIndexed { index, plan ->
                        Box(modifier = Modifier.width(320.dp)) {
                            PlanCard(
                                tier = plan.tier,
                                price = "${plan.price}/",
                                description = plan.description,
                                savingsLabel = plan.savingsLabel,
                                icon = plan.icon,
                                bgColor = plan.bgColor,
                                edgeColor = plan.edgeColor,
                                features = plan.features,
                                onSelect = { selectedPlanIndex = index }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pagination Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 16.dp)
                ) {
                    plans.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(width = if (selectedPlanIndex == index) 24.dp else 8.dp, height = 8.dp)
                                .clip(CircleShape)
                                .background(if (selectedPlanIndex == index) PrimaryColor else Color.Gray.copy(alpha = 0.5f))
                        )
                    }
                }

                Text(
                    text = "subscription automatically renews, cancel\nanytime in settings.",
                    style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Gray),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "By continuing you agree to our Terms of Service.",
                    style = AppTextTheme.medium.copy(fontSize = 13.sp, color = Gray),
                    modifier = Modifier.clickable { /* Logic */ }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Selected Plan Summary
                val selectedPlan = plans[selectedPlanIndex]
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFB)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF1F5F9))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Selected Plan", style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${selectedPlan.tier} plan",
                                    style = AppTextTheme.bold.copy(fontSize = 18.sp, color = DarkBlue)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "${selectedPlan.price}/${selectedPlan.period}",
                                    style = AppTextTheme.bold.copy(fontSize = 18.sp, color = DarkBlue)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(PrimaryColor.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "save ${selectedPlan.savingsLabel}",
                                style = AppTextTheme.bold.copy(fontSize = 12.sp, color = PrimaryColor)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                CommonButton(
                    onClick = { /* Payment Flow */ },
                    text = "Continue To Payment",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    @Composable
    private fun PricingTab(text: String, isSelected: Boolean, onClick: () -> Unit) {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .clickable { onClick() },
            color = if (isSelected) PrimaryColor else Color.Transparent,
            shape = RoundedCornerShape(100.dp)
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text(
                    text = text,
                    style = AppTextTheme.bold.copy(
                        fontSize = 14.sp,
                        color = if (isSelected) White else Gray
                    )
                )
            }
        }
    }
}

data class SubscriptionPlan(
    val tier: String,
    val price: String,
    val period: String,
    val description: String,
    val savingsLabel: String,
    val bgColor: Color,
    val edgeColor: Color,
    val icon: DrawableResource,
    val features: List<String>
)
