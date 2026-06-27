package `in`.gym.trak.studio

import `in`.gym.trak.studio.theme.GymTrakTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.transitions.SlideTransition
import `in`.gym.trak.studio.data.repository.SessionInvalidationBus
import `in`.gym.trak.studio.features.auth.LoginScreen
import `in`.gym.trak.studio.features.splash.SplashScreen
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.login_bg
import org.jetbrains.compose.resources.painterResource

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import `in`.gym.trak.studio.components.NoInternetDialog
import `in`.gym.trak.studio.network.NetworkMonitor
import `in`.gym.trak.studio.components.ActiveWorkoutOverlay
import `in`.gym.trak.studio.components.CommonProgress
import `in`.gym.trak.studio.data.repository.OwnerDashboardRepository
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.data.repository.WorkoutManager
import `in`.gym.trak.studio.features.member.ActiveWorkoutScreen
import `in`.gym.trak.studio.network.ApiResult
import `in`.gym.trak.studio.notifications.WorkoutLiveNotificationController
import `in`.gym.trak.studio.notifications.WorkoutNavigationBus
import `in`.gym.trak.studio.notifications.startWorkoutNotificationOpenListener
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Composable
@Preview
fun App() {
    val isConnected by NetworkMonitor.isConnected.collectAsState()
    val scope = rememberCoroutineScope()

    GymTrakTheme {
        if (!isConnected) {
            NoInternetDialog()
        }
        val focusManager = LocalFocusManager.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        focusManager.clearFocus()
                    })
                }
        ) {
            Image(
                painter = painterResource(Res.drawable.login_bg),
                contentDescription = "Global Background",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Navigator(SplashScreen()) { navigator ->
                val workoutRepository = remember { OwnerDashboardRepository() }
                val workoutStopOverlayMutex = remember { Mutex() }
                var workoutStopFromOverlayLoading by remember { mutableStateOf(false) }

                fun tryOpenActiveWorkoutFromNotification() {
                    if (!WorkoutNavigationBus.hasPendingOpenActiveWorkout()) return
                    if (!SessionManager.isLoggedIn) return
                    val active = WorkoutManager.activeWorkout.value ?: return
                    if (navigator.lastItem is ActiveWorkoutScreen) {
                        WorkoutNavigationBus.clearPendingOpenActiveWorkout()
                        WorkoutManager.touchActiveWorkoutClock()
                        WorkoutLiveNotificationController.refreshPresentation(active)
                        return
                    }
                    WorkoutNavigationBus.clearPendingOpenActiveWorkout()
                    WorkoutManager.touchActiveWorkoutClock()
                    WorkoutLiveNotificationController.refreshPresentation(active)
                    WorkoutManager.updateWorkoutSession(showFloatingOverlay = false)
                    navigator.push(
                        ActiveWorkoutScreen(
                            showSkipSection = false,
                            workoutIdForCompletion = active.workoutId,
                            workoutId = active.workoutId,
                            useCompleteApiOnFinish = !active.workoutId.isNullOrBlank(),
                        ),
                    )
                }

                LaunchedEffect(Unit) {
                    startWorkoutNotificationOpenListener()
                    SessionInvalidationBus.events.collect {
                        navigator.replaceAll(LoginScreen())
                    }
                }
                LaunchedEffect(Unit) {
                    WorkoutNavigationBus.events.collect {
                        tryOpenActiveWorkoutFromNotification()
                    }
                }
                LaunchedEffect(navigator.lastItem) {
                    tryOpenActiveWorkoutFromNotification()
                }
                Box(modifier = Modifier.fillMaxSize()) {
                    SlideTransition(navigator)
                    
                    ActiveWorkoutOverlay(
                        onClick = {
                            val active = WorkoutManager.activeWorkout.value
                            navigator.push(ActiveWorkoutScreen(
                                showSkipSection = false,
                                workoutIdForCompletion = active?.workoutId,
                                useCompleteApiOnFinish = active?.workoutId != null
                            ))
                        },
                        onDelete = {
                            val activeSnapshot = WorkoutManager.activeWorkout.value
                            val workoutId = activeSnapshot?.workoutId?.takeIf { it.isNotBlank() }
                            if (workoutId == null) {
                                WorkoutManager.stopWorkout()
                                return@ActiveWorkoutOverlay
                            }
                            scope.launch {
                                workoutStopOverlayMutex.withLock {
                                    val token = SessionManager.accessToken
                                    if (token.isEmpty()) {
                                        WorkoutManager.stopWorkout()
                                        return@withLock
                                    }
                                    val gymId = SessionManager
                                        .effectiveGymIdForMemberApis()
                                        .ifBlank { SessionManager.gymId }
                                    workoutStopFromOverlayLoading = true
                                    try {
                                        val result = if (
                                            SessionManager.userRole.equals("member", ignoreCase = true)
                                        ) {
                                            workoutRepository.deleteWorkoutLegacy(
                                                token,
                                                workoutId,
                                                gymId.ifBlank { null },
                                            )
                                        } else {
                                            workoutRepository.deleteWorkout(
                                                token,
                                                gymId,
                                                workoutId,
                                            )
                                        }
                                        if (result is ApiResult.Success) {
                                            WorkoutManager.stopWorkout()
                                        }
                                    } finally {
                                        workoutStopFromOverlayLoading = false
                                    }
                                }
                            }
                        }
                    )
                    if (workoutStopFromOverlayLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            CommonProgress()
                        }
                    }
                }
            }
        }
    }
}
