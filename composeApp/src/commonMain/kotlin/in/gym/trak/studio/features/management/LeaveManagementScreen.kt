package `in`.gym.trak.studio.features.management

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import `in`.gym.trak.studio.components.*
import `in`.gym.trak.studio.data.model.LeaveDTO
import `in`.gym.trak.studio.data.model.TrainerDTO
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.data.repository.SessionManager.hasPermission
import `in`.gym.trak.studio.theme.*
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import gym.composeapp.generated.resources.*
import `in`.gym.trak.studio.getCurrentTimeMillis
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource

data class MonthOption(val label: String, val value: String)

/**
 * Screen for managing trainer leave requests (Image 2).
 */
class LeaveManagementScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val leaves by screenModel.leaves.collectAsState()
        val isLoading by screenModel.leavesLoading.collectAsState()
        val trainers by screenModel.trainers.collectAsState()

        var selectedTrainer by remember { mutableStateOf<TrainerDTO?>(null) }
        var selectedTrainerId by remember { mutableStateOf<String?>(null) }
        var selectedMonthOption by remember { mutableStateOf<MonthOption?>(null) }
        var selectedStatus by remember { mutableStateOf<String?>(null) }
        var showSearchBar by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }


        var showRejectDialog by remember { mutableStateOf(false) }
        var rejectRequestId by remember { mutableStateOf<String?>(null) }


        val statusOptions = listOf("PENDING", "APPROVED", "REJECTED", "CANCELLED")
        val months = remember {
            val list = mutableListOf<MonthOption>()
            val current = Instant.fromEpochMilliseconds(getCurrentTimeMillis())
            val timeZone = kotlinx.datetime.TimeZone.currentSystemDefault()
            val today = current.toLocalDateTime(timeZone)

            // Generate last 6 months
            for (i in 0 until 6) {
                var targetMonth = today.monthNumber - i
                var targetYear = today.year
                while (targetMonth <= 0) {
                    targetMonth += 12
                    targetYear -= 1
                }

                val monthName = when (targetMonth) {
                    1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
                    7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
                    else -> ""
                }
                val monthValue = "${targetYear}-${targetMonth.toString().padStart(2, '0')}"
                list.add(MonthOption("$monthName $targetYear", monthValue))
            }
            list
        }




        LaunchedEffect(Unit) {
            screenModel.loadTrainers()
            screenModel.loadLeaves()
        }

        LaunchedEffect(selectedTrainerId, selectedMonthOption, selectedStatus, searchQuery) {
            screenModel.loadLeaves(
                trainerId = selectedTrainerId,
                month = selectedMonthOption?.value,
                status = selectedStatus,
                q = searchQuery.takeIf { it.isNotBlank() }
            )
        }


        if (showRejectDialog && rejectRequestId != null) {
            RejectReasonDialog(
                onDismiss = {
                    showRejectDialog = false
                    rejectRequestId = null
                },
                onConfirm = { reason ->
                    screenModel.rejectLeave(rejectRequestId!!, reason) {
                        screenModel.loadLeaves(
                            trainerId = selectedTrainerId,
                            month = selectedMonthOption?.value,
                            status = selectedStatus
                        )
                        showRejectDialog = false
                        rejectRequestId = null
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                GymAppBar(
                    title = "Leave Management",
                    onBackClick = { navigator?.pop() },
                    actions = {
                        IconButton(onClick = {
                            showSearchBar = !showSearchBar
                            if (!showSearchBar) searchQuery = ""
                        }) {
                            Icon(
                                imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Black,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                if (showSearchBar) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search by trainer name",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                }

                // Filters
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val trainerOptions = remember(trainers) {
                        listOf(TrainerDTO(fullName = "All Trainers", gymUserId = "")) + trainers
                    }

                    CommonDropdown(
                        options = trainerOptions,
                        selectedOption = selectedTrainer ?: trainerOptions.first(),
                        onOptionSelected = {
                            selectedTrainer = it
                            selectedTrainerId = if (it.gymUserId.isEmpty()) null else it.gymUserId
                        },
                        modifier = Modifier.weight(1.3f),
                        placeholder = "Trainer",
                        optionToString = { it.fullName }
                    )

                    CommonDropdown(
                        options = months,
                        selectedOption = selectedMonthOption,
                        onOptionSelected = {
                            selectedMonthOption = it
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = "Month",
                        optionToString = { it.label }
                    )


                    CommonDropdown(
                        options = listOf("All") + statusOptions,
                        selectedOption = selectedStatus ?: "All",
                        onOptionSelected = {
                            selectedStatus = if (it == "All") null else it
                        },
                        modifier = Modifier.weight(1f),
                        placeholder = "Status"
                    )
                }




                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE7F7F2))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Apply for leave",
                                style = AppTextTheme.bold.copy(fontSize = 14.sp, color = Black)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Submit a new leave request.",
                                style = AppTextTheme.medium.copy(fontSize = 13.sp, color = Gray)
                            )
                        }
                        Button(
                            onClick = {
                                navigator?.push(
                                    ApplyLeaveScreen(showTrainerSelection = true)
                                )
                            },
                            shape = RoundedCornerShape(100.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                        ) {
                            Text(
                                "Create Leave",
                                style = AppTextTheme.bold.copy(fontSize = 14.sp, color = White)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (leaves.isEmpty() && !isLoading) {
                    AppEmptyStateView(
                        image = Res.drawable.img_no_leave,
                        title = "No Leave Requests Found",
                        subtitle = if (searchQuery.isNotBlank()) {
                            "No leave records match your search."
                        } else {
                            "No leave requests are available for the selected filters."
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(leaves) { request: LeaveDTO ->
                            LeaveManagementCard(
                                request = request,
                                onApprove = { id ->
                                    screenModel.approveLeave(id) {
                                        screenModel.loadLeaves(
                                            trainerId = selectedTrainerId,
                                            month = selectedMonthOption?.value,
                                            status = selectedStatus
                                        )
                                    }
                                },
                                onReject = { id ->
                                    rejectRequestId = id
                                    showRejectDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun LeaveManagementCard(
    request: LeaveDTO,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    CommonCard(
        content = {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(Res.drawable.gym_boy),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp).clip(CircleShape)
                            .border(1.dp, Color(0xFFF1F5F9), CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${request.trainer_name} - ${request.leave_type}",
                            style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black)
                        )
                        Text(
                            text = "${request.start_date} - ${request.end_date}  •  ${request.days} Days",
                            style = AppTextTheme.medium.copy(fontSize = 13.sp, color = Gray)
                        )
                    }
                    StatusBadge(status = request.status)
                }

                if (request.status.uppercase() == "PENDING") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (
                            hasPermission(
                                SessionManager.PermissionKeys.KEY_LEAVE_REJECT
                            )
                        )
                            CommonOutlineButton(
                                text = "Reject",
                                onClick = { onReject(request.id) },
                                modifier = Modifier.weight(1f).height(40.dp),
                                borderColor = GrayBorderColor,
                                textColor = Black
                            )
                        if (
                            hasPermission(
                                SessionManager.PermissionKeys.KEY_LEAVE_APPROVE
                            )
                        )
                            CommonButton(
                                text = "Approve",
                                onClick = { onApprove(request.id) },
                                modifier = Modifier.weight(1f).height(40.dp)
                            )
                    }
                }
            }
        }
    )
}

@Composable
fun RejectReasonDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = White,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Reject Leave",
                    style = AppTextTheme.bold.copy(fontSize = 20.sp, color = Black)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Please provide a reason for rejecting this leave request.",
                    style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(20.dp))

                CommonTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    placeholder = "Enter reason here...",
                    modifier = Modifier.fillMaxWidth(),
                    isMultiline = true,
                    singleLine = false
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CommonOutlineButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        borderColor = GrayBorderColor,
                        textColor = Black
                    )
                    CommonButton(
                        text = "Reject",
                        onClick = {
                            if (reason.isNotBlank()) {
                                onConfirm(reason)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        color = RedColor,
                        enabled = reason.isNotBlank()
                    )
                }
            }
        }
    }
}


