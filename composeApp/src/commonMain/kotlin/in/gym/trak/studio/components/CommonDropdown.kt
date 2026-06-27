package `in`.gym.trak.studio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.platform.LocalDensity
import `in`.gym.trak.studio.theme.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun <T> CommonDropdown(
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select Option",
    optionToString: (T) -> String = { it.toString() },
    optionToAnnotatedString: (T) -> AnnotatedString? = { null },
    leadingIcon: DrawableResource? = null,
    errorText: String? = null
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        CommonCard (
            shape = RoundedCornerShape(30.dp),
            content = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(White, RoundedCornerShape(30.dp))
                        .border(
                            1.dp,
                            if (errorText != null) Color.Red else if (expanded) PrimaryColor else TextFiledBorderColor,
                            RoundedCornerShape(30.dp)
                        )
                        .clickable { expanded = true }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (leadingIcon != null) {
                        Icon(
                            painter = painterResource(leadingIcon),
                            contentDescription = null,
                            tint = Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    val annotatedText = selectedOption?.let { optionToAnnotatedString(it) }
                    if (annotatedText != null) {
                        Text(
                            text = annotatedText,
                            style = AppTextTheme.medium.copy(fontSize = 14.sp),
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(
                            text = if (selectedOption != null && optionToString(selectedOption).isNotEmpty())
                                optionToString(selectedOption)
                            else
                                placeholder,
                            style = AppTextTheme.medium.copy(fontSize = 14.sp),
                            color = if (selectedOption != null) Color.Black else Gray,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Dropdown",
                        tint = Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        )


        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = RoundedCornerShape(30.dp),
            containerColor = White,
            tonalElevation = 0.dp,
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.92f),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        val annotatedText = optionToAnnotatedString(option)
                        if (annotatedText != null) {
                            Text(
                                text = annotatedText,
                                style = AppTextTheme.medium.copy(fontSize = 14.sp)
                            )
                        } else {
                            Text(
                                text = optionToString(option),
                                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Color.Black)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }

        if (errorText != null) {
            Text(
                text = errorText,
                color = Color.Red,
                style = AppTextTheme.regular.copy(fontSize = 12.sp),
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

/**
 * Compact chip-style dropdown (e.g. Weekly / Monthly / Yearly).
 * Uses [Popup] + [Surface] instead of [DropdownMenu] for consistent styling on iOS.
 */
@Composable
fun ChipDropdownMenu(
    selectedLabel: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val menuGapPx = with(LocalDensity.current) { 6.dp.roundToPx() }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(White, RoundedCornerShape(20.dp))
                .border(1.dp, TextFiledBorderColor, RoundedCornerShape(20.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = selectedLabel,
                style = AppTextTheme.medium.copy(fontSize = 13.sp, color = Black),
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Select period",
                tint = Gray,
                modifier = Modifier.size(16.dp),
            )
        }

        if (expanded) {
            Popup(
                alignment = Alignment.BottomStart,
                offset = IntOffset(0, menuGapPx),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = White,
                    shadowElevation = 8.dp,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .widthIn(min = 140.dp)
                        .border(1.dp, TextFiledBorderColor, RoundedCornerShape(12.dp)),
                ) {
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        options.forEach { option ->
                            val isSelected = option.equals(selectedLabel, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) PrimaryColor.copy(alpha = 0.10f) else Color.Transparent,
                                    )
                                    .clickable {
                                        onOptionSelected(option)
                                        expanded = false
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = option,
                                    style = AppTextTheme.medium.copy(
                                        fontSize = 14.sp,
                                        color = if (isSelected) PrimaryColor else Black,
                                    ),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
