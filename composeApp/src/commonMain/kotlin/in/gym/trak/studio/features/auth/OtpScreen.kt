package `in`.gym.trak.studio.features.auth

import `in`.gym.trak.studio.components.AppScrollDefaults
import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.PrimaryColor
import com.ban.otptextfield.OtpTextField
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.login_bg
import gym.composeapp.generated.resources.otp_bg
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.delay

import cafe.adriel.voyager.core.model.rememberScreenModel
import androidx.compose.runtime.collectAsState

import `in`.gym.trak.studio.features.dashboard.OwnerDashboardScreen
import `in`.gym.trak.studio.features.member.MemberDashboardScreen
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.viewmodel.auth.AuthScreenModel

data class OtpScreen(val phoneNumber: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { AuthScreenModel() }

        LoadingScreenHandler(screenModel = screenModel) {
            OtpContent(
                phoneNumber = phoneNumber,
                onVerificationSuccess = { otpValue ->
                    screenModel.verifyOtp(phoneNumber, otpValue) { isRegistered, tempToken, role ->
                        if (isRegistered) {
                            SessionManager.isLoggedIn = true
                            val dashboardScreen = when (role) {
                                "gym_owner" -> OwnerDashboardScreen()
                                "trainer" -> OwnerDashboardScreen()
                                else -> MemberDashboardScreen()
                            }
                            navigator.replaceAll(dashboardScreen)
                        } else {
                            navigator.push(RoleSelectionScreen(tempToken = tempToken ?: ""))
                        }
                    }
                },
                onResendOtp = {
                    screenModel.resendOtp(phoneNumber) { /* callback on success — timer already reset in UI */ }
                }
            )
        }
    }
}


@Composable
@Preview
fun OtpContentPreview() {
    OtpContent(
        phoneNumber = "6225533333",
        onVerificationSuccess = {},
        onResendOtp = {}
    )
}

@Composable
fun OtpContent(
    phoneNumber: String,
    onVerificationSuccess: (String) -> Unit,
    onResendOtp: () -> Unit = {}
) {
    val otpLength = 4
    var otp by remember { mutableStateOf("") }
    var timeLeft by remember { mutableStateOf(30) }
    var isTimerRunning by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    val focusManager = LocalFocusManager.current
    var otpValue by remember {
        mutableStateOf("")
    }

    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            timeLeft = 30
            while (timeLeft > 0) {
                delay(1000L)
                timeLeft -= 1
            }
            isTimerRunning = false
        }
    }

    fun formatPhone(s: String): String {
        return if (s.length >= 10) {
            s.take(5) + " " + s.drop(5).take(5)
        } else s
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
            ) {
                Image(
                    painter = painterResource(Res.drawable.otp_bg),
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

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Enter Verification Code",
                    style = AppTextTheme.bold.copy(
                        fontSize = 28.sp,
                        color = Color.Black
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "We've Sent A 4-Digit Code To +91 ${formatPhone(phoneNumber)}",
                    style = AppTextTheme.regular.copy(
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                OtpTextField(
                    otpCount = otpLength,
                    otpText = otpValue,
                    onOtpTextChange = { value, otpInputFilled ->
                        otpValue = value
                        if (otpInputFilled) {
                            focusManager.clearFocus()
                        }
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Didn't Receive Code?",
                        style = AppTextTheme.medium.copy(
                            fontSize = 14.sp,
                            color = Color(0xFFDC2626)
                        )
                    )
                    if (isTimerRunning && timeLeft > 0) {
                        Text(
                            text = buildAnnotatedString {
                                append("Resend In ")
                                withStyle(SpanStyle(color = PrimaryColor)) {
                                    append("0:${timeLeft.toString().padStart(2, '0')}s")
                                }
                            },
                            style = AppTextTheme.regular.copy(
                                fontSize = 14.sp,
                                color = Color(0xFF6B7280)
                            )
                        )
                    } else {
                        Text(
                            text = "Resend OTP",
                            style = AppTextTheme.medium.copy(
                                fontSize = 14.sp,
                                color = PrimaryColor
                            ),
                            modifier = Modifier.clickable {
                                isTimerRunning = true
                                otpValue = ""
                                focusManager.clearFocus()
                                onResendOtp()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                CommonButton(
                    onClick = { if (otpValue.length == otpLength) onVerificationSuccess(otpValue) },
                    text = "Verify & Continue",
                    enabled = otpValue.length == otpLength
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
