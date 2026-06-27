package `in`.gym.trak.studio.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.AppTextTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun CountryCodePicker(
    modifier: Modifier = Modifier,
    code: String = "+91",
    onClick: () -> Unit = {},
    flagDrawable: DrawableResource? = null
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .width(100.dp)
            .background(Color(0xFFF7F8F9), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFE8ECF4), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (flagDrawable != null) {
                Image(
                    painter = painterResource(flagDrawable),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp, 16.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                // Fallback: Indian flag with colored stripes
                Column(
                    modifier = Modifier
                        .size(24.dp, 16.dp)
                        .border(0.5.dp, Color.LightGray)
                ) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFFFF9933)))
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color.White))
                    Box(modifier = Modifier.weight(1f).fillMaxWidth().background(Color(0xFF138808)))
                }
            }
            
            Spacer(modifier = Modifier.width(6.dp))
            
            Text(
                text = code,
                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Color.Black)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = "▼",
                style = AppTextTheme.regular.copy(fontSize = 8.sp, color = Color.Gray)
            )
        }
    }
}
