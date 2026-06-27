package `in`.gym.trak.studio.features.dashboard

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Settings
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.annotation.ExperimentalVoyagerApi
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.stack.StackEvent
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.transitions.ScreenTransition
import coil3.compose.rememberAsyncImagePainter
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonOutlineButton
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.data.model.AttendancePunchResponse
import `in`.gym.trak.studio.data.model.effectiveAction
import `in`.gym.trak.studio.data.model.effectiveAttendedOn
import `in`.gym.trak.studio.data.model.effectiveCheckedInAt
import `in`.gym.trak.studio.data.model.effectiveCheckedOutAt
import `in`.gym.trak.studio.data.model.resolveActionTitle
import `in`.gym.trak.studio.data.model.resolveMessage
import `in`.gym.trak.studio.data.model.resolvePayload
import `in`.gym.trak.studio.data.model.resolveQrImage
import `in`.gym.trak.studio.data.model.resolveSuccess
import `in`.gym.trak.studio.utils.DateUtils
import `in`.gym.trak.studio.data.repository.SessionManager.userRole
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import kotlinx.coroutines.delay
import org.publicvalue.multiplatform.qrcode.CodeType
import org.publicvalue.multiplatform.qrcode.ScannerWithPermissions

@OptIn(ExperimentalVoyagerApi::class)
class AttendanceScannerScreen(
    private val autoLoadQrOnOpen: Boolean = true,
    private val openScannerDirectly: Boolean = false
) : Screen, ScreenTransition {
    override fun enter(lastEvent: StackEvent): EnterTransition = EnterTransition.None

    override fun exit(lastEvent: StackEvent): ExitTransition = ExitTransition.None

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val attendanceQr by screenModel.attendanceQr.collectAsState()
        val isLoading by screenModel.attendanceQrLoading.collectAsState()
        val punchLoading by screenModel.attendancePunchLoading.collectAsState()
        val punchResult by screenModel.attendancePunchResult.collectAsState()
        val errorMessage by screenModel.errorMessage.collectAsState()
        val snackbarHostState = remember { SnackbarHostState() }
        var showScanner by remember { mutableStateOf(openScannerDirectly) }
        var scannedValue by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(errorMessage) {
            val error = errorMessage ?: return@LaunchedEffect
            snackbarHostState.showSnackbar(error)
            screenModel.clearError()
        }

        LaunchedEffect(autoLoadQrOnOpen) {
            if (autoLoadQrOnOpen) {
                screenModel.loadMyAttendanceQr()
            }
        }

        val qrImage = attendanceQr?.resolveQrImage()
        val qrPayload = attendanceQr?.resolvePayload()
        val qrHint = attendanceQr?.hint
        val effectiveRole = (attendanceQr?.role ?: userRole).uppercase()
        val isTrainer = effectiveRole == "TRAINER" || effectiveRole == "GYM_TRAINER"
        val isMemberRole = userRole.equals("member", ignoreCase = true)
        val hasQr = !qrImage.isNullOrBlank()

        val dismissScanner: () -> Unit = {
            if (openScannerDirectly) {
                navigator?.pop()
            } else {
                showScanner = false
            }
        }

        val openScanner: () -> Unit = {
            scannedValue = null
            screenModel.clearAttendancePunchResult()
            showScanner = true
        }

        if (showScanner) {
            ScannerOverlay(
                modifier = Modifier.fillMaxSize(),
                onDismiss = dismissScanner,
                onScanned = { value ->
                    scannedValue = value
                    showScanner = false
                    screenModel.punchAttendanceByToken(value)
                }
            )
        } else {
            Scaffold(
                topBar = {
                    GymAppBar(
                        title = "Attendance QR Code",
                        onBackClick = { navigator?.pop() }
                    )
                },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                containerColor = White
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    if (hasQr) {
                        GeneratedQrContent(
                            qrImage = qrImage,
                            qrPayload = qrPayload,
                            qrHint = qrHint,
                            isLoading = isLoading,
                            isTrainer = isTrainer,
                            onTrainerPunch = { screenModel.punchTrainerAttendance() },
                            onScanQrCode = openScanner
                        )
                    } else {
                        GenerateQrEmptyState(
                            isLoading = isLoading,
                            showLoadQrButton = !isMemberRole,
                            onGenerate = { screenModel.loadMyAttendanceQr() },
                            onScanQrCode = openScanner
                        )
                    }
                }
            }
        }

        if (!scannedValue.isNullOrBlank() || punchResult != null || punchLoading) {
            ModalBottomSheet(
                onDismissRequest = {
                    scannedValue = null
                    screenModel.clearAttendancePunchResult()
                },
                containerColor = White
            ) {
                AttendancePunchResultSheetContent(
                    punchLoading = punchLoading,
                    punchResult = punchResult,
                    onDone = {
                        scannedValue = null
                        screenModel.clearAttendancePunchResult()
                    }
                )
            }
        }
    }
}

@Composable
private fun AttendancePunchResultSheetContent(
    punchLoading: Boolean,
    punchResult: AttendancePunchResponse?,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Attendance Punch",
            style = AppTextTheme.bold.copy(fontSize = 18.sp, color = Black)
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (punchLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF8FAFC))
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = PrimaryColor)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Processing scanned token…",
                        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val isSuccess = punchResult?.resolveSuccess() != false
            val action = punchResult?.effectiveAction()?.lowercase()
            val statusColor = if (isSuccess) PrimaryColor else Color(0xFFE53935)
            val statusIcon = when {
                !isSuccess -> Icons.Default.Error
                action == "clock_out" -> Icons.Default.Logout
                else -> Icons.Default.Login
            }

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isSuccess) punchResult?.resolveActionTitle() ?: "Success" else "Failed",
                style = AppTextTheme.bold.copy(fontSize = 20.sp, color = Black),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = punchResult?.resolveMessage() ?: "Scanned token received.",
                style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray),
                textAlign = TextAlign.Center
            )

            if (isSuccess && punchResult != null) {
                Spacer(modifier = Modifier.height(16.dp))
                AttendancePunchDetailsCard(result = punchResult)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        CommonButton(
            text = "Done",
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun AttendancePunchDetailsCard(result: AttendancePunchResponse) {
    val action = result.effectiveAction()?.lowercase()
    val attendedOn = formatAttendancePunchDate(result.effectiveAttendedOn())
    val checkedIn = formatAttendancePunchDateTime(result.effectiveCheckedInAt())
    val checkedOut = formatAttendancePunchDateTime(result.effectiveCheckedOutAt())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF8FAFC))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (attendedOn.isNotBlank()) {
            AttendancePunchDetailRow(label = "Attendance date", value = attendedOn)
        }
        when (action) {
            "clock_in" -> {
                if (checkedIn.isNotBlank()) {
                    AttendancePunchDetailRow(label = "Check-in time", value = checkedIn)
                }
            }
            "clock_out" -> {
                if (checkedIn.isNotBlank()) {
                    AttendancePunchDetailRow(label = "Check-in time", value = checkedIn)
                }
                if (checkedOut.isNotBlank()) {
                    AttendancePunchDetailRow(label = "Check-out time", value = checkedOut)
                }
            }
            else -> {
                if (checkedIn.isNotBlank()) {
                    AttendancePunchDetailRow(label = "Time", value = checkedIn)
                }
                if (checkedOut.isNotBlank()) {
                    AttendancePunchDetailRow(label = "Check-out", value = checkedOut)
                }
            }
        }
    }
}

@Composable
private fun AttendancePunchDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Gray),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = AppTextTheme.semiBold.copy(fontSize = 13.sp, color = Black),
            textAlign = TextAlign.End
        )
    }
}

private fun formatAttendancePunchDate(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return DateUtils.formatBirthDateForDisplay(iso).ifBlank { iso }
}

private fun formatAttendancePunchDateTime(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return DateUtils.formatShortDateTime(iso).ifBlank { iso }
}

@Composable
private fun GenerateQrEmptyState(
    isLoading: Boolean,
    showLoadQrButton: Boolean,
    onGenerate: () -> Unit,
    onScanQrCode: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = PrimaryColor,
            modifier = Modifier.size(52.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "QR code is not available yet.",
            style = AppTextTheme.bold.copy(fontSize = 20.sp, color = Black),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (showLoadQrButton) {
                "Tap Load QR Code to fetch your attendance QR."
            } else {
                "Your QR loads automatically when available. You can also scan a gym QR code to check in."
            },
            style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator(color = PrimaryColor)
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (showLoadQrButton) {
            CommonButton(
                text = "Load QR Code",
                enabled = !isLoading,
                onClick = onGenerate,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        CommonOutlineButton(
            text = "Scan QR Code",
            onClick = onScanQrCode,
            modifier = Modifier.fillMaxWidth(),
            borderColor = Color(0xFFE5E7EB),
            textColor = Black
        )
    }
}

@Composable
private fun GeneratedQrContent(
    qrImage: String?,
    qrPayload: String?,
    qrHint: String?,
    isLoading: Boolean,
    isTrainer: Boolean,
    onTrainerPunch: () -> Unit,
    onScanQrCode: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Show this QR to members for attendance check-in",
            style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF7F7F8)),
            contentAlignment = Alignment.Center
        ) {
            if (!qrImage.isNullOrBlank()) {
                Image(
                    painter = rememberAsyncImagePainter(model = qrImage),
                    contentDescription = "Gym Attendance QR",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )
            } else {
                Text(
                    text = "QR image not available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Gray
                )
            }
        }

//        if (!qrPayload.isNullOrBlank()) {
//            Spacer(modifier = Modifier.height(16.dp))
//            Text(
//                text = "Payload: $qrPayload",
//                style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray),
//                textAlign = TextAlign.Center
//            )
//        }

        if (!qrHint.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = qrHint,
                style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        if (isLoading) {
            CircularProgressIndicator(color = PrimaryColor)
            Spacer(modifier = Modifier.height(12.dp))
        }

//        if (isTrainer) {
//            CommonButton(
//                text = "Punch My Attendance",
//                enabled = !isLoading,
//                onClick = onTrainerPunch,
//                modifier = Modifier.fillMaxWidth()
//            )
//            Spacer(modifier = Modifier.height(10.dp))
//        }

        CommonOutlineButton(
            text = "Scan QR Code",
            onClick = onScanQrCode,
            modifier = Modifier.fillMaxWidth(),
            borderColor = Color(0xFFE5E7EB),
            textColor = Black
        )
    }
}

@Composable
private fun ScannerOverlay(
    onDismiss: () -> Unit,
    onScanned: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var flashlightOn by remember { mutableStateOf(false) }
    var cameraReady by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // iOS UIKitView needs layout to settle before the camera preview layer sizes correctly.
        delay(150)
        cameraReady = true
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            GymAppBar(
                title = "Scan QR Code",
                onBackClick = onDismiss,
                containerColor = Color.Black.copy(alpha = 0.55f),
                contentColor = White,
                actions = {
                    IconButton(onClick = { flashlightOn = !flashlightOn }) {
                        Icon(
                            imageVector = if (flashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Toggle Flash",
                            tint = White
                        )
                    }
                }
            )
        },
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (cameraReady) {
                ScannerWithPermissions(
                    onScanned = { result ->
                        onScanned(result)
                        true
                    },
                    types = listOf(CodeType.QR),
                    enableTorch = flashlightOn,
                    permissionText = "Camera access is required to scan QR codes.",
                    openSettingsLabel = "Open Settings",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = White)
                }
            }

            Text(
                text = "Point camera to a QR code",
                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = White),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp)
                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}
