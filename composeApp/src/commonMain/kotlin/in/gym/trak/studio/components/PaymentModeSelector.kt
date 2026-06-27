package `in`.gym.trak.studio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.*

/**
 * A segmented two-option toggle used for payment mode selection (e.g. Cash / UPI).
 * [options] must have exactly 2 items. The selected option is highlighted with [PrimaryColor].
 */
@Composable
fun PaymentModeSelector(
    options: List<String> = listOf("Cash", "UPI"),
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(100.dp))
//            .background(Color(0xFFF0F0F0))
        ,
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            val isSelected = option == selectedOption
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(100.dp))
                    .background(if (isSelected) PrimaryColor else TextFiledColor)
                    .clickable { onOptionSelected(option) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = AppTextTheme.regular.copy(
                        fontSize = 14.sp,
                        color = if (isSelected) White else Gray
                    )
                )
            }
        }
    }
}
