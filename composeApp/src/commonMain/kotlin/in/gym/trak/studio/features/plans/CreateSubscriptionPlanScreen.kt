package `in`.gym.trak.studio.features.plans

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.core.model.rememberScreenModel
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import `in`.gym.trak.studio.components.AppScrollDefaults
import `in`.gym.trak.studio.components.AppScrollableScreen
import `in`.gym.trak.studio.components.*
import `in`.gym.trak.studio.data.model.CreateSubscriptionWithBodyRequest
import `in`.gym.trak.studio.data.repository.SessionManager.userId
import `in`.gym.trak.studio.theme.*
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_minus
import gym.composeapp.generated.resources.ic_right
import org.jetbrains.compose.resources.painterResource
import `in`.gym.trak.studio.base.Constants

/**
 * Screen to customize and create a new membership/subscription plan.
 */
class CreateSubscriptionPlanScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        
        var planName by remember { mutableStateOf("") }
        var billingCycle by remember { mutableStateOf("Month") }
        var price by remember { mutableStateOf("") }
        var discount by remember { mutableStateOf("") }
        var newFeature by remember { mutableStateOf("") }

        val features =
            remember { mutableStateListOf("Unlimited access to gym", "Free locker access") }

        Scaffold(
            topBar = {
                GymAppBar(
                    title = "Create New Subscription Plan",
                    onBackClick = { navigator?.pop() }
                )
            },
            containerColor = androidx.compose.ui.graphics.Color.Transparent
        ) { padding ->
            AppScrollableScreen(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = AppScrollDefaults.bottomContentPadding),
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Header Headline
                StaggeredEntranceItem(index = 0) {
                    Text(
                        text = "Customize Your Offer",
                        style = AppTextTheme.semiBold.copy(fontSize = 24.sp),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                StaggeredEntranceItem(index = 1) {
                    Text(
                        text = "Create A Pack That Stands Out",
                        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Plan Name Field
                StaggeredEntranceItem(index = 2) {
                    Column {
                        Text(
                            text = "Plan Name",
                            style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        CommonTextField(
                            value = planName,
                            onValueChange = { planName = it },
                            placeholder = "e.g. Gold Membership",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Price and Discount Section
                StaggeredEntranceItem(index = 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Price",
                                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            CommonTextField(
                                value = price,
                                onValueChange = { price = it },
                                placeholder = "${Constants.RUPEE} 0.00",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Discount (%)",
                                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            CommonTextField(
                                value = if (discount.isEmpty()) "" else "$discount%",
                                onValueChange = { newValue ->
                                    val digitsOnly = newValue.filter { it.isDigit() }
                                    if (digitsOnly.length <= 2) {
                                        discount = digitsOnly
                                    }
                                },
                                placeholder = "0%",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Billing Cycle
                StaggeredEntranceItem(index = 4) {
                    Column {
                        Text(
                            text = "Billing Cycle",
                            style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        CycleToggle(
                            option1 = "Month",
                            option2 = "Year",
                            selectedOption = billingCycle,
                            onOptionSelected = { billingCycle = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Features Section
                StaggeredEntranceItem(index = 5) {
                    Column {
                        Text(
                            text = "Plan Features",
                            style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        features.forEachIndexed { index, feature ->
                            FeatureRow(
                                feature = feature,
                                onRemove = { features.removeAt(index) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Add new feature input
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = newFeature,
                                onValueChange = { newFeature = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                textStyle = AppTextTheme.regular.copy(fontSize = 14.sp),
                                decorationBox = { innerTextField ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .padding(horizontal = 12.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        if (newFeature.isEmpty()) {
                                            Text(
                                                "Add a feature...",
                                                style = AppTextTheme.regular.copy(
                                                    fontSize = 14.sp,
                                                    color = Gray
                                                )
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            IconButton(
                                onClick = {
                                    if (newFeature.isNotBlank()) {
                                        features.add(newFeature)
                                        newFeature = ""
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Add",
                                    tint = PrimaryColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Publish Button
                StaggeredEntranceItem(index = 6) {
                    CommonButton(
                        onClick = {
                            val request = CreateSubscriptionWithBodyRequest(
                                plan_name = planName,
                                plan_discount = discount.toIntOrNull() ?: 0,
                                plan_description = "Gym Plan",
                                billing_cycle = billingCycle,
                                price = price.toIntOrNull() ?: 0,
                                plan_features = features.toList(),
                                member_id = userId
                            )
                            screenModel.createSubscriptionWithBody(request) {
                                navigator?.pop()
                            }
                        },
                        text = "Publish Plan",
                        modifier = Modifier.fillMaxWidth(),
                        rightIcon = painterResource(Res.drawable.ic_right)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun FeatureRow(feature: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(Res.drawable.ic_right),
                contentDescription = null,
                tint = PrimaryColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = feature, style = AppTextTheme.regular.copy(fontSize = 14.sp))
        }
        IconButton(onClick = onRemove) {
            Icon(
                painter = painterResource(Res.drawable.ic_minus), // Assuming this exists or replace with default
                contentDescription = "Remove",
                tint = RedColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
