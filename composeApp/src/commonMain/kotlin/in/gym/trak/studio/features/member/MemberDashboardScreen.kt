package `in`.gym.trak.studio.features.member

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import `in`.gym.trak.studio.components.BackHandler
import `in`.gym.trak.studio.components.ConfirmationDialog
import `in`.gym.trak.studio.components.rememberExitApplication
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import `in`.gym.trak.studio.data.repository.WorkoutManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_bottom_diet
import gym.composeapp.generated.resources.ic_bottom_home
import gym.composeapp.generated.resources.ic_bottom_stats
import gym.composeapp.generated.resources.ic_bottom_workout
import gym.composeapp.generated.resources.ic_diet
import gym.composeapp.generated.resources.ic_leaderboard
import gym.composeapp.generated.resources.ic_profile
import gym.composeapp.generated.resources.ic_user
import gym.composeapp.generated.resources.ic_workout
import gym.composeapp.generated.resources.userIcon
import org.jetbrains.compose.resources.painterResource

import cafe.adriel.voyager.core.model.rememberScreenModel

class MemberDashboardScreen : Screen {
    @Composable
    override fun Content() {
        val screenModel = rememberScreenModel { MemberDashboardScreenModel() }
        val homeWorkoutScreenModel = rememberScreenModel { MemberWorkoutScreenModel() }
        val selectedTab by screenModel.selectedTab.collectAsState()
        val memberDashboard by screenModel.memberDashboard.collectAsState()
        val isRefreshing by screenModel.isRefreshing.collectAsState()
        var showExitDialog by remember { mutableStateOf(false) }
        val exitApp = rememberExitApplication()
        val defaultTab = "Home"

        BackHandler {
            when {
                showExitDialog -> showExitDialog = false
                selectedTab != defaultTab -> screenModel.onTabSelected(defaultTab)
                else -> showExitDialog = true
            }
        }

        if (showExitDialog) {
            ConfirmationDialog(
                onDismissRequest = { showExitDialog = false },
                onConfirm = { exitApp() },
                title = "Exit app?",
                message = "Are you sure you want to close the application?",
                confirmText = "Exit",
                dismissText = "Cancel",
            )
        }

        DisposableEffect(Unit) {
            WorkoutManager.bottomPadding = 80.dp
            onDispose {
                WorkoutManager.bottomPadding = 0.dp
            }
        }

        LoadingScreenHandler(screenModel = screenModel) {
            Scaffold(
                modifier = Modifier.fillMaxSize().background(Color.Transparent),
                containerColor = Color.Transparent,
                bottomBar = {
                    MemberBottomNavigationBar(
                        selectedItem = selectedTab,
                        onItemSelected = { screenModel.onTabSelected(it) }
                    )
                }
            ) { paddingValues ->
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                    when (selectedTab) {
                        "Home" -> LoadingScreenHandler(screenModel = homeWorkoutScreenModel) {
                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = { screenModel.refreshMemberDashboard() },
                                state = rememberPullToRefreshState(),
                                modifier = Modifier.fillMaxSize(),
                                indicator = {}
                            ) {
                                MemberHomeScreen(
                                    dashboard = memberDashboard,
                                    memberWorkoutScreenModel = homeWorkoutScreenModel,
                                    onAfterWorkoutSheetFinished = { screenModel.refreshMemberDashboard() },
                                )
                            }
                        }
                        "Workout" -> MemberWorkoutScreen()
                        "Diet" -> MemberDietScreen()
                        "Stats" -> MemberStatsScreen()
                        "Profile" -> MemberProfileScreen().Content()
                        else -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "$selectedTab Screen Coming Soon")
                        }
                    }
                }
            }
        }
    }
}

@Preview()
@Composable
fun MemberDashboardScreenPreview() {
    MemberDashboardScreen()
}

@Composable
fun MemberBottomNavigationBar(selectedItem: String, onItemSelected: (String) -> Unit) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple("Home", Res.drawable.ic_bottom_home, "Home"),
            Triple("Workout", Res.drawable.ic_bottom_workout, "Workout"),
            Triple("Diet", Res.drawable.ic_bottom_diet, "Diet"),
            Triple("Stats", Res.drawable.ic_bottom_stats, "Stats"),
            Triple("Profile", Res.drawable.userIcon, "Profile")
        )

        items.forEach { (label, icon, value) ->
            val isSelected = selectedItem == value

            NavigationBarItem(
                selected = isSelected,
                modifier = Modifier.padding(0.dp),
                onClick = { onItemSelected(value) },
                icon = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(icon),
                            contentDescription = label,
                            tint = if (isSelected) PrimaryColor else Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                label = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(0.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) PrimaryColor else Gray,
                            style = AppTextTheme.medium.copy(fontSize = 11.sp)
                        )
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 4.dp)
                                    .height(6.dp)
                                    .width(40.dp)
                                    .background(
                                        PrimaryColor,
                                        shape = RoundedCornerShape(topStart = 50.dp, topEnd = 50.dp),
                                    )
                            )
                        } else {
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

// MemberDietScreen implementation is now in a separate file
// MemberStatsScreen implementation is now in a separate file
// MemberProfileScreen implementation is now in a separate file
