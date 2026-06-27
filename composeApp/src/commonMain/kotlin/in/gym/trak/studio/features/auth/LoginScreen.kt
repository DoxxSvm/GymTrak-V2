package `in`.gym.trak.studio.features.auth

import `in`.gym.trak.studio.components.AppScrollDefaults
import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CountryCodePicker
import `in`.gym.trak.studio.components.CustomTextField
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.flag
import gym.composeapp.generated.resources.login_bg
import gym.composeapp.generated.resources.login_image
import org.jetbrains.compose.resources.painterResource

import cafe.adriel.voyager.core.model.rememberScreenModel
import androidx.compose.runtime.collectAsState

import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.viewmodel.auth.AuthScreenModel

class LoginScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { AuthScreenModel() }

        LoadingScreenHandler(screenModel = screenModel) {
            LoginContent(
                onNext = { phoneNumber ->
                    screenModel.login(phoneNumber) {
                        navigator.push(OtpScreen(phoneNumber = phoneNumber))
                    }
                },
                onLogInWithTrainer = {
                    navigator.push(TrainerLoginScreen())
                }
            )
        }
    }
}


@Composable
@Preview
fun LoginContentPreView() {
    LoginContent(onLogInWithTrainer = {}, onNext = {})
}

@Composable
fun LoginContent(
    onNext: (String) -> Unit,
    onLogInWithTrainer: () -> Unit = {
    }
) {
    var phoneNumber by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current

    // Automatic API call when 10 digits are entered
    LaunchedEffect(phoneNumber) {
        if (phoneNumber.length == 10) {
            onNext(phoneNumber)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            }
    ) {
        Image(
            painter = painterResource(Res.drawable.login_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        AppScrollableScreen(
            modifier = Modifier.safeDrawingPadding(),
            scrollState = scrollState,
            contentPadding = AppScrollDefaults.noContentPadding,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Spacer(modifier = Modifier.height(65.dp))
            // Header image: login_image.png (top ~40–45%, with fade at bottom)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
            ) {
                Image(
                    painter = painterResource(Res.drawable.login_image),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                // Soft gradient fade at bottom into content area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.White)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Welcome To (black, bold)
            Text(
                text = "Welcome To",
                style = AppTextTheme.bold.copy(
                    fontSize = 36.sp,
                    color = Color.Black
                )
            )
            // Gym Trak (green, bold, larger)
            Text(
                text = "Gym Trak",
                style = AppTextTheme.bold.copy(
                    fontSize = 30.sp,
                    color = PrimaryColor
                )
            )

            Spacer(modifier = Modifier.height(20.dp))


            Column(
                modifier = Modifier.padding(horizontal = 20.dp)
            ) {

                Text(
                    text = "Enter Your Phone Number To Continue",
                    style = AppTextTheme.regular.copy(
                        fontSize = 14.sp,
                        color = Color.Black.copy(alpha = 0.8f)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Phone input row: country code (flag + +91) + phone field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CountryCodePicker(
                        onClick = { /* Handle country selection */ },
                        flagDrawable = Res.drawable.flag
                    )
                    CustomTextField(
                        value = phoneNumber,
                        onValueChange = {
                            if (it.length <= 10 && it.all { c -> c.isDigit() }) phoneNumber = it
                        },
                        placeholder = "123544 26660",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        textStyle = AppTextTheme.medium.copy(fontSize = 16.sp, color = Color.Black),
                        autofillType = AutofillType.PhoneNumberDevice
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Next button (green, full width, white text)
                CommonButton(
                    onClick = { if (phoneNumber.length >= 10) onNext(phoneNumber) },
                    text = "Next",
                    enabled = phoneNumber.length >= 10,

                )
                Spacer(modifier = Modifier.height(20.dp))

                // Log In With Trainer (Log In With grey, Trainer green underlined)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Text(
                        text = buildAnnotatedString {
                            withStyle(style = SpanStyle(color = Color.Black.copy(alpha = 0.8f))) {
                                append("Log In With ")
                            }
                            withStyle(
                                style = SpanStyle(
                                    color = PrimaryColor,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append("Trainer")
                            }
                            withStyle(style = SpanStyle(color = Color.Black.copy(alpha = 0.8f))) {
                                append("Or ")
                            }

                            withStyle(
                                style = SpanStyle(
                                    color = PrimaryColor,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append("Staff")
                            }
                        },
                        style = AppTextTheme.medium.copy(fontSize = 14.sp),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clickable(onClick = onLogInWithTrainer)
                    )
                }

            }
            // Enter Your Phone Number To Continue (left-aligned, smaller black)

//            Spacer(modifier = Modifier.weight(1f))
//            Spacer(modifier = Modifier.height(32.dp))
            Spacer(modifier = Modifier.weight(1f))
            // Privacy Policy and Terms Of Service (bottom, centered)
            Box(
                modifier = Modifier.padding(horizontal = 36.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        append("By Continuing, You Agree To Our ")
                        withStyle(
                            style = SpanStyle(
                                color = PrimaryColor,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("Privacy\nPolicy")
                        }
                        append(" And ")
                        withStyle(
                            style = SpanStyle(
                                color = PrimaryColor,
                                textDecoration = TextDecoration.Underline
                            )
                        ) {
                            append("Terms Of Service")
                        }
                    },
                    style = AppTextTheme.regular.copy(
                        fontSize = 12.sp,
                        color = Color.Black.copy(alpha = 0.7f)
                    ),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
