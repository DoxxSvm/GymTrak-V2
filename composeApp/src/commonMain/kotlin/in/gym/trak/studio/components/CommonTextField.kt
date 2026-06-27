package `in`.gym.trak.studio.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.TextFiledBorderColor
import `in`.gym.trak.studio.theme.TextFiledColor
import `in`.gym.trak.studio.theme.White
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.eyeoff
import gym.composeapp.generated.resources.eyeopen
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun CommonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    borderRadius: Dp = 30.dp,
    leadingIconDrawable: DrawableResource? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isPassword: Boolean = false,
    isMultiline: Boolean = false,

    passwordVisibleIcon: DrawableResource? = Res.drawable.eyeopen,
    passwordHiddenIcon: DrawableResource? = Res.drawable.eyeoff,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions? = null,
    keyboardCapitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    textStyle: TextStyle = AppTextTheme.medium.copy(fontSize = 14.sp),
    readOnly: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    suffix: @Composable (() -> Unit)? = null,
    errorText: String? = null,
    autofillType: AutofillType? = null
) {
    val focusManager = LocalFocusManager.current
    val autofill = LocalAutofill.current
    var isFocused by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

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

    val effectiveVisible = isPassword && passwordVisible
    val effectiveVisualTransformation = if (isPassword && !passwordVisible)
        PasswordVisualTransformation()
    else if (visualTransformation != VisualTransformation.None)
        visualTransformation
    else
        VisualTransformation.None

    Column {
        CommonCard(
            shape = RoundedCornerShape(borderRadius),
            content = {
                Row(
                    modifier = modifier
                        .height(if (isMultiline) 100.dp else 48.dp)
                        .background(White, RoundedCornerShape(borderRadius))
                        .border(
                            1.dp,
                            if (errorText != null) Color.Red else if (isFocused) PrimaryColor else Color.Transparent,
                            RoundedCornerShape(borderRadius)
                        )
                        .padding(horizontal = 16.dp, if (isMultiline) 16.dp else 0.dp),
                    verticalAlignment = if (isMultiline) Alignment.Top else Alignment.CenterVertically
                ) {
                    if (leadingIconDrawable != null) {
                        Icon(
                            painter = painterResource(leadingIconDrawable),
                            contentDescription = null,
                            tint = Gray,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = textStyle.copy(color = Gray)
                            )
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            modifier = Modifier
                                .fillMaxWidth()
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

                            keyboardOptions = if (keyboardOptions.imeAction == ImeAction.Default && singleLine) {
                                keyboardOptions.copy(
                                    capitalization = keyboardCapitalization,
                                    imeAction = ImeAction.Next
                                )
                            } else {
                                keyboardOptions.copy(capitalization = keyboardCapitalization)
                            },
                            keyboardActions = keyboardActions ?: KeyboardActions(
                                onNext = {
                                    focusManager.moveFocus(FocusDirection.Next)
                                }
                            ),
                            singleLine = singleLine,
                            visualTransformation = effectiveVisualTransformation,
                            cursorBrush = SolidColor(PrimaryColor),
                            readOnly = readOnly,
                            enabled = enabled
                        )
                    }

                    if (suffix != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        suffix()
                    }

                    if (isPassword && passwordHiddenIcon != null && passwordVisibleIcon != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Image(
                            painter = painterResource(if (effectiveVisible) passwordVisibleIcon else passwordHiddenIcon),
                            contentDescription = if (effectiveVisible) "Hide password" else "Show password",
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { passwordVisible = !passwordVisible }
                        )
                    } else if (trailingIcon != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        trailingIcon()
                    }
                }
            }
        )

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
