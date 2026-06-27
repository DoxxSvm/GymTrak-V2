package `in`.gym.trak.studio.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.*
import gym.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun PlanCard(
    tier: String,
    price: String,
    icon: DrawableResource,
    bgColor: Color,
    edgeColor: Color = Color(0xFFA0AEC0),
    description: String = "For Small Studios",
    savingsLabel: String = "15%",
    periodSuffix: String = "month",
    selectButtonText: String = "Select",
    features: List<String>,
    onSelect: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(40.dp))
            .background(edgeColor)
            .padding(8.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(bgColor)
    ) {
        // Savings Corner
        Box(
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(bottomStart = 100.dp))
                .background(Color.White)
                .padding(top = 16.dp, end = 16.dp),
            contentAlignment = Alignment.TopEnd
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "save",
                    style = AppTextTheme.semiBold.copy(fontSize = 12.sp, color = DarkBlue)
                )
                Text(
                    text = savingsLabel,
                    style = AppTextTheme.bold.copy(fontSize = 14.sp, color = DarkBlue)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header with Icon and Name
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier = Modifier.size(60.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = White.copy(alpha = 0.2f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(icon),
                            contentDescription = null,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = tier,
                    style = AppTextTheme.bold.copy(fontSize = 24.sp, color = White)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Price
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = price,
                    style = AppTextTheme.bold.copy(fontSize = 32.sp, color = White)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = periodSuffix,
                    style = AppTextTheme.medium.copy(fontSize = 14.sp, color = White.copy(alpha = 0.6f)),
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                style = AppTextTheme.regular.copy(fontSize = 14.sp, color = White.copy(alpha = 0.7f)),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = White.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 8.dp), thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // Features
            features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_active_status),
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = feature,
                        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = White.copy(alpha = 0.9f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Select Button
            Surface(
                onClick = onSelect,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(100.dp),
                color = White
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = selectButtonText,
                        style = AppTextTheme.bold.copy(fontSize = 16.sp, color = DarkBlue),
                    )
                }
            }
        }
    }
}
