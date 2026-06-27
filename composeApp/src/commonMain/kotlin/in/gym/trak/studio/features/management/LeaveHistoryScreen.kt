package `in`.gym.trak.studio.features.management

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.StatusBadge
import `in`.gym.trak.studio.data.model.LeaveDTO
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import org.jetbrains.compose.resources.painterResource


class LeaveHistoryScreen(

    private val applicantName: String? = null,
    private val applicantId: String? = null,
    private val applicantImageUrl: String? = null
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val leaves by screenModel.leaves.collectAsState()
        val isLoading by screenModel.leavesLoading.collectAsState()

        var selectedFilter by remember { mutableStateOf("All") }

        LaunchedEffect(selectedFilter) {
            val status = when (selectedFilter) {
                "Approve" -> "APPROVED"
                "Pending" -> "PENDING"
                "Rejected" -> "REJECTED"
                else -> null
            }
            screenModel.loadLeaves(status = status)
        }

        Scaffold(
            topBar = {
                GymAppBar(
                    title = "Leave History",
                    onBackClick = { navigator?.pop() },
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
                // Horizontal Tabs
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistoryFilterChip("All", selectedFilter == "All") { selectedFilter = "All" }
                    HistoryFilterChip("Approve", selectedFilter == "Approve") { selectedFilter = "Approve" }
                    HistoryFilterChip("Pending", selectedFilter == "Pending") { selectedFilter = "Pending" }
                    HistoryFilterChip("Rejected", selectedFilter == "Rejected") { selectedFilter = "Rejected" }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Remaining Balance Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE7F7F2))
                ) {
                    CommonButton(
                        onClick = {
                            navigator?.push(
                                ApplyLeaveScreen(
                                    applicantId = applicantId,
                                    applicantName = applicantName,
                                    applicantImageUrl = applicantImageUrl
                                )
                            )
                        },
                        text = "Create Leave",
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))


                if (leaves.isEmpty() && !isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No history found", style = AppTextTheme.medium.copy(color = Gray))
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(leaves) { leave: LeaveDTO -> HistoryLeaveCard(leave) }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryFilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(100.dp),
        color = if (isSelected) PrimaryColor else White,
        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFF1F5F9)),
        modifier = Modifier.height(36.dp)
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp), contentAlignment = Alignment.Center) {
            Text(text = label, style = AppTextTheme.bold.copy(fontSize = 13.sp, color = if (isSelected) White else Gray))
        }
    }
}

@Composable
fun HistoryLeaveCard(leave: LeaveDTO) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${leave.trainer_name} - ${leave.leave_type}",
                    style = AppTextTheme.bold.copy(fontSize = 15.sp, color = Black)
                )
                Text(
                    text = "${leave.start_date} - ${leave.end_date}  •  ${leave.days} Days",
                    style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray)
                )
            }
            StatusBadge(status = leave.status)
        }
    }
}


