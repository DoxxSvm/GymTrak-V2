package `in`.gym.trak.studio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.AppTextTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * A tab chip with an icon and label, used in member and trainer detail screens.
 */
@Composable
fun DetailTabChip(
    label: String,
    icon: DrawableResource,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) PrimaryColor else White,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFFF1F5F9)
        ),
        modifier = Modifier.height(48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) White else Color(0xFFFFF7E6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = AppTextTheme.bold.copy(
                    fontSize = 14.sp,
                    color = if (isSelected) White else Color.Black
                )
            )
        }
    }
//    Surface(
//        onClick = onClick,
//        shape = RoundedCornerShape(24.dp),
//        color = if (isSelected) PrimaryColor else White,
//        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
//            1.dp,
//            Color(0xFFF1F5F9)
//        ),
//        modifier = modifier.height(48.dp)
//    ) {
//        Row(
//            verticalAlignment = Alignment.CenterVertically,
//            modifier = Modifier.padding(horizontal = 16.dp)
//        ) {
//            Box(
//                modifier = Modifier
//                    .size(32.dp)
//                    .clip(CircleShape)
//                    .background(if (isSelected) White else Color(0xFFFFF7E6)),
//                contentAlignment = Alignment.Center
//            ) {
//                Icon(
//                    painter = painterResource(icon),
//                    contentDescription = null,
//                    tint = if (isSelected) PrimaryColor else Color(0xFFD48B45),
//                    modifier = Modifier.size(16.dp)
//                )
//            }
//            Spacer(modifier = Modifier.width(8.dp))
//            Text(
//                text = label,
//                style = AppTextTheme.bold.copy(
//                    fontSize = 14.sp,
//                    color = if (isSelected) White else Gray
//                )
//            )
//        }
//    }
}
