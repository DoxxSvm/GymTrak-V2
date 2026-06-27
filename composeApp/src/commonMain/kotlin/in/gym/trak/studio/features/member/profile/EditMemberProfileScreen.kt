package `in`.gym.trak.studio.features.member

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.rememberAsyncImagePainter
import io.github.ismoy.imagepickerkmp.domain.config.ImagePickerConfig
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import io.github.ismoy.imagepickerkmp.presentation.ui.components.ImagePickerLauncher
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.GenderSelector
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import `in`.gym.trak.studio.data.model.MemberProfileDetailResponse
import `in`.gym.trak.studio.data.model.MemberProfileUpdateRequest
import `in`.gym.trak.studio.getCurrentTimeMillis
import `in`.gym.trak.studio.utils.DateUtils
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.viewmodel.member.MemberProfileScreenModel
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.gym_boy
import org.jetbrains.compose.resources.painterResource
import kotlin.jvm.Transient

class EditMemberProfileScreen(
    private val initialProfile: MemberProfileDetailResponse? = null,
    @Transient private val onUpdateSuccess: (() -> Unit)? = null,
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { MemberProfileScreenModel() }

        var fullName by rememberSaveable { mutableStateOf(initialProfile?.name ?: "") }
        var age by rememberSaveable { mutableStateOf(initialProfile?.age?: "") }
        var selectedGender by rememberSaveable {
            mutableStateOf(initialProfile?.gender.orEmpty().normalizeGenderUi())
        }

        var fullNameError by remember { mutableStateOf<String?>(null) }
        var ageError by remember { mutableStateOf<String?>(null) }
        val profileFromVm by screenModel.memberDetail.collectAsState()
        val isUploadingImage by screenModel.isUploadingImage.collectAsState()
        var showImagePickerBottomSheet by remember { mutableStateOf(false) }
        var launchGallery by remember { mutableStateOf(false) }
        var launchCamera by remember { mutableStateOf(false) }
        var pendingPickerAction by remember { mutableStateOf<String?>(null) }

        var profileImageUrl by rememberSaveable { mutableStateOf(initialProfile?.profile_image) }

        LaunchedEffect(Unit) {
            if (initialProfile == null) {
                screenModel.loadProfile(showFullLoader = false)
            }
        }

        LaunchedEffect(profileFromVm) {
            val profile = profileFromVm ?: return@LaunchedEffect
            if (fullName.isBlank()) fullName = profile.name
            if (age.isBlank()) age = profile.dob?.let(::dobToAge).orEmpty()
            if (selectedGender.isBlank()) {
                selectedGender = profile.gender.orEmpty().normalizeGenderUi()
            }
            if (profileImageUrl.isNullOrBlank()) {
                profileImageUrl = profile.profile_image
            }
        }

        LaunchedEffect(showImagePickerBottomSheet, pendingPickerAction) {
            if (showImagePickerBottomSheet || pendingPickerAction == null) return@LaunchedEffect
            when (pendingPickerAction) {
                "gallery" -> launchGallery = true
                "camera" -> launchCamera = true
            }
            pendingPickerAction = null
        }

        LoadingScreenHandler(screenModel = screenModel) {
            androidx.compose.material3.Scaffold(
                topBar = {
                    GymAppBar(
                        title = "Edit Profile",
                        onBackClick = { navigator?.pop() },
                    )
                },
            ) { padding ->
                AppScrollableScreen(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(12.dp))

                    StaggeredEntranceItem(index = 0) {
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Box(
                                modifier = Modifier
                                    .size(92.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF3F4F6))
                                    .border(1.dp, Color(0xFFE5E7EB), CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                val painter = if (profileImageUrl.isNullOrBlank()) {
                                    painterResource(Res.drawable.gym_boy)
                                } else {
                                    rememberAsyncImagePainter(profileImageUrl)
                                }
                                Image(
                                    painter = painter,
                                    contentDescription = "Profile photo",
                                    modifier = Modifier
                                        .size(86.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop,
                                )

                                if (isUploadingImage) {
                                    Box(
                                        modifier = Modifier
                                            .size(86.dp)
                                            .clip(CircleShape)
                                            .background(Color.Black.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(22.dp),
                                            color = White,
                                            strokeWidth = 2.dp,
                                        )
                                    }
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = PrimaryColor,
                                modifier = Modifier
                                    .size(30.dp)
                                    .clickable { showImagePickerBottomSheet = true },
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = White,
                                        modifier = Modifier.size(14.dp),
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Upload photo",
                        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black),
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    StaggeredEntranceItem(index = 1) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            FieldLabel("Name")
                            CommonTextField(
                                value = fullName,
                                onValueChange = {
                                    fullName = it
                                    fullNameError = null
                                },
                                placeholder = "eg. John",
                                errorText = fullNameError,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    StaggeredEntranceItem(index = 2) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            FieldLabel("Age")
                            CommonTextField(
                                value = age,
                                onValueChange = {
                                    age = it.filter(Char::isDigit).take(2)
                                    ageError = null
                                },
                                placeholder = "Enter your age",
                                errorText = ageError,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    StaggeredEntranceItem(index = 3) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            FieldLabel("Gender")
                            GenderSelector(
                                selectedGender = selectedGender.ifBlank { "Male" },
                                onGenderSelected = { selectedGender = it },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    StaggeredEntranceItem(index = 4) {
                        CommonButton(
                            text = "Update Profile",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                var isValid = true
                                if (fullName.isBlank()) {
                                    fullNameError = "Name is required"
                                    isValid = false
                                }
                                if (age.isBlank()) {
                                    ageError = "Age is required"
                                    isValid = false
                                }
                                if (!isValid) return@CommonButton

                                val request = MemberProfileUpdateRequest(
                                    fullName = fullName.trim(),
                                    age = age,
                                    gender = selectedGender.lowercase(),
                                    profile_image = profileImageUrl,
                                )
                                screenModel.updateProfile(request) {
                                    onUpdateSuccess?.invoke()
                                    navigator?.pop()
                                }
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
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
                    subtitle = "Choose from gallery or take a new picture",
                )
            }

            if (launchGallery) {
                GalleryPickerLauncher(
                    onPhotosSelected = { photos ->
                        launchGallery = false
                        photos.firstOrNull()?.let { photo ->
                            screenModel.uploadImage(photo.loadBytes(), "member_avatar.jpg") { uploadedUrl ->
                                if (!uploadedUrl.isNullOrBlank()) profileImageUrl = uploadedUrl
                            }
                        }
                    },
                    onError = { launchGallery = false },
                    onDismiss = { launchGallery = false },
                )
            }

            if (launchCamera) {
                ImagePickerLauncher(
                    config = ImagePickerConfig(
                        onPhotoCaptured = { photo ->
                            launchCamera = false
                            screenModel.uploadImage(photo.loadBytes(), "member_avatar.jpg") { uploadedUrl ->
                                if (!uploadedUrl.isNullOrBlank()) profileImageUrl = uploadedUrl
                            }
                        },
                        onError = { launchCamera = false },
                    ),
                )
            }
        }
    }
}

private fun String.normalizeGenderUi(): String {
    return when (trim().lowercase()) {
        "male" -> "Male"
        "female" -> "Female"
        "other" -> "Other"
        else -> ""
    }
}

private fun dobToAge(dob: String): String {
    val dateOnly = DateUtils.birthDateToIsoDateOnly(dob) ?: return ""
    val year = dateOnly.take(4).toIntOrNull() ?: return ""
    val currentYear = (getCurrentTimeMillis() / 31_556_952_000L + 1970).toInt()
    val age = (currentYear - year).coerceAtLeast(0)
    return age.toString()
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black),
        modifier = Modifier.padding(bottom = 8.dp),
    )
}
