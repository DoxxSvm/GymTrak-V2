package `in`.gym.trak.studio.features.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.rememberAsyncImagePainter
import `in`.gym.trak.studio.components.ConfirmationDialog
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.data.model.UserOwnedGymDTO
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.data.repository.SessionManager.hasPermission
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.RedColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_gym_subscription
import org.jetbrains.compose.resources.painterResource
import kotlin.jvm.Transient

class MyGymsScreen(
    @Transient private val sharedDashboardScreenModel: OwnerDashboardScreenModel? = null,
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = sharedDashboardScreenModel ?: rememberScreenModel { OwnerDashboardScreenModel() }
        val userGyms by screenModel.userGyms.collectAsState()
        val userGymsLoading by screenModel.userGymsLoading.collectAsState()
        var gymPendingDelete by remember { mutableStateOf<UserOwnedGymDTO?>(null) }
        val isTrainer = SessionManager.userRole.equals("trainer", ignoreCase = true)
        val canAddGym = !isTrainer
        val canEditGym = hasPermission(SessionManager.PermissionKeys.KEY_GYM_UPDATE)
        val canDeleteGym = hasPermission(SessionManager.PermissionKeys.KEY_GYM_DELETE)

        LaunchedEffect(Unit) {
            screenModel.loadUserGyms()
        }

        gymPendingDelete?.let { gym ->
            ConfirmationDialog(
                onDismissRequest = { gymPendingDelete = null },
                onConfirm = {
                    screenModel.deleteOwnedGym(gym.id) { }
                },
                title = "Delete gym",
                message = "Delete \"${gym.name.ifBlank { "this gym" }}\"? This permanently removes the gym and related data.",
                confirmText = "Delete",
                isDangerAction = true
            )
        }

        LoadingScreenHandler(screenModel = screenModel) {
            Scaffold(
                topBar = {
                    GymAppBar(
                        title = "My gyms",
                        onBackClick = { navigator.pop() }
                    )
                },
                floatingActionButton = {
                    if (canAddGym) {
                        FloatingActionButton(
                            onClick = {
                                navigator.push(
                                    AddGymScreen(sharedDashboardScreenModel = screenModel)
                                )
                            },
                            containerColor = PrimaryColor,
                            contentColor = White,
                            shape = CircleShape
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add gym")
                        }
                    }
                },
                containerColor = Color.Transparent
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    if (userGymsLoading && userGyms.isEmpty()) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = PrimaryColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (!userGymsLoading && userGyms.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No gyms yet. Tap + to add one.",
                                style = AppTextTheme.medium.copy(fontSize = 15.sp, color = Gray)
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(userGyms, key = { it.id }) { gym ->
                                MyGymRow(
                                    gym = gym,
                                    canEdit = canEditGym,
                                    canDelete = canDeleteGym,
                                    onEdit = {
                                        navigator.push(
                                            AddGymScreen(
                                                sharedDashboardScreenModel = screenModel,
                                                existingGym = gym
                                            )
                                        )
                                    },
                                    onDelete = { gymPendingDelete = gym }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyGymRow(
    gym: UserOwnedGymDTO,
    canEdit: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = White,
        border = BorderStroke(1.dp, PrimaryColor.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center
            ) {
                val thumb = gym.logoUrl
                if (!thumb.isNullOrBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(thumb),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        painter = painterResource(Res.drawable.ic_gym_subscription),
                        contentDescription = null,
                        tint =Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gym.name.ifBlank { "Gym" },
                    style = AppTextTheme.bold.copy(fontSize = 15.sp, color = Black),
                    maxLines = 1
                )
                val addr = gym.address
                if (!addr.isNullOrBlank()) {
                    Text(
                        text = addr,
                        style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray),
                        maxLines = 2
                    )
                }
            }
            if (canEdit) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = PrimaryColor
                    )
                }
            }
            if (canDelete) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = RedColor
                    )
                }
            }
        }
    }
}
