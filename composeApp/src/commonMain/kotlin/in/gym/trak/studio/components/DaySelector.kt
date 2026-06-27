package `in`.gym.trak.studio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White

/**
 * A horizontal row of selectable days of the week.
 */
@Composable
fun DaySelector(
    selectedDays: Set<String>,
    onDayToggle: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = listOf("M", "T", "W", "T", "F", "S", "S")
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        days.forEachIndexed { index, day ->
            val dayKey = "$day$index" // Handle duplicate day initials
            val isSelected = selectedDays.contains(dayKey)
            
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) PrimaryColor else White)
                    .clickable { onDayToggle(dayKey) }
                    .then(if (!isSelected) Modifier.background(White, CircleShape) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                // If not selected, it looks like a bubble with a border in some screens
                // but in the image, unselected days have no border, just Gray text
                Text(
                    text = day,
                    style = AppTextTheme.bold.copy(
                        fontSize = 13.sp,
                        color = if (isSelected) White else Gray
                    )
                )
            }
        }
    }
}
