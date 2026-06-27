package `in`.gym.trak.studio.data.model

import kotlinx.serialization.Serializable

object WhatsAppAutomationTemplateIds {
    const val ONBOARDING_WELCOME = "onboarding_welcome"
    const val EXPIRE_7_DAYS = "expire_7_days"
    const val EXPIRE_3_DAYS = "expire_3_days"
    const val EXPIRED_REMINDER = "expired_reminder"
    const val PAYMENT_RECEIVED = "payment_received"
}

@Serializable
data class WhatsAppAutomationResponse(
    val screenTitle: String = "Message Templates",
    val screenDescription: String? = null,
    val templates: List<WhatsAppAutomationTemplateDTO> = emptyList(),
)

@Serializable
data class WhatsAppAutomationTemplateDTO(
    val id: String,
    val title: String = "",
    val description: String? = null,
    val enabled: Boolean = false,
    val autoTrigger: String? = null,
    val message: String? = null,
    val defaultMessage: String? = null,
) {
    val supportsCustomMessage: Boolean get() = id == WhatsAppAutomationTemplateIds.ONBOARDING_WELCOME
}

@Serializable
data class UpdateWhatsAppAutomationRequest(
    val templates: List<UpdateWhatsAppAutomationTemplateItem>,
)

@Serializable
data class UpdateWhatsAppAutomationTemplateItem(
    val id: String,
    val enabled: Boolean,
    val message: String? = null,
)

data class EditableWhatsAppAutomationTemplate(
    val id: String,
    val title: String,
    val description: String,
    val enabled: Boolean,
    val message: String,
    val defaultMessage: String?,
    val supportsCustomMessage: Boolean,
)

fun WhatsAppAutomationTemplateDTO.toEditable(): EditableWhatsAppAutomationTemplate =
    EditableWhatsAppAutomationTemplate(
        id = id,
        title = title,
        description = description.orEmpty(),
        enabled = enabled,
        message = message?.takeIf { it.isNotBlank() } ?: defaultMessage.orEmpty(),
        defaultMessage = defaultMessage,
        supportsCustomMessage = supportsCustomMessage,
    )

fun EditableWhatsAppAutomationTemplate.toUpdateItem(): UpdateWhatsAppAutomationTemplateItem =
    UpdateWhatsAppAutomationTemplateItem(
        id = id,
        enabled = enabled,
        message = message.trim().takeIf { it.isNotBlank() && supportsCustomMessage },
    )
