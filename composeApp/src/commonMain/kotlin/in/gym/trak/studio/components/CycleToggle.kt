package `in`.gym.trak.studio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.BlueLightColor
import `in`.gym.trak.studio.theme.OffGreenColor
import `in`.gym.trak.studio.theme.TextFiledColor

@Composable
fun CycleToggle(
    option1: String,
    option2: String,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
  CommonCard (
      modifier = modifier.height(48.dp),
      shape = RoundedCornerShape(60.dp),

      backgroundColor = White,
      content = {
          Row(
              modifier = Modifier.padding(4.dp),
              verticalAlignment = Alignment.CenterVertically
          ) {
              Box(
                  modifier = Modifier
                      .weight(1f)
                      .fillMaxHeight()
                      .clip(RoundedCornerShape(60.dp))
                      .background(if (selectedOption == option1) PrimaryColor else Color.Transparent)
                      .clickable { onOptionSelected(option1) },
                  contentAlignment = Alignment.Center
              ) {
                  Text(
                      text = option1,
                      style = AppTextTheme.semiBold.copy(
                          fontSize = 16.sp,
                          color = if (selectedOption == option1) White else Black.copy(alpha = 0.60f)
                      )
                  )
              }
              Box(
                  modifier = Modifier
                      .weight(1f)
                      .fillMaxHeight()
                      .clip(RoundedCornerShape(60.dp))
                      .background(if (selectedOption == option2) PrimaryColor else Color.Transparent)
                      .clickable { onOptionSelected(option2) },
                  contentAlignment = Alignment.Center
              ) {
                  Text(
                      text = option2,
                      style = AppTextTheme.semiBold.copy(
                          fontSize = 16.sp,
                          color = if (selectedOption == option2) White else  Black.copy(alpha = 0.60f)
                      )
                  )
              }
          }
      }
  )
}
