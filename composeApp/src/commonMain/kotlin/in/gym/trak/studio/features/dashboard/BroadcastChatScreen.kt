package `in`.gym.trak.studio.features.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.features.dashboard.ImageAvatar
import `in`.gym.trak.studio.data.model.BroadcastMessageDTO
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.utils.DateUtils
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel

import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Subject
import androidx.compose.ui.text.style.TextOverflow
import cafe.adriel.voyager.core.model.rememberScreenModel
import `in`.gym.trak.studio.features.member.ImagePickerBottomSheet
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import io.github.ismoy.imagepickerkmp.presentation.ui.components.ImagePickerLauncher
import io.github.ismoy.imagepickerkmp.domain.config.ImagePickerConfig
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import kotlinx.coroutines.launch

class BroadcastChatScreen(private val channelId: String, private val canSendMessage: Boolean = true) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val messages by screenModel.broadcastMessages.collectAsState()
        val channelDetail by screenModel.broadcastChannelDetail.collectAsState()
        
        var titleText by remember { mutableStateOf("") }
        var descriptionText by remember { mutableStateOf("") }
        var uploadedImageUrl by remember { mutableStateOf<String?>(null) }
        
        var showDescriptionField by remember { mutableStateOf(false) }
        var showImagePickerBottomSheet by remember { mutableStateOf(false) }
        var launchGallery by remember { mutableStateOf(false) }
        var launchCamera by remember { mutableStateOf(false) }
        var pendingPickerAction by remember { mutableStateOf<String?>(null) }
        
        val listState = rememberLazyListState()
        val bringIntoViewRequester = remember { BringIntoViewRequester() }
        val scope = rememberCoroutineScope()

        LaunchedEffect(channelId) {
            screenModel.loadBroadcastMessages(channelId)
            screenModel.loadBroadcastChannelDetail(channelId)
        }

        LaunchedEffect(messages) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(0)
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

        LoadingScreenHandler(screenModel) {
            Scaffold(
                containerColor = Color(0xFFF7F7F8),
                topBar = {
                    BroadcastChatTopBar(
                        title = channelDetail?.name ?: "Broadcast",
                        imageUrl = channelDetail?.imageUrl,
                        onBackClick = { navigator.pop() },
                        onSettingsClick = if (canSendMessage) {
                            { navigator.push(BroadcastSettingsScreen(channelId)) }
                        } else {
                            null
                        },
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .imePadding()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        userScrollEnabled = true,
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        reverseLayout = true
                    ) {
                        items(messages) { message ->
                            ChatMessageRow(message)
                        }
                    }

                    // Message Input Area
                    if (canSendMessage) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding(),
                            color = White,
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                            ) {
                                // Image Preview
                                if (uploadedImageUrl != null) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .padding(bottom = 12.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    ) {
                                        androidx.compose.foundation.Image(
                                            painter = coil3.compose.rememberAsyncImagePainter(uploadedImageUrl),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(8.dp)
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(White.copy(alpha = 0.8f))
                                                .clickable { uploadedImageUrl = null },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Remove Image",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                // Description Field (Optional)
                                if (showDescriptionField) {
                                    CommonTextField(
                                        value = descriptionText,
                                        onValueChange = { descriptionText = it },
                                        placeholder = "Optional Description",
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                        singleLine = false,
                                        borderRadius = 12.dp,
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Add Image Button
                                    IconButton(onClick = { showImagePickerBottomSheet = true }) {
                                        Icon(
                                            Icons.Default.AddPhotoAlternate,
                                            contentDescription = "Add Image",
                                            tint = if (uploadedImageUrl != null) PrimaryColor else Gray
                                        )
                                    }

                                    // Toggle Description Button
                                    IconButton(onClick = { showDescriptionField = !showDescriptionField }) {
                                        Icon(
                                            imageVector = if (showDescriptionField) {
                                                Icons.Default.Close
                                            } else {
                                                Icons.Default.Subject
                                            },
                                            contentDescription = "Toggle Description",
                                            tint = if (showDescriptionField) PrimaryColor else Gray,
                                        )
                                    }
                                    val canSend = titleText.isNotBlank() || !uploadedImageUrl.isNullOrBlank()
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .bringIntoViewRequester(bringIntoViewRequester)
                                            .onFocusEvent {
                                                if (it.isFocused) {
                                                    scope.launch {
                                                        bringIntoViewRequester.bringIntoView()
                                                    }
                                                }
                                            },
                                    ) {
                                        CommonTextField(
                                            value = titleText,
                                            onValueChange = { titleText = it },
                                            placeholder = "Write Message Title...",
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = false,
                                            borderRadius = 24.dp,
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconButton(
                                        onClick = {
                                            if (!canSend) return@IconButton
                                            screenModel.sendBroadcastMessage(
                                                channelId = channelId,
                                                title = titleText,
                                                description = descriptionText.ifBlank { null },
                                                imageUrl = uploadedImageUrl,
                                            ) {
                                                titleText = ""
                                                descriptionText = ""
                                                uploadedImageUrl = null
                                                showDescriptionField = false
                                                screenModel.loadBroadcastMessages(channelId)
                                            }
                                        },
                                        enabled = canSend,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (canSend) PrimaryColor
                                                else PrimaryColor.copy(alpha = 0.4f),
                                            ),
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send",
                                            tint = White,
                                            modifier = Modifier.size(22.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
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
                    title = "Upload Broadcast Image",
                    subtitle = "Choose a photo to include in your broadcast card"
                )
            }

            if (launchGallery) {
                GalleryPickerLauncher(
                    onPhotosSelected = { photos ->
                        launchGallery = false
                        photos.firstOrNull()?.let { photo ->
                            screenModel.uploadImage(photo.loadBytes(), "broadcast_msg.jpg") { url ->
                                uploadedImageUrl = url
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
                            screenModel.uploadImage(photo.loadBytes(), "broadcast_msg.jpg") { url ->
                                uploadedImageUrl = url
                            }
                        },
                        onError = { launchCamera = false }
                    )
                )
            }
        }
    }

    @Composable
    private fun ChatMessageRow(message: BroadcastMessageDTO) {
        val authorName = message.createdBy?.fullName ?: message.senderName ?: "Admin"
        val title = message.title?.takeIf { it.isNotBlank() }
            ?: message.text?.takeIf { it.isNotBlank() }
            ?: message.message?.takeIf { it.isNotBlank() }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (!title.isNullOrBlank()) {
                        Text(
                            text = title,
                            style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (!message.imageUrl.isNullOrBlank()) {
                        androidx.compose.foundation.Image(
                            painter = coil3.compose.rememberAsyncImagePainter(message.imageUrl),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (!message.description.isNullOrBlank()) {
                        Text(
                            text = message.description!!,
                            style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = authorName,
                            style = AppTextTheme.medium.copy(
                                fontSize = 11.sp,
                                color = Color(0xFF10B981),
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = DateUtils.formatChatTime(message.createdAt),
                                style = AppTextTheme.regular.copy(fontSize = 11.sp, color = Gray),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BroadcastChatTopBar(
    title: String,
    imageUrl: String?,
    onBackClick: () -> Unit,
    onSettingsClick: (() -> Unit)?,
) {
    Surface(
        color = White,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Black,
                )
            }
            ImageAvatar(size = 36.dp, url = imageUrl)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (onSettingsClick != null) {
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Black,
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    }
}
