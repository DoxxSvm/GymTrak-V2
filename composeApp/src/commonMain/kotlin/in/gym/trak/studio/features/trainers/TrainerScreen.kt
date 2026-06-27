package `in`.gym.trak.studio.features.trainers

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.SubcomposeAsyncImage
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.components.SearchBar
import `in`.gym.trak.studio.data.model.GymStaffListRole
import `in`.gym.trak.studio.data.model.TrainerDTO
import `in`.gym.trak.studio.theme.*
import gym.composeapp.generated.resources.*
import `in`.gym.trak.studio.base.Constants
import `in`.gym.trak.studio.components.CommonProgressOverlay
import `in`.gym.trak.studio.components.GymAppBar
import org.jetbrains.compose.resources.painterResource

import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel

class TrainerScreen(
    private val sharedScreenModel: OwnerDashboardScreenModel? = null,
    private val listRole: String = GymStaffListRole.TRAINER
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = sharedScreenModel ?: rememberScreenModel { OwnerDashboardScreenModel() }
        val trainers by screenModel.trainers.collectAsState()
        val searchQuery by screenModel.trainerSearchQuery.collectAsState()
        val isLoading by screenModel.isLoading.collectAsState()
        var isPullRefreshRequested by remember { mutableStateOf(false) }

        val listTitle = if (listRole == GymStaffListRole.STAFF) "Staff" else "Trainers"
        val emptyMessage = if (listRole == GymStaffListRole.STAFF) "No staff found." else "No trainers found."

        val pullRefreshState = rememberPullRefreshState(
            refreshing = isPullRefreshRequested && isLoading,
            onRefresh = {
                isPullRefreshRequested = true
                screenModel.loadTrainers()
            }
        )

        LaunchedEffect(listRole) {
            screenModel.setTrainersListRole(listRole)
            screenModel.loadTrainers()
        }
        LaunchedEffect(isLoading) {
            if (!isLoading) isPullRefreshRequested = false
        }

        // If it's a pull-to-refresh, we don't want the big blocky overlay.
        TrainerLoadingScreenHandler(
            isLoading = isLoading,
            isPullRefreshing = isPullRefreshRequested && isLoading
        ) {
            Scaffold(
                topBar = {
                    GymAppBar(
                        title = listTitle,
                        onBackClick = { navigator?.pop() }
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            navigator?.push(
                                AddTrainerScreen(
                                    listRole = listRole,
                                    onTrainerAdded = {
                                        screenModel.loadTrainers()
                                    }
                                )
                            )
                        },
                        containerColor = PrimaryColor,
                        contentColor = White,
                        shape = CircleShape
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add")
                    }
                },
                containerColor = Color.Transparent
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .pullRefresh(pullRefreshState)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                    ) {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = { screenModel.onTrainerSearchQueryChanged(it) },
                            placeholder = "Search by name or Specialization"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (!isLoading && trainers.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(emptyMessage, color = Gray)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(trainers, key = { it.gymUserId }) { trainer ->
                                    TrainerListItem(
                                        data = trainer,
                                        onClick = {
                                            navigator?.push(
                                                TrainerDetailScreen(
                                                    trainerId = trainer.gymUserId,
                                                    onRefresh = { screenModel.loadTrainers() },
                                                    listRole = listRole
                                                )
                                            )
                                        }
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(80.dp)) }
                            }
                        }
                    }

                    PullRefreshIndicator(
                        refreshing = isPullRefreshRequested && isLoading,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter),
                        contentColor = PrimaryColor
                    )
                }
            }
        }
    }
}

@Composable
fun TrainerLoadingScreenHandler(
    isLoading: Boolean,
    isPullRefreshing: Boolean,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        if (isLoading && !isPullRefreshing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(enabled = false) { }
            ) {
                CommonProgressOverlay()
            }
        }
    }
}

@Composable
fun TrainerListItem(data: TrainerDTO, onClick: () -> Unit) {
    CommonCard(
        modifier = Modifier.clickable { onClick() },
        content = {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // If avatarUrl was available we would use Coil/Kamel. Using placeholder for now

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = data.avatarUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = PrimaryColor,
                                strokeWidth = 2.dp
                            )
                        },
                        error = {
                            Image(
                                painter = painterResource(Res.drawable.gym_boy),
                                contentDescription = "Placeholder",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.fullName,
                        style = AppTextTheme.regular.copy(fontSize = 14.sp),
                        color = Black
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${data.salary?.experience ?: "N/A"} Exp",
                            style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
                        )
                        Text(
                            text = "  |  ",
                            style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
                        )
                        Icon(
                            painter = painterResource(Res.drawable.ic_dumble),
                            contentDescription = null,
                            tint = PrimaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = data.expertise.joinToString(", ").takeIf { it.isNotEmpty() }
                                ?: "Gen. Training",
                            style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Gray),
                            maxLines = 1
                        )
                    }
                }

                Surface(
                    color = PrimaryColor.copy(alpha = 0.20f),
                    shape = RoundedCornerShape(30.dp)
                ) {
                    Text(
                        text = "${Constants.RUPEE}${data.salary?.salaryCents ?: 0} ${data.salary?.salaryPeriod ?: "M"}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Black)
                    )
                }
            }
        }
    )
}

