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
import androidx.compose.runtime.saveable.rememberSaveable
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
import `in`.gym.trak.studio.data.model.CreateTrainerCompatRequest
import `in`.gym.trak.studio.data.model.GymStaffListRole
import `in`.gym.trak.studio.data.model.TrainerCompatShiftRequest
import `in`.gym.trak.studio.data.model.TrainerCredentials
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.features.location.SelectAddressScreen
import `in`.gym.trak.studio.features.member.ImagePickerBottomSheet
import `in`.gym.trak.studio.theme.*
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import gym.composeapp.generated.resources.*
import `in`.gym.trak.studio.getCurrentTimeMillis
import io.github.ismoy.imagepickerkmp.domain.config.ImagePickerConfig
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import io.github.ismoy.imagepickerkmp.presentation.ui.components.ImagePickerLauncher
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource

import kotlin.jvm.Transient

private data class AddTrainerFormErrors(
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val salary: String? = null,
    val expertise: String? = null,
    val trainerId: String? = null,
    val password: String? = null
) {
    /** True if any field failed validation (non-null error message). */
    fun hasErrors(): Boolean =
        listOf(firstName, lastName, phone, email, salary, expertise, trainerId, password).any { it != null }
}

class AddTrainerScreen(
    private val listRole: String = GymStaffListRole.TRAINER,
    @Transient private val onTrainerAdded: (() -> Unit)? = null
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val activeScreenModel = remember { OwnerDashboardScreenModel() }

        val isStaff = listRole == GymStaffListRole.STAFF
        val roleNoun = if (isStaff) "Staff" else "Trainer"
        val expertiseLabel = if (isStaff) "Staff role / expertise *" else "Trainer expertise *"
        val expertiseOptions = if (isStaff) {
            listOf(
                "Front Desk",
                "Reception",
                "Operations",
                "Sales & Billing",
                "Facility / Housekeeping",
                "Customer Support",
                "Administration"
            )
        } else {
            listOf(
                "Personal Trainer",
                "Conditioning Coach",
                "CrossFit Coach",
                "Bodybuilding Coach",
                "Yoga Specialist",
                "Nutritionist"
            )
        }

        var firstName by rememberSaveable { mutableStateOf("") }
        var lastName by rememberSaveable { mutableStateOf("") }
        var dob by rememberSaveable { mutableStateOf("") }
        var gender by rememberSaveable { mutableStateOf("Male") }
        var address by rememberSaveable { mutableStateOf("") }
        var addressLatitude by remember { mutableStateOf<Double?>(null) }
        var addressLongitude by remember { mutableStateOf<Double?>(null) }
        var experience by rememberSaveable { mutableStateOf("") }
        var salary by rememberSaveable { mutableStateOf("") }
        var salaryDuration by rememberSaveable { mutableStateOf("Month") }
        var expertise by rememberSaveable { mutableStateOf(setOf<String>()) }

        val focusManager = LocalFocusManager.current
        var phone by rememberSaveable { mutableStateOf("") }
        var email by rememberSaveable { mutableStateOf("") }
        var avatarUrl by rememberSaveable { mutableStateOf<String?>(null) }

        // Login username (trainer or staff)
        var trainerId by rememberSaveable { mutableStateOf("") }
        var password by rememberSaveable { mutableStateOf("") }
        var generateCredentials by rememberSaveable { mutableStateOf(false) }

        var shifts by remember { mutableStateOf(emptyList<ShiftData>()) }
        var editingShiftIndex by remember { mutableStateOf<Int?>(null) }

        var showAddShiftSheet by remember { mutableStateOf(false) }
        var showPermissionsSheet by remember { mutableStateOf(false) }
        var showDatePicker by remember { mutableStateOf(false) }
        var showImagePickerBottomSheet by remember { mutableStateOf(false) }
        var formErrors by remember { mutableStateOf(AddTrainerFormErrors()) }

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
                onAddShift = { title, start, end, selectedDays ->
                    if (editingShiftIndex != null) {
                        // Update existing (just the first day if we are editing a single row)
                        val index = editingShiftIndex!!
                        val firstDay = selectedDays.firstOrNull() ?: shiftToEdit!!.dayOfWeek
                        shifts = shifts.toMutableList().apply {
                            this[index] = ShiftData(title, "$start to $end", firstDay)
                        }
                        // If multiple days were selected during edit, add the others as new?
                        // Usually edit should just update the one row.
                    } else {
                        selectedDays.forEach { day ->
                            shifts = shifts + ShiftData(title, "$start to $end", day)
                        }
                    }
                    showAddShiftSheet = false
                    editingShiftIndex = null
                }
            )
        }

        if (showPermissionsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPermissionsSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = Color.White,
                dragHandle = null
            ) {
                TrainerPermissionsSheet(
                    onDismiss = { showPermissionsSheet = false },
                    sheetTitle = if (isStaff) "Staff permissions" else "Trainer permissions",
                    modules = if (isStaff) {
                        StaffPermissionCatalog.modules
                    } else {
                        TrainerPermissionCatalog.modules
                    },
                    onComplete = { permissionKeys ->
                        showPermissionsSheet = false
                        val isoDob = dob.split("/").let { parts ->
                            if (parts.size == 3) "${parts[2]}-${
                                parts[1].padStart(
                                    2,
                                    '0'
                                )
                            }-${parts[0].padStart(2, '0')}" else null
                        }
                        val compatShifts = shifts.map { s ->
                            val times = s.time.split(" to ")
                            TrainerCompatShiftRequest(
                                dayOfWeek = s.dayOfWeek,
                                startTime = times.getOrNull(0)?.trim().orEmpty(),
                                endTime = times.getOrNull(1)?.trim().orEmpty()
                            )
                        }
                        val request = CreateTrainerCompatRequest(
                            gymId = SessionManager.gymId,
                            phone = phone,
                            full_name = "$firstName $lastName".trim(),
                            dob = isoDob,
                            email = email,
                            gender = gender.lowercase(),
                            experience = experience.takeIf { it.isNotBlank() },
                            address = address.takeIf { it.isNotBlank() },
                            profile_image = avatarUrl,
                            salary = salary.toIntOrNull() ?: 0,
                            salary_type = when (salaryDuration) {
                                "Month" -> "monthly"
                                "Week" -> "weekly"
                                "Year" -> "yearly"
                                "Hour" -> "hourly"
                                else -> "monthly"
                            },
                            expertise = expertise.toList(),
                            shifts = compatShifts.takeIf { it.isNotEmpty() },
                            credentials = if (trainerId.isNotBlank() && password.isNotBlank())
                                TrainerCredentials(
                                    trainer_id = trainerId.trim(),
                                    password = password
                                ) else null,
                            permissions = permissionKeys,
                            role = listRole
                        )
                        activeScreenModel.createTrainerCompat(request) {
                            onTrainerAdded?.invoke()
                            navigator?.pop()
                        }
                    }
                )
            }
        }

        val addTitle = "Add New $roleNoun"
        val experienceLabel = if (isStaff) "Experience (years)" else "Experience"
        val credentialsHeader = if (isStaff) "Staff login credentials" else "Trainer login credentials"
        val loginIdLabel = "$roleNoun ID / username *"
        val uploadFileLabel = if (isStaff) "staff_avatar.jpg" else "trainer_avatar.jpg"

        LoadingScreenHandler(screenModel = activeScreenModel) {
            Scaffold(
                topBar = {
                    GymAppBar(
                        title = addTitle,
                        onBackClick = { navigator?.pop() }
                    )
                },
                containerColor = Color.Transparent
            ) { padding ->
                AppScrollableScreen(
                    modifier = Modifier.fillMaxSize().padding(top = 100.dp),
                    dismissKeyboardOnTap = true,
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
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Update photo",
                        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    LabeledField(label = "First name *") {
                        CommonTextField(
                            value = firstName,
                            onValueChange = {
                                firstName = it
                                if (formErrors.firstName != null) {
                                    formErrors = formErrors.copy(firstName = null)
                                }
                            },
                            placeholder = "e.g. John",
                            leadingIconDrawable = Res.drawable.userIcon,
                            errorText = formErrors.firstName
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledField(label = "Last name *") {
                        CommonTextField(
                            value = lastName,
                            onValueChange = {
                                lastName = it
                                if (formErrors.lastName != null) {
                                    formErrors = formErrors.copy(lastName = null)
                                }
                            },
                            placeholder = "e.g. Doe",
                            leadingIconDrawable = Res.drawable.userIcon,
                            errorText = formErrors.lastName
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledField(label = "Phone number *") {
                        CommonTextField(
                            value = phone,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number
                            ),
                            onValueChange = {
                                if (it.length <= 10) {
                                    phone = it
                                    if (formErrors.phone != null) {
                                        formErrors = formErrors.copy(phone = null)
                                    }
                                    if (it.length == 10) focusManager.clearFocus()
                                }
                            },
                            placeholder = "10-digit mobile number",
                            leadingIconDrawable = Res.drawable.ic_phone,
                            errorText = formErrors.phone
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledField(label = "Email") {
                        CommonTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                if (formErrors.email != null) {
                                    formErrors = formErrors.copy(email = null)
                                }
                            },
                            placeholder = "e.g. john@example.com",
                            leadingIconDrawable = Res.drawable.ic_message,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Email
                            ),
                            errorText = formErrors.email
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

                    LabeledField(label = "Address (Map Selection)") {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            CommonTextField(
                                value = address,
                                onValueChange = {},
                                placeholder = "Tap to select location on map",
                                leadingIconDrawable = Res.drawable.ic_location,
                                readOnly = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            // Transparent overlay to catch clicks and navigate
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(Color.Transparent)
                                    .clickable {
                                        navigator?.push(SelectAddressScreen(initialAddress = address) { picked ->
                                            address = picked.address
                                            addressLatitude = picked.latitude
                                            addressLongitude = picked.longitude
                                        })
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledField(label = experienceLabel) {
                        CommonTextField(
                            value = experience,
                            keyboardOptions = KeyboardOptions.Default.copy(
                                keyboardType = KeyboardType.Number
                            ),
                            onValueChange = { experience = it },
                            placeholder = if (isStaff) "e.g. 3 years" else "e.g. 5+ years",
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
                                onValueChange = {
                                    salary = it
                                    if (formErrors.salary != null) {
                                        formErrors = formErrors.copy(salary = null)
                                    }
                                },
                                placeholder = "Amount",
                                leadingIconDrawable = Res.drawable.ic_salary,
                                errorText = formErrors.salary
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

                    LabeledField(label = expertiseLabel) {
                        ExpertiseSelector(
                            options = expertiseOptions,
                            selectedExpertise = expertise,
                            expertisePlaceholder = if (isStaff) {
                                "Select or type staff role"
                            } else {
                                "Select or type expertise"
                            },
                            errorText = formErrors.expertise,
                            onDismissError = {
                                if (formErrors.expertise != null) {
                                    formErrors = formErrors.copy(expertise = null)
                                }
                            },
                            onToggle = { option ->
                                if (formErrors.expertise != null) {
                                    formErrors = formErrors.copy(expertise = null)
                                }
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
                            text = if (isStaff) "Work shifts" else "Add shift",
                            style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    shifts.forEachIndexed { index, shift ->
                        ShiftListItem(
                            shift = shift,
                            onDelete = {
                                shifts = shifts.toMutableList().apply { removeAt(index) }
                            },
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
                            text = "Add shift",
                            style = AppTextTheme.medium.copy(fontSize = 15.sp, color = Gray)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Color(0xFFF7F8F9), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE8ECF4), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = PrimaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = credentialsHeader,
                            style = AppTextTheme.medium.copy(fontSize = 15.sp, color = Black),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    LabeledField(label = loginIdLabel) {
                        CommonTextField(
                            value = trainerId,
                            onValueChange = {
                                trainerId = it
                                if (formErrors.trainerId != null) {
                                    formErrors = formErrors.copy(trainerId = null)
                                }
                            },
                            placeholder = if (isStaff) "e.g. staff_riya" else "e.g. trainer_john",
                            leadingIconDrawable = Res.drawable.userIcon,
                            errorText = formErrors.trainerId
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    LabeledField(label = "Password *") {
                        CommonTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                if (formErrors.password != null) {
                                    formErrors = formErrors.copy(password = null)
                                }
                            },
                            placeholder = "Minimum 8 characters",
                            isPassword = true,
                            leadingIconDrawable = Res.drawable.lockicon,
                            errorText = formErrors.password
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    CommonButton(
                        onClick = {
                            val emailRegex =
                                Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
                            val salaryAmount = salary.toIntOrNull()
                            val expertiseHint =
                                if (isStaff) {
                                    "Select at least one staff role or expertise"
                                } else {
                                    "Select at least one area of expertise"
                                }
                            formErrors = AddTrainerFormErrors(
                                firstName = if (firstName.isBlank()) {
                                    "First name is required"
                                } else {
                                    null
                                },
                                lastName = if (lastName.isBlank()) {
                                    "Last name is required"
                                } else {
                                    null
                                },
                                phone = when {
                                    phone.isBlank() -> "Phone number is required"
                                    phone.length != 10 -> "Enter a valid 10-digit phone number"
                                    !phone.all { it.isDigit() } -> "Phone number must contain digits only"
                                    else -> null
                                },
                                email = if (email.isNotBlank() && !emailRegex.matches(email)) {
                                    "Enter a valid email address"
                                } else {
                                    null
                                },
                                salary = when {
                                    salary.isBlank() -> "Salary is required"
                                    salaryAmount == null -> "Enter a valid whole number for salary"
                                    salaryAmount < 0 -> "Salary cannot be negative"
                                    else -> null
                                },
                                expertise = if (expertise.isEmpty()) expertiseHint else null,
                                trainerId = if (trainerId.isBlank()) {
                                    "$roleNoun ID or username is required"
                                } else {
                                    null
                                },
                                password = when {
                                    password.isBlank() -> "Password is required"
                                    password.length < 8 -> {
                                        "Password must be at least 8 characters"
                                    }
                                    else -> null
                                }
                            )
                            if (!formErrors.hasErrors()) {
                                showPermissionsSheet = true
                            }
                        },
                        text = "Save and Next",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(24.dp))
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
                    title = "Upload Profile Photo",
                    subtitle = "Choose from gallery or take a new picture"
                )
            }

            if (launchGallery) {
                GalleryPickerLauncher(
                    onPhotosSelected = { photos ->
                        launchGallery = false
                        photos.firstOrNull()?.let { photo ->
                            activeScreenModel.uploadImage(
                                photo.loadBytes(),
                                uploadFileLabel
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
                            activeScreenModel.uploadImage(
                                photo.loadBytes(),
                                uploadFileLabel
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

    @Composable
    fun ExpertiseSelector(
        options: List<String>,
        selectedExpertise: Set<String>,
        onToggle: (String) -> Unit,
        expertisePlaceholder: String = "Select or type expertise",
        errorText: String? = null,
        onDismissError: () -> Unit = {}
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
                        if (errorText != null) onDismissError()
                    },
                    placeholder = expertisePlaceholder,
                    errorText = errorText,
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
}
