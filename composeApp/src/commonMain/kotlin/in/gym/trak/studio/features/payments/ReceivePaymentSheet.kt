package `in`.gym.trak.studio.features.payments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.base.Constants
import `in`.gym.trak.studio.components.AppScrollableSheetColumn
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonDropdown
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.DatePickerField
import `in`.gym.trak.studio.components.PaymentModeSelector
import `in`.gym.trak.studio.components.SectionLabel
import `in`.gym.trak.studio.data.model.PlanDTO
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.utils.DateUtils
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** When set, the sheet collects payment for this subscription (no plan dropdown). */
data class ReceivePaymentLockedContext(
    val gymPlanId: String,
    val memberSubscriptionId: String,
    val planDisplayName: String,
    val defaultAmountRupee: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceivePaymentSheet(
    plans: List<PlanDTO>,
    plansLoading: Boolean,
    lockedContext: ReceivePaymentLockedContext? = null,
    isSubmittingPayment: Boolean = false,
    onDismiss: () -> Unit,
    onCreatePlan: () -> Unit = {},
    onReceivePayment: (
        memberSubscriptionId: String,
        gymPlanId: String,
        amount: String,
        mode: String,
        paymentDateStored: String,
    ) -> Unit = { _, _, _, _, _ -> },
) {
    var selectedPlanIndex by remember(plans) { mutableStateOf(0) }
    val currentPlan = plans.getOrNull(selectedPlanIndex)

    var amount by remember { mutableStateOf("0") }
    LaunchedEffect(lockedContext, plans, selectedPlanIndex) {
        amount = when {
            lockedContext != null -> lockedContext.defaultAmountRupee.coerceAtLeast(0).toString()
            else -> currentPlan?.priceCents?.toString() ?: "0"
        }
    }

    var selectedMode by remember { mutableStateOf("Cash") }
    var paymentDate by remember { mutableStateOf(DateUtils.getCurrentDateIso()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState()

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        val instant = Instant.fromEpochMilliseconds(it)
                        paymentDate = instant.toString()
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color.White).padding(
                vertical = 24.dp,
                horizontal = 16.dp
            )
            .padding(bottom = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(48.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color(0xFFE0E0E0))
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Collect Payment",
                style = AppTextTheme.semiBold.copy(fontSize = 16.sp)
            )
            IconButton(
                onClick = onDismiss,
                enabled = !isSubmittingPayment,
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF1F1F1))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Black,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(modifier = Modifier.fillMaxWidth()) {
            when {
                plansLoading && lockedContext == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PrimaryColor)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Loading plans…",
                                style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray)
                            )
                        }
                    }
                }

                plans.isEmpty() && lockedContext == null -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "No subscription plans found. Create a plan to collect payments.",
                            style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray)
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        CommonButton(
                            onClick = onCreatePlan,
                            enabled = !isSubmittingPayment,
                            text = "Create Plan",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                else -> {
                    AppScrollableSheetColumn {
                        if (lockedContext != null) {
                            SectionLabel(text = "Plan")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = lockedContext.planDisplayName,
                                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black)
                            )
                        } else {
                            SectionLabel(text = "Plan")
                            Spacer(modifier = Modifier.height(8.dp))

                            CommonDropdown(
                                options = plans,
                                selectedOption = currentPlan,
                                onOptionSelected = { plan ->
                                    val index = plans.indexOf(plan)
                                    if (index != -1) {
                                        selectedPlanIndex = index
                                        amount = plan.priceCents.toString()
                                    }
                                },
                                placeholder = "Select plan",
                                optionToString = { plan ->
                                    val price = plan.priceCents
                                    "${plan.name} — $price ${Constants.RUPEE}"
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        SectionLabel(text = "Amount")
                        Spacer(modifier = Modifier.height(8.dp))
                        CommonTextField(
                            value = amount,
                            onValueChange = { amount = it },
                            placeholder = "Amount",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        SectionLabel(text = "Mode")
                        Spacer(modifier = Modifier.height(8.dp))
                        PaymentModeSelector(
                            selectedOption = selectedMode,
                            onOptionSelected = { selectedMode = it }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        SectionLabel(text = "Date")
                        Spacer(modifier = Modifier.height(8.dp))
                        DatePickerField(
                            value = try {
                                val instant = Instant.parse(paymentDate)
                                val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
                                "${localDateTime.dayOfMonth}/${localDateTime.monthNumber}/${localDateTime.year}"
                            } catch (e: Exception) {
                                paymentDate
                            },
                            onPickerClick = { showDatePicker = true }
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        val amountOk = amount.toIntOrNull()?.let { it > 0 } == true
                        val canSubmitLocked = lockedContext != null &&
                            lockedContext.memberSubscriptionId.isNotBlank() &&
                            lockedContext.gymPlanId.isNotBlank() &&
                            amountOk &&
                            !isSubmittingPayment
                        val canSubmitOpen = lockedContext == null &&
                            currentPlan != null &&
                            amountOk &&
                            !isSubmittingPayment

                        CommonButton(
                            enabled = canSubmitLocked || canSubmitOpen,
                            onClick = {
                                if (lockedContext != null) {
                                    onReceivePayment(
                                        lockedContext.memberSubscriptionId,
                                        lockedContext.gymPlanId,
                                        amount,
                                        selectedMode.lowercase(),
                                        paymentDate,
                                    )
                                } else {
                                    currentPlan?.let { plan ->
                                        onReceivePayment(
                                            "",
                                            plan.id,
                                            amount,
                                            selectedMode.lowercase(),
                                            paymentDate,
                                        )
                                    }
                                }
                            },
                            text = "Receive Payment"
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }

            if (isSubmittingPayment) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.82f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PrimaryColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Recording payment…",
                            style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray)
                        )
                    }
                }
            }
        }
    }
}
