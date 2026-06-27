package `in`.gym.trak.studio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeviceTokenRequest(
    val token: String,
)

@Serializable
data class DeviceTokenRegisterResponse(
    val ok: Boolean = true,
)

@Serializable
data class NotificationFeedResponse(
    val items: List<NotificationDTO> = emptyList(),
    val data: List<NotificationLegacyDataDTO> = emptyList(),
    val nextCursor: String? = null,
)

/** Legacy `data` array shape from the same feed response. */
@Serializable
data class NotificationLegacyDataDTO(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "",
    @SerialName("created_at") val createdAt: String = "",
    val read: Boolean = false,
)

@Serializable
data class NotificationDTO(
    val id: String = "",
    val gymId: String = "",
    val title: String = "",
    val body: String = "",
    val type: String = "INFO",
    val readAt: String? = null,
    val createdAt: String = "",
    val metadata: NotificationMetadataDTO? = null,
    val entity: NotificationEntityDTO? = null,
    val actor: NotificationActorDTO? = null,
    val member: NotificationMemberDTO? = null,
)

@Serializable
data class NotificationMemberDTO(
    val gymUserId: String = "",
    val name: String = "",
)

@Serializable
data class NotificationMetadataDTO(
    val deepLink: NotificationDeepLinkDTO? = null,
    val category: String? = null,
    val actorName: String? = null,
    val gymUserId: String? = null,
    val memberName: String? = null,
    val amountCents: Int? = null,
    val salaryPaymentId: String? = null,
    val expenseId: String? = null,
)

@Serializable
data class NotificationDeepLinkDTO(
    val screen: String = "",
    val params: NotificationDeepLinkParamsDTO? = null,
)

@Serializable
data class NotificationDeepLinkParamsDTO(
    val gymId: String? = null,
    val gymUserId: String? = null,
    val paymentId: String? = null,
    val memberSubscriptionId: String? = null,
    val expenseId: String? = null,
    val salaryPaymentId: String? = null,
)

@Serializable
data class NotificationEntityDTO(
    val type: String = "",
    val id: String = "",
)

@Serializable
data class NotificationActorDTO(
    val id: String = "",
    val name: String = "",
    val phone: String? = null,
)

@Serializable
data class NotificationMarkReadResponse(
    val ok: Boolean = true,
)
