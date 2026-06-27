package `in`.gym.trak.studio.features.trainers

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.PopupProperties
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.rememberAsyncImagePainter
import `in`.gym.trak.studio.components.*
import `in`.gym.trak.studio.data.model.*
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import `in`.gym.trak.studio.features.member.ImagePickerBottomSheet
import `in`.gym.trak.studio.theme.*
import gym.composeapp.generated.resources.*
import `in`.gym.trak.studio.getCurrentTimeMillis
import `in`.gym.trak.studio.utils.DateUtils
import `in`.gym.trak.studio.utils.PhoneNumberUtils
import io.github.ismoy.imagepickerkmp.domain.config.ImagePickerConfig
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import io.github.ismoy.imagepickerkmp.presentation.ui.components.ImagePickerLauncher
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource

import kotlin.jvm.Transient

class EditTrainerScreen(
    private val trainerId: String,
    private val initialData: TrainerDetailResponse,
    @Transient private val onRefresh: () -> Unit,
    private val isSelfEdit: Boolean = false,
    private val listRole: String = GymStaffListRole.TRAINER
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = remember { OwnerDashboardScreenModel() }

        // Split name (Handle nullability)
        val fullName = initialData.user?.fullName ?: ""
        val names = fullName.split(" ")
        var firstName by remember { mutableStateOf(names.getOrNull(0) ?: "") }
        var lastName by remember { mutableStateOf(names.drop(1).joinToString(" ")) }

        // DOB: API ISO / date-only → common display dd/MM/yyyy ([DateUtils.formatBirthDateForDisplay])
        var dob by remember {
            mutableStateOf(DateUtils.formatBirthDateForDisplay(initialData.profile?.dateOfBirth))
        }

        var gender by remember {
            mutableStateOf(initialData.profile?.gender?.replaceFirstChar { it.uppercase() }
                ?: "Male")
        }
        var address by remember { mutableStateOf(initialData.profile?.address ?: "") }
        var experience by remember { mutableStateOf(initialData.profile?.experience ?: "") }
        var salary by remember {
            mutableStateOf(
                initialData.profile?.salaryCents?.toString() ?: "0"
            )
        }
        var salaryDuration by remember {
            mutableStateOf(
                initialData.profile?.salaryPeriod?.lowercase()?.replaceFirstChar { it.uppercase() }
                    ?.replace("ly", "") ?: "Month"
            )
        }
        var expertise by remember { mutableStateOf(initialData.expertise.toSet()) }

        val focusManager = LocalFocusManager.current
        var phone by remember {
            mutableStateOf(PhoneNumberUtils.indianNationalDigitsForInput(initialData.user?.phone))
        }
        var email by remember { mutableStateOf(initialData.user?.email ?: "") }
        var avatarUrl by remember { mutableStateOf(initialData.user?.avatarUrl) }

        var shifts by remember {
            mutableStateOf(
                initialData.shifts.map { ShiftData("Shift", "${it.startTime} to ${it.endTime}", it.dayOfWeek) }
            )
        }
        var editingShiftIndex by remember { mutableStateOf<Int?>(null) }

        var showAddShiftSheet by remember { mutableStateOf(false) }
        var showDatePicker by remember { mutableStateOf(false) }
        var showImagePickerBottomSheet by remember { mutableStateOf(false) }
        var validationError by remember { mutableStateOf<String?>(null) }

        var launchGallery by remember { mutableStateOf(false) }
        var launchCamera by remember { mutableStateOf(false) }
        var pendingPickerAction by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(showImagePickerBottomSheet, pendingPickerAction) {
            if (showImagePickerBottomSheet || pendingPickerAction == null) return@LaunchedEffect
            when (pendingPickerAction) {
                "gallery" -> launchGallery = true
                "camera" -> launchCamera = true
            }
            pendingPickerAction = null
        }

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState(
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                        return utcTimeMillis <= getCurrentTimeMillis()
                    }
                }
            )

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.UTC)
                            dob = "${date.dayOfMonth}/${date.monthNumber}/${date.year}"
                        }
                        showDatePicker = false
                    }) {
                        Text("OK", color = PrimaryColor)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePicker = false }) {
                        Text("Cancel", color = Gray)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (showAddShiftSheet || editingShiftIndex != null) {
            val shiftToEdit = editingShiftIndex?.let { shifts.getOrNull(it) }
            val initialStartTime = shiftToEdit?.time?.split(" to ")?.getOrNull(0) ?: "08:00"
            val initialEndTime = shiftToEdit?.time?.split(" to ")?.getOrNull(1) ?: "09:00"
            val initialDays = shiftToEdit?.let { listOf(it.dayOfWeek) } ?: emptyList()

            AddShiftDialog(
                onDismiss = { 
                    showAddShiftSheet = false
                    editingShiftIndex = null
                },
                initialDays = initialDays,
                initialStartTime = initialStartTime,
                initialEndTime = initialEndTime,
                isEdit = shiftToEdit != null,
                onAddShift = { titleParam, start, end, selectedDays ->
                    if (editingShiftIndex != null) {
                        val index = editingShiftIndex!!
                        val firstDay = selectedDays.firstOrNull() ?: shiftToEdit!!.dayOfWeek
                        shifts = shifts.toMutableList().apply {
                            this[index] = ShiftData(titleParam, "$start to $end", firstDay)
                        }
                    } else {
                        selectedDays.forEach { day ->
                            shifts = shifts + ShiftData(titleParam, "$start to $end", day)
                        }
                    }
                    showAddShiftSheet = false
                    editingShiftIndex = null
                }
            )
        }


        LoadingScreenHandler(screenModel = screenModel) {
            Scaffold(
                topBar = {
                    GymAppBar(
                        title = if (listRole == GymStaffListRole.STAFF) "Edit Staff" else "Edit Trainer",
                        onBackClick = { navigator?.pop() }
                    )
                },
                containerColor = Color.Transparent
            ) { padding ->
                AppScrollableScreen(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFF7E6))
                                .border(2.dp, Color(0xFFF1F5F9), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val painter = if (avatarUrl != null)
                                rememberAsyncImagePainter(avatarUrl!!)
                            else painterResource(Res.drawable.gym_boy)

                            Image(
                                painter = painter,
                                contentDescription = "Profile Picture",
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(PrimaryColor)
                                .border(2.dp, White, CircleShape)
                                .clickable { showImagePickerBottomSheet = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Update photo",
                        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    LabeledField(label = "First Name *") {
                        CommonTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            placeholder = "eg. John",
                            leadingIconDrawable = Res.drawable.userIcon
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledField(label = "Last Name *") {
                        CommonTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            placeholder = "eg. Doe",
                            leadingIconDrawable = Res.drawable.userIcon
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledField(label = "Phone Number *") {
                        CommonTextField(
                            value = phone,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number
                            ),
                            onValueChange = {
                                val digits = it.filter { c -> c.isDigit() }.take(10)
                                phone = digits
                                if (digits.length == 10) focusManager.clearFocus()
                            },
                            placeholder = "10-digit mobile number",
                            leadingIconDrawable = Res.drawable.ic_phone
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledField(label = "Email") {
                        CommonTextField(
                            value = email,
                            onValueChange = { email = it },
                            placeholder = "eg. john@example.com",
                            leadingIconDrawable = Res.drawable.ic_message
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledField(label = "Date of birth") {
                        DatePickerField(
                            value = dob,
                            onPickerClick = { showDatePicker = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledField(label = "Gender") {
                        GenderSelector(
                            selectedGender = gender,
                            onGenderSelected = { gender = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledField(label = "Address") {
                        CommonTextField(
                            value = address,
                            onValueChange = { address = it },
                            placeholder = "At-Rajkot",
                            leadingIconDrawable = Res.drawable.ic_location
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledField(label = "Experience") {
                        CommonTextField(
                            value = experience,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number
                            ),
                            onValueChange = { experience = it },
                            placeholder = "5+ Years",
                            leadingIconDrawable = Res.drawable.ic_experience
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        LabeledField(label = "Salary *", modifier = Modifier.weight(1f)) {
                            CommonTextField(
                                value = salary,
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Number
                                ),
                                onValueChange = { salary = it },
                                placeholder = "$ 50",
                                leadingIconDrawable = Res.drawable.ic_salary
                            )
                        }
                        LabeledField(label = "Duration", modifier = Modifier.weight(1f)) {
                            CommonDropdown(
                                options = listOf("Month", "Week", "Year"),
                                selectedOption = salaryDuration,
                                onOptionSelected = { salaryDuration = it }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledField(label = "Trainer Expertise *") {
                        ExpertiseSelector(
                            options = listOf(
                                "Personal Trainer",
                                "Conditioning Coach",
                                "CrossFit Coach",
                                "Bodybuilding Coach",
                                "Yoga Specialist",
                                "Nutritionist"
                            ),
                            selectedExpertise = expertise,
                            onToggle = { option ->
                                expertise = if (expertise.contains(option)) {
                                    expertise - option
                                } else {
                                    expertise + option
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Add Shift",
                            style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    shifts.forEachIndexed { index, shift ->
                        ShiftListItem(
                            shift = shift,
                            onDelete = { shifts = shifts.toMutableList().apply { removeAt(index) } },
                            onEdit = { editingShiftIndex = index }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .drawBehind {
                                drawRoundRect(
                                    color = Gray.copy(alpha = 0.5f),
                                    style = Stroke(
                                        width = 3f,
                                        pathEffect = PathEffect.dashPathEffect(
                                            floatArrayOf(15f, 15f), 0f
                                        )
                                    ),
                                    cornerRadius = CornerRadius(100.dp.toPx())
                                )
                            }
                            .clickable { showAddShiftSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Add Shift",
                            style = AppTextTheme.medium.copy(fontSize = 15.sp, color = Gray)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    if (validationError != null) {
                        Text(
                            text = validationError!!,
                            color = RedColor,
                            style = AppTextTheme.medium.copy(fontSize = 12.sp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    CommonButton(
                        onClick = {
                            val emailRegex =
                                Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
                            when {
                                firstName.isBlank() -> validationError = "First name is required"
                                lastName.isBlank() -> validationError = "Last name is required"
                                phone.isBlank() -> validationError = "Phone number is required"
                                phone.length != 10 -> validationError =
                                    "Enter a valid 10-digit mobile number"

                                email.isNotBlank() && !emailRegex.matches(email) -> validationError =
                                    "Enter a valid email address"

                                salary.isBlank() -> validationError = "Salary is required"
                                expertise.isEmpty() -> validationError =
                                    "Select at least one expertise"

                                else -> {
                                    validationError = null
                                    
                                    // Convert DOB back to ISO
                                    val isoDob = dob.split("/").let { parts ->
                                        if (parts.size == 3) "${parts[2]}-${
                                            parts[1].padStart(
                                                2,
                                                '0'
                                            )
                                        }-${parts[0].padStart(2, '0')}" else null
                                    }

                                    if (isSelfEdit) {
                                        val request = UpdateTrainerProfileRequest(
                                            role = listRole,
                                            fullName = "$firstName $lastName".trim(),
                                            avatarUrl = avatarUrl,
                                            profile_image = avatarUrl,
                                            firstName = firstName,
                                            lastName = lastName,
                                            dateOfBirth = isoDob,
                                            gender = gender.lowercase(),
                                            experience = experience.takeIf { it.isNotBlank() },
                                            address = address.takeIf { it.isNotBlank() },
                                            expertise = expertise.toList(),
                                            salary = salary.toIntOrNull(),
                                            salaryCents = salary.toIntOrNull()?.let { it * 100 },
                                            salaryPeriod = when (salaryDuration) {
                                                "Month" -> "MONTHLY"
                                                "Week" -> "WEEKLY"
                                                "Year" -> "YEARLY"
                                                "Hour" -> "HOURLY"
                                                else -> "MONTHLY"
                                            },
                                            shifts = shifts.map { s ->
                                                val times = s.time.split(" to ")
                                                TrainerShiftUpdate(
                                                    dayOfWeek = s.dayOfWeek,
                                                    startTime = times.getOrNull(0) ?: "",
                                                    endTime = times.getOrNull(1) ?: ""
                                                )
                                            }
                                        )
                                        screenModel.updateTrainerSelfProfile(request) {
                                            onRefresh()
                                            navigator?.pop()
                                        }
                                    } else {
                                        val request = UpdateTrainerRequest(
                                            role = listRole,
                                            fullName = "$firstName $lastName".trim(),
                                            email = email.trim().takeIf { it.isNotBlank() },
                                            avatarUrl = avatarUrl,
                                            dateOfBirth = isoDob,
                                            gender = gender.lowercase(),
                                            experience = experience.takeIf { it.isNotBlank() },
                                            address = address.takeIf { it.isNotBlank() },
                                            expertise = expertise.toList(),
                                            salaryCents = salary.toIntOrNull(),
                                            salaryPeriod = when (salaryDuration) {
                                                "Month" -> "MONTHLY"
                                                "Week" -> "WEEKLY"
                                                "Year" -> "YEARLY"
                                                "Hour" -> "HOURLY"
                                                else -> "MONTHLY"
                                            },
                                            shifts = shifts.map { s ->
                                                val times = s.time.split(" to ")
                                                TrainerShiftRequest(
                                                    dayOfWeek = s.dayOfWeek,
                                                    startTime = times.getOrNull(0) ?: "",
                                                    endTime = times.getOrNull(1) ?: ""
                                                )
                                            },
                                            permissions = initialData.permissions,
                                            isActive = initialData.isActive
                                        )
                                        screenModel.updateTrainer(
                                            trainerId,
                                            request,
                                            onSuccess = {
                                                onRefresh()
                                                navigator?.pop()
                                            },
                                            role = listRole
                                        )
                                    }
                                }
                            }
                        },
                        text = "Update Trainer",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            if (showImagePickerBottomSheet) {
                ImagePickerBottomSheet(
                    onDismiss = {
                        showImagePickerBottomSheet = false
                        pendingPickerAction = null
                    },
                    onGalleryClick = {
                        showImagePickerBottomSheet = false
                        pendingPickerAction = "gallery"
                    },
                    onCameraClick = {
                        showImagePickerBottomSheet = false
                        pendingPickerAction = "camera"
                    },
                    title = "Update Profile Photo",
                    subtitle = "Choose from gallery or take a new picture"
                )
            }

            if (launchGallery) {
                GalleryPickerLauncher(
                    onPhotosSelected = { photos ->
                        launchGallery = false
                        photos.firstOrNull()?.let { photo ->
                            screenModel.uploadImage(
                                photo.loadBytes(),
                                "trainer_avatar.jpg"
                            ) { uploadedUrl ->
                                if (uploadedUrl != null) avatarUrl = uploadedUrl
                            }
                        }
                    },
                    onError = { launchGallery = false },
                    onDismiss = { launchGallery = false }
                )
            }

            if (launchCamera) {
                ImagePickerLauncher(
                    config = ImagePickerConfig(
                        onPhotoCaptured = { photo ->
                            launchCamera = false
                            screenModel.uploadImage(
                                photo.loadBytes(),
                                "trainer_avatar.jpg"
                            ) { uploadedUrl ->
                                if (uploadedUrl != null) avatarUrl = uploadedUrl
                            }
                        },
                        onError = { launchCamera = false }
                    )
                )
            }
        }
    }
}

@Composable
fun ExpertiseSelector(
    options: List<String>,
    selectedExpertise: Set<String>,
    onToggle: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Column {
        Box(modifier = Modifier.fillMaxWidth()) {
            CommonTextField(
                value = query,
                onValueChange = {
                    query = it
                    expanded = true
                },
                placeholder = "Select or type expertise",
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (query.isNotBlank()) {
                        onToggle(query.trim())
                        query = ""
                        expanded = false
                    }
                    focusManager.clearFocus()
                }),
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Dropdown",
                        tint = Gray,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { expanded = !expanded }
                            .padding(4.dp)
                    )
                }
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .background(Color.White, RoundedCornerShape(16.dp)),
                properties = PopupProperties(focusable = false)
            ) {
                val filteredOptions = if (query.isBlank()) options else options.filter {
                    it.contains(
                        query,
                        ignoreCase = true
                    )
                }

                if (query.isNotBlank() && !options.any {
                        it.equals(
                            query,
                            ignoreCase = true
                        )
                    }) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Add custom: \"$query\"",
                                style = AppTextTheme.semiBold.copy(
                                    fontSize = 14.sp,
                                    color = PrimaryColor
                                )
                            )
                        },
                        onClick = {
                            onToggle(query.trim())
                            query = ""
                            expanded = false
                        }
                    )
                }

                filteredOptions.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                option,
                                style = AppTextTheme.medium.copy(
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            )
                        },
                        onClick = {
                            onToggle(option)
                            query = ""
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedExpertise.forEach { expertise ->
                ExpertiseChip(expertise, onRemove = { onToggle(expertise) })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpertiseChip(expertise: String, onRemove: () -> Unit) {
    Surface(
        modifier = Modifier.height(40.dp),
        shape = RoundedCornerShape(100.dp),
        color = White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8ECF4))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = expertise,
                style = AppTextTheme.medium.copy(fontSize = 13.sp, color = Gray)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove",
                modifier = Modifier.size(16.dp).clickable { onRemove() },
                tint = Gray
            )
        }
    }
}


