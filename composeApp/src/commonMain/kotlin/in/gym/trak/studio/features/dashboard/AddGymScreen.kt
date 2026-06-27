package `in`.gym.trak.studio.features.dashboard

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import `in`.gym.trak.studio.data.model.CreateOwnedGymRequest
import `in`.gym.trak.studio.data.model.UserOwnedGymDTO
import `in`.gym.trak.studio.data.model.resolvedLatitude
import `in`.gym.trak.studio.data.model.resolvedLongitude
import `in`.gym.trak.studio.features.location.SelectGymLocationScreen
import `in`.gym.trak.studio.features.member.ImagePickerBottomSheet
import `in`.gym.trak.studio.theme.*
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_location
import io.github.ismoy.imagepickerkmp.domain.config.ImagePickerConfig
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import io.github.ismoy.imagepickerkmp.presentation.ui.components.ImagePickerLauncher
import org.jetbrains.compose.resources.painterResource
import kotlin.jvm.Transient

private const val ADD_GYM_LOGO_FILE = "add_gym_logo.jpg"
private const val EDIT_GYM_LOGO_FILE = "edit_gym_logo.jpg"

/**
 * Create ([POST /gyms]) or update ([PATCH /gyms/{id}]) an owned gym.
 * Logo uses the same image upload endpoint as the rest of the app.
 */
class AddGymScreen(
    @Transient private val sharedDashboardScreenModel: OwnerDashboardScreenModel? = null,
    @Transient private val existingGym: UserOwnedGymDTO? = null,
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel =
            sharedDashboardScreenModel ?: rememberScreenModel { OwnerDashboardScreenModel() }

        val isEditMode = existingGym != null

        var gymName by rememberSaveable { mutableStateOf("") }
        var gymAddress by rememberSaveable { mutableStateOf("") }
        // Persist across navigation — plain remember was cleared when returning from the map screen.
        var pickedLatSave by rememberSaveable { mutableStateOf("") }
        var pickedLngSave by rememberSaveable { mutableStateOf("") }
        var gstin by rememberSaveable { mutableStateOf("") }
        var logoUrl by rememberSaveable { mutableStateOf<String?>(null) }

        var gymNameError by remember { mutableStateOf<String?>(null) }
        var gymAddressError by remember { mutableStateOf<String?>(null) }
        var locationError by remember { mutableStateOf<String?>(null) }
        var logoError by remember { mutableStateOf<String?>(null) }
        var gstinError by remember { mutableStateOf<String?>(null) }

        var showImagePicker by remember { mutableStateOf(false) }
        var launchGallery by remember { mutableStateOf(false) }
        var launchCamera by remember { mutableStateOf(false) }
        var pendingPickerAction by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(existingGym?.id) {
            val g = existingGym ?: return@LaunchedEffect
            gymName = g.name
            gymAddress = g.address.orEmpty()
            pickedLatSave = g.resolvedLatitude()?.toString().orEmpty()
            pickedLngSave = g.resolvedLongitude()?.toString().orEmpty()
            gstin = g.gstin.orEmpty()
            logoUrl = g.logoUrl
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
        val isUploadingImage by screenModel.isUploadingImage.collectAsState()

        LoadingScreenHandler(screenModel = screenModel) {
        Scaffold(
            topBar = {
                GymAppBar(
                    title = if (isEditMode) "Edit gym" else "Add Gym",
                    onBackClick = { navigator.pop() }
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            AppScrollableScreen(
                modifier = Modifier.fillMaxSize().padding(top = 100.dp),
                scrollState = scrollState,
                contentPadding = PaddingValues(24.dp),
                dismissKeyboardOnTap = true,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StaggeredEntranceItem(index = 0) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        FieldLabel("Gym name")
                        CommonTextField(
                            value = gymName,
                            onValueChange = {
                                gymName = it
                                gymNameError = null
                            },
                            placeholder = "Enter gym name",
                            errorText = gymNameError,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                StaggeredEntranceItem(index = 1) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        FieldLabel("Gym address")
                        CommonTextField(
                            value = gymAddress,
                            onValueChange = {
                                gymAddress = it
                                gymAddressError = null
                            },
                            placeholder = "Enter address or pick on map",
                            errorText = gymAddressError,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Surface(
                                    onClick = {
                                        navigator.push(
                                            SelectGymLocationScreen(gymAddress = gymAddress) { picked ->
                                                gymAddress = picked.address
                                                pickedLatSave = picked.latitude.toString()
                                                pickedLngSave = picked.longitude.toString()
                                                gymAddressError = null
                                                locationError = null
                                            }
                                        )
                                    },
                                    shape = CircleShape,
                                    color = Color(0xFFF3FBF9),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_location),
                                            contentDescription = "Pick on map",
                                            tint = PrimaryColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        )
                        if (pickedLatSave.isNotBlank() && pickedLngSave.isNotBlank()) {
                            Text(
                                text = "Location: $pickedLatSave, $pickedLngSave",
                                style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray),
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                            )
                        }
                        locationError?.let { err ->
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                style = AppTextTheme.regular.copy(fontSize = 12.sp),
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                StaggeredEntranceItem(index = 2) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        FieldLabel("GSTIN (optional)")
                        CommonTextField(
                            value = gstin,
                            onValueChange = {
                                gstin = it.uppercase()
                                gstinError = null
                            },
                            placeholder = "15-character GSTIN",
                            errorText = gstinError,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                StaggeredEntranceItem(index = 3) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        FieldLabel("Gym logo")
                        DashedLogoUploadBox(
                            logoUrl = logoUrl,
                            isUploading = isUploadingImage,
                            onClick = {
                                showImagePicker = true
                                logoError = null
                            }
                        )
                        logoError?.let { err ->
                            Text(
                                text = err,
                                color = MaterialTheme.colorScheme.error,
                                style = AppTextTheme.regular.copy(fontSize = 12.sp),
                                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                StaggeredEntranceItem(index = 4) {
                    CommonButton(
                        text = if (isEditMode) "Save changes" else "Create gym",
                        onClick = {
                            var ok = true
                            val effectiveLogo = logoUrl ?: existingGym?.logoUrl
                            if (gymName.isBlank()) {
                                gymNameError = "Gym name is required"
                                ok = false
                            }
                            if (gymAddress.isBlank()) {
                                gymAddressError = "Address is required"
                                ok = false
                            }
                            if (pickedLatSave.isBlank() || pickedLngSave.isBlank()) {
                                locationError = "Pick the gym location on the map"
                                ok = false
                            }
                            if (effectiveLogo.isNullOrBlank()) {
                                logoError = "Please upload a gym logo"
                                ok = false
                            }
                            val gstRegex =
                                "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$".toRegex()
                            if (gstin.isNotBlank() && !gstin.matches(gstRegex)) {
                                gstinError = "Invalid GSTIN format"
                                ok = false
                            }
                            if (!ok) return@CommonButton

                            val latitude = pickedLatSave.toDoubleOrNull()
                            val longitude = pickedLngSave.toDoubleOrNull()
                            if (latitude == null || longitude == null) {
                                locationError = "Pick the gym location on the map"
                                return@CommonButton
                            }

                            val request = CreateOwnedGymRequest(
                                name = gymName.trim(),
                                address = gymAddress.trim(),
                                latitude = latitude,
                                longitude = longitude,
                                gstin = gstin.trim(),
                                logoUrl = effectiveLogo
                            )
                            if (isEditMode && existingGym != null) {
                                screenModel.updateOwnedGym(
                                    gymId = existingGym.id,
                                    request = request,
                                    onSuccess = { navigator.pop() }
                                )
                            } else {
                                screenModel.createOwnedGym(
                                    request,
                                    onSuccess = { navigator.pop() }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))
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
                    title = "Upload Gym Logo",
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
                                if (isEditMode) EDIT_GYM_LOGO_FILE else ADD_GYM_LOGO_FILE
                            ) { uploaded ->
                                if (uploaded != null) {
                                    logoUrl = uploaded
                                    logoError = null
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
                                if (isEditMode) EDIT_GYM_LOGO_FILE else ADD_GYM_LOGO_FILE
                            ) { uploaded ->
                                if (uploaded != null) {
                                    logoUrl = uploaded
                                    logoError = null
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
