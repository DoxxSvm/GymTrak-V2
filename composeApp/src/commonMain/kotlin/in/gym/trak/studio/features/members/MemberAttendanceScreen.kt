package `in`.gym.trak.studio.features.members

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import `in`.gym.trak.studio.components.ConfirmationDialog
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel

/**
 * Standalone attendance view for a member, using the same UI/API as the Attendance tab on [MemberDetailScreen].
 */
class MemberAttendanceScreen(
    private val memberId: String,
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val memberDetail by screenModel.memberDetail.collectAsState()
        val attendanceSummary by screenModel.memberAttendanceSummary.collectAsState()
        var showDeleteBiometricDialog by remember { mutableStateOf(false) }
        var selectedAttendanceDate by rememberSaveable { mutableStateOf<String?>(null) }

        if (showDeleteBiometricDialog) {
            ConfirmationDialog(
                onDismissRequest = { showDeleteBiometricDialog = false },
                onConfirm = {
                    showDeleteBiometricDialog = false
                },
                title = "Delete Biometric",
                message = "Are you sure you want to delete this member's biometric data?",
                confirmText = "Delete",
                isDangerAction = true
            )
        }

        LaunchedEffect(memberId) {
            if (memberId.isNotBlank()) {
                screenModel.loadMemberDetail(memberId)
            }
        }

        val gymUserId = memberDetail?.gymUserId?.takeIf { it.isNotBlank() } ?: memberId
        LaunchedEffect(gymUserId, selectedAttendanceDate) {
            if (gymUserId.isNotBlank()) {
                loadMemberAttendanceSummaryForDate(
                    screenModel = screenModel,
                    memberId = gymUserId,
                    dateIso = selectedAttendanceDate,
                )
            }
        }

        LoadingScreenHandler(screenModel = screenModel) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    GymAppBar(
                        title = "Attendance",
                        onBackClick = { navigator?.pop() }
                    )
                }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    contentAlignment = if (attendanceSummary == null) Alignment.Center else Alignment.TopCenter
                ) {
                    if (attendanceSummary == null) {
                        MemberDetailAttendanceContent(
                            memberDetail = memberDetail,
                            attendanceSummary = null,
                            onDeleteBiometric = { showDeleteBiometricDialog = true },
                            selectedFilterDate = selectedAttendanceDate,
                            onFilterDateChange = { selectedAttendanceDate = it },
                        )
                    } else {
                        AppScrollableScreen(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            MemberDetailAttendanceContent(
                                memberDetail = memberDetail,
                                attendanceSummary = attendanceSummary,
                                onDeleteBiometric = { showDeleteBiometricDialog = true },
                                selectedFilterDate = selectedAttendanceDate,
                                onFilterDateChange = { selectedAttendanceDate = it },
                                showViewAllHistoryButton = false,
                            )
                        }
                    }
                }
            }
        }
    }
}
