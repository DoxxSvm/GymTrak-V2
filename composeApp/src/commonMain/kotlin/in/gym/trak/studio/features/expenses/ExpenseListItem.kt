package `in`.gym.trak.studio.features.expenses

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.theme.*
import `in`.gym.trak.studio.utils.DateUtils
import gym.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun ExpenseListItem(
    name: String,
    category: String,
    date: String,
    amount: String,
    icon: DrawableResource,
    iconBgColor: Color = Color(0xFFF1F5F9)
) {
    CommonCard(

        content = {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = name, style = AppTextTheme.semiBold.copy(fontSize = 14.sp, color = Black))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = category, style = AppTextTheme.regular.copy(fontSize = 12.sp, color = PrimaryColor))
                        Text(text = "  •  ", style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Black.copy(alpha = 0.80f)))
                        Text(text = date, style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Black.copy(alpha = 0.80f)))
                    }
                }
                Text(text = amount, style = AppTextTheme.semiBold.copy(fontSize = 14.sp, color = Black))
            }

        }
    )
}
