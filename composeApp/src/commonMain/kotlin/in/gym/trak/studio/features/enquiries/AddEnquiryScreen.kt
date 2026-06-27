package `in`.gym.trak.studio.features.enquiries

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.rememberAsyncImagePainter
import `in`.gym.trak.studio.components.*
import `in`.gym.trak.studio.data.model.CreateEnquiryRequest
import `in`.gym.trak.studio.data.model.EnquiryDTO
import `in`.gym.trak.studio.data.model.UpdateEnquiryRequest
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import `in`.gym.trak.studio.features.location.SelectAddressScreen
import `in`.gym.trak.studio.features.member.ImagePickerBottomSheet
import `in`.gym.trak.studio.theme.*
import gym.composeapp.generated.resources.*
import io.github.ismoy.imagepickerkmp.domain.config.ImagePickerConfig
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import io.github.ismoy.imagepickerkmp.presentation.ui.components.ImagePickerLauncher
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource

class AddEnquiryScreen(val enquiry: EnquiryDTO? = null, val onResult: () -> Unit = {}) : Screen {


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val activeScreenModel = remember { OwnerDashboardScreenModel() }

        // Form state
        var firstName by rememberSaveable { mutableStateOf("") }
        var lastName by rememberSaveable { mutableStateOf("") }
        var phone by rememberSaveable { mutableStateOf("") }
        var gender by rememberSaveable { mutableStateOf("Male") }
        var address by rememberSaveable { mutableStateOf("") }
        var addressLatitude by remember { mutableStateOf<Double?>(null) }
        var addressLongitude by remember { mutableStateOf<Double?>(null) }
        var enquiryDate by rememberSaveable { mutableStateOf("") }
        var medium by rememberSaveable { mutableStateOf("") }
        var interestedIn by rememberSaveable { mutableStateOf("") }
        var notes by rememberSaveable { mutableStateOf("") }
        var avatarUrl by rememberSaveable { mutableStateOf<String?>(null) }

        var showOptionalDetails by rememberSaveable { mutableStateOf(false) }
        var validationError by remember { mutableStateOf<String?>(null) }

        val focusManager = LocalFocusManager.current

        // Date Picker state
        var showDatePicker by remember { mutableStateOf(false) }

        // Image Picker state
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

        // Pre-fill if object is passed
        LaunchedEffect(enquiry) {
            if (enquiry != null) {
                firstName = enquiry.firstName ?: ""
                lastName = enquiry.lastName ?: ""
                phone = enquiry.phone ?: ""
                gender = enquiry.gender?.replaceFirstChar { it.uppercase() } ?: "Male"
                address = enquiry.address ?: ""
                medium = enquiry.medium ?: ""
                interestedIn = enquiry.interestedIn ?: ""
                notes = enquiry.notes ?: ""
                avatarUrl = enquiry.photoUrl

                // Parse date
                enquiry.enquiryDate?.let { iso ->
                    try {
                        val date =
                            Instant.parse(iso).toLocalDateTime(TimeZone.currentSystemDefault())
                        enquiryDate = "${date.dayOfMonth}/${date.monthNumber}/${date.year}"
                    } catch (e: Exception) {
                    }
                }
                if (address.isNotBlank() || medium.isNotBlank() || interestedIn.isNotBlank()) {
                    showOptionalDetails = true
                }
            }
        }



        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()
            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.UTC)
                            enquiryDate = "${date.dayOfMonth}/${date.monthNumber}/${date.year}"
                        }
                        showDatePicker = false
                    }) { Text("OK", color = PrimaryColor) }
                }
            ) { DatePicker(state = datePickerState) }
        }

        LoadingScreenHandler(screenModel = activeScreenModel) {
            Scaffold(
                topBar = {
                    GymAppBar(
                        title = if (enquiry == null) "Add Enquiry" else "Edit Enquiry",
                        onBackClick = { navigator?.pop() }
                    )
                },


                containerColor = Color.Transparent
            ) { padding ->
                AppScrollableScreen(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    dismissKeyboardOnTap = true,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Photo Section
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
                                contentDescription = "Enquiry Photo",
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
                    Text(
                        text = "Member photo",
                        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray),
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    LabeledField(label = "First Name *") {
                        CommonTextField(
                            value = firstName,
                            keyboardCapitalization = KeyboardCapitalization.Words,
                            onValueChange = { firstName = it },
                            placeholder = "eg. John",
                            leadingIconDrawable = Res.drawable.userIcon
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledField(label = "Last Name *") {
                        CommonTextField(
                            value = lastName,
                            keyboardCapitalization = KeyboardCapitalization.Words,
                            onValueChange = { lastName = it },
                            placeholder = "eg. Doe",
                            leadingIconDrawable = Res.drawable.userIcon
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledField(label = "Mobile Number *") {
                        CommonTextField(
                            value = phone,
                            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                            onValueChange = { if (it.length <= 10) phone = it },
                            placeholder = "+(91) 56555 523222",
                            leadingIconDrawable = Res.drawable.ic_phone
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledField(label = "Date of Enquiry") {
                        DatePickerField(
                            value = enquiryDate,
                            onPickerClick = { showDatePicker = true },
                            placeholder = "Select Date"
                        )
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

                        LabeledField(label = "Address") {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                CommonTextField(
                                    value = address,
                                    onValueChange = {},
                                    placeholder = "At-Rajkot",
                                    leadingIconDrawable = Res.drawable.ic_location,
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
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

                        LabeledField(label = "Medium") {
                            CommonDropdown(
                                options = listOf("Social Media", "Walk-in", "Reference", "Other"),
                                selectedOption = medium,
                                onOptionSelected = { medium = it },
                                placeholder = "Select medium"
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        LabeledField(label = "Interested in") {
                            CommonDropdown(
                                options = listOf(
                                    "Standard Plan",
                                    "Premium Plan",
                                    "Personal Training",
                                    "Yoga"
                                ),
                                selectedOption = interestedIn,
                                onOptionSelected = { interestedIn = it },
                                placeholder = "Select interest"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    LabeledField(label = "Notes") {
                        CommonTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = "Add additional details about the enquiry.",
                            singleLine = false,
                            isMultiline = true,
                            modifier = Modifier.height(100.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))




                    Spacer(modifier = Modifier.height(16.dp))


                    if (validationError != null) {
                        Text(
                            text = validationError!!,
                            color = RedColor,
                            style = AppTextTheme.medium.copy(fontSize = 12.sp),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    if (enquiry?.status != "CONVERTED") {
                        CommonButton(
                            text = if (enquiry == null) "Create Enquiry" else "Update Enquiry",
                            onClick = {
                                when {
                                    firstName.isBlank() -> validationError = "First name is required"
                                    lastName.isBlank() -> validationError = "Last name is required"
                                    phone.length != 10 -> validationError = "Enter valid phone number"
                                    else -> {
                                        validationError = null
                                        val isoDate = enquiryDate.split("/").let {
                                            if (it.size == 3) "${it[2]}-${
                                                it[1].padStart(
                                                    2,
                                                    '0'
                                                )
                                            }-${it[0].padStart(2, '0')}T00:00:00.000Z" else null
                                        }

                                        if (enquiry == null) {

                                            val request = CreateEnquiryRequest(
                                                gymId = SessionManager.gymId,
                                                name = firstName.trim() + " " + lastName.trim(),
                                                firstName = firstName.trim(),
                                                lastName = lastName.trim(),
                                                phone = phone.trim(),
                                                gender = gender.lowercase(),
                                                address = address.trim().takeIf { it.isNotBlank() },
                                                medium = medium.takeIf { it.isNotBlank() },
                                                interestedIn = interestedIn.takeIf { it.isNotBlank() },
                                                notes = notes.trim().takeIf { it.isNotBlank() },
                                                enquiryDate = isoDate,
                                                photoUrl = avatarUrl
                                            )
                                            activeScreenModel.createEnquiry(request) {
                                                onResult()
                                                navigator?.pop()
                                            }

                                        } else {
                                            val request =
                                                UpdateEnquiryRequest(
                                                    name = firstName.trim() + " " + lastName.trim(),
                                                    firstName = firstName.trim(),
                                                    lastName = lastName.trim(),
                                                    gymId = enquiry.gymId,
                                                    phone = phone.trim(),
                                                    gender = gender.lowercase(),
                                                    address = address.trim().takeIf { it.isNotBlank() },
                                                    medium = medium.takeIf { it.isNotBlank() },
                                                    interestedIn = interestedIn.takeIf { it.isNotBlank() },
                                                    notes = notes.trim().takeIf { it.isNotBlank() },
                                                    enquiryDate = isoDate,
                                                    photoUrl = avatarUrl
                                                )
                                            activeScreenModel.updateEnquiry(enquiry.id, request) {
                                                onResult()
                                                navigator?.pop()
                                            }


                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        )
                    }


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
                    title = "Member Photo"
                )
            }
            if (launchGallery) {
                GalleryPickerLauncher(
                    onPhotosSelected = {
                        activeScreenModel.uploadImage(
                            it.first().loadBytes(),
                            "enquiry_avatar.jpg"
                        ) { avatarUrl = it }; launchGallery = false
                    },
                    onDismiss = { launchGallery = false },
                    onError = {
                        launchGallery = false
                        println("Workout Gallery Picker Error: ${it.message}")
                    },

                    )
            }
            if (launchCamera) {
                ImagePickerLauncher(
                    config = ImagePickerConfig(
                        onPhotoCaptured = {
                            activeScreenModel.uploadImage(
                                it.loadBytes(),
                                "enquiry_avatar.jpg"
                            ) { avatarUrl = it }; launchCamera = false
                        },
                        onError = {
                            launchGallery = false
                            println("Workout Gallery Picker Error: ${it.message}")
                        },
                    )
                )
            }
        }
    }

    @Composable
    fun LabeledField(label: String, content: @Composable () -> Unit) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Black),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}
