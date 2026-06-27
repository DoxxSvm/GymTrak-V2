package `in`.gym.trak.studio.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.JsonNames

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class BroadcastMemberDTO(
    @JsonNames("id", "membershipId", "gymUserId")
    val id: String = "",
    val user: MemberUserDTO
)

@Serializable
data class AddBroadcastMembersRequest(
    val gymUserIds: List<String>
)

@Serializable
data class BroadcastMembersResponse(
    val page: Int = 0,
    val limit: Int = 0,
    val total: Int = 0,
    val data: List<BroadcastMemberDTO> = emptyList()
)

@Serializable
data class BroadcastChannelsResponse(
    val channels: List<BroadcastChannelDTO> = emptyList(),
    val items: List<BroadcastChannelDTO> = emptyList(),
    val data: List<BroadcastChannelDTO> = emptyList()
)

@Serializable
data class BroadcastChannelDTO(
    val id: String,
    val name: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val memberCount: Int = 0,
    val messageCount: Int = 0,
    val lastMessage: BroadcastMessageDTO? = null,
    val last_message: BroadcastMessageDTO? = null,
) {
    val resolvedLastMessage: BroadcastMessageDTO? get() = lastMessage ?: last_message
}

@Serializable
data class BroadcastMessageDTO(
    val id: String? = null,
    val channelId: String? = null,
    val title: String? = null,
    val description: String? = null,
    val preview: String? = null,
    val imageUrl: String? = null,
    val createdByUserId: String? = null,
    val createdAt: String? = null,
    val createdBy: BroadcastAuthorDTO? = null,
    val text: String? = null, // Compatibility
    val message: String? = null, // Compatibility
    val senderName: String? = null, // Compatibility
    val senderRole: String? = null, // Compatibility
    val senderAvatarUrl: String? = null, // Compatibility
    val isPinned: Boolean = false,
)

/** Preview line for channel list rows. */
fun BroadcastMessageDTO.displayPreview(): String =
    preview?.takeIf { it.isNotBlank() }
        ?: title?.takeIf { it.isNotBlank() }
        ?: description?.takeIf { it.isNotBlank() }
        ?: text?.takeIf { it.isNotBlank() }
        ?: message?.takeIf { it.isNotBlank() }
        ?: ""

@Serializable
data class BroadcastAuthorDTO(
    val id: String,
    val fullName: String? = null,
    val avatarUrl: String? = null,
    val phone: String? = null
)

@Serializable
data class BroadcastMessagesResponse(
    val page: Int = 0,
    val limit: Int = 0,
    val total: Int = 0,
    val data: List<BroadcastMessageDTO> = emptyList()
)

@Serializable
data class BroadcastChannelDetailDTO(
    val id: String,
    val name: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val memberCount: Int = 0,
    val createdAt: String? = null,
    val members: List<BroadcastMemberDTO> = emptyList(),
    val isAdmin: Boolean = false
)

@Serializable
data class CreateBroadcastChannelRequest(
    val gymId: String,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val avatarUrl: String? = null
)

@Serializable
data class UpdateBroadcastChannelRequest(
    val name: String? = null,
    val description: String? = null,
    val imageUrl: String? = null
)

@Serializable
data class CreateBroadcastChannelResponse(
    val channelId: String? = null,
    val id: String? = null,
    val message: String? = null
)

@Serializable
data class CreateBroadcastMessageRequest(
    val title: String,
    val description: String? = null,
    val imageUrl: String? = null
)
