package `in`.gym.trak.studio.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryGreenColor
import `in`.gym.trak.studio.theme.RedColor
import `in`.gym.trak.studio.theme.White
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_delete_outline
import gym.composeapp.generated.resources.ic_edit_outline
import org.jetbrains.compose.resources.painterResource

data class FoodItem(
    val name: String,
    val quantity: String
)

@Composable
fun DietPlanCard(
    mealName: String,
    caloriesLine: String,
    scheduleTime: String? = null,
    byLine: String? = null,
    icon: ImageVector = Icons.Default.WbSunny,
    iconBgColor: Color = Color(0xFFD1FAE5),
    iconTintColor: Color = Color(0xFF047857),
    foods: List<FoodItem>,
    onCardClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    showEditButton: Boolean = true,
    showDeleteButton: Boolean = false,
    modifier: Modifier = Modifier
) {
    val hasActions = showEditButton || showDeleteButton

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x14000000),
                spotColor = Color(0x14000000)
            )
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(iconBgColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = mealName,
                        tint = iconTintColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mealName,
                        style = AppTextTheme.bold.copy(fontSize = 18.sp, color = Color.Black)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = caloriesLine,
                        style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Gray)
                    )
                    scheduleTime?.takeIf { it.isNotBlank() }?.let { t ->
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = t,
                            style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray.copy(alpha = 0.85f))
                        )
                    }
                    byLine?.takeIf { it.isNotBlank() }?.let { by ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = by,
                            style = AppTextTheme.medium.copy(fontSize = 12.sp, color = PrimaryGreenColor)
                        )
                    }
                }

                if (hasActions) {
                    val editInteraction = remember { MutableInteractionSource() }
                    val deleteInteraction = remember { MutableInteractionSource() }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (showEditButton) {
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        onClick = onEditClick,
                                        indication = null,
                                        interactionSource = editInteraction
                                    ),
                                color = White,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_edit_outline),
                                        contentDescription = "Edit",
                                        tint = Color.Black,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                        if (showDeleteButton) {
                            Surface(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable(
                                        onClick = onDeleteClick,
                                        indication = null,
                                        interactionSource = deleteInteraction
                                    ),
                                color = White,
                                shape = CircleShape,
                                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(Res.drawable.ic_delete_outline),
                                        contentDescription = "Delete",
                                        tint = RedColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    foods.forEachIndexed { index, food ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = food.name,
                                style = AppTextTheme.medium.copy(fontSize = 15.sp, color = Color.Black),
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = food.quantity,
                                style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Color(0xFF475569)),
                                textAlign = TextAlign.End,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (index < foods.lastIndex) {
                            HorizontalDivider(
                                color = Color(0xFFF1F5F9),
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        }
    }
}
