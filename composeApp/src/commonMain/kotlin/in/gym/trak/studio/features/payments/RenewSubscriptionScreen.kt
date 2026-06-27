package `in`.gym.trak.studio.features.payments

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import `in`.gym.trak.studio.components.*
import `in`.gym.trak.studio.theme.*

/**
 * Full-screen flow for renewing a member's existing subscription.
 * Shows current plan info, editable validity period (start / expiry),
 * financials with an editable price override, and total payable.
 */
class RenewSubscriptionScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        RenewSubscriptionContent(onBackClick = { navigator?.pop() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenewSubscriptionContent(onBackClick: () -> Unit = {}) {
    var startDate by remember { mutableStateOf("Oct 24,2023") }
    var expiryDate by remember { mutableStateOf("Oct 24,2023") }
    var customPrice by remember { mutableStateOf("1050.00") }
    val standardPrice = "$1200.00"
    val totalPayable = "$ ${customPrice.toDoubleOrNull()?.let { it } ?: 0.0}"

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Renew Subscription",
                        style = AppTextTheme.bold.copy(fontSize = 18.sp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                // Total Payable row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Payable",
                        style = AppTextTheme.medium.copy(fontSize = 15.sp, color = Gray)
                    )
                    Text(
                        text = totalPayable,
                        style = AppTextTheme.bold.copy(fontSize = 22.sp, color = Black)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                CommonButton(
                    onClick = { /* confirm renewal */ },
                    text = "Confirm Renewal"
                )
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        AppScrollableScreen(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Member header card (compact variant for renewal)
            MemberHeaderCard(
                imageUrl = "",
                name = "John Den",
                membershipType = "Member Since Oct 2025"
            )

            // Current subscription info card
            SubscriptionInfoCard()

            // Validity Period Section
            SectionLabel(text = "Validity Period")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Start Date column
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Start Date",
                                style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            DatePickerField(
                                value = startDate,
                                onPickerClick = { /* open picker */ }
                            )
                        }
                        // Expiry Date column
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Expiry Date",
                                style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            DatePickerField(
                                value = expiryDate,
                                onPickerClick = { /* open picker */ }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Auto Calculated Base On Start Date",
                        style = AppTextTheme.regular.copy(
                            fontSize = 11.sp,
                            color = PrimaryColor
                        )
                    )
                }
            }

            // Financials Section
            SectionLabel(text = "Financials")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Standard Base Price",
                            style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Gray)
                        )
                        Text(
                            text = standardPrice,
                            style = AppTextTheme.medium.copy(
                                fontSize = 14.sp,
                                color = Gray,
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    AmountInputField(
                        value = customPrice,
                        onValueChange = { customPrice = it },
                        prefix = "$",
                        placeholder = "0.00"
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
