package `in`.gym.trak.studio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White

@Composable
fun GenderSelector(selectedGender: String, onGenderSelected: (String) -> Unit) {
    CommonCard (
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(30.dp),

        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(White, RoundedCornerShape(30.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Male", "Female", "Other").forEach { gender ->
                    val isSelected = selectedGender == gender
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(30.dp))
                            .background(if (isSelected) PrimaryColor else Color.Transparent)
                            .clickable { onGenderSelected(gender) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = gender,
                            style = AppTextTheme.medium.copy(
                                fontSize = 14.sp,
                                color = if (isSelected) White else Gray
                            )
                        )
                    }
                }
            }

        }
    )

}
