package `in`.gym.trak.studio.features.shop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.DarkBlue
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.GrayBorderColor
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White

@Composable
fun ShopTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    isMultiline: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions? = null,
    readOnly: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    val defaultKeyboardActions = remember {
        KeyboardActions(
            onNext = {
                focusManager.moveFocus(FocusDirection.Next)
            }
        )
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = AppTextTheme.medium.copy(
                color = DarkBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isMultiline) 120.dp else 56.dp)
                .background(White, RoundedCornerShape(12.dp))
                .border(1.dp, GrayBorderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = if (isMultiline) 12.dp else 0.dp),
            contentAlignment = if (isMultiline) Alignment.TopStart else Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = if (isMultiline) Alignment.Top else Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Box(modifier = Modifier.weight(1f)) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = AppTextTheme.regular.copy(color = Gray, fontSize = 14.sp)
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = AppTextTheme.regular.copy(color = Black, fontSize = 14.sp),
                        cursorBrush = SolidColor(PrimaryColor),
                        keyboardOptions = if (keyboardOptions.imeAction == ImeAction.Default && !isMultiline) {
                            keyboardOptions.copy(imeAction = ImeAction.Next)
                        } else {
                            keyboardOptions
                        },
                        keyboardActions = keyboardActions ?: defaultKeyboardActions,
                        singleLine = !isMultiline,
                        readOnly = readOnly
                    )
                }
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingIcon()
                }
            }
        }
    }
}

@Composable
fun ShopDropdown(
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = AppTextTheme.medium.copy(
                color = DarkBlue,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(White, RoundedCornerShape(12.dp))
                .border(1.dp, GrayBorderColor, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (selectedOption.isEmpty()) "Select category" else selectedOption,
                    style = AppTextTheme.regular.copy(
                        color = if (selectedOption.isEmpty()) Gray else Black,
                        fontSize = 14.sp
                    )
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Gray
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(White)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = AppTextTheme.bold.copy(
            color = DarkBlue,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        ),
        modifier = Modifier.padding(vertical = 16.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PriceRow(
    label1: String,
    value1: String,
    onValueChange1: (String) -> Unit,
    label2: String,
    value2: String,
    onValueChange2: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ShopTextField(
            label = label1,
            value = value1,
            onValueChange = onValueChange1,
            placeholder = "$ 0.00",
            modifier = Modifier.weight(1f),
            leadingIcon = { Text("$", color = Gray) }
        )
        ShopTextField(
            label = label2,
            value = value2,
            onValueChange = onValueChange2,
            placeholder = "$ 0.00",
            modifier = Modifier.weight(1f),
            leadingIcon = { Text("$", color = Gray) }
        )
    }
}
