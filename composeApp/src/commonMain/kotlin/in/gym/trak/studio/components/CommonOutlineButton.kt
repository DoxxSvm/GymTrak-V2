package `in`.gym.trak.studio.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White

/**
 * Reusable primary button used across the app.
 * Styled with theme [PrimaryColor], rounded corners, and consistent height.
 */
@Composable
fun CommonOutlineButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    textColor: Color = White,
    color: Color = White,

    borderColor: Color = PrimaryColor,
    leftIcon: Painter? = null,
    rightIcon: Painter? = null,
    content: @Composable (RowScope.() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(if (content != null) 48.dp else 44.dp),
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {

        Button(
            onClick = onClick,
            modifier = modifier
                .fillMaxWidth()
                .height(44.dp),
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = color,
                contentColor = textColor,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = textColor.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(100.dp),
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),

        ) {

            if (content != null) {
                content()
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {

                if (leftIcon != null) {
                    Icon(
                        painter = leftIcon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Text(
                    text = text,
                    style = AppTextTheme.medium.copy(
                        fontSize = 14.sp,
                        color = textColor
                    )
                )

                if (rightIcon != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        painter = rightIcon,
                        contentDescription = null,
                        tint = textColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        }

}
