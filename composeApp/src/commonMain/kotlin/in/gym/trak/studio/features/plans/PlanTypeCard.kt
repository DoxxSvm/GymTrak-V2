package `in`.gym.trak.studio.features.plans

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.OffGreenColor
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * A selectable card representing a type of gym plan.
 */
@Composable
fun PlanTypeCard(
    label: String,
    icon: DrawableResource,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    CommonCard(
        modifier = modifier,
        content = {
            Box(
                modifier = modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) OffGreenColor else White)
                    .border(
                        1.dp,
                        if (isSelected) PrimaryColor else PrimaryColor,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable(enabled = enabled) { onClick() }
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = PrimaryColor,
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.TopEnd)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        painter = painterResource(icon),
                        contentDescription = null,
                        tint = PrimaryColor,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = label,
                        style = AppTextTheme.bold.copy(fontSize = 14.sp, color = Black)
                    )
                }
            }
        }
    )

}
