package `in`.gym.trak.studio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.GrayBorderColor
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.PrimaryGreenColor
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_gold
import org.jetbrains.compose.resources.painterResource

@Composable
fun SubscriptionInfoCard(
    planName: String = "Gold annual membership",
    duration: String = "12 Months",
    startDate: String = "Feb 24,2026",
    expiryDate: String = "Feb 24,2027",
    basePrice: String? = "$ 500",
    basePriceLabel: String = "Base Price",
    modifier: Modifier = Modifier
) {
    CommonCard(
        content = {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))

                    .padding(16.dp)
            ) {
                // Plan name row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // King icon badge

                    Icon(
                        painter = painterResource(Res.drawable.ic_gold),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = planName,
                        style = AppTextTheme.medium.copy(fontSize = 14.sp),
                        modifier = Modifier.weight(1f)
                    )
                    // Duration badge
                    Text(
                        text = duration,
                        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = PrimaryColor)
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = GrayBorderColor
                )

                // Dates row
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Start Date",
                            style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = startDate,
                            style = AppTextTheme.medium.copy(fontSize = 14.sp)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Expiry Date",
                            style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = expiryDate,
                            style = AppTextTheme.medium.copy(fontSize = 14.sp)
                        )
                    }
                }

                if (!basePrice.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryGreenColor.copy(alpha = 0.20f))
                            .border(
                                width = 1.dp,
                                color = PrimaryGreenColor.copy(alpha = 0.30f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = basePriceLabel,
                            style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Black.copy(alpha = 0.60f))
                        )
                        Text(
                            text = basePrice,
                            style = AppTextTheme.bold.copy(fontSize = 16.sp, color = PrimaryColor)
                        )
                    }
                }
            }
        }
    )
}
