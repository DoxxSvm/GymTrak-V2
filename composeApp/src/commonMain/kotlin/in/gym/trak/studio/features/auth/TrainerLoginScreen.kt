package `in`.gym.trak.studio.features.auth

import `in`.gym.trak.studio.components.AppScrollDefaults
import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.autofill.AutofillType
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.features.dashboard.OwnerDashboardScreen
import `in`.gym.trak.studio.features.member.MemberDashboardScreen
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.viewmodel.auth.AuthScreenModel
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.lockicon
import gym.composeapp.generated.resources.login_bg
import gym.composeapp.generated.resources.userIcon
import org.jetbrains.compose.resources.painterResource

class TrainerLoginScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { AuthScreenModel() }

        LoadingScreenHandler(screenModel = screenModel) {
            TrainerLoginContent(
                onSignIn = { username, password ->
                    screenModel.passwordLogin(username, password) { role ->
                        val destination = when (role) {
                            "gym_owner" -> OwnerDashboardScreen()
                            "trainer" -> OwnerDashboardScreen()
                            else -> MemberDashboardScreen() // trainer / member
                        }
                        navigator.replaceAll(destination)
                    }
                },
                onBack = { navigator.pop() }
            )
        }
    }
}


@Composable
@Preview
fun TrainerLoginContentPreview() {
    TrainerLoginContent(onSignIn = { _, _ -> }, onBack = {})
}

@Composable
fun TrainerLoginContent(
    onSignIn: (username: String, password: String) -> Unit,
    onBack: () -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // Blue radius circle - Top Left (ref: #09B27E @ 20% opacity, 330dp)

        Image(
            painter = painterResource(Res.drawable.login_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            GymAppBar(
                title = "",
                onBackClick = onBack,
            )
            AppScrollableScreen(
                modifier = Modifier.weight(1f),
                contentPadding = AppScrollDefaults.screenContentPadding(horizontal = 24.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {

            Text(
                text = "Trainer Or Staff Login",
                style = AppTextTheme.bold.copy(
                    fontSize = 28.sp,
                    color = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sign In With Your Credentials",
                style = AppTextTheme.regular.copy(
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Username
            Text(
                text = "Username",
                style = AppTextTheme.medium.copy(
                    fontSize = 14.sp,
                    color = Color(0xFF374151)
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(8.dp))
            CommonTextField(
                value = username,
                onValueChange = { username = it.trimStart() },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Enter Your Username",
                leadingIconDrawable = Res.drawable.userIcon,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                autofillType = AutofillType.Username
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Password
            Text(
                text = "Password",
                style = AppTextTheme.medium.copy(
                    fontSize = 14.sp,
                    color = Color(0xFF374151)
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Start
            )
            Spacer(modifier = Modifier.height(8.dp))
            CommonTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Enter Your Password",
                leadingIconDrawable = Res.drawable.lockicon,
                isPassword = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                autofillType = AutofillType.Password
            )

            Spacer(modifier = Modifier.height(32.dp))
            CommonButton(
                onClick = {
                    if (username.isNotBlank() && password.isNotBlank()) {
                        onSignIn(username, password)
                    }
                },
                text = "Sign In",
                enabled = username.isNotBlank() && password.isNotBlank()
            )
            }
        }
    }
}
