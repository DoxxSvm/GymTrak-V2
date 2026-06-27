package `in`.gym.trak.studio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** POST `plans/member-plans` — assign gym plan to member (creates subscription). */
@Serializable
data class AssignMemberPlanRequest(
    val member_id: String,
    @SerialName("gym_plan_id")
    val gym_plan_id: String,
    @SerialName("date")
    val start_date: String,
    val discount: Int = 0,
)

@Serializable
data class AssignMemberPlanResponse(
    val success: Boolean = false,
    val subscription_id: String? = null,
    val message: String? = null,
)

/**
 * Create plan (compat) — matches API body:
 * GYM_MEMBERSHIP / PT_PLAN / BATCH_PLAN, optional [trainerId], optional [batchDetails].
 */
@Serializable
data class CreatePlanCompatRequest(
    val gymId: String,
    val planType: String,
    val planName: String,
    val durationDays: Int,
    val price: Double,
    val trainerId: String? = null,
    @SerialName("batch_details") val batchDetails: BatchDetailsRequest? = null
)

@Serializable
data class BatchDetailsRequest(
    val working_days: List<Int>,
    val gender: String,
    val shifts: List<BatchShiftTime>
)

@Serializable
data class BatchShiftTime(
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String
)

@Serializable
data class CreatePlanResponse(
    val id: String,
    val gymId: String,
    val type: String,
    val name: String,
    val durationDays: Int,
    val priceCents: Int,
    val currency: String,
    val isActive: Boolean,
    val metadata: kotlinx.serialization.json.JsonObject? = null,
    val trainer: PlanTrainer? = null,
    val batch: PlanBatch? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class PlanTrainer(
    val gymUserId: String,
    val name: String,
    val phone: String,
    val userId: String
)

@Serializable
data class PlanBatch(
    val daysOfWeek: List<Int>,
    val gender: String,
    val shifts: List<PlanShift>
)

@Serializable
data class PlanShift(
    val id: String,
    val startTime: String,
    val endTime: String,
    val sortOrder: Int
)

@Serializable
data class PlanListResponse(
    val items: List<PlanDTO>,
    val total: Int = 0,
    val subscriptionCount: Int = 0,
)

@Serializable
data class PlanDTO(
    val id: String,
    val gymId: String,
    val type: String,
    val name: String,
    val durationDays: Int,
    val priceCents: Int,
    val currency: String,
    val isActive: Boolean,
    val metadata: kotlinx.serialization.json.JsonObject? = null,
    val clientsCount: Int = 0,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class PlanEnrolledResponse(
    val plan: PlanShortInfo,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val items: List<EnrolledSubscription>
)

@Serializable
data class PlanShortInfo(
    val id: String,
    val name: String,
    val type: String
)

@Serializable
data class EnrolledSubscription(
    val id: String,
    val status: String,
    val startsAt: String,
    val endsAt: String,
    val priceCents: Int,
    val paidCents: Int,
    val currency: String,
    val member: EnrolledMember
)

@Serializable
data class EnrolledMember(
    val gymUserId: String,
    val userId: String,
    val name: String,
    val phone: String,
    val email: String? = null
)
