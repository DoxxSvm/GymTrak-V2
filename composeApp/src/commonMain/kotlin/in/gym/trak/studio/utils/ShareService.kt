package `in`.gym.trak.studio.utils

import `in`.gym.trak.studio.base.Constants
import `in`.gym.trak.studio.data.model.MemberPaymentHistoryEntryDTO
import `in`.gym.trak.studio.data.model.PaymentItemDTO
import `in`.gym.trak.studio.data.model.resolvedPaymentMethodLabel
import `in`.gym.trak.studio.data.model.resolvedPlanTitle
import `in`.gym.trak.studio.data.model.resolvedReceivedBy
import `in`.gym.trak.studio.data.model.ProductDetailDTO
import `in`.gym.trak.studio.data.model.WorkoutDetailResponse

/**
 * Cross-platform text sharing entry point. Use [ShareService.share] for generic content or the
 * typed helpers below for existing app share flows.
 */
data class ShareContent(
    val text: String,
    val subject: String? = null,
    val url: String? = null,
)

enum class ShareChannel {
    System,
    WhatsApp,
}

object ShareService {

    fun buildMessage(content: ShareContent): String {
        val body = content.text.trim()
        val url = content.url?.trim().orEmpty()
        return when {
            body.isNotEmpty() && url.isNotEmpty() -> "$body\n\n$url"
            url.isNotEmpty() -> url
            else -> body
        }.trim()
    }

    fun share(content: ShareContent, channel: ShareChannel = ShareChannel.System) {
        val message = buildMessage(content)
        if (message.isBlank()) return
        platformShare(
            SharePayload(
                text = message,
                subject = content.subject,
                channel = channel,
            )
        )
    }

    fun shareText(text: String, subject: String? = null, channel: ShareChannel = ShareChannel.System) {
        share(ShareContent(text = text, subject = subject), channel)
    }

    fun sharePaymentItem(item: PaymentItemDTO) {
        val memberName = item.memberUser?.fullName?.takeIf { it.isNotBlank() } ?: "Member"
        val plan = item.resolvedPlanTitle()
        val mode = item.resolvedPaymentMethodLabel()
        val date = item.completedAt?.substringBefore("T")?.takeIf { it.isNotBlank() }
            ?: item.createdAt?.substringBefore("T")
            ?: "N/A"
        val amount = "${Constants.RUPEE} ${item.amountCents}"
        val receivedBy = item.resolvedReceivedBy().takeIf { it != "—" }
        val reference = item.reference?.takeIf { it.isNotBlank() }
        val invoice = item.invoiceId?.takeIf { it.isNotBlank() }

        val lines = buildList {
            add("Payment Receipt")
            add("Member: $memberName")
            add("Plan: $plan")
            add("Amount: $amount")
            add("Mode: $mode")
            add("Date: $date")
            receivedBy?.let { add("Received by: $it") }
            reference?.let { add("Reference: $it") }
            invoice?.let { add("Invoice: $it") }
        }
        share(
            ShareContent(
                text = lines.joinToString("\n"),
                subject = "Payment - $memberName",
            )
        )
    }

    fun shareMemberPaymentEntry(
        entry: MemberPaymentHistoryEntryDTO,
        memberName: String? = null,
    ) {
        val plan = entry.gymPlanName?.takeIf { it.isNotBlank() } ?: "Payment"
        val mode = entry.paymentMethod?.takeIf { it.isNotBlank() } ?: "—"
        val date = entry.date?.let { DateUtils.formatBirthDateForDisplay(it) }?.takeIf { it.isNotBlank() }
            ?: entry.date.orEmpty().ifBlank { "—" }
        val receivedBy = entry.receivedBy?.takeIf { it.isNotBlank() }
        val amount = "${Constants.RUPEE} ${entry.amount ?: 0}"

        val lines = buildList {
            add("Payment Receipt")
            memberName?.takeIf { it.isNotBlank() }?.let { add("Member: $it") }
            add("Plan: $plan")
            add("Amount: $amount")
            add("Mode: $mode")
            add("Date: $date")
            receivedBy?.let { add("Received by: $it") }
        }
        share(
            ShareContent(
                text = lines.joinToString("\n"),
                subject = "Payment - $plan",
            )
        )
    }

    fun shareProduct(detail: ProductDetailDTO) {
        val price = detail.discountPrice.takeIf { it > 0.0 } ?: detail.price
        val lines = buildList {
            add(detail.name.ifBlank { "Product" })
            if (detail.category.isNotBlank()) add("Category: ${detail.category}")
            add("Price: ${Constants.RUPEE} ${price.toInt()}")
            detail.description?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
        val imageUrl = detail.images.firstOrNull { it.isNotBlank() }
        share(
            ShareContent(
                text = lines.joinToString("\n"),
                subject = detail.name.ifBlank { "Product" },
                url = imageUrl,
            )
        )
    }

    fun shareExercise(exerciseName: String, category: String? = null, summary: String? = null) {
        val lines = buildList {
            add(exerciseName.ifBlank { "Exercise" })
            category?.takeIf { it.isNotBlank() }?.let { add("Category: $it") }
            summary?.takeIf { it.isNotBlank() }?.let { add(it) }
        }
        share(
            ShareContent(
                text = lines.joinToString("\n"),
                subject = exerciseName.ifBlank { "Exercise" },
            )
        )
    }

    fun shareWorkoutImage(
        pngBytes: ByteArray,
        channel: ShareChannel = ShareChannel.System,
        subject: String = "Workout",
    ) {
        if (pngBytes.isEmpty()) return
        platformShareImage(
            ShareImagePayload(
                pngBytes = pngBytes,
                subject = subject,
                channel = channel,
            ),
        )
    }
}

data class SharePayload(
    val text: String,
    val subject: String? = null,
    val channel: ShareChannel = ShareChannel.System,
)

internal expect fun platformShare(payload: SharePayload)

data class ShareImagePayload(
    val pngBytes: ByteArray,
    val fileName: String = "workout_share.png",
    val subject: String? = null,
    val channel: ShareChannel = ShareChannel.System,
)

internal expect fun platformShareImage(payload: ShareImagePayload)
