package `in`.gym.trak.studio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExpenseDTO(
    val id: String,
    val gymId: String,
    val bill_name: String? = "",
    val category: String,
    val date: String? = "",
    val trainer_id: String? = "",
    val payment_mode: String? = "cash",
    val amountCents: Long? = null,
    val amount: Long? = 0L,
    val gst: Double? = 0.0,
    val currency: String? = "INR"
)

@Serializable
data class ExpenseResponse(
    val total: Int,
    val limit: Int,
    val offset: Int,
    val items: List<ExpenseDTO>,
    val totalAmountCents: Long? = 0L,
    val percentageVsLastMonth: Double? = 0.0
)

@Serializable
data class CreateExpenseRequest(
    val gymId: String,
    val bill_name: String,
    val category: String,
    val date: String,
    val trainer_id: String? = null,
    val payment_mode: String = "cash",
    val amount: Double,
    val gst: Double? = 0.0
)

@Serializable
data class UpdateExpenseRequest(
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val categorySlug: String? = null,
    val occurredOn: String? = null,
    val date: String? = null,
    val trainer_id: String? = null,
    val amountCents: Long? = null,
    val amount: Double? = null,
    val payment_mode: String? = null,
    val gstPercent: Double? = null
)

@Serializable
data class DeleteExpenseResponse(
    val ok: Boolean
)

