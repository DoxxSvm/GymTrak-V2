package `in`.gym.trak.studio.features.members

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.rememberAsyncImagePainter
import `in`.gym.trak.studio.components.*
import `in`.gym.trak.studio.data.model.AddMemberRequest
import `in`.gym.trak.studio.data.model.toMemberProfileUpdateRequest
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import `in`.gym.trak.studio.features.location.SelectAddressScreen
import `in`.gym.trak.studio.features.member.ImagePickerBottomSheet
import `in`.gym.trak.studio.theme.*
import gym.composeapp.generated.resources.*
import `in`.gym.trak.studio.getCurrentTimeMillis
import `in`.gym.trak.studio.utils.DateUtils
import io.github.ismoy.imagepickerkmp.domain.config.ImagePickerConfig
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import io.github.ismoy.imagepickerkmp.presentation.ui.components.ImagePickerLauncher
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource

import kotlin.jvm.Transient

class AddMemberScreen(
    @Transient private val screenModel: OwnerDashboardScreenModel? = null,
    @Transient private val onMemberAdded: (() -> Unit)? = null,
    private val memberId: String? = null
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val activeScreenModel = screenModel ?: remember { OwnerDashboardScreenModel() }

        // Form state
        var firstName by rememberSaveable { mutableStateOf("") }
        var lastName by rememberSaveable { mutableStateOf("") }
        var phone by rememberSaveable { mutableStateOf("") }
        var email by rememberSaveable { mutableStateOf("") }
        var gender by rememberSaveable { mutableStateOf("Male") }
        var address by rememberSaveable { mutableStateOf("") }
        var addressLatitude by remember { mutableStateOf<Double?>(null) }
        var addressLongitude by remember { mutableStateOf<Double?>(null) }
        var dob by rememberSaveable { mutableStateOf("") }
        var age by rememberSaveable { mutableStateOf("") }
        var joinDate by rememberSaveable { mutableStateOf("") }
        var aadhaarNumber by rememberSaveable { mutableStateOf("") }
        var weight by rememberSaveable { mutableStateOf("") }
        var height by rememberSaveable { mutableStateOf("") }
        var notes by rememberSaveable { mutableStateOf("") }
        var emergencyName by rememberSaveable { mutableStateOf("") }
        var emergencyPhone by rememberSaveable { mutableStateOf("") }
        var avatarUrl by rememberSaveable { mutableStateOf<String?>(null) }
        var isLead by rememberSaveable { mutableStateOf(false) }

        var showOptionalDetails by rememberSaveable { mutableStateOf(false) }
        data class AddMemberFieldErrors(
            val firstName: String? = null,
            val lastName: String? = null,
            val phone: String? = null,
            val email: String? = null,
            val joinDate: String? = null,
            val form: String? = null
        )
        var fieldErrors by remember { mutableStateOf(AddMemberFieldErrors()) }

        /** True after we copy [memberDetail] into the form once — avoids wiping local edits when the flow refreshes. */
        var hasAppliedRemoteMemberForm by remember(memberId) { mutableStateOf(false) }

        val focusManager = LocalFocusManager.current

        // Pre-fill if editing
        LaunchedEffect(memberId) {
            if (memberId != null) {
                activeScreenModel.loadMemberDetail(memberId)
            }
        }

        val memberDetail by activeScreenModel.memberDetail.collectAsState()

        LaunchedEffect(memberId, memberDetail) {
            val id = memberId ?: return@LaunchedEffect
            if (hasAppliedRemoteMemberForm) return@LaunchedEffect
            val detail = memberDetail ?: return@LaunchedEffect
            val idMatches =
                detail.gymUserId == id || detail.summary.id == id || detail.user.id == id
            if (!idMatches) return@LaunchedEffect

            fun digitsPhone(p: String): String {
                val d = p.filter { it.isDigit() }
                return if (d.length >= 10) d.takeLast(10) else p.filter { it.isDigit() }
            }

            val sfn = detail.summary.first_name?.trim()?.takeIf { it.isNotEmpty() }
            val sln = detail.summary.last_name?.trim()?.takeIf { it.isNotEmpty() }
            if (sfn != null || sln != null) {
                firstName = sfn.orEmpty()
                lastName = sln.orEmpty()
            } else {
                val full = detail.user.fullName.trim()
                val parts = full.split(Regex("\\s+"), limit = 2)
                firstName = parts.getOrNull(0).orEmpty()
                lastName = parts.getOrNull(1).orEmpty()
            }

            phone = digitsPhone(detail.user.phone.ifBlank { detail.summary.phone })
            email = detail.user.email?.trim().orEmpty()
            gender = detail.gender?.replaceFirstChar { it.uppercase() } ?: "Male"
            address = detail.summary.address?.trim().orEmpty()

            dob = DateUtils.formatBirthDateForDisplay(detail.dateOfBirth)
                .ifBlank { DateUtils.formatBirthDateForDisplay(detail.summary.dob) }
            joinDate = DateUtils.formatBirthDateForDisplay(detail.joinedAt)

            age = listOf(detail.age, detail.summary.age, detail.user.age)
                .mapNotNull { v ->
                    v?.toString()?.trim()?.takeIf { it.isNotEmpty() }
                }
                .firstOrNull()
                ?: addMemberUiDobToAge(dob).orEmpty()

            height = detail.user.heightCm?.toString()?.removeSuffix(".0").orEmpty()
            weight = detail.user.weightKg?.toString()?.removeSuffix(".0").orEmpty()

            notes = detail.notes?.trim().orEmpty().ifBlank {
                detail.summary.notes?.trim().orEmpty()
            }
            aadhaarNumber = detail.summary.aadhaar_number?.trim().orEmpty()

            emergencyName = detail.emergencyContactName?.trim().orEmpty().ifBlank {
                detail.summary.emergency_name?.trim().orEmpty()
            }
            emergencyPhone = run {
                val raw = detail.emergencyContactPhone?.trim().orEmpty().ifBlank {
                    detail.summary.emergency_contact_phone?.trim().orEmpty()
                }
                if (raw.isEmpty()) "" else digitsPhone(raw)
            }

            avatarUrl = listOf(
                detail.user.avatarUrl,
                detail.user.profile_image,
                detail.summary.profile_image
            ).firstOrNull { !it.isNullOrBlank() }

            isLead = detail.isLead
            hasAppliedRemoteMemberForm = true
        }

        // Date Picker states
        var showDobPicker by remember { mutableStateOf(false) }
        var showJoinDatePicker by remember { mutableStateOf(false) }

        // Image Picker states
        var showImagePickerBottomSheet by remember { mutableStateOf(false) }
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

        // Handle Date Selection
        if (showDobPicker) {
            val datePickerState = rememberDatePickerState(
                selectableDates = object : SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                        utcTimeMillis <= getCurrentTimeMillis()
                }
            )
            DatePickerDialog(
                onDismissRequest = { showDobPicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.UTC)
                            dob = "${date.dayOfMonth}/${date.monthNumber}/${date.year}"
                            addMemberUiDobToAge(dob)?.let { computed -> age = computed }
                        }
                        showDobPicker = false
                    }) { Text("OK", color = PrimaryColor) }
                },
                dismissButton = {
                    TextButton(onClick = { showDobPicker = false }) { Text("Cancel", color = Gray) }
                }
            ) { DatePicker(state = datePickerState) }
        }

        LaunchedEffect(showJoinDatePicker) {
            if (!showJoinDatePicker) {
                focusManager.clearFocus()
            }
        }

        if (showJoinDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = {
                    showJoinDatePicker = false
                    focusManager.clearFocus()
                },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.UTC)
                            joinDate = "${date.dayOfMonth}/${date.monthNumber}/${date.year}"
                            fieldErrors = fieldErrors.copy(joinDate = null, form = null)
                        }
                        showJoinDatePicker = false
                        focusManager.clearFocus()
                    }) { Text("OK", color = PrimaryColor) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showJoinDatePicker = false
                        focusManager.clearFocus()
                    }) { Text("Cancel", color = Gray) }
                }
            ) { DatePicker(state = datePickerState) }
        }

        LoadingScreenHandler(screenModel = activeScreenModel) {
            Scaffold(
                topBar = {
                    GymAppBar(
                        title = if (memberId == null) "Add Member" else "Edit Member",
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
                    
                    // Profile Photo (Referencing AddTrainerScreen approach)
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
                                modifier = Modifier.size(92.dp).clip(CircleShape),
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
                        text = "Member photo",
                        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Basic Details
                    LabeledField(label = "First Name *") {
                        CommonTextField(
                            value = firstName,
                            keyboardCapitalization = KeyboardCapitalization.Words,
                            onValueChange = {
                                firstName = it
                                fieldErrors = fieldErrors.copy(firstName = null, form = null)
                            },
                            placeholder = "eg. John",
                            leadingIconDrawable = Res.drawable.userIcon,
                            errorText = fieldErrors.firstName
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledField(label = "Last Name *") {
                        CommonTextField(
                            keyboardCapitalization = KeyboardCapitalization.Words,

                            value = lastName,
                            onValueChange = {
                                lastName = it
                                fieldErrors = fieldErrors.copy(lastName = null, form = null)
                            },
                            placeholder = "eg. Doe",
                            leadingIconDrawable = Res.drawable.userIcon,
                            errorText = fieldErrors.lastName
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledField(label = "Phone Number *") {
                        CommonTextField(
                            value = phone,
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            onValueChange = {
                                if (it.length <= 10) {
                                    phone = it
                                    fieldErrors = fieldErrors.copy(phone = null, form = null)
                                    if (it.length == 10) focusManager.clearFocus()
                                }
                            },
                            placeholder = "eg. 9876543210",
                            leadingIconDrawable = Res.drawable.ic_phone,
                            errorText = fieldErrors.phone
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledField(label = "Email") {
                        CommonTextField(
                            value = email,
                            onValueChange = {
                                email = it
                                fieldErrors = fieldErrors.copy(email = null, form = null)
                            },
                            placeholder = "eg. john@example.com",
                            leadingIconDrawable = Res.drawable.ic_message,
                            errorText = fieldErrors.email
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledField(label = "Date of joining *") {
                        DatePickerField(
                            value = joinDate,
                            placeholder = "Select Join Date",
                            onPickerClick = {
                                focusManager.clearFocus()
                                showJoinDatePicker = true
                            }
                        )
                        fieldErrors.joinDate?.let { error ->
                            Text(
                                text = error,
                                color = RedColor,
                                style = AppTextTheme.medium.copy(fontSize = 12.sp),
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledField(label = "Gender *") {
                        GenderSelector(
                            selectedGender = gender,
                            onGenderSelected = { gender = it }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Optional Details toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .background(Color(0xFFF7F8F9), RoundedCornerShape(12.dp))
                            .border(1.dp, Color(0xFFE8ECF4), RoundedCornerShape(12.dp))
                            .clickable { showOptionalDetails = !showOptionalDetails }
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = PrimaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Optional Details",
                            style = AppTextTheme.medium.copy(fontSize = 15.sp, color = Black),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = if (showOptionalDetails) Icons.Default.Close else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Gray
                        )
                    }

                    if (showOptionalDetails) {
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
                                                showOptionalDetails = true
                                            })
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LabeledField(label = "Date of Birth") {
                            DatePickerField(
                                value = dob,
                                onPickerClick = { showDobPicker = true }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LabeledField(label = "Age") {
                            CommonTextField(
                                value = age,
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Number
                                ),
                                onValueChange = {
                                    if (it.length <= 3 && it.all { c -> c.isDigit() }) {
                                        age = it
                                    }
                                },
                                placeholder = "e.g. 25",
                                leadingIconDrawable = Res.drawable.ic_profile
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LabeledField(label = "Aadhaar Number") {
                            CommonTextField(
                                value = aadhaarNumber,
                                onValueChange = { 
                                    if (it.length <= 12 && it.all { char -> char.isDigit() }) {
                                        aadhaarNumber = it 
                                    }
                                },
                                visualTransformation = AadhaarVisualTransformation(),
                                placeholder = "0000 0000 0000",
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                leadingIconDrawable = Res.drawable.ic_adhar
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            LabeledField(label = "Weight (KG)", modifier = Modifier.weight(1f)) {
                                CommonTextField(
                                    value = weight,
                                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                    onValueChange = { 
                                        if (it.length <= 3 && it.all { char -> char.isDigit() }) {
                                            weight = it 
                                        }
                                    },
                                    visualTransformation = UnitVisualTransformation("kg"),
                                    placeholder = "70"
                                )
                            }
                            LabeledField(label = "Height (CM)", modifier = Modifier.weight(1f)) {
                                CommonTextField(
                                    value = height,
                                    keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                    onValueChange = { 
                                        if (it.length <= 3 && it.all { char -> char.isDigit() }) {
                                            height = it 
                                        }
                                    },
                                    visualTransformation = UnitVisualTransformation("cm"),
                                    placeholder = "175"
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LabeledField(label = "Emergency Contact Name") {
                            CommonTextField(
                                keyboardCapitalization = KeyboardCapitalization.Words,

                                value = emergencyName,
                                onValueChange = { emergencyName = it },
                                placeholder = "Relative Name"
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LabeledField(label = "Emergency Contact Phone") {
                            CommonTextField(
                                value = emergencyPhone,
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                                onValueChange = {
                                    if (it.length <= 10) {
                                        emergencyPhone = it
                                        if (it.length == 10) focusManager.clearFocus()
                                    }
                                },
                                placeholder = "Relative Phone"
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LabeledField(label = "Notes (${notes.length}/200)") {
                            CommonTextField(
                                keyboardCapitalization = KeyboardCapitalization.Words,
                                value = notes,
                                onValueChange = { 
                                    if (it.length <= 200) notes = it 
                                },
                                placeholder = "Any medical history or goals",
                                singleLine = false,
                                isMultiline = true,

                                modifier = Modifier.height(100.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    if (fieldErrors.form != null) {
                        Text(
                            text = fieldErrors.form!!,
                            color = RedColor,
                            style = AppTextTheme.medium.copy(fontSize = 12.sp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    CommonButton(
                        text = if (memberId == null) "Create Member" else "Update Member",
                        onClick = {
                            val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$")
                            val validationErrors = AddMemberFieldErrors(
                                firstName = if (firstName.isBlank()) "First name is required" else null,
                                lastName = if (lastName.isBlank()) "Last name is required" else null,
                                phone = if (phone.length != 10) "Enter valid 10-digit phone number" else null,
                                joinDate = if (joinDate.isBlank()) "Date of joining is required" else null,
                                email = if (email.isNotBlank() && !emailRegex.matches(email)) "Enter a valid email address" else null
                            )
                            fieldErrors = validationErrors
                            if (validationErrors != AddMemberFieldErrors()) return@CommonButton

                            val gymId = SessionManager.gymId
                            if (gymId.isEmpty()) {
                                fieldErrors = fieldErrors.copy(form = "Gym session expired. Please relogin.")
                                return@CommonButton
                            }

                            // Format dates for API
                            fun formatToIso(d: String): String? = d.split("/").let { parts ->
                                if (parts.size == 3) "${parts[2]}-${parts[1].padStart(2, '0')}-${parts[0].padStart(2, '0')}" else null
                            }

                            val addRequest = AddMemberRequest(
                                gymId = gymId,
                                phone = phone,
                                fullName = "$firstName $lastName".trim(),
                                first_name = firstName.trim(),
                                last_name = lastName.trim(),
                                email = email.trim().takeIf { it.isNotBlank() },
                                gender = gender.lowercase(),
                                address = address.trim().takeIf { it.isNotBlank() },
                                aadhaar_number = aadhaarNumber.trim().takeIf { it.isNotBlank() },
                                dateOfBirth = formatToIso(dob),
                                dob = formatToIso(dob),
                                date_of_joining = formatToIso(joinDate),
                                heightCm = height.toDoubleOrNull(),
                                weightKg = weight.toDoubleOrNull(),
                                notes = notes.trim().takeIf { it.isNotBlank() },
                                emergencyContactName = emergencyName.trim().takeIf { it.isNotBlank() },
                                emergencyContactPhone = emergencyPhone.trim().takeIf { it.isNotBlank() },
                                isLead = isLead,
                                avatarUrl = avatarUrl,
                                age = age.trim().takeIf { it.isNotBlank() }
                                    ?: addMemberUiDobToAge(dob)
                                    ?: memberDetail?.age?.toString()
                                    ?: memberDetail?.summary?.age?.toString()
                                    ?: memberDetail?.user?.age?.toString()
                            )
                            if (memberId == null) {
                                activeScreenModel.addMember(addRequest) {
                                    onMemberAdded?.invoke()
                                    navigator?.pop()
                                }
                            } else {
                                activeScreenModel.updateMemberProfile(
                                    memberId,
                                    addRequest.toMemberProfileUpdateRequest()
                                ) {
                                    onMemberAdded?.invoke()
                                    navigator?.pop()
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Image Picker Bottom Sheets (Referencing AddTrainerScreen)
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
                    title = "Upload Member Photo",
                    subtitle = "Choose from gallery or take a new picture"
                )
            }

            if (launchGallery) {
                GalleryPickerLauncher(
                    onPhotosSelected = { photos ->
                        launchGallery = false
                        photos.firstOrNull()?.let { photo ->
                            activeScreenModel.uploadImage(photo.loadBytes(), "member_avatar.jpg") { uploadedUrl ->
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
                            activeScreenModel.uploadImage(photo.loadBytes(), "member_avatar.jpg") { uploadedUrl ->
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
    fun LabeledField(label: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
        Column(modifier = modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Black),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }

}

/** `dd/MM/yyyy` → age string when DOB is set (used for [AddMemberRequest.age]). */
private fun addMemberUiDobToAge(d: String): String? {
    val parts = d.trim().split("/")
    if (parts.size != 3) return null
    val year = parts[2].toIntOrNull() ?: return null
    val currentYear = (getCurrentTimeMillis() / 31_556_952_000L + 1970).toInt()
    return (currentYear - year).coerceAtLeast(0).toString()
}

class AadhaarVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 12) text.text.substring(0..11) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i % 4 == 3 && i != 11) out += " "
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 3) return offset
                if (offset <= 7) return offset + 1
                if (offset <= 11) return offset + 2
                return offset + 2
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 4) return offset
                if (offset <= 9) return offset - 1
                if (offset <= 14) return offset - 2
                return offset - 2
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

class UnitVisualTransformation(val unit: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.text.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val out = text.text + " " + unit
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset
            override fun transformedToOriginal(offset: Int): Int {
                if (offset > text.text.length) return text.text.length
                return offset
            }
        }
        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}
