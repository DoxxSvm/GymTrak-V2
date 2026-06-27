package `in`.gym.trak.studio.features.management

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import coil3.compose.rememberAsyncImagePainter
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.CommonDropdown
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.DatePickerField
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.data.model.GymStaffListRole
import `in`.gym.trak.studio.data.model.TrainerDTO
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import gym.composeapp.generated.resources.*
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import `in`.gym.trak.studio.getCurrentTimeMillis
import org.jetbrains.compose.resources.painterResource

private val leaveTypeOptions = listOf(
    "Medical Leave",
    "Personal Leave",
    "Maternity Leave",
    "Casual Leave"
)

private fun displayLeaveTypeToApi(display: String): String? = when (display) {
    "Medical Leave" -> "MEDICAL"
    "Personal Leave" -> "PERSONAL"
    "Maternity Leave" -> "MATERNITY"
    "Casual Leave" -> "CASUAL"
    else -> null
}

private fun todayLocal(): LocalDate =
    Instant.fromEpochMilliseconds(getCurrentTimeMillis()).toLocalDateTime(TimeZone.currentSystemDefault()).date

private fun millisToLocalDate(utcTimeMillis: Long): LocalDate =
    Instant.fromEpochMilliseconds(utcTimeMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date

/**
 * Screen for creating a new leave request.
 * Integrates POST /leaves; dates cannot be before today; end date must be on or after start date.
 */
class ApplyLeaveScreen(
    private val applicantName: String? = null,
    private val applicantId: String? = null,
    private val applicantImageUrl: String? = null,
    /** When true (e.g. opened from Profile → Leave Management), show trainer picker for the leave subject. */
    private val showTrainerSelection: Boolean = false,
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val profile by screenModel.profileData.collectAsState()
        val trainers by screenModel.trainers.collectAsState()

        var selectedTrainer by remember { mutableStateOf<TrainerDTO?>(null) }
        var trainerSelectionError by remember { mutableStateOf<String?>(null) }

        var startDate by remember { mutableStateOf("") }
        var endDate by remember { mutableStateOf("") }
        var leaveType by remember { mutableStateOf("") }
        var reason by remember { mutableStateOf("") }

        var showStartDatePicker by remember { mutableStateOf(false) }
        var showEndDatePicker by remember { mutableStateOf(false) }

        var startDateError by remember { mutableStateOf<String?>(null) }
        var endDateError by remember { mutableStateOf<String?>(null) }
        var typeError by remember { mutableStateOf<String?>(null) }
        var reasonError by remember { mutableStateOf<String?>(null) }

        val today = remember { todayLocal() }

        LaunchedEffect(showTrainerSelection) {
            if (showTrainerSelection) {
                screenModel.setTrainersListRole(GymStaffListRole.TRAINER)
                screenModel.loadTrainers()
            }
        }

        LaunchedEffect(applicantName, showTrainerSelection) {
            if (!showTrainerSelection && applicantName.isNullOrBlank()) {
                screenModel.loadProfile()
            }
        }

        val resolvedApplicantName = when {
            showTrainerSelection ->
                selectedTrainer?.fullName?.takeIf { it.isNotBlank() } ?: "Select a trainer"
            else ->
                applicantName?.takeIf { it.isNotBlank() }
                    ?: profile?.personalInfo?.fullName?.takeIf { it.isNotBlank() }
                    ?: "User"
        }
        val resolvedApplicantImageUrl = when {
            showTrainerSelection ->
                selectedTrainer?.avatarUrl?.takeIf { it.isNotBlank() }
            else ->
                applicantImageUrl?.takeIf { it.isNotBlank() }
                    ?: profile?.personalInfo?.profileImage?.takeIf { it.isNotBlank() }
        }
        val resolvedApplicantRole = when {
            showTrainerSelection -> "Trainer"
            !applicantName.isNullOrBlank() -> "Trainer"
            else ->
                SessionManager.userRole
                    .replace('_', ' ')
                    .lowercase()
                    .replaceFirstChar { it.uppercase() }
                    .ifBlank { "Gym User" }
        }

        val minEndDate = remember(startDate) {
            if (startDate.isBlank()) return@remember today
            try {
                val start = LocalDate.parse(startDate)
                maxOf(start, today)
            } catch (_: Exception) {
                today
            }
        }

        if (showStartDatePicker) {
            val startPickerState = rememberDatePickerState(
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        val d = millisToLocalDate(utcTimeMillis)
                        return d >= today
                    }
                }
            )
            DatePickerDialog(
                onDismissRequest = { showStartDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val selectedDate = startPickerState.selectedDateMillis?.let(::millisToLocalDate)
                            if (selectedDate != null) {
                                startDate = selectedDate.toString()
                                startDateError = null
                                if (endDate.isNotEmpty()) {
                                    try {
                                        val end = LocalDate.parse(endDate)
                                        if (end < selectedDate) {
                                            endDate = ""
                                            endDateError = null
                                        }
                                    } catch (_: Exception) {
                                        endDate = ""
                                        endDateError = null
                                    }
                                }
                            }
                            showStartDatePicker = false
                        }
                    ) { Text("OK", color = PrimaryColor) }
                },
                dismissButton = {
                    TextButton(onClick = { showStartDatePicker = false }) { Text("Cancel", color = Gray) }
                }
            ) {
                DatePicker(state = startPickerState)
            }
        }

        if (showEndDatePicker) {
            val endPickerState = rememberDatePickerState(
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        val d = millisToLocalDate(utcTimeMillis)
                        return d >= minEndDate
                    }
                }
            )
            DatePickerDialog(
                onDismissRequest = { showEndDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val selectedDate = endPickerState.selectedDateMillis?.let(::millisToLocalDate)
                            if (selectedDate != null) {
                                endDate = selectedDate.toString()
                                endDateError = null
                            }
                            showEndDatePicker = false
                        }
                    ) { Text("OK", color = PrimaryColor) }
                },
                dismissButton = {
                    TextButton(onClick = { showEndDatePicker = false }) { Text("Cancel", color = Gray) }
                }
            ) {
                DatePicker(state = endPickerState)
            }
        }

        fun validate(): Boolean {
            var isValid = true
            startDateError = null
            endDateError = null
            typeError = null
            reasonError = null
            trainerSelectionError = null

            if (startDate.isEmpty()) {
                startDateError = "Please select a start date."
                isValid = false
            }
            if (endDate.isEmpty()) {
                endDateError = "Please select an end date."
                isValid = false
            }
            if (leaveType.isEmpty()) {
                typeError = "Please select a leave type."
                isValid = false
            }
            if (reason.isBlank()) {
                reasonError = "Please enter a reason for leave."
                isValid = false
            }
            if (showTrainerSelection) {
                val id = selectedTrainer?.gymUserId?.trim().orEmpty()
                if (selectedTrainer == null || id.isEmpty()) {
                    trainerSelectionError = "Please select a trainer."
                    isValid = false
                }
            }

            val startParsed = try {
                if (startDate.isEmpty()) null else LocalDate.parse(startDate)
            } catch (_: Exception) {
                startDateError = "Please select a valid start date."
                isValid = false
                null
            }

            val endParsed = try {
                if (endDate.isEmpty()) null else LocalDate.parse(endDate)
            } catch (_: Exception) {
                endDateError = "Please select a valid end date."
                isValid = false
                null
            }

            if (startParsed != null && startParsed < today) {
                startDateError = "Start date cannot be earlier than today."
                isValid = false
            }

            if (startParsed != null && endParsed != null) {
                if (endParsed < startParsed) {
                    endDateError = "End date cannot be earlier than start date."
                    isValid = false
                }
            }

            if (leaveType.isNotEmpty() && displayLeaveTypeToApi(leaveType) == null) {
                typeError = "Please select a valid leave type."
                isValid = false
            }

            return isValid
        }

        LoadingScreenHandler(screenModel = screenModel) {
            Scaffold(
                topBar = {
                    GymAppBar(
                        title = "Apply For Leave",
                        onBackClick = { navigator?.pop() },
                    )
                },
                containerColor = Color.Transparent
            ) { padding ->
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding()
                ) {
                    AppScrollableScreen(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter),
                        scrollState = scrollState,
                    ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    CommonCard(
                        content = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Image(
                                    painter = resolvedApplicantImageUrl?.let { rememberAsyncImagePainter(it) }
                                        ?: painterResource(Res.drawable.gym_boy),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .border(1.dp, Color.White, CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text(
                                        text = resolvedApplicantName,
                                        style = AppTextTheme.semiBold.copy(fontSize = 14.sp, color = Black)
                                    )
                                    Text(
                                        text = resolvedApplicantRole,
                                        style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
                                    )
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    if (showTrainerSelection) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_workout),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Trainer",
                                style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        CommonDropdown(
                            options = trainers,
                            selectedOption = selectedTrainer,
                            onOptionSelected = {
                                selectedTrainer = it
                                trainerSelectionError = null
                            },
                            placeholder = "Select trainer",
                            optionToString = { it.fullName.ifBlank { it.username.orEmpty() } },
                            errorText = trainerSelectionError,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    Text(
                        text = "Select Date Range",
                        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Start Date", style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black))
                            Spacer(modifier = Modifier.height(8.dp))
                            DatePickerField(
                                value = startDate,
                                placeholder = "Select",
                                onPickerClick = { showStartDatePicker = true }
                            )
                            startDateError?.let {
                                Text(text = it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "End Date", style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black))
                            Spacer(modifier = Modifier.height(8.dp))
                            DatePickerField(
                                value = endDate,
                                placeholder = "Select",
                                onPickerClick = { showEndDatePicker = true }
                            )
                            endDateError?.let {
                                Text(text = it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_leave_type),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Leave Type",
                            style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    CommonDropdown(
                        options = leaveTypeOptions,
                        selectedOption = leaveType.ifEmpty { null },
                        onOptionSelected = {
                            leaveType = it
                            typeError = null
                        },
                        placeholder = "Select type"
                    )
                    typeError?.let {
                        Text(text = it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_recent),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Reason for Leave",
                            style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    CommonTextField(
                        value = reason,
                        onValueChange = {
                            reason = it
                            reasonError = null
                        },
                        placeholder = "Explain your reason for leave...",
                        isMultiline = true,
                        borderRadius = 12.dp
                    )
                    reasonError?.let {
                        Text(text = it, color = Color.Red, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    CommonButton(
                        onClick = {
                            if (!validate()) return@CommonButton
                            val apiType = displayLeaveTypeToApi(leaveType) ?: return@CommonButton
                            val trainerIdForRequest = if (showTrainerSelection) {
                                selectedTrainer?.gymUserId?.trim().orEmpty()
                            } else {
                                applicantId.orEmpty()
                            }
                            screenModel.createLeave(
                                leaveTypeApi = apiType,
                                startDateIso = startDate,
                                endDateIso = endDate,
                                trainerId = trainerIdForRequest,
                                reason = reason,
                                onSuccess = { navigator?.pop() }
                            )
                        },
                        text = "Apply for Leave",
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}
