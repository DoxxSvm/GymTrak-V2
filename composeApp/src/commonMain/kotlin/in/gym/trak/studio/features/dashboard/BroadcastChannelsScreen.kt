package `in`.gym.trak.studio.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.gym_boy
import gym.composeapp.generated.resources.ic_trainer_edit
import gym.composeapp.generated.resources.img_no_plan
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.SearchBar
import `in`.gym.trak.studio.components.AppEmptyStateView
import `in`.gym.trak.studio.components.ConfirmationDialog
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.data.model.BroadcastChannelDTO
import `in`.gym.trak.studio.data.model.BroadcastChannelDetailDTO
import `in`.gym.trak.studio.data.model.BroadcastMemberDTO
import `in`.gym.trak.studio.data.model.BroadcastMessageDTO
import `in`.gym.trak.studio.data.model.CreateBroadcastChannelResponse
import `in`.gym.trak.studio.data.model.displayPreview
import `in`.gym.trak.studio.utils.DateUtils
import `in`.gym.trak.studio.features.member.ImagePickerBottomSheet
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import io.github.ismoy.imagepickerkmp.domain.config.ImagePickerConfig
import io.github.ismoy.imagepickerkmp.domain.extensions.loadBytes
import io.github.ismoy.imagepickerkmp.presentation.ui.components.GalleryPickerLauncher
import io.github.ismoy.imagepickerkmp.presentation.ui.components.ImagePickerLauncher
import org.jetbrains.compose.resources.painterResource

class BroadcastChannelsScreen(private val isReadOnly: Boolean = false) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        var searchQuery by remember { mutableStateOf("") }
        val channels by screenModel.broadcastChannels.collectAsState()
        val isLoading by screenModel.broadcastChannelsLoading.collectAsState()

        LaunchedEffect(searchQuery) {
            screenModel.loadBroadcastChannels(search = searchQuery.takeIf { it.isNotBlank() })
        }

        LoadingScreenHandler(screenModel) {
            Scaffold(
                containerColor = Color(0xFFF7F7F8),
                topBar = {
                    GymAppBar(
                        title = "My Channels",
                        onBackClick = { navigator.pop() }
                    )
                },
                floatingActionButton = {
                    if (!isReadOnly) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(PrimaryColor)
                                .clickable { navigator.push(CreateBroadcastChannelScreen()) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Create Channel",
                                tint = White,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                ) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search..."
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isLoading && channels.isEmpty()) {
                            // Handled by LoadingScreenHandler or we can show a specific loading UI here
                        } else if (channels.isEmpty()) {
                            item {
                                AppEmptyStateView(
                                    image = Res.drawable.img_no_plan, // Using an existing image, user can change if they want
                                    title = "No Channels Found",
                                    subtitle = "Create your first broadcast channel to start reaching out to your members.",
                                    modifier = Modifier.padding(top = 40.dp),
                                    useCardContainer = false
                                )
                            }
                        } else {
                            items(channels) { channel ->
                                BroadcastChannelRow(channel, onClick = {
                                    navigator.push(BroadcastChatScreen(channel.id, canSendMessage = !isReadOnly))
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}

class CreateBroadcastChannelScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val isCreating by screenModel.broadcastChannelCreating.collectAsState()
        var channelName by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var imageUrl by remember { mutableStateOf<String?>(null) }
        var validationError by remember { mutableStateOf<String?>(null) }
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

        LoadingScreenHandler(screenModel = screenModel) {
        Scaffold(
            containerColor = Color(0xFFF7F7F8),
            topBar = {
                GymAppBar(
                    title = "Create Broadcast Channel",
                    onBackClick = { navigator.pop() }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(126.dp)
                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { showImagePickerBottomSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (!imageUrl.isNullOrBlank()) {
                        androidx.compose.foundation.Image(
                            painter = coil3.compose.rememberAsyncImagePainter(imageUrl),
                            contentDescription = "Channel Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    tint = Black
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Upload Image",
                                style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    "Channel Name",
                    style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black)
                )
                Spacer(modifier = Modifier.height(8.dp))
                CommonTextField(
                    value = channelName,
                    onValueChange = { channelName = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Enter Channel Name",
                    singleLine = true,
                    borderRadius = 24.dp
                )

                Spacer(modifier = Modifier.height(16.dp))
                Text("Description", style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black))
                Spacer(modifier = Modifier.height(8.dp))
                CommonTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp),
                    placeholder = "Enter Description here.....",
                    borderRadius = 14.dp,
                    isMultiline = true,
                    singleLine = false
                )

                Spacer(modifier = Modifier.weight(1f))
                CommonButton(
                    text = "Continue",
                    enabled = !isCreating,
                    onClick = {
                        val trimmedName = channelName.trim()
                        val trimmedDescription = description.trim()
                        validationError = when {
                            trimmedName.isBlank() -> "Channel name is required."
                            trimmedName.length < 3 -> "Channel name must be at least 3 characters."
                            trimmedName.length > 60 -> "Channel name must be at most 60 characters."
                            trimmedDescription.length > 300 -> "Description must be at most 300 characters."
                            else -> null
                        }
                        if (validationError != null) return@CommonButton

                        screenModel.createBroadcastChannel(
                            name = trimmedName,
                            description = trimmedDescription.ifBlank { null },
                            imageUrl = imageUrl
                        ) { response ->
                            val channelId = response.channelId ?: response.id
                            if (channelId != null) {
                                navigator.push(AddBroadcastMemberScreen(channelId))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    color = PrimaryColor
                )
                if (!validationError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = validationError!!,
                        style = AppTextTheme.medium.copy(
                            fontSize = 12.sp,
                            color = Color(0xFFE53935)
                        )
                    )
                }
                Spacer(modifier = Modifier.height(22.dp))
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
                title = "Upload Channel Photo",
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
                            "broadcast_channel.jpg"
                        ) { uploadedUrl ->
                            if (uploadedUrl != null) imageUrl = uploadedUrl
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
                            "broadcast_channel.jpg"
                        ) { uploadedUrl ->
                            if (uploadedUrl != null) imageUrl = uploadedUrl
                        }
                    },
                    onError = { launchCamera = false }
                )
            )
        }
        }
    }
}

class AddBroadcastMemberScreen(
    private val channelId: String,
    private val closeWithPopOnSuccess: Boolean = false
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        var searchQuery by remember { mutableStateOf("") }
        val members by screenModel.broadcastMembers.collectAsState()
        val isLoading by screenModel.broadcastMembersLoading.collectAsState()
        val selectedUserIds = remember { mutableStateListOf<String>() }

        LaunchedEffect(searchQuery) {
            screenModel.loadAllBroadcastMembers(search = searchQuery.takeIf { it.isNotBlank() })
        }

        LoadingScreenHandler(screenModel) {
            Scaffold(
                containerColor = Color(0xFFF7F7F8),
                topBar =
                    {
                        GymAppBar(
                            title = "Add Member",
                            onBackClick = {
                                navigator.pop()
                            }
                        )
                    }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        "Selected Members",
                        style = AppTextTheme.medium.copy(fontSize = 16.sp, color = Black)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (selectedUserIds.isEmpty()) {
                        Text(
                            "No members selected",
                            style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(selectedUserIds.toList()) { userId ->
                                val member = members.find { it.id == userId }
                                Box {
                                    ImageAvatar(size = 52.dp, url = member?.user?.avatarUrl)
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(White)
                                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                                            .clickable { selectedUserIds.remove(userId) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = null,
                                            tint = Black,
                                            modifier = Modifier.size(10.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search By Name Or Phone Number"
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        if (isLoading && members.isEmpty()) {
                            // Handled by LoadingScreenHandler
                        } else if (members.isEmpty()) {
                            item {
                                AppEmptyStateView(
                                    image = Res.drawable.img_no_plan,
                                    title = "No Members Found",
                                    subtitle = "We couldn't find any members matching your search.",
                                    useCardContainer = false,
                                    modifier = Modifier.padding(top = 20.dp)
                                )
                            }
                        } else {
                            items(members) { member ->
                                val isSelected = selectedUserIds.contains(member.id)
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (isSelected) selectedUserIds.remove(member.id)
                                            else selectedUserIds.add(member.id)
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(
                                            horizontal = 12.dp,
                                            vertical = 10.dp
                                        ),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ImageAvatar(size = 50.dp, url = member.user.avatarUrl)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                member.user.fullName ?: "Unnamed",
                                                style = AppTextTheme.bold.copy(
                                                    fontSize = 18.sp,
                                                    color = Black
                                                )
                                            )
                                            Text(
                                                member.user.phone ?: "",
                                                style = AppTextTheme.regular.copy(
                                                    fontSize = 14.sp,
                                                    color = Gray
                                                )
                                            )
                                        }
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF10B981)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    CommonButton(
                        text = "Add Selected Members",
                        enabled = selectedUserIds.isNotEmpty(),
                        onClick = {
                            screenModel.addMembersToChannel(channelId, selectedUserIds.toList()) {
                                if (closeWithPopOnSuccess) {
                                    navigator.pop()
                                } else {
                                    navigator.replaceAll(BroadcastChannelsScreen())
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = PrimaryColor
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                }
            }
        }
    }
}

class EditBroadcastChannelScreen(
    private val channelId: String,
    private val initialName: String? = null,
    private val initialDescription: String? = null,
    private val initialImageUrl: String? = null
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        var channelName by remember { mutableStateOf(initialName.orEmpty()) }
        var description by remember { mutableStateOf(initialDescription.orEmpty()) }
        var imageUrl by remember { mutableStateOf(initialImageUrl) }
        var validationError by remember { mutableStateOf<String?>(null) }
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

        LoadingScreenHandler(screenModel = screenModel) {
            Scaffold(
                containerColor = Color(0xFFF7F7F8),
                topBar = {
                    GymAppBar(
                        title = "Edit Broadcast Profile",
                        onBackClick = { navigator.pop() }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(126.dp)
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { showImagePickerBottomSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!imageUrl.isNullOrBlank()) {
                            androidx.compose.foundation.Image(
                                painter = coil3.compose.rememberAsyncImagePainter(imageUrl),
                                contentDescription = "Channel Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF1F5F9)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.AddAPhoto,
                                        contentDescription = null,
                                        tint = Black
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Upload Image",
                                    style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        "Channel Name",
                        style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    CommonTextField(
                        value = channelName,
                        onValueChange = { channelName = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Enter Channel Name",
                        singleLine = true,
                        borderRadius = 24.dp
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Description", style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black))
                    Spacer(modifier = Modifier.height(8.dp))
                    CommonTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(104.dp),
                        placeholder = "Enter Description here.....",
                        borderRadius = 14.dp,
                        isMultiline = true,
                        singleLine = false
                    )

                    Spacer(modifier = Modifier.weight(1f))
                    CommonButton(
                        text = "Save Changes",
                        onClick = {
                            val trimmedName = channelName.trim()
                            val trimmedDescription = description.trim()
                            validationError = when {
                                trimmedName.isBlank() && trimmedDescription.isBlank() && imageUrl.isNullOrBlank() ->
                                    "Please update at least one field."
                                trimmedName.isNotBlank() && trimmedName.length < 3 ->
                                    "Channel name must be at least 3 characters."
                                trimmedName.length > 60 ->
                                    "Channel name must be at most 60 characters."
                                trimmedDescription.length > 300 ->
                                    "Description must be at most 300 characters."
                                else -> null
                            }
                            if (validationError != null) return@CommonButton

                            screenModel.updateBroadcastChannel(
                                channelId = channelId,
                                name = trimmedName.ifBlank { null },
                                description = trimmedDescription.ifBlank { null },
                                imageUrl = imageUrl
                            ) {
                                navigator.pop()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        color = PrimaryColor
                    )
                    if (!validationError.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = validationError!!,
                            style = AppTextTheme.medium.copy(
                                fontSize = 12.sp,
                                color = Color(0xFFE53935)
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(22.dp))
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
                    title = "Upload Channel Photo",
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
                                "broadcast_channel.jpg"
                            ) { uploadedUrl ->
                                if (uploadedUrl != null) imageUrl = uploadedUrl
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
                                "broadcast_channel.jpg"
                            ) { uploadedUrl ->
                                if (uploadedUrl != null) imageUrl = uploadedUrl
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
fun BroadcastChannelRow(channel: BroadcastChannelDTO, onClick: () -> Unit) {
    val name = channel.name?.takeIf { it.isNotBlank() } ?: "Unnamed Channel"
    val lastMessage = channel.resolvedLastMessage
    val previewText = lastMessage?.displayPreview()?.takeIf { it.isNotBlank() } ?: "No messages yet."
    val lastSeenIso = lastMessage?.createdAt?.takeIf { it.isNotBlank() }
        ?: channel.updatedAt?.takeIf { it.isNotBlank() }
    val lastSeenLabel = DateUtils.formatConversationListTime(lastSeenIso)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ImageAvatar(size = 52.dp, url = channel.imageUrl)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = AppTextTheme.bold.copy(fontSize = 18.sp, color = Black))
            Text(
                previewText,
                style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Color(0xFF1F2937)),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (lastSeenLabel.isNotBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                lastSeenLabel,
                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Color(0xFF374151)),
            )
        }
    }
}



