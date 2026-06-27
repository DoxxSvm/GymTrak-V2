package `in`.gym.trak.studio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.*
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_atm
import org.jetbrains.compose.resources.painterResource

/**
 * Numeric input field with a card/payment icon on the left.
 * Used for Amount, Additional Fee, and Freeze Fee inputs.
 */
@Composable
fun AmountInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "0.00",
    prefix: String = ""
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Color(0xFFF7F8F9), RoundedCornerShape(12.dp))
            .border(
                1.dp,
                if (isFocused) PrimaryColor else Color(0xFFE8ECF4),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_atm),
            contentDescription = null,
            tint = PrimaryColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        if (prefix.isNotEmpty()) {
            Text(
                text = prefix,
                style = AppTextTheme.bold.copy(fontSize = 16.sp, color = PrimaryColor)
            )
            Spacer(modifier = Modifier.width(4.dp))
        }
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = AppTextTheme.medium.copy(fontSize = 16.sp, color = Gray)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = { input ->
                    if (input.all { it.isDigit() || it == '.' }) onValueChange(input)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFocused = it.isFocused },
                textStyle = AppTextTheme.bold.copy(fontSize = 16.sp, color = PrimaryColor),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                cursorBrush = SolidColor(PrimaryColor)
            )
        }
    }
}
