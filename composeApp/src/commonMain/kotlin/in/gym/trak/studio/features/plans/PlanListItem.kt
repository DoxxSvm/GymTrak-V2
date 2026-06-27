package `in`.gym.trak.studio.features.plans

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.base.Constants
import `in`.gym.trak.studio.data.model.PlanDTO
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.CommonOutlineButton
import `in`.gym.trak.studio.theme.RedColor
import `in`.gym.trak.studio.theme.*
import gym.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * A card representing a gym plan in a list, as shown in Image 4.
 */
@Composable
fun PlanListItem(
    modifier: Modifier = Modifier,
    name: String,
    planType: String,
    duration: String,
    clients: String,
    price: String,
    primaryButtonText: String = "View Details",
    enabled: Boolean = true,
    onViewDetails: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
) {
    CommonCard(
        modifier = modifier,
        content = {
            Box {
                if (onDeleteClick != null) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete plan",
                            tint = RedColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Image(
                    painter = painterResource(Res.drawable.img_plan_bg),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )

                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = planType,
                        style = AppTextTheme.semiBold.copy(fontSize = 14.sp, color = PrimaryColor)
                    )
                    Text(
                        text = name,
                        style = AppTextTheme.semiBold.copy(fontSize = 16.sp, color = Black)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_clock),
                                    contentDescription = null,
                                    tint = Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = duration,
                                    style = AppTextTheme.regular.copy(
                                        fontSize = 14.sp,
                                        color = Gray
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_active_client),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$clients Active Clients",
                                    style = AppTextTheme.medium.copy(fontSize = 13.sp, color = Gray)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = price,
                                style = AppTextTheme.bold.copy(
                                    fontSize = 24.sp,
                                    color = PrimaryColor
                                )
                            )
                            Text(
                                text = "Per ${duration}",
                                style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    CommonOutlineButton(
                        onClick = onViewDetails,
                        text = primaryButtonText,
                        textColor = Black,
                        enabled = enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    )
                }
            }
        }
    )
}

fun PlanDTO.planListDurationLabel(): String =
    if (durationDays >= 30) "${durationDays / 30} Months" else "$durationDays Days"

fun PlanDTO.planTypeDisplayLabel(): String = when (type.uppercase()) {
    "GYM_MEMBERSHIP" -> "Gym Membership"
    "PT_PLAN" -> "Pt Plan"
    "BATCH_PLAN" -> "Batch Plan"
    else -> type.replace('_', ' ').trim()
        .lowercase()
        .replaceFirstChar { it.uppercaseChar() }
}

fun PlanDTO.planListPriceLabel(): String {
    val whole = priceCents / 100
    val fraction = (priceCents % 100).toString().padStart(2, '0')
    return "${Constants.RUPEE}$priceCents"
}

@Composable
fun PlanListRowFromDto(
    plan: PlanDTO,
    modifier: Modifier = Modifier,
    primaryButtonText: String = "View Details",
    enabled: Boolean = true,
    onPrimaryClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
) {
    PlanListItem(
        modifier = modifier,
        name = plan.name,
        planType = plan.planTypeDisplayLabel(),
        duration = plan.planListDurationLabel(),
        clients = "${plan.clientsCount}",
        price = plan.planListPriceLabel(),
        primaryButtonText = primaryButtonText,
        enabled = enabled,
        onViewDetails = onPrimaryClick,
        onDeleteClick = onDeleteClick,
    )
}
