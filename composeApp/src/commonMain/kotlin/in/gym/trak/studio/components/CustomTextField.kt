package `in`.gym.trak.studio.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White

@Composable
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    textStyle: TextStyle = AppTextTheme.medium.copy(fontSize = 16.sp),

    // NEW
    autofillType: AutofillType? = null
) {
    val focusManager = LocalFocusManager.current
    val autofill = LocalAutofill.current
    var isFocused by remember { mutableStateOf(false) }

    val autofillNode = remember(autofillType) {
        AutofillNode(
            autofillTypes = autofillType?.let { listOf(it) } ?: emptyList(),
            onFill = { filledValue ->
                onValueChange(filledValue)
            }
        )
    }

    if (autofillType != null) {
        LocalAutofillTree.current += autofillNode
    }

    val defaultKeyboardActions = KeyboardActions(
        onNext = {
            focusManager.moveFocus(FocusDirection.Next)
        }
    )

    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .border(
                width = 1.dp,
                color = if (isFocused) PrimaryColor else Color(0xFFE8ECF4),
                shape = RoundedCornerShape(12.dp)
            )
            .onGloballyPositioned { coordinates ->
                if (autofillType != null) {
                    autofillNode.boundingBox = coordinates.boundsInWindow()
                }
            }
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused

                if (autofillType != null) {
                    if (focusState.isFocused) {
                        autofill?.requestAutofillForNode(autofillNode)
                    } else {
                        autofill?.cancelAutofillForNode(autofillNode)
                    }
                }
            },
        textStyle = textStyle.copy(color = Color.Black),
        placeholder = {
            Text(
                text = placeholder,
                style = textStyle.copy(color = Color(0xFF8391A1))
            )
        },
        leadingIcon = leadingIcon,
        singleLine = true,
        keyboardOptions = if (keyboardOptions.imeAction == ImeAction.Default) {
            keyboardOptions.copy(imeAction = ImeAction.Next)
        } else {
            keyboardOptions
        },
        keyboardActions = keyboardActions ?: defaultKeyboardActions,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = White,
            unfocusedContainerColor = White,
            disabledContainerColor = White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent
        )
    )
}
