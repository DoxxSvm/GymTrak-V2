package `in`.gym.trak.studio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> CommonBottomSheetPicker(
    options: List<T>,
    selectedOption: T?,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Select Option",
    title: String = placeholder,
    optionToString: (T) -> String = { it.toString() },
    optionToAnnotatedString: (T) -> AnnotatedString? = { null },
) {
    var showSheet by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(showSheet) {
        if (showSheet) {
            focusManager.clearFocus()
        }
    }

    // Trigger button — same look as CommonDropdown
    Box(modifier = modifier.fillMaxWidth()) {
        CommonCard(
            shape = RoundedCornerShape(30.dp),
            content = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(White, RoundedCornerShape(30.dp))
                        .border(
                            1.dp,
                            if (showSheet) PrimaryColor else TextFiledBorderColor,
                            RoundedCornerShape(30.dp)
                        )
                        .clickable { showSheet = true }
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        contentDescription = "Open picker",
                        tint = Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        )
    }

    // Bottom sheet
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = White,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Handle + title
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 8.dp),
                ) {
                    Text(
                        text = title,
                        style = AppTextTheme.semiBold.copy(fontSize = 16.sp, color = Color.Black),
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                }

                HorizontalDivider(color = Color(0xFFE5E7EB))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(options) { option ->
                        val isSelected = option == selectedOption
                        val annotatedText = optionToAnnotatedString(option)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOptionSelected(option)
                                    showSheet = false
                                }
                                .background(
                                    if (isSelected) PrimaryColor.copy(alpha = 0.06f) else Color.Transparent
                                )
                                .padding(horizontal = 24.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (annotatedText != null) {
                                Text(
                                    text = annotatedText,
                                    style = AppTextTheme.medium.copy(fontSize = 15.sp),
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                Text(
                                    text = optionToString(option),
                                    style = AppTextTheme.medium.copy(
                                        fontSize = 15.sp,
                                        color = if (isSelected) PrimaryColor else Color.Black
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = PrimaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
