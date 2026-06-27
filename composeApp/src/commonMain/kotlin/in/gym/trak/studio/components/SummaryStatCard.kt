package `in`.gym.trak.studio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.OffGreenColor
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * A small card displaying a summary statistic with an icon and label.
 * Used in screens like Trainer Plans.
 */
@Composable
fun SummaryStatCard(
    label: String,
    value: String,
    icon: DrawableResource,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier,
    containerColor: Color = Color(0xFFE7F7F2)
) {
    val bgColor = if (isSelected) OffGreenColor else White
    val borderColor = if (isSelected) PrimaryColor else Color.Transparent
    val textColor = if (isSelected) PrimaryColor else Gray

    CommonCard (
        modifier = modifier,
        content = {
            Box(
                modifier = modifier.fillMaxWidth()
                    .background(bgColor, RoundedCornerShape(16.dp))
                    .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = null,
                            tint = if (isSelected) PrimaryColor else Gray,
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = label,
                            style = AppTextTheme.semiBold.copy(
                                fontSize = 12.sp,
                                color = textColor
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = value,
                        style = AppTextTheme.semiBold.copy(
                            fontSize = 18.sp,
                            color = Black
                        )
                    )
                }
            }
        }
    )
//    Box(
//        modifier = modifier
//            .fillMaxWidth()
//            .background(bgColor, RoundedCornerShape(20.dp))
//            .border(1.5.dp, borderColor, RoundedCornerShape(20.dp))
//            .padding(16.dp)
//    ) {
//        Column {
//            Row(verticalAlignment = Alignment.CenterVertically) {
//                Icon(
//                    painter = painterResource(icon),
//                    contentDescription = null,
//                    tint = if (isSelected) PrimaryColor else Gray,
//                    modifier = Modifier.size(18.dp)
//                )
//
//                Spacer(modifier = Modifier.width(8.dp))
//
//                Text(
//                    text = label,
//                    style = AppTextTheme.semiBold.copy(
//                        fontSize = 12.sp,
//                        color = textColor
//                    )
//                )
//            }
//
//            Spacer(modifier = Modifier.height(8.dp))
//
//            Text(
//                text = value,
//                style = AppTextTheme.semiBold.copy(
//                    fontSize = 18.sp,
//                    color = Black
//                )
//            )
//        }
//    }
}

//    Box(
//        modifier = modifier
//            .fillMaxWidth()
//            .background(containerColor, RoundedCornerShape(12.dp))
//            .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(12.dp))
//            .padding(16.dp)
//    ) {
//        Column {
//            Row(verticalAlignment = Alignment.CenterVertically) {
//                Icon(
//                    painter = painterResource(icon),
//                    contentDescription = null,
//                    tint = PrimaryColor,
//                    modifier = Modifier.size(18.dp)
//                )
//                Spacer(modifier = Modifier.width(8.dp))
//                Text(
//                    text = label,
//                    style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray)
//                )
//            }
//            Spacer(modifier = Modifier.height(8.dp))
//            Text(
//                text = value,
//                style = AppTextTheme.bold.copy(fontSize = 20.sp, color = Black)
//            )
//        }
//    }
//}
