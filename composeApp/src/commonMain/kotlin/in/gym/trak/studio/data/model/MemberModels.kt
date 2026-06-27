package `in`.gym.trak.studio.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull
import org.jetbrains.compose.resources.DrawableResource

/** Accepts JSON number or string (e.g. height/weight from member detail APIs). */
private object NullableDoubleFlexibleSerializer : KSerializer<Double?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("NullableDoubleFlexible", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Double? {
        val jsonDecoder = decoder as? JsonDecoder ?: return null
        val element = jsonDecoder.decodeJsonElement()
        if (element !is JsonPrimitive) return null
        if (!element.isString) {
            return element.doubleOrNull
                ?: element.longOrNull?.toDouble()
                ?: element.intOrNull?.toDouble()
        }
        val s = element.content.trim()
        if (s.isEmpty()) return null
        return s.toDoubleOrNull()
    }

    override fun serialize(encoder: Encoder, value: Double?) {
        if (value == null) encoder.encodeNull()
        else encoder.encodeDouble(value)
    }
}

/** Accepts JSON number or string for member profile `age` (APIs vary). */
private object NullableAgeStringSerializer : KSerializer<String?> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("NullableAgeStringFlexible", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): String? {
        val jsonDecoder = decoder as? JsonDecoder ?: return null
        val element = jsonDecoder.decodeJsonElement()
        if (element !is JsonPrimitive) return null
        if (element.isString) {
            val s = element.content.trim()
            return s.ifEmpty { null }
        }
        element.intOrNull?.let { return it.toString() }
        element.longOrNull?.let { return it.toString() }
        element.doubleOrNull?.let { return it.toInt().toString() }
        return null
    }

    override fun serialize(encoder: Encoder, value: String?) {
        if (value == null) encoder.encodeNull()
        else encoder.encodeString(value)
    }
}

// ─── Workout Flow Models ────────────────────────────────────────────────────────────

data class WorkoutSet(
    val setNumber: Int,
    val prev: String = "—",
    var kg: String = "60",
    var reps: String = "10",
    var isDone: Boolean = false,
    var serverSetId: String? = null,
)

enum class ExerciseType { REPS, TIME }

data class ActiveExercise(
    val id: String = "",
    val name: String,
    val icon: DrawableResource,
    val assetUrl: String? = null,
    val restTimer: String = "Off",   // "Off", "5s", "20s", etc.
    val type: ExerciseType = ExerciseType.REPS,
    var workoutExerciseId: String? = null,
    val sets: MutableList<WorkoutSet> = mutableListOf()
)

@Serializable
data class MemberListResponse(
    val members: List<MemberDTO> = emptyList(),
    val items: List<MemberDTO> = emptyList(), // Fallback if API uses items instead of members
    val pagination: PaginationDTO? = null,
    val stats: MemberStats? = null,
    val total: Int = 0,
    val limit: Int = 20,
    val offset: Int = 0
)

@Serializable
data class MemberDTO(
    val id: String? = null,
    val user_id: String? = null,
    val name: String = "",
    val first_name: String = "",
    val last_name: String = "",
    val phone: String = "",
    val country_code: String = "+91",
    val status: String = "active", // "active", "inactive", "expired"
    val membership_expiry: String? = null,
    val profile_image: String? = null,
    val daysLeft: String? = null // Computed or API returned
)

@Serializable
data class PaginationDTO(
    val page: Int,
    val total_pages: Int,
    val total_records: Int
)

@Serializable
data class MemberStats(
    val active: Int,
    val inactive: Int,
    val expired: Int
)

@Serializable
data class AddMemberRequest(
    val gymId: String? = null,
    val phone: String,
    val fullName: String,
    val first_name: String,
    val last_name: String,
    val email: String? = null,
    val isLead: Boolean = false,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val notes: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val dateOfBirth: String? = null,
    val dob: String? = null,
    val gender: String? = null,
    val address: String? = null,
    val aadhaar_number: String? = null,
    val date_of_joining: String? = null,
    val membershipEndsAt: String? = null,
    val avatarUrl: String? = null,
    val age: String? = null,
    val initialSubscription: InitialSubscriptionDTO? = null
)

@Serializable
data class InitialSubscriptionDTO(
    val planId: String? = null,
    val gymPlanId: String? = null,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val priceCents: Int = 0,
    val currency: String = "INR"
)

@Serializable
data class AddMemberResponse(
    val memberId: String? = null,
    val id: String? = null,
    val message: String? = null
)

@Serializable
data class MemberPaymentSummaryDTO(
    val paidThisYear: Int = 0,
    val outstandingAmount: Int = 0,
    @SerialName("total_price_cents")
    val total_price_cents: Int = 0,
    @SerialName("total_paid_cents")
    val total_paid_cents: Int = 0,
)

/**
 * Embedded payment rows on member detail. Fields are optional so partially-shaped API items still decode.
 */
@Serializable
data class MemberPaymentHistoryEntryDTO(
    val gymPlanName: String? = null,
    val paymentMethod: String? = null,
    val date: String? = null,
    val receivedBy: String? = null,
    val amount: Int? = null,
)

@Serializable
data class MemberDetailResponse(
    val gymUserId: String = "",
    val gymId: String = "",
    val lifecycleStatus: String = "",
    val isLead: Boolean = false,
    val isActive: Boolean = false,
    val membershipEndsAt: String? = null,
    val joinedAt: String? = null,
    val notes: String? = null,
    val emergencyContactName: String? = null,
    val emergencyContactPhone: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val age: Int? = null,
    val summary: MemberSummaryDTO = MemberSummaryDTO(),
    val subscription: MemberSubscriptionDTO = MemberSubscriptionDTO(),
    val user: MemberUserDTO = MemberUserDTO(),
    val contact: MemberContactDTO = MemberContactDTO(),
    val tabs: MemberTabsDTO = MemberTabsDTO(),
    val attendance: MemberEmbeddedAttendanceDTO? = null,
    val paymentSummary: MemberPaymentSummaryDTO? = null,
    val paymentHistory: List<MemberPaymentHistoryEntryDTO> = emptyList(),
)

@Serializable
data class MemberProfileWellnessDTO(
    val bmi: Double? = null,
    val bmiCategory: String? = null,
    @Serializable(with = NullableDoubleFlexibleSerializer::class)
    val maintenanceCalories: Double? = null,
)

@Serializable
data class MemberProfileGymDTO(
    val id: String = "",
    val name: String = "",
    val slug: String? = null,
    val address: String? = null,
    val timezone: String? = null,
    val gstin: String? = null,
    @SerialName("logo_url")
    val logo_url: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class MemberProfileDetailResponse(
    val id: String,
    val name: String,
    val phone: String,
    val gender: String? = null,
    val dob: String? = null,
    @Serializable(with = NullableAgeStringSerializer::class)
    val age: String? = null,
    @SerialName("join_date")
    val join_date: String? = null,
    val status: String? = null,
    @SerialName("profile_image")
    val profile_image: String? = null,
    @Serializable(with = NullableDoubleFlexibleSerializer::class)
    val heightCm: Double? = null,
    @Serializable(with = NullableDoubleFlexibleSerializer::class)
    val weightKg: Double? = null,
    val activityLevel: String? = null,
    val fitnessGoal: String? = null,
    val wellness: MemberProfileWellnessDTO? = null,
    val gym: MemberProfileGymDTO? = null,
    val stats: SubscriptionStatsDTO? = null,
    val current_subscription: CurrentSubscriptionDTO? = null,
    val current_subscriptions: List<CurrentSubscriptionDTO> = emptyList(),
    val upcoming_subscriptions: List<CurrentSubscriptionDTO> = emptyList(),
    val expired_subscriptions: List<CurrentSubscriptionDTO> = emptyList(),
    @SerialName("past_subscriptions")
    val past_subscriptions: List<CurrentSubscriptionDTO> = emptyList(),
    val freeze_subscriptions: List<CurrentSubscriptionDTO> = emptyList(),
)

/** Active plans from profile API: prefers [current_subscriptions], falls back to [current_subscription]. */
fun MemberProfileDetailResponse.activeSubscriptions(): List<CurrentSubscriptionDTO> {
    if (current_subscriptions.isNotEmpty()) return current_subscriptions
    return listOfNotNull(current_subscription)
}

fun MemberProfileDetailResponse.primarySubscription(): CurrentSubscriptionDTO? =
    current_subscription ?: current_subscriptions.firstOrNull()

@Serializable
data class MemberProfileUpdateRequest(
    val fullName: String? = null,
    @SerialName("first_name") val first_name: String? = null,
    @SerialName("last_name") val last_name: String? = null,
    val email: String? = null,
    val age: String? = null,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val activityLevel: String? = null,
    val fitnessGoal: String? = null,
    val gender: String? = null,
    val avatarUrl: String? = null,
    @SerialName("profile_image") val profile_image: String? = null,
    val isLead: Boolean? = null,
    val isActive: Boolean? = null,
    val notes: String? = null,
    val address: String? = null,
    @SerialName("aadhaar_number") val aadhaar_number: String? = null,
    val emergencyContactName: String? = null,
    @SerialName("emergency_name") val emergency_name: String? = null,
    val emergencyContactPhone: String? = null,
    @SerialName("emergency_contact_phone") val emergency_contact_phone: String? = null,
    val dateOfBirth: String? = null,
    val dob: String? = null,
    val membershipEndsAt: String? = null,
    @SerialName("date_of_joining") val date_of_joining: String? = null,
    val phone: String? = null,
    val initialSubscription: InitialSubscriptionDTO? = null,
    val maintenanceCalories: Double? = null,
)

/**
 * Same field coverage as [AddMemberRequest] for `PATCH members/{id}/profile` (gymId is query, not body).
 */
fun AddMemberRequest.toMemberProfileUpdateRequest(): MemberProfileUpdateRequest {
    val ec = emergencyContactPhone
    val en = emergencyContactName
    return MemberProfileUpdateRequest(
        fullName = fullName,
        first_name = first_name,
        last_name = last_name,
        email = email,
        age = age,
        heightCm = heightCm,
        weightKg = weightKg,
        gender = gender,
        avatarUrl = avatarUrl,
        profile_image = avatarUrl,
        isLead = isLead,
        notes = notes,
        address = address,
        aadhaar_number = aadhaar_number,
        emergencyContactName = en,
        emergency_name = en,
        emergencyContactPhone = ec,
        emergency_contact_phone = ec,
        dateOfBirth = dateOfBirth,
        dob = dob,
        membershipEndsAt = membershipEndsAt,
        date_of_joining = date_of_joining,
        phone = phone,
        initialSubscription = initialSubscription,
    )
}

@Serializable
data class MemberSummaryDTO(
    val id: String = "",
    val name: String = "",
    val phone: String = "",
    val status: String = "",
    val plan_name: String? = null,
    val expiry_date: String? = null,
    @SerialName("member_subscription_id")
    val member_subscription_id: String? = null,
    @SerialName("price_cents")
    val price_cents: Int? = null,
    @SerialName("selling_price")
    val selling_price: Int? = null,
    @SerialName("paid_cents")
    val paid_cents: Int? = null,
    val amount_pending: Int? = null,
    @SerialName("extension_fees_total")
    val extension_fees_total: Int? = null,
    val profile_image: String? = null,
    val first_name: String? = null,
    val last_name: String? = null,
    val address: String? = null,
    val dob: String? = null,
    val aadhaar_number: String? = null,
    val emergency_name: String? = null,
    val emergency_contact_phone: String? = null,
    val notes: String? = null,
    val age: Int? = null
)

@Serializable
data class MemberSubscriptionDTO(
    val stats: SubscriptionStatsDTO = SubscriptionStatsDTO(),
    val current_subscription: CurrentSubscriptionDTO? = null,
    val current_subscriptions: List<CurrentSubscriptionDTO> = emptyList(),
    val upcoming_subscriptions: List<CurrentSubscriptionDTO> = emptyList(),
    val expired_subscriptions: List<CurrentSubscriptionDTO> = emptyList(),
    val freeze_subscriptions: List<CurrentSubscriptionDTO> = emptyList(),
)

/** Active plans from API: prefers [current_subscriptions], falls back to [current_subscription]. */
fun MemberSubscriptionDTO.activeSubscriptions(): List<CurrentSubscriptionDTO> {
    if (current_subscriptions.isNotEmpty()) return current_subscriptions
    return listOfNotNull(current_subscription)
}

fun MemberSubscriptionDTO.currentSubscriptionIds(): Set<String> {
    val fromList = current_subscriptions.mapNotNull { it.member_subscription_id?.takeIf { id -> id.isNotBlank() } }
    if (fromList.isNotEmpty()) return fromList.toSet()
    return setOfNotNull(current_subscription?.member_subscription_id?.takeIf { it.isNotBlank() })
}

fun MemberSubscriptionDTO.isCurrentSubscription(sub: CurrentSubscriptionDTO): Boolean {
    val id = sub.member_subscription_id?.takeIf { it.isNotBlank() } ?: return false
    return id in currentSubscriptionIds()
}

fun MemberSubscriptionDTO.subscriptionActionLabel(sub: CurrentSubscriptionDTO): String =
    if (isCurrentSubscription(sub)) "Extend" else "Renew"

@Serializable
data class SubscriptionStatsDTO(
    val active_subscription: Int = 0,
    val pending_payment: Int = 0,
    val overdue: Int = 0
)

@Serializable
data class CurrentSubscriptionDTO(
    @SerialName("member_subscription_id")
    val member_subscription_id: String? = null,
    @SerialName("gym_plan_id")
    val gym_plan_id: String? = null,
    val plan_name: String = "",
    val start_date: String = "",
    val expiry_date: String = "",
    val remaining_days: Int? = null,
    @SerialName("catalog_plan_price")
    val catalog_plan_price: Int? = null,
    @SerialName("plan_price")
    val plan_price: Int? = null,
    @SerialName("price_cents")
    val price_cents: Int? = null,
    @SerialName("selling_price")
    val selling_price: Int? = null,
    @SerialName("extension_fees_total")
    val extension_fees_total: Int? = null,
    @SerialName("extension_fee_total")
    val extension_fee_total: Int? = null,
    val amount_paid: Int? = null,
    @SerialName("paid_cents")
    val paid_cents: Int? = null,
    val amount_pending: Int? = null,
    val freeze_start_date: String? = null,
    val freeze_end_date: String? = null,
    val duration_days: Int? = null,
)

/** Agreed contract selling price from the API (not plan catalog price). */
fun CurrentSubscriptionDTO.resolvedSellingPrice(): Int? =
    selling_price?.takeIf { it > 0 }
        ?: price_cents?.takeIf { it > 0 }

fun CurrentSubscriptionDTO.resolvedAmountPaid(): Int =
    amount_paid?.takeIf { it > 0 } ?: paid_cents?.takeIf { it > 0 } ?: 0

fun CurrentSubscriptionDTO.resolvedAmountPending(): Int =
    amount_pending?.coerceAtLeast(0) ?: 0

fun CurrentSubscriptionDTO.resolvedExtensionFeesTotal(): Int =
    extension_fees_total?.takeIf { it > 0 }
        ?: extension_fee_total?.coerceAtLeast(0)
        ?: 0

/** Total contract value for UI: matches paid + pending when selling price is set. */
fun CurrentSubscriptionDTO.contractTotalAmount(): Int {
    resolvedSellingPrice()?.let { return it }
    val paid = resolvedAmountPaid()
    val pending = resolvedAmountPending()
    if (paid > 0 || pending > 0) return paid + pending
    plan_price?.takeIf { it > 0 }?.let { return it }
    catalog_plan_price?.takeIf { it > 0 }?.let { return it }
    return 0
}

fun CurrentSubscriptionDTO.hasSellingPrice(): Boolean = resolvedSellingPrice() != null

@Serializable
data class MemberUserDTO(
    val id: String = "",
    val fullName: String = "",
    val phone: String = "",
    val email: String? = null,
    @Serializable(with = NullableDoubleFlexibleSerializer::class)
    val heightCm: Double? = null,
    @Serializable(with = NullableDoubleFlexibleSerializer::class)
    val weightKg: Double? = null,
    val createdAt: String = "",
    val avatarUrl: String? = null,
    @SerialName("profile_image")
    val profile_image: String? = null,
    val age: Int? = null,
    val address: String? = null,
    @SerialName("aadhaar_number")
    val aadhaar_number: String? = null,
)

@Serializable
data class MemberContactDTO(
    val phone: String = "",
    val telUri: String = "",
    val whatsappUrl: String = ""
)

@Serializable
data class MemberTabsDTO(
    val subscriptions: String = "",
    val attendance: String = "",
    val attendance_history: String? = null,
    val payments: String = ""
)

@Serializable
data class MemberEmbeddedAttendanceDTO(
    val lifetime_check_ins: Int = 0,
    val last_check_in_at: String? = null,
    val last_attended_on: String? = null,
    val links: MemberEmbeddedAttendanceLinksDTO? = null
)

@Serializable
data class MemberEmbeddedAttendanceLinksDTO(
    val summary: String? = null,
    val history: String? = null
)

@Serializable
data class MemberAttendanceSummaryResponse(
    val filter: AttendanceFilterDTO,
    val gym_timezone: String? = null,
    val stats: AttendanceStatsDTO,
    val calendar: List<CalendarDayDTO> = emptyList(),
    val recent_logs: List<RecentLogDTO> = emptyList(),
    val months_overview: List<MonthOverviewDTO> = emptyList()
)

@Serializable
data class AttendanceFilterDTO(
    val year: Int,
    val month: Int,
    val month_label: String
)

@Serializable
data class AttendanceStatsDTO(
    val days_present_month: Int = 0,
    val days_in_month: Int = 0,
    val lifetime_check_ins: Int = 0,
    val present_days: Int = 0,
    val total_days: Int = 0
)

@Serializable
data class CalendarDayDTO(
    val date: String,
    val status: String // "present", "absent", etc.
)

@Serializable
data class RecentLogDTO(
    val id: String,
    val headline: String? = null,
    val punctuality: String? = null,
    val punctuality_label: String? = null,
    val attended_on: String? = null,
    val checked_in_at: String? = null,
    val checked_out_at: String? = null,
    val display_relative: String? = null
)

/** True when any attendance timestamp falls on [dateIso] (`yyyy-MM-dd`). */
fun RecentLogDTO.matchesAttendanceDate(dateIso: String): Boolean {
    val target = dateIso.trim().take(10)
    if (target.length != 10) return true
    return listOfNotNull(attended_on, checked_in_at, checked_out_at).any { raw ->
        raw.substringBefore("T").take(10) == target
    }
}

fun List<RecentLogDTO>.filteredByAttendanceDate(dateIso: String?): List<RecentLogDTO> {
    val target = dateIso?.trim()?.takeIf { it.isNotBlank() } ?: return this
    return filter { it.matchesAttendanceDate(target) }
}

@Serializable
data class MonthOverviewDTO(
    val year: Int,
    val month: Int,
    val month_label: String,
    val days_present: Int,
    val total_check_ins: Int,
    val days_in_month: Int
)

@Serializable
data class SubscriptionListResponse(
    val total: Int = 0,
    val limit: Int = 20,
    val offset: Int = 0,
    val items: List<SubscriptionDTO> = emptyList()
)

@Serializable
data class SubscriptionDTO(
    val id: String,
    val status: String,
    val startsAt: String? = null,
    val endsAt: String? = null,
    val priceCents: Int = 0,
    val paidCents: Int = 0,
    val balanceDueCents: Int = 0,
    val currency: String = "INR",
    val plan: PlanShortDTO? = null,
    val member: MemberShortDTO? = null
)

@Serializable
data class PlanShortDTO(
    val id: String,
    val name: String,
    val type: String? = null,
    val interval: String? = null,
    val durationDays: Int = 0,
    val priceCents: Int = 0,
    val currency: String = "INR"
)

@Serializable
data class MemberShortDTO(
    val gymUserId: String,
    val name: String,
    val phone: String
)

@Serializable
data class ReceivePaymentRequest(
    val type: String,
    val member_id: String,
    val member_subscription_id: String,
    val gym_plan_id: String,
    val amount: Int,
    val payment_mode: String,
    val date: String,
)

@Serializable
data class ExtendPlanPaymentRequest(
    val type: String,
    val member_id: String,
    val member_subscription_id: String? = null,
    val gym_plan_id: String,
    /** Agreed price for extend/renew; optional [addition_fee] for extension charges. */
    val addition_fee: Int? = null,
    val selling_price: Int? = null,
    val payment_mode: String,
    val date: String,
    val start_date: String? = null,
)

@Serializable
data class FreezeSubscriptionRequest(
    val gymId: String,
    val member_subscription_id: String,
    val freeze_start_date: String,
    val duration_days: Int,
    val freeze_fee: Int = 0,
    val reason: String = "",
)

@Serializable
data class UnfreezeSubscriptionRequest(
    val gymId: String,
    val member_subscription_id: String,
)

@Serializable
data class ReceivePaymentResponse(
    /** Present on standard receive-payment; omitted on extend_plan success. */
    val id: String = "",
    val type: String? = null,
    val amountCents: Int = 0,
    val currency: String = "INR",
    val status: String? = null,
    val method: String? = null,
    val reference: String? = null,
    val description: String? = null,
    val memberSubscriptionId: String? = null,
    val completedAt: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class CreateSubscriptionCompatRequest(
    val member_id: String,
    val plan_id: String? = null,
    val plan_name: String? = null,
    val duration_months: Int = 1,
    val price: Int = 0,
    val start_date: String? = null,
    val discount: Int = 0
)

@Serializable
data class CreateSubscriptionWithBodyRequest(
    val plan_name: String,
    val plan_discount: Int = 0,
    val plan_description: String? = null,
    val billing_cycle: String = "monthly",
    val price: Int,
    val plan_features: List<String> = emptyList(),
    val start_date: String? = null,
    val member_id: String
)
@Serializable
data class PaymentAnalyticsResponse(
    val total_revenue: Double = 0.0,
    val total_transactions: Int = 0,
    val range: String = "",
    val chart_data: List<PaymentChartDataDTO> = emptyList(),
    val recent_transactions: List<RecentPaymentTransactionDTO> = emptyList(),
    val totalCents: Int = 0,
    val currency: String = "INR",
    val series: List<PaymentSeriesDTO> = emptyList(),
    val items: List<PaymentItemDTO> = emptyList(),
)

@Serializable
data class PaymentChartDataDTO(
    val label: String = "",
    val month: String = "",
    val revenue: Double = 0.0,
) {
    /** API sends `label` (e.g. Jan, Mon, 2026); older payloads may use `month`. */
    val axisLabel: String
        get() = label.trim().ifEmpty { month.trim() }.ifEmpty { "—" }
}

@Serializable
data class RecentPaymentTransactionDTO(
    val member_name: String,
    val amount: Double,
    val date: String,
    val mode: String
)

@Serializable
data class PaymentSeriesDTO(
    val date: String,
    val amountCents: Int
)

@Serializable
data class PaymentItemDTO(
    val id: String,
    val amountCents: Int,
    val currency: String,
    val method: String? = null,
    val reference: String? = null,
    val description: String? = null,
    val memberSubscriptionId: String? = null,
    val completedAt: String? = null,
    val createdAt: String? = null,
    val memberUserId: String? = null,
    val status: String? = null,
    val invoiceId: String? = null,
    val memberUser: MemberShortInfoDTO? = null,
    val gymPlanName: String? = null,
    @SerialName("plan_name")
    val plan_name: String? = null,
    val receivedBy: String? = null,
    @SerialName("received_by_name")
    val received_by_name: String? = null,
)

fun PaymentItemDTO.resolvedPlanTitle(): String =
    gymPlanName?.takeIf { it.isNotBlank() }
        ?: plan_name?.takeIf { it.isNotBlank() }
        ?: description?.takeIf { it.isNotBlank() }
        ?: "Subscription Payment"

fun PaymentItemDTO.resolvedReceivedBy(): String =
    receivedBy?.takeIf { it.isNotBlank() }
        ?: received_by_name?.takeIf { it.isNotBlank() }
        ?: "—"

fun PaymentItemDTO.resolvedPaymentMethodLabel(): String {
    val raw = method?.trim().orEmpty()
    if (raw.isEmpty()) return "—"
    return raw.lowercase().replaceFirstChar { it.uppercase() }
}

@Serializable
data class MemberShortInfoDTO(
    val id: String = "",
    val fullName: String = "",
    val phone: String? = null,
)

@Serializable
data class PaymentListResponse(
    val total: Int = 0,
    val limit: Int = 20,
    val offset: Int = 0,
    val items: List<PaymentItemDTO> = emptyList()
)
@Serializable
data class WorkoutDTO(
    @SerialName("workout_id")
    val workout_id: String? = null,
    val id: String? = null,
    val title: String? = null,
    val date: String? = null,
    @SerialName("exercise_count")
    val exercise_count: Int? = null,
    @SerialName("created_by")
    val created_by: String? = null,
    @SerialName("created_by_role")
    val created_by_role: String? = null,
    @SerialName("created_by_name")
    val created_by_name: String? = null,
    val is_saved: Boolean = false,
    val completed: Boolean = false,
    val exercises: List<WorkoutExerciseDTO> = emptyList()
) {
    val workoutId: String get() = workout_id ?: id ?: ""
}

/** Display label for workout author (name preferred, then role, then created_by). */
fun WorkoutDTO.createdByDisplayLabel(): String? {
    created_by_name?.takeIf { it.isNotBlank() }?.let { return it }
    created_by_role?.takeIf { it.isNotBlank() }?.let { role ->
        return role.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }
    created_by?.takeIf { it.isNotBlank() }?.let { source ->
        return source.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }
    return null
}

/** Row from GET `workouts/history` (JWT subject, paginated). */
@Serializable
data class MemberWorkoutHistoryItemDTO(
    @SerialName("workout_id")
    val workout_id: String = "",
    @SerialName("user_id")
    val user_id: String? = null,
    val title: String = "",
    @SerialName("started_at")
    val started_at: String? = null,
    @SerialName("ended_at")
    val ended_at: String? = null,
    val completed: Boolean = false,
    @SerialName("is_saved")
    val is_saved: Boolean = false,
    @SerialName("total_volume")
    val total_volume: Int = 0,
    @SerialName("total_sets")
    val total_sets: Int = 0,
    val duration: String? = null,
    @SerialName("exercise_count")
    val exercise_count: Int = 0,
) {
    val workoutId: String get() = workout_id
}

@Serializable
data class MemberWorkoutHistoryPageResponse(
    val userId: String? = null,
    val page: Int = 0,
    val limit: Int = 20,
    val total: Int = 0,
    val items: List<MemberWorkoutHistoryItemDTO> = emptyList(),
)

@Serializable
data class WorkoutExerciseDTO(
    val id: String? = null,
    @SerialName("workout_id")
    val workout_id_alt: String? = null,
    val workoutId: String? = null,
    @SerialName("exercise_id")
    val exercise_id_alt: String? = null,
    val exerciseId: String? = null,
    val createdAt: String? = null,
    val exercise: ExerciseRowDTO? = null,
    val sets: List<WorkoutDetailSetDTO> = emptyList()
) {
    val exerciseIdResolved: String get() = exerciseId ?: exercise_id_alt ?: ""
}
@Serializable
data class ExerciseRowDTO(
    val id: String,
    val name: String,
    @SerialName("asset_url")
    val asset_url: String? = null,
    val equipment: String? = null,
    @SerialName("primaryMuscle")
    val primary_muscle: String? = null,
    @SerialName("secondaryMuscles")
    val secondary_muscles: List<String> = emptyList(),
    @SerialName("exercise_type")
    val exercise_type: String? = null,
    @SerialName("isActive")
    val is_active: Boolean = true,
    @SerialName("createdAt")
    val created_at: String? = null
)

@Serializable
data class MetadataResponse(
    val equipments: List<EquipmentDTO> = emptyList(),
    val muscles: List<MuscleDTO> = emptyList(),
    val exercise_types: List<ExerciseTypeDTO> = emptyList()
)

@Serializable
data class EquipmentDTO(
    val label: String,
    val value: String,
    val icon: String? = null
)

@Serializable
data class MuscleDTO(
    val label: String,
    val value: String
)

@Serializable
data class ExerciseTypeDTO(
    val label: String,
    val value: String,
    val fields: List<String> = emptyList()
)
@Serializable
data class CreateExerciseRequest(
    val name: String,
    val equipment: String,
    val primary_muscle: String,
    val exercise_type: String,
    val asset_url: String? = null,
    val secondary_muscles: List<String> = emptyList(),
    val is_active: Boolean = true
)

@Serializable
data class CreateWorkoutRequest(
    val member_id: String? = null,

    @SerialName("created_by")
    val created_by: String,
    val title: String,
    val notes: String? = null,
    val exercises: List<WorkoutExerciseRequest>
)

@Serializable
data class CompleteWorkoutRequest(
    @SerialName("workout_id")
    val workoutId: String,
    val title: String,
    val notes: String? = null,
    val exercises: List<WorkoutExerciseRequest>
)

@Serializable
data class UpdateWorkoutLegacyRequest(
    @SerialName("workout_id")
    val workoutId: String,
    val title: String? = null,
    val notes: String? = null,
    val completed: Boolean? = null,
    val isSaved: Boolean? = null,
)

@Serializable
data class WorkoutExerciseRequest(
    val exercise_id: String,
    val sets: List<WorkoutSetRequest>
)

@Serializable
data class WorkoutSetRequest(
    val set_number: Int,
    val reps: Int,
    val weight: Double
)

@Serializable
data class CreateSetRequest(
    @SerialName("workout_exercise_id")
    val workoutExerciseId: String,
    @SerialName("set_number")
    val setNumber: Int,
    val reps: Int? = null,
    val weight: Double? = null,
    val time: Int? = null,
)

@Serializable
data class UpdateSetRequest(
    val reps: Int? = null,
    val weight: Double? = null,
    val time: Int? = null,
    val completed: Boolean,
)

@Serializable
data class WorkoutDetailSetDTO(
    val id: String = "",
    @SerialName("set_number")
    val set_number_alt: Int? = null,
    @SerialName("setNumber")
    val setNumber: Int? = null,
    val reps: Int = 0,
    val weight: Double = 0.0,
    val time: Int? = null,
    val completed: Boolean = false
) {
    val set_number: Int get() = set_number_alt ?: setNumber ?: 0
}

@Serializable
data class WorkoutDetailExerciseDTO(
    val id: String? = null,
    @SerialName("workout_exercise_id")
    val workout_exercise_id_alt: String? = null,
    @SerialName("workoutExerciseId")
    val workoutExerciseIdAlt: String? = null,
    @SerialName("exercise_id")
    val exercise_id_alt: String? = null,
    @SerialName("exerciseId")
    val exerciseId: String? = null,
    val name: String,
    @SerialName("asset_url")
    val asset_url_alt: String? = null,
    @SerialName("assetUrl")
    val assetUrl: String? = null,
    @SerialName("exercise_type")
    val exercise_type_alt: String? = null,
    @SerialName("exerciseType")
    val exerciseType: String? = null,
    val sets: List<WorkoutDetailSetDTO> = emptyList()
) {
    val workout_exercise_id: String get() =
        workout_exercise_id_alt ?: workoutExerciseIdAlt ?: id.orEmpty()
    val exercise_id: String get() = exercise_id_alt ?: exerciseId ?: ""
    val asset_url: String? get() = asset_url_alt ?: assetUrl
    val exercise_type: String? get() = exercise_type_alt ?: exerciseType
}

@Serializable
data class WorkoutDetailResponse(
    @SerialName("workout_id")
    val workout_id: String? = null,
    val id: String? = null,
    val title: String,
    @SerialName("created_by")
    val created_by: String? = null,
    @SerialName("created_by_role")
    val created_by_role: String? = null,
    @SerialName("created_by_name")
    val created_by_name: String? = null,
    val duration: String? = null,
    val volume: String? = null,
    val sets: Int = 0,
    val is_saved: Boolean = false,
    val exercises: List<WorkoutDetailExerciseDTO> = emptyList()
) {
    val workoutId: String get() = workout_id ?: id ?: ""
}

/** Display label for workout detail author (name preferred, then role, then created_by). */
fun WorkoutDetailResponse.createdByDisplayLabel(): String? {
    created_by_name?.takeIf { it.isNotBlank() }?.let { return it }
    created_by_role?.takeIf { it.isNotBlank() }?.let { role ->
        return role.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }
    created_by?.takeIf { it.isNotBlank() }?.let { source ->
        return source.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }
    return null
}

fun WorkoutDetailExerciseDTO.isDurationExercise(): Boolean =
    exercise_type?.contains("DURATION", ignoreCase = true) == true

fun WorkoutDetailExerciseDTO.isDurationWeightExercise(): Boolean =
    isDurationExercise() && exercise_type?.contains("WEIGHT", ignoreCase = true) == true

@Serializable
data class WorkoutCompletionResponse(
    @SerialName("workout_id")
    val workout_id: String = "",
    val title: String = "",
    val completed: Boolean = true,
    val date: String? = null,
    @SerialName("date_label")
    val date_label: String? = null,
    @SerialName("started_at")
    val started_at: String? = null,
    @SerialName("ended_at")
    val ended_at: String? = null,
    @SerialName("duration_minutes")
    val duration_minutes: Int = 0,
    val duration: String? = null,
    @SerialName("volume_kg")
    val volume_kg: Double? = null,
    val volume: String? = null,
    val sets: Int = 0,
    val exercises: List<WorkoutDetailExerciseDTO> = emptyList(),
) {
    val workoutId: String get() = workout_id
}

@Serializable
data class UpdateWorkoutCompletionRequest(
    @SerialName("duration_minutes")
    val duration_minutes: Int,
)
