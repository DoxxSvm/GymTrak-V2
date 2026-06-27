package `in`.gym.trak.studio.features.member

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.CommonOutlineButton
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.BlueLightColor
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.GrayBorderColor
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.RedColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.data.model.WorkoutDetailResponse
import `in`.gym.trak.studio.utils.DateUtils
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_camera
import gym.composeapp.generated.resources.ic_duration
import gym.composeapp.generated.resources.ic_gallery
import gym.composeapp.generated.resources.ic_sets
import gym.composeapp.generated.resources.ic_volume
import gym.composeapp.generated.resources.img_discard_workout
import io.github.ismoy.imagepickerkmp.domain.config.ImagePickerConfig
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import io.github.ismoy.imagepickerkmp.presentation.ui.components.ImagePickerLauncher
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import coil3.compose.rememberAsyncImagePainter
import org.jetbrains.compose.resources.painterResource
import kotlin.jvm.Transient

class SaveWorkoutScreen(
    @Transient private val stoppedWorkout: WorkoutDetailResponse? = null,
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val durationLabel = stoppedWorkout?.duration?.takeIf { it.isNotBlank() } ?: "03:39"
        val volumeLabel = stoppedWorkout?.volume?.takeIf { it.isNotBlank() } ?: "0 Kg"
        val setsLabel = stoppedWorkout?.let { "${it.sets}" } ?: "0"
        val workoutTitleLabel = stoppedWorkout?.title?.takeIf { it.isNotBlank() } ?: "Workout"
        val whenLabel = DateUtils.formatNowAsDayMonthYearTime()

        var notes by remember { mutableStateOf("") }
        var isPublic by remember { mutableStateOf(false) }
        var showDiscardDialog by remember { mutableStateOf(false) }
        var visibility by remember { mutableStateOf("Everyone") }
        var showImagePickerBottomSheet by remember { mutableStateOf(false) }
        var launchGallery by remember { mutableStateOf(false) }
        var launchCamera by remember { mutableStateOf(false) }
        var pendingPickerAction by remember { mutableStateOf<String?>(null) }
        var selectedPhotos by remember { mutableStateOf(listOf<String>()) }

        LaunchedEffect(showImagePickerBottomSheet, pendingPickerAction) {
            if (showImagePickerBottomSheet || pendingPickerAction == null) return@LaunchedEffect
            when (pendingPickerAction) {
                "gallery" -> launchGallery = true
                "camera" -> launchCamera = true
            }
            pendingPickerAction = null
        }


        if (showDiscardDialog) {
            DiscardWorkoutDialog(
                onDismiss = { showDiscardDialog = false },
                onDiscard = {
                    showDiscardDialog = false
                    navigator.popUntilRoot()
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    GymAppBar(
                        title = "Save Workout",
                        onBackClick = { navigator.pop() },
                        actions = {
                            Box(
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .clip(RoundedCornerShape(50.dp))
                                    .background(PrimaryColor)
                                    .clickable {
                                        navigator.push(
                                            WorkoutSuccessScreen(
                                                workoutId = stoppedWorkout?.workoutId.orEmpty(),
                                                workout = stoppedWorkout,
                                            )
                                        )
                                    }
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    "Save",
                                    style = AppTextTheme.semiBold.copy(
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    )
                },
                containerColor = Color.Transparent
            )
            { paddingValues ->
                AppScrollableScreen(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = workoutTitleLabel,
                        style = AppTextTheme.semiBold.copy(fontSize = 18.sp, color = Black),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )

                    CommonCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 0.dp,
                        borderColor = Color(0xFFE5E7EB),
                        backgroundColor = White
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            WorkoutDetailStat(
                                modifier = Modifier.weight(1f),
                                icon = Res.drawable.ic_duration,
                                label = "Duration",
                                value = durationLabel,
                                valueColor = Black
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(GrayBorderColor)
                            )
                            WorkoutDetailStat(
                                modifier = Modifier.weight(1f),
                                icon = Res.drawable.ic_volume,
                                label = "Volume",
                                value = volumeLabel,
                                valueColor = Black
                            )
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .height(40.dp)
                                    .background(GrayBorderColor)
                            )
                            WorkoutDetailStat(
                                modifier = Modifier.weight(1f),
                                icon = Res.drawable.ic_sets,
                                label = "Sets",
                                value = setsLabel,
                                valueColor = Black
                            )
                        }
                    }

                    // Date Card
                    CommonCard(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = 0.dp,
                        backgroundColor = Color.White
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "When:",
                                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Color.Black)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                whenLabel,
                                style = AppTextTheme.semiBold.copy(
                                    fontSize = 14.sp,
                                    color = PrimaryColor
                                )
                            )
                        }
                    }

                    if (selectedPhotos.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            selectedPhotos.forEach { uri ->
                                Image(
                                    painter = rememberAsyncImagePainter(uri),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    // Photo Picker — Dotted border
                    DottedBorderBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clickable { showImagePickerBottomSheet = true },
                        cornerRadius = 16.dp,
                        color = Color(0xFFB0B0B0)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_camera),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Add Workout Photos",
                                style = AppTextTheme.medium.copy(
                                    fontSize = 14.sp,
                                    color = Black
                                )
                            )
                        }
                    }

                    // Notes Card
                    CommonTextField(
                        value = notes,
                        onValueChange = {
                            notes = it
                        },
                        placeholder = "Description notes here...",
                        isMultiline = true,
                        borderRadius = 12.dp
                    )

                    // Visibility Card
                    CommonCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { },
                        elevation = 0.dp,
                        borderColor = Color(0xFFE5E7EB),
                        backgroundColor = Color.White
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Visibility",
                                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Color.Black),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                visibility,
                                style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

//                    Spacer(modifier = Modifier.height(8.dp))
//
//                    CommonOutlineButton(
//                        onClick = { showDiscardDialog = true },
//                        borderColor = Color.Transparent,
//                        color = RedColor.copy(alpha = 0.1f),
//                        textColor = RedColor.copy(alpha = 0.8f),
//                        text = "Discard Workout",
//                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (launchGallery) {
                GalleryPickerLauncher(
                    onPhotosSelected = { photos ->
                        launchGallery = false
                        selectedPhotos = selectedPhotos + photos.map { it.uri }
                        println("Selected Workout Photos: ${photos.map { it.uri }}")
                    },
                    onError = { 
                        launchGallery = false 
                        println("Workout Gallery Picker Error: ${it.message}")
                    },
                    onDismiss = { launchGallery = false }
                )
            }

            if (launchCamera) {
                ImagePickerLauncher(
                    config = ImagePickerConfig(
                        onPhotoCaptured = { photo ->
                            launchCamera = false
                            selectedPhotos = selectedPhotos + listOf(photo.uri)
                            println("Captured Workout Photo: ${photo.uri}")
                        },
                        onError = { 
                            launchCamera = false 
                            println("Workout Camera Picker Error: ${it.message}")
                        }
                    )
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
                }
            )
        }
    }

}

@Composable
fun SaveWorkoutStat(
    icon: org.jetbrains.compose.resources.DrawableResource,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFFFF3E0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray))
        Text(value, style = AppTextTheme.bold.copy(fontSize = 14.sp, color = Color.Black))
    }
}

@Composable
fun DottedBorderBox(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    color: Color = Color.Gray,
    strokeWidth: Dp = 1.5.dp,
    dashLength: Dp = 8.dp,
    gapLength: Dp = 6.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val stroke = Stroke(
                width = strokeWidth.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(dashLength.toPx(), gapLength.toPx()),
                    0f
                )
            )
            val radius = cornerRadius.toPx()
            val path = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = stroke.width / 2,
                        top = stroke.width / 2,
                        right = size.width - stroke.width / 2,
                        bottom = size.height - stroke.width / 2,
                        cornerRadius = CornerRadius(radius, radius)
                    )
                )
            }
            drawPath(path = path, color = color, style = stroke)
        }
        content()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerBottomSheet(
    onDismiss: () -> Unit,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    title: String = "Add Workout Photos",
    subtitle: String = "Choose a source to add your workout photo"
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFE5E7EB))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(
                title,
                style = AppTextTheme.bold.copy(fontSize = 18.sp),
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                subtitle,
                style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Gray),
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Gallery option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BlueLightColor)
                    .clickable { onGalleryClick() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_gallery),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Gallery",
                        style = AppTextTheme.semiBold.copy(fontSize = 15.sp, color = Color.Black)
                    )
                    Text(
                        "Choose from your photo library",
                        style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Gray)
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Camera option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BlueLightColor)
                    .clickable { onCameraClick() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF3B82F6).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_camera),
                        contentDescription = null,
                        tint = Color.Unspecified,

                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Camera",
                        style = AppTextTheme.semiBold.copy(fontSize = 15.sp, color = Color.Black)
                    )
                    Text(
                        "Take a photo right now",
                        style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Gray)
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Cancel button
            CommonButton(
                onClick = onDismiss,
                text = "Cancel"
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun DiscardWorkoutDialog(onDismiss: () -> Unit, onDiscard: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        CommonCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            backgroundColor = White,
            borderColor = Color(0xFFF1F5F9)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(Res.drawable.img_discard_workout),
                    contentDescription = "Discard Workout",
                    modifier = Modifier.size(160.dp),
                    contentScale = ContentScale.Fit
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = "Are You sure you want to\ndiscard this workout?",
                    style = AppTextTheme.medium.copy(fontSize = 15.sp, color = Color.Black),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Discard Workout (Red button)
                    androidx.compose.material3.Button(
                        onClick = onDiscard,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = RedColor.copy(alpha = 0.9f)),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Discard Workout", style = AppTextTheme.semiBold.copy(fontSize = 13.sp, color = White))
                    }
                    
                    // Cancel (Outline button)
                    androidx.compose.material3.OutlinedButton(
                        onClick = onDismiss,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Color.Black),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier.weight(1f).height(48.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Cancel", style = AppTextTheme.semiBold.copy(fontSize = 14.sp, color = Color.Black))
                    }
                }
            }
        }
    }
}

