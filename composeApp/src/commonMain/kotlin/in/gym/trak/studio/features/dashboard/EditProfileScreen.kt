package `in`.gym.trak.studio.features.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.core.model.rememberScreenModel
import coil3.compose.rememberAsyncImagePainter
import `in`.gym.trak.studio.components.AppScrollableScreen
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import `in`.gym.trak.studio.data.model.GymDetailsUpdate
import `in`.gym.trak.studio.data.model.PersonalInfoUpdate
import `in`.gym.trak.studio.data.model.UpdateProfileRequest
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.features.location.SelectGymLocationScreen
import `in`.gym.trak.studio.features.member.ImagePickerBottomSheet
import `in`.gym.trak.studio.theme.*
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import gym.composeapp.generated.resources.*
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.data.model.UpdateOwnerProfileRequest
import `in`.gym.trak.studio.features.location.SelectAddressScreen
import `in`.gym.trak.studio.features.trainers.LabeledField
import io.github.ismoy.imagepickerkmp.domain.config.ImagePickerConfig
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import io.github.ismoy.imagepickerkmp.presentation.ui.components.ImagePickerLauncher
import org.jetbrains.compose.resources.painterResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.jvm.Transient

class EditProfileScreen(
    @Transient private val sharedDashboardScreenModel: OwnerDashboardScreenModel? = null,
    private val onUpdateSuccess: (() -> Unit)? = null
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel =
            sharedDashboardScreenModel ?: rememberScreenModel { OwnerDashboardScreenModel() }
        
        var fullName by rememberSaveable { mutableStateOf("") }
        var gymName by rememberSaveable { mutableStateOf("") }
        var gymAddress by rememberSaveable { mutableStateOf("") }
        var gymLatitude by remember { mutableStateOf<Double?>(null) }
        var gymLongitude by remember { mutableStateOf<Double?>(null) }
        var gymGstNumber by rememberSaveable { mutableStateOf("") }
        var profilePhotoUrl by rememberSaveable { mutableStateOf<String?>(null) }
        var gymLogoUrl by rememberSaveable { mutableStateOf<String?>(null) }

        var fullNameError by remember { mutableStateOf<String?>(null) }
        var gymNameError by remember { mutableStateOf<String?>(null) }
        var gymAddressError by remember { mutableStateOf<String?>(null) }
        var gymGstNumberError by remember { mutableStateOf<String?>(null) }
        var gymLogoError by remember { mutableStateOf<String?>(null) }

        var showImagePicker by remember { mutableStateOf(false) }
        var currentPickingType by remember { mutableStateOf("profile") } // "profile" or "logo"

        var launchGallery by remember { mutableStateOf(false) }
        var launchCamera by remember { mutableStateOf(false) }
        var pendingPickerAction by remember { mutableStateOf<String?>(null) }

        val profileData by screenModel.profileData.collectAsState()

        LaunchedEffect(Unit) {
            screenModel.loadProfile()
        }



        LaunchedEffect(profileData) {
            profileData?.let { data ->
                // Only update if current value is default/blank. 
                // This prevents clearing user edits when returning from camera/map.
                if (fullName == "") fullName = data.personalInfo?.fullName ?: fullName
                if (gymName == "") gymName = data.gymDetails?.gymName ?: gymName
                if (profilePhotoUrl == null) profilePhotoUrl = data.personalInfo?.profileImage
                if (gymLogoUrl == null) gymLogoUrl = data.gymDetails?.gymLogo
                if (gymGstNumber == "") gymGstNumber = data.gymDetails?.gstNumber ?: gymGstNumber
                if (gymAddress == "") gymAddress = data.gymDetails?.gymAddress ?: gymAddress

            }
        }

        LaunchedEffect(showImagePicker, pendingPickerAction) {
            if (showImagePicker || pendingPickerAction == null) return@LaunchedEffect
            when (pendingPickerAction) {
                "gallery" -> launchGallery = true
                "camera" -> launchCamera = true
            }
            pendingPickerAction = null
        }

        val focusManager = LocalFocusManager.current
        val scrollState = rememberScrollState()
        val scrollCoroutineScope = rememberCoroutineScope()

        LoadingScreenHandler(screenModel = screenModel) {
        Scaffold(
            topBar = {
                GymAppBar(
                    title = "Edit Profile",
                    onBackClick = {
                        navigator.pop()
                    }
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            AppScrollableScreen(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                scrollState = scrollState,
                contentPadding = PaddingValues(24.dp),
                dismissKeyboardOnTap = true,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Profile Photo Section
                StaggeredEntranceItem(index = 0) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF3F4F6))
                                .border(2.dp, PrimaryColor.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val painter = if (profilePhotoUrl != null)
                                rememberAsyncImagePainter(profilePhotoUrl)
                            else painterResource(Res.drawable.gym_boy)

                            Image(
                                painter = painter,
                                contentDescription = "Profile Photo",
                                modifier = Modifier.size(92.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            val isUploading by screenModel.isUploadingImage.collectAsState()
                            if (isUploading && currentPickingType == "profile") {
                                Box(
                                    modifier = Modifier
                                        .size(92.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = White,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                        Surface(
                            onClick = {
                                currentPickingType = "profile"
                                showImagePicker = true
                            },
                            shape = CircleShape,
                            color = PrimaryColor,
                            modifier = Modifier.size(30.dp).offset(x = (-4).dp, y = (-4).dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit photo",
                                    tint = White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                StaggeredEntranceItem(index = 1) {
                    Text(
                        "Upload photo",
                        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Personal Information Section
                Column(modifier = Modifier.fillMaxWidth()) {
                    StaggeredEntranceItem(index = 2) {
                        Text(
                            "Personal Information",
                            style = AppTextTheme.bold.copy(fontSize = 16.sp),
                            color = Black
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Full Name
                    StaggeredEntranceItem(index = 3) {
                        FocusableScrollableTextFieldRow(
                            label = "Full Name",
                            value = fullName,
                            onValueChange = {
                                fullName = it
                                fullNameError = null
                            },
                            placeholder = "Enter Full Name",
                            errorText = fullNameError,
                            scrollCoroutineScope = scrollCoroutineScope,
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Gym Name
                    StaggeredEntranceItem(index = 4) {
                        FocusableScrollableTextFieldRow(
                            label = "Gym Name",
                            value = gymName,
                            onValueChange = {
                                gymName = it
                                gymNameError = null
                            },
                            placeholder = "Enter Gym Name",
                            errorText = gymNameError,
                            scrollCoroutineScope = scrollCoroutineScope,
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Gym Address
                    StaggeredEntranceItem(index = 5) {

                        LabeledField(label = "Address (Map Selection)") {
                            Box(modifier = Modifier.fillMaxWidth()) {
                                CommonTextField(
                                    value = gymAddress,
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
                                            navigator.push(SelectGymLocationScreen(gymAddress = gymAddress) { picked ->
                                                gymAddress = picked.address
                                                gymLatitude = picked.latitude
                                                gymLongitude = picked.longitude
                                                gymAddressError = null
                                            })
                                        }
                                )
                            }
                        }
//                        FocusableScrollableTextFieldRow(
//                            label = "Gym Address",
//                            value = gymAddress,
//                            onValueChange = {
//                                gymAddress = it
//                                gymAddressError = null
//                            },
//                            placeholder = "Enter Gym Address",
//                            errorText = gymAddressError,
//                            scrollCoroutineScope = scrollCoroutineScope,
//                            trailingIcon = {
//                                Surface(
//                                    onClick = {
//                                        navigator.push(SelectGymLocationScreen(gymAddress = gymAddress) { picked ->
//                                            gymAddress = picked.address
//                                            gymLatitude = picked.latitude
//                                            gymLongitude = picked.longitude
//                                            gymAddressError = null
//                                        })
//                                    },
//                                    shape = CircleShape,
//                                    color = Color(0xFFF3FBF9),
//                                    modifier = Modifier.size(40.dp)
//                                ) {
//                                    Box(contentAlignment = Alignment.Center) {
//                                        Icon(
//                                            painter = painterResource(Res.drawable.ic_location),
//                                            contentDescription = "Pick on map",
//                                            tint = PrimaryColor,
//                                            modifier = Modifier.size(20.dp)
//                                        )
//                                    }
//                                }
//                            },
//                        )

                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Gym GST Number
                    StaggeredEntranceItem(index = 6) {
                        FocusableScrollableTextFieldRow(
                            label = "Gym GST Number",
                            value = gymGstNumber,
                            onValueChange = {
                                gymGstNumber = it
                                gymGstNumberError = null
                            },
                            placeholder = "Enter GST Number",
                            errorText = gymGstNumberError,
                            scrollCoroutineScope = scrollCoroutineScope,
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Upload Gym Logo
                    StaggeredEntranceItem(index = 7) {
                        Column {
                            val isUploadingImage by screenModel.isUploadingImage.collectAsState()
                            DashedLogoUploadBox(
                                logoUrl = gymLogoUrl,
                                isUploading = isUploadingImage && currentPickingType == "logo",
                                onClick = {
                                    currentPickingType = "logo"
                                    showImagePicker = true
                                    gymLogoError = null
                                }
                            )
                            if (gymLogoError != null) {
                                Text(
                                    text = gymLogoError!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = AppTextTheme.regular.copy(fontSize = 12.sp),
                                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    StaggeredEntranceItem(index = 8) {
                        CommonButton(
                            text = "Update Profile",
                            onClick = {
                                var isValid = true

                                if (fullName.isBlank()) {
                                    fullNameError = "Full name is required"
                                    isValid = false
                                }
                                if (gymName.isBlank()) {
                                    gymNameError = "Gym name is required"
                                    isValid = false
                                }
                                if (gymAddress.isBlank()) {
                                    gymAddressError = "Gym address is required"
                                    isValid = false
                                }

                                val gstRegex = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$".toRegex()
                                if (gymGstNumber.isNotBlank() && !gymGstNumber.matches(gstRegex)) {
                                    gymGstNumberError = "Invalid GST Number format"
                                    isValid = false
                                }

                                if (gymLogoUrl == null) {
                                    gymLogoError = "Gym logo is required"
                                    isValid = false
                                }

                                if (isValid) {
                                    val request = UpdateOwnerProfileRequest(
                                        fullName = fullName,
                                        avatarUrl = profilePhotoUrl,
                                        profile_image = profilePhotoUrl,
                                        gymName = gymName,
                                        gymAddress = gymAddress,
                                        gymGstNumber = gymGstNumber,
                                        gymLogoUrl = gymLogoUrl
                                    )

                                    screenModel.updateOwnerSelfProfile(request) {
                                        // On success
                                        onUpdateSuccess?.invoke()
                                        navigator.pop()
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

            }
        }

            if (showImagePicker) {
                ImagePickerBottomSheet(
                    onDismiss = {
                        showImagePicker = false
                        pendingPickerAction = null
                    },
                    onGalleryClick = {
                        showImagePicker = false
                        pendingPickerAction = "gallery"
                    },
                    onCameraClick = {
                        showImagePicker = false
                        pendingPickerAction = "camera"
                    },
                    title = if (currentPickingType == "logo") "Upload Gym Logo" else "Upload Profile Photo",
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
                                if (currentPickingType == "logo") "gym_logo.jpg" else "profile_avatar.jpg"
                            ) { uploadedUrl ->
                                if (uploadedUrl != null) {
                                    if (currentPickingType == "logo") gymLogoUrl = uploadedUrl
                                    else profilePhotoUrl = uploadedUrl
                                }
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
                                if (currentPickingType == "logo") "gym_logo.jpg" else "profile_avatar.jpg"
                            ) { uploadedUrl ->
                                if (uploadedUrl != null) {
                                    if (currentPickingType == "logo") gymLogoUrl = uploadedUrl
                                    else profilePhotoUrl = uploadedUrl
                                }
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
private fun FocusableScrollableTextFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    errorText: String?,
    scrollCoroutineScope: CoroutineScope,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    Column {
        FieldLabel(label)
        CommonTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            modifier = Modifier
                .fillMaxWidth()
                .bringIntoViewRequester(bringIntoViewRequester)
                .onFocusEvent {
                    if (it.isFocused) {
                        scrollCoroutineScope.launch {
                            bringIntoViewRequester.bringIntoView()
                        }
                    }
                },
            errorText = errorText,
            trailingIcon = trailingIcon,
        )
    }
}

@Composable
fun FieldLabel(text: String) {
    Text(
        text = text,
        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black),
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun DashedLogoUploadBox(logoUrl: String?, isUploading: Boolean, onClick: () -> Unit) {
    val stroke =
        Stroke(width = 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .drawBehind {
                drawRoundRect(
                    color = Color(0xFFD1D5DB),
                    style = stroke,
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
            }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (logoUrl != null) {
            Image(
                painter = rememberAsyncImagePainter(logoUrl),
                contentDescription = "Gym Logo",
                modifier = Modifier.fillMaxSize().padding(12.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Fit
            )

            if (isUploading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = White)
                }
            } else {
                // Overlay an edit button
                Surface(
                    onClick = onClick,
                    color = PrimaryColor.copy(alpha = 0.8f),
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        } else {
            if (isUploading) {
                CircularProgressIndicator(color = PrimaryColor)
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color(0xFFE7F7F2), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = PrimaryColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Upload Gym Logo",
                        style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "PNG, JPG, WEBP up to 10MB",
                        style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = PrimaryColor,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Browse Files",
                                style = AppTextTheme.bold.copy(fontSize = 14.sp, color = White)
                            )
                        }
                    }
                }
            }
        }
    }
}
