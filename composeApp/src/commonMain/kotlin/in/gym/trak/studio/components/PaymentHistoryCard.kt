package `in`.gym.trak.studio.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.base.Constants
import `in`.gym.trak.studio.data.model.MemberPaymentHistoryEntryDTO
import `in`.gym.trak.studio.data.model.PaymentItemDTO
import `in`.gym.trak.studio.data.model.resolvedPaymentMethodLabel
import `in`.gym.trak.studio.data.model.resolvedPlanTitle
import `in`.gym.trak.studio.data.model.resolvedReceivedBy
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.PrimaryGreenColor
import `in`.gym.trak.studio.utils.DateUtils
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_share
import org.jetbrains.compose.resources.painterResource

@Composable
fun PaymentHistoryCard(
    planTitle: String,
    paymentMethod: String,
    dateLabel: String,
    receivedBy: String,
    amount: Int,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    memberName: String? = null,
    memberPhone: String? = null,
) {
    CommonCard(
        modifier = modifier,
        content = {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = planTitle,
                            style = AppTextTheme.semiBold.copy(fontSize = 16.sp),
                        )
                        if (!memberName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = memberName,
                                style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Gray),
                            )
                        }
                        if (!memberPhone.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = memberPhone,
                                style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray.copy(alpha = 0.85f)),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(onClick = onShare),
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_share),
                            contentDescription = "Share payment",
                            modifier = Modifier.size(20.dp),
                            tint = Gray,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = PrimaryGreenColor.copy(alpha = 0.20f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = paymentMethod,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Black),
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  $dateLabel",
                        style = AppTextTheme.regular.copy(
                            fontSize = 13.sp,
                            color = Black.copy(alpha = 0.9f),
                        ),
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Received by: ",
                            style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray),
                        )
                        Text(
                            text = receivedBy,
                            style = AppTextTheme.bold.copy(
                                fontSize = 14.sp,
                                color = PrimaryColor,
                            ),
                        )
                    }
                    Text(
                        text = "${Constants.RUPEE} $amount",
                        style = AppTextTheme.bold.copy(fontSize = 18.sp, color = PrimaryColor),
                    )
                }
            }
        },
    )
}

fun MemberPaymentHistoryEntryDTO.toPaymentHistoryCardModel(
    memberName: String? = null,
    memberPhone: String? = null,
    dateFormatter: (String) -> String = { raw ->
        DateUtils.formatBirthDateForDisplay(raw).ifBlank { raw }
    },
): PaymentHistoryCardModel = PaymentHistoryCardModel(
    planTitle = gymPlanName?.takeIf { it.isNotBlank() } ?: "Payment",
    memberName = memberName?.takeIf { it.isNotBlank() },
    memberPhone = memberPhone?.takeIf { it.isNotBlank() },
    paymentMethod = paymentMethod?.takeIf { it.isNotBlank() } ?: "—",
    dateLabel = date?.let(dateFormatter)?.takeIf { it.isNotBlank() } ?: "—",
    receivedBy = receivedBy?.takeIf { it.isNotBlank() } ?: "—",
    amount = amount ?: 0,
)

fun PaymentItemDTO.toPaymentHistoryCardModel(): PaymentHistoryCardModel {
    val rawDate = completedAt ?: createdAt
    val formattedDate = rawDate?.substringBefore("T")?.let { datePart ->
        DateUtils.formatBirthDateForDisplay(datePart).ifBlank { datePart }
    } ?: "—"
    return PaymentHistoryCardModel(
        planTitle = resolvedPlanTitle(),
        memberName = memberUser?.fullName?.takeIf { it.isNotBlank() },
        memberPhone = memberUser?.phone?.takeIf { it.isNotBlank() },
        paymentMethod = resolvedPaymentMethodLabel(),
        dateLabel = formattedDate,
        receivedBy = resolvedReceivedBy(),
        amount = amountCents,
    )
}

data class PaymentHistoryCardModel(
    val planTitle: String,
    val paymentMethod: String,
    val dateLabel: String,
    val receivedBy: String,
    val amount: Int,
    val memberName: String? = null,
    val memberPhone: String? = null,
)

@Composable
fun PaymentHistoryCard(
    model: PaymentHistoryCardModel,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PaymentHistoryCard(
        planTitle = model.planTitle,
        paymentMethod = model.paymentMethod,
        dateLabel = model.dateLabel,
        receivedBy = model.receivedBy,
        amount = model.amount,
        memberName = model.memberName,
        memberPhone = model.memberPhone,
        onShare = onShare,
        modifier = modifier,
    )
}
