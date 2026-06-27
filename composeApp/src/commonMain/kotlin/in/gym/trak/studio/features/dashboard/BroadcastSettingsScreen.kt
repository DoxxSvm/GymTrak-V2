package `in`.gym.trak.studio.features.dashboard

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_trainer_edit
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.ConfirmationDialog
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.SearchBar
import `in`.gym.trak.studio.data.model.BroadcastMemberDTO
import `in`.gym.trak.studio.theme.*
import `in`.gym.trak.studio.utils.DateUtils
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import org.jetbrains.compose.resources.painterResource

class BroadcastSettingsScreen(private val channelId: String) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val channelDetail by screenModel.broadcastChannelDetail.collectAsState()
        val members by screenModel.broadcastMembers.collectAsState()
        val membersLoading by screenModel.broadcastMembersLoading.collectAsState()
        var memberSearch by remember { mutableStateOf("") }
        var showDeleteDialog by remember { mutableStateOf(false) }

        LaunchedEffect(channelId) {
            screenModel.loadBroadcastChannelDetail(channelId)
            screenModel.loadBroadcastMembers(channelId)
        }
        LaunchedEffect(memberSearch) {
            screenModel.loadBroadcastMembers(channelId, search = memberSearch.takeIf { it.isNotBlank() })
        }

        if (showDeleteDialog) {
            ConfirmationDialog(
                onDismissRequest = { showDeleteDialog = false },
                onConfirm = {
                    screenModel.deleteBroadcastChannel(channelId) {
                        showDeleteDialog = false
                        navigator.replaceAll(BroadcastChannelsScreen())
                    }
                },
                title = "Delete Broadcast?",
                message = "This action is permanent and will delete all messages and history for all members. This cannot be undone.",
                confirmText = "Delete",
                isDangerAction = true
            )
        }

        Scaffold(
            containerColor = Color(0xFFF7F7F8),
            topBar = {
                GymAppBar(
                    title = "Broadcast Settings",
                    onBackClick = { navigator.pop() }
                )
            }
        ) { paddingValues ->
            AppScrollableScreen(
                modifier = Modifier.fillMaxSize().padding(top = 100.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                
                ImageAvatar(size = 100.dp, url = channelDetail?.imageUrl)
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = channelDetail?.name ?: "Unnamed Channel",
                    style = AppTextTheme.bold.copy(fontSize = 20.sp, color = Black)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${channelDetail?.memberCount ?: 0} members",
                        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Color(0xFF10B981))
                    )
                    Text(
                        text = " • ${DateUtils.formatBroadcastDate(channelDetail?.createdAt)}",
                        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray)
                    )
                }
                
                if (!channelDetail?.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = channelDetail?.description!!,
                        style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                CommonButton(
                    text = "Edit profile",
                    onClick = {
                        navigator.push(
                            EditBroadcastChannelScreen(
                                channelId = channelId,
                                initialName = channelDetail?.name,
                                initialDescription = channelDetail?.description,
                                initialImageUrl = channelDetail?.imageUrl
                            )
                        )
                    },
                    modifier = Modifier.width(180.dp),
                    color = Color(0xFFE6F7F1),
                    textColor = Black,
                    leftIcon = painterResource(Res.drawable.ic_trainer_edit)
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = White
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    navigator.push(
                                        AddBroadcastMemberScreen(
                                            channelId = channelId,
                                            closeWithPopOnSuccess = true
                                        )
                                    )
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = White)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = "Add members",
                                style = AppTextTheme.medium.copy(fontSize = 16.sp, color = Black)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        SearchBar(
                            query = memberSearch,
                            onQueryChange = { memberSearch = it },
                            placeholder = "Search member by name or phone"
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (membersLoading && members.isEmpty()) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(vertical = 16.dp),
                                color = PrimaryColor
                            )
                        } else {
                            members.forEach { member ->
                                MemberRow(member)
                            }
                        }
                        if (!membersLoading && members.isEmpty()) {
                            Text(
                                text = "No members found",
                                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray),
                                modifier = Modifier
                                    .padding(vertical = 16.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Broadcast", style = AppTextTheme.bold.copy(fontSize = 16.sp, color = White))
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    @Composable
    private fun MemberRow(member: BroadcastMemberDTO) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ImageAvatar(size = 48.dp, url = member.user.avatarUrl)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.user.fullName ?: "Unnamed",
                    style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black)
                )
            }
            Text(
                text = member.user.phone ?: "",
                style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray)
            )
        }
    }
}
