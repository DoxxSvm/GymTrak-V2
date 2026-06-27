package `in`.gym.trak.studio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.*
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_cale
import org.jetbrains.compose.resources.painterResource


@Composable
fun DatePickerField(
    value: String,
    modifier: Modifier = Modifier,
    placeholder: String = "DD / MM / YYYY",
    enabled: Boolean = true,
    onPickerClick: () -> Unit = {}
) {
    val bgAlpha = if (enabled) 1f else 0.55f
    CommonCard(
        shape = RoundedCornerShape(30.dp),
        content = {
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .border(
                        1.dp,
                        if (enabled) TextFiledBorderColor else TextFiledBorderColor.copy(alpha = 0.4f),
                        RoundedCornerShape(30.dp)
                    )
                    .background(if (enabled) Color.Transparent else Color(0xFFF5F5F5))
                    .clickable(enabled = enabled) { onPickerClick() }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value.ifEmpty { placeholder },
                    style = AppTextTheme.medium.copy(
                        fontSize = 15.sp,
                        color = when {
                            value.isEmpty() -> Gray.copy(alpha = bgAlpha)
                            !enabled -> Gray
                            else -> Black
                        }
                    ),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(Res.drawable.ic_cale),
                    contentDescription = if (enabled) "Pick date" else "Auto-calculated",
                    tint = if (enabled) Gray else Gray.copy(alpha = 0.4f),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    )
}
