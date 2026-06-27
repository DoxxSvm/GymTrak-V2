package `in`.gym.trak.studio.features.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.features.auth.GetStartedScreen
import `in`.gym.trak.studio.features.auth.LoginScreen
import `in`.gym.trak.studio.features.member.MemberDashboardScreen
import `in`.gym.trak.studio.features.dashboard.OwnerDashboardScreen
import `in`.gym.trak.studio.theme.PrimaryColor
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.splash_screen
import org.jetbrains.compose.resources.painterResource
import `in`.gym.trak.studio.utils.NotificationManager
import `in`.gym.trak.studio.utils.requestNotificationPermission

/**
 * Entry point of the application.
 * Shows branding and performs mandatory checks (versioning, session, etc.) before proceeding.
 */
class SplashScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = rememberScreenModel { SplashViewModel() }
        val updateState by viewModel.updateState.collectAsState()

        LaunchedEffect(Unit) {
            requestNotificationPermission()
            NotificationManager.initialize()
            viewModel.checkAppVersion()
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(Res.drawable.splash_screen),
                contentDescription = "Splash Screen",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Handle Update Logic
            when (val state = updateState) {
                is UpdateState.UpdateRequired -> {
                    UpdateDialog(
                        isForce = state.isForce,
                        onDismiss = {
                            if (!state.isForce) {
                                // Re-run logic to determine where to go after "Later"
                                viewModel.checkAppVersion() 
                            }
                        },
                        onUpdate = {
                            // Link to Store would go here
                        }
                    )
                }
                is UpdateState.NoUpdateRequired -> {
                    LaunchedEffect(Unit) {
                        val screen = when(state.target) {
                            NavigationTarget.Intro -> GetStartedScreen()
                            NavigationTarget.Login -> LoginScreen()
                            NavigationTarget.OwnerDashboard -> OwnerDashboardScreen()
                            NavigationTarget.TrainerOwnerDashboard -> OwnerDashboardScreen()
                            NavigationTarget.MemberDashboard -> MemberDashboardScreen()
                        }
                        navigator.replace(screen)
                    }
                }
                else -> {}
            }
        }
    }

    @Composable
    private fun UpdateDialog(isForce: Boolean, onDismiss: () -> Unit, onUpdate: () -> Unit) {
        AlertDialog(
            onDismissRequest = { if (!isForce) onDismiss() },
            title = { Text("Update Available") },
            text = { Text("A new version of the app is available. Please update to continue using the best features.") },
            confirmButton = {
                TextButton(onClick = onUpdate) {
                    Text("Update Now", color = PrimaryColor)
                }
            },
            dismissButton = if (!isForce) {
                {
                    TextButton(onClick = onDismiss) {
                        Text("Later")
                    }
                }
            } else null
        )
    }
}
