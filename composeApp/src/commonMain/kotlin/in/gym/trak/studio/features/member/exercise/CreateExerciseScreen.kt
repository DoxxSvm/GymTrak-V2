package `in`.gym.trak.studio.features.member.exercise

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.rememberAsyncImagePainter
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.gym_boy
import gym.composeapp.generated.resources.ic_camera
import `in`.gym.trak.studio.components.CommonBottomSheetPicker
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import `in`.gym.trak.studio.data.model.CreateExerciseRequest
import `in`.gym.trak.studio.features.member.ImagePickerBottomSheet
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.GrayBorderColor
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.RedColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import io.github.ismoy.imagepickerkmp.domain.config.ImagePickerConfig
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import io.github.ismoy.imagepickerkmp.presentation.ui.components.ImagePickerLauncher
import org.jetbrains.compose.resources.painterResource

/**
 * Screen for creating a new exercise with name, equipment, muscle group and type.
 */
class CreateExerciseScreen(val onExerciseCreated: () -> Unit = {}) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val metadata by screenModel.metadata.collectAsState()

        var exerciseName by remember { mutableStateOf("") }
        var selectedEquipment by remember { mutableStateOf<String?>(null) }
        var selectedMuscleGroup by remember { mutableStateOf<String?>(null) }
        var selectedType by remember { mutableStateOf<String?>(null) }
        var assetUrl by remember { mutableStateOf<String?>(null) }

        var showImagePickerBottomSheet by remember { mutableStateOf(false) }
        var launchGallery by remember { mutableStateOf(false) }
        var launchCamera by remember { mutableStateOf(false) }
        var pendingPickerAction by remember { mutableStateOf<String?>(null) }
        var exerciseNameError by remember { mutableStateOf<String?>(null) }
        var equipmentError by remember { mutableStateOf<String?>(null) }
        var muscleGroupError by remember { mutableStateOf<String?>(null) }
        var typeError by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            screenModel.loadMetadata()
        }

        LaunchedEffect(showImagePickerBottomSheet, pendingPickerAction) {
            if (showImagePickerBottomSheet || pendingPickerAction == null) return@LaunchedEffect
            when (pendingPickerAction) {
                "gallery" -> launchGallery = true
                "camera" -> launchCamera = true
            }
            pendingPickerAction = null
        }
        val scrollState = rememberScrollState()

        val equipmentOptions = metadata?.equipments ?: emptyList()
        val muscleGroupOptions = metadata?.muscles ?: emptyList()
        val exerciseTypeOptions = metadata?.exercise_types ?: emptyList()

        LoadingScreenHandler(screenModel = screenModel) {
        Scaffold(
            topBar = {
                GymAppBar(
                    title = "Create Exercise",
                    onBackClick = { navigator.pop() }
                )
            },
//            bottomBar = {
//                Box(modifier = Modifier.Companion.padding(24.dp).navigationBarsPadding()) {
//                    CommonButton(
//                        text = "Create Exercise",
//                        onClick = {
//                            val normalizedName = exerciseName.trim()
//                            exerciseNameError = when {
//                                normalizedName.isBlank() -> "Exercise name is required."
//                                normalizedName.length < 2 -> "Exercise name must be at least 2 characters."
//                                else -> null
//                            }
//                            equipmentError =
//                                if (selectedEquipment == null) "Please select equipment." else null
//                            muscleGroupError =
//                                if (selectedMuscleGroup == null) "Please select primary muscle group." else null
//                            typeError =
//                                if (selectedType == null) "Please select exercise type." else null
//
//                            if (
//                                exerciseNameError != null ||
//                                equipmentError != null ||
//                                muscleGroupError != null ||
//                                typeError != null
//                            ) return@CommonButton
//
//                            screenModel.createExercise(
//                                CreateExerciseRequest(
//                                    name = normalizedName,
//                                    equipment = selectedEquipment!!,
//                                    primary_muscle = selectedMuscleGroup!!,
//                                    exercise_type = selectedType!!,
//                                    asset_url = assetUrl
//                                )
//                            ) {
//                                onExerciseCreated()
//                                navigator.pop()
//                            }
//                        },
//                    )
//                }
//            },
            containerColor = Color.Companion.Transparent
        ) { paddingValues ->
            LoadingScreenHandler(screenModel = screenModel) {
                AppScrollableScreen(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    scrollState = scrollState,
                    contentPadding = PaddingValues(24.dp),
                    dismissKeyboardOnTap = true,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.Companion.height(20.dp))

                    // Asset Picker
                    StaggeredEntranceItem(index = 0) {
                        Column(
                            modifier = Modifier.Companion.clickable {
                                showImagePickerBottomSheet = true
                            },
                            horizontalAlignment = Alignment.Companion.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier.Companion
                                    .size(100.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, GrayBorderColor, CircleShape)
                                    .background(Color(0xFFF9FAFB)),
                                contentAlignment = Alignment.Companion.Center
                            ) {
                                val painter = if (!assetUrl.isNullOrEmpty()) {
                                    rememberAsyncImagePainter(assetUrl)
                                } else {
                                    painterResource(Res.drawable.gym_boy)
                                }
                                Image(
                                    painter = painter,
                                    contentDescription = "Exercise Asset",
                                    modifier = Modifier.Companion.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Companion.Crop,
                                )
                                if (assetUrl == null) {
                                    Box(
                                        modifier = Modifier.Companion
                                            .fillMaxSize()
                                            .background(Color.Companion.Black.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Companion.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_camera),
                                            contentDescription = "Add Asset",
                                            tint = White,
                                            modifier = Modifier.Companion.size(32.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.Companion.height(12.dp))
                            Text(
                                text = if (assetUrl != null) "Change Asset" else "Add Asset",
                                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.Companion.height(40.dp))

                    // Form Fields
                    StaggeredEntranceItem(index = 1) {
                        Column {
                            InputFieldLabel("Exercise Name")
                            CommonTextField(
                                value = exerciseName,
                                onValueChange = {
                                    exerciseName = it
                                    exerciseNameError = null
                                },
                                placeholder = "eg. Dumbbell Press",
                                modifier = Modifier.Companion.fillMaxWidth(),
                                errorText = exerciseNameError
                            )
                        }
                    }

                    Spacer(modifier = Modifier.Companion.height(24.dp))

                    StaggeredEntranceItem(index = 2) {
                        Column {
                            InputFieldLabel("Equipment")
                            CommonBottomSheetPicker(
                                options = equipmentOptions.map { it.label },
                                selectedOption = equipmentOptions.find { it.value == selectedEquipment }?.label,
                                onOptionSelected = { label ->
                                    selectedEquipment =
                                        equipmentOptions.find { it.label == label }?.value
                                    equipmentError = null
                                },
                                placeholder = "Select Equipment",
                                title = "Select Equipment"
                            )
                            equipmentError?.let {
                                Text(
                                    text = it,
                                    color = RedColor,
                                    style = AppTextTheme.regular.copy(fontSize = 12.sp),
                                    modifier = Modifier.Companion.padding(top = 4.dp, start = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.Companion.height(24.dp))

                    StaggeredEntranceItem(index = 3) {
                        Column {
                            InputFieldLabel("Primary Muscle Group")
                            CommonBottomSheetPicker(
                                options = muscleGroupOptions.map { it.label },
                                selectedOption = muscleGroupOptions.find { it.value == selectedMuscleGroup }?.label,
                                onOptionSelected = { label ->
                                    selectedMuscleGroup =
                                        muscleGroupOptions.find { it.label == label }?.value
                                    muscleGroupError = null
                                },
                                placeholder = "Select Muscle Group",
                                title = "Select Muscle Group"
                            )
                            muscleGroupError?.let {
                                Text(
                                    text = it,
                                    color = RedColor,
                                    style = AppTextTheme.regular.copy(fontSize = 12.sp),
                                    modifier = Modifier.Companion.padding(top = 4.dp, start = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.Companion.height(24.dp))

                    StaggeredEntranceItem(index = 4) {
                        Column {
                            InputFieldLabel("Exercise Type")
                            CommonBottomSheetPicker(
                                options = exerciseTypeOptions,
                                selectedOption = exerciseTypeOptions.find { it.value == selectedType },
                                onOptionSelected = { type ->
                                    selectedType = type.value
                                    typeError = null
                                },
                                optionToAnnotatedString = { type ->
                                    buildAnnotatedString {
                                        withStyle(
                                            SpanStyle(
                                                color = Color.Companion.Black,
                                                fontWeight = FontWeight.Companion.SemiBold
                                            )
                                        ) {
                                            append(type.label)
                                        }
                                        append("   ")
                                        type.fields.forEachIndexed { index, field ->
                                            withStyle(
                                                SpanStyle(
                                                    color = PrimaryColor.copy(alpha = 0.7f),
                                                    fontSize = 10.sp
                                                )
                                            ) {
                                                append("▪ ")
                                            }
                                            withStyle(
                                                SpanStyle(
                                                    color = PrimaryColor,
                                                    fontWeight = FontWeight.Companion.Medium,
                                                    fontSize = 11.sp
                                                )
                                            ) {
                                                append(field)
                                            }
                                            if (index < type.fields.size - 1) append("  ")
                                        }
                                    }
                                },
                                placeholder = "Select Type",
                                title = "Select Exercise Type"
                            )
                            typeError?.let {
                                Text(
                                    text = it,
                                    color = RedColor,
                                    style = AppTextTheme.regular.copy(fontSize = 12.sp),
                                    modifier = Modifier.Companion.padding(top = 4.dp, start = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    CommonButton(
                        text = "Create Exercise",
                        onClick = {
                            val normalizedName = exerciseName.trim()
                            exerciseNameError = when {
                                normalizedName.isBlank() -> "Exercise name is required."
                                normalizedName.length < 2 -> "Exercise name must be at least 2 characters."
                                else -> null
                            }
                            equipmentError =
                                if (selectedEquipment == null) "Please select equipment." else null
                            muscleGroupError =
                                if (selectedMuscleGroup == null) "Please select primary muscle group." else null
                            typeError =
                                if (selectedType == null) "Please select exercise type." else null

                            if (
                                exerciseNameError != null ||
                                equipmentError != null ||
                                muscleGroupError != null ||
                                typeError != null
                            ) return@CommonButton

                            screenModel.createExercise(
                                CreateExerciseRequest(
                                    name = normalizedName,
                                    equipment = selectedEquipment!!,
                                    primary_muscle = selectedMuscleGroup!!,
                                    exercise_type = selectedType!!,
                                    asset_url = assetUrl
                                )
                            ) {
                                onExerciseCreated()
                                navigator.pop()
                            }
                        },
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
                    title = "Upload Exercise Photo",
                    subtitle = "Choose an image to represent this exercise"
                )
            }

            if (launchGallery) {
                GalleryPickerLauncher(
                    onPhotosSelected = { photos ->
                        launchGallery = false
                        photos.firstOrNull()?.let { photo ->
                            screenModel.uploadImage(
                                photo.loadBytes(),
                                "exercise_asset.jpg"
                            ) { uploadedUrl ->
                                if (uploadedUrl != null) assetUrl = uploadedUrl
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
                                "exercise_asset.jpg"
                            ) { uploadedUrl ->
                                if (uploadedUrl != null) assetUrl = uploadedUrl
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
    private fun InputFieldLabel(text: String) {
        Text(
            text = text,
            style = AppTextTheme.bold.copy(fontSize = 14.sp, color = Color.Companion.Black),
            modifier = Modifier.Companion.fillMaxWidth().padding(bottom = 12.dp)
        )
    }
}
