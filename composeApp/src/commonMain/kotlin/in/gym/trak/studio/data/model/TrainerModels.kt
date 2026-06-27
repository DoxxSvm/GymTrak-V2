package `in`.gym.trak.studio.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Values for trainer/staff list and compat-create APIs (`role` query / body). */
object GymStaffListRole {
    const val TRAINER = "TRAINER"
    const val STAFF = "STAFF"
}

// ── List Trainers ────────────────────────────────────────────────────────────

@Serializable
data class TrainerListResponse(
    val total: Int = 0,
    val limit: Int = 20,
    val offset: Int = 0,
    val items: List<TrainerDTO> = emptyList()
)

@Serializable
data class TrainerDTO(
    val gymUserId: String = "",
    val userId: String = "",
    val fullName: String = "",
    val phone: String? = null,
    val email: String? = null,
    val username: String? = null,
    val avatarUrl: String? = null,
    val isActive: Boolean = true,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val joinedAt: String? = null,
    val expertise: List<String> = emptyList(),
    val salary: TrainerSalaryDTO? = null
)

@Serializable
data class TrainerSalaryDTO(
    val salaryCents: Int = 0,
    val salaryPeriod: String? = "MONTHLY",
    val contractStartsAt: String? = null,
    val contractEndsAt: String? = null,
    val experience: String? = null,
    val address: String? = null
)

// ── Create Trainer ───────────────────────────────────────────────────────────

@Serializable
data class CreateTrainerRequest(
    val gymId: String,
    val phone: String,
    val fullName: String,
    val email: String? = null,
    val avatarUrl: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val experience: String? = null,
    val address: String? = null,
    val expertise: List<String> = emptyList(),
    val salaryCents: Int = 0,
    val salaryPeriod: String? = "MONTHLY",
    val contractStartsAt: String? = null,
    val contractEndsAt: String? = null,
    val notes: String? = null,
    val shifts: List<TrainerShiftRequest> = emptyList(),
    val permissions: TrainerPermissionsRequest = TrainerPermissionsRequest(),
    val generateLoginCredentials: Boolean = false,
    val username: String? = null,
    val password: String? = null
)

@Serializable
data class UpdateTrainerRequest(
    val fullName: String? = null,
    val email: String? = null,
    val avatarUrl: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val experience: String? = null,
    val address: String? = null,
    val expertise: List<String>? = null,
    val salaryCents: Int? = null,
    val salaryPeriod: String? = null,
    val contractStartsAt: String? = null,
    val contractEndsAt: String? = null,
    val notes: String? = null,
    val shifts: List<TrainerShiftRequest>? = null,
    val planIds: List<String>? = null,
    /** Selected permission key names (same strings as dashboard / SessionManager.PermissionKeys). */
    val permissions: List<String>? = null,
    val isActive: Boolean? = null,
    val role: String? = null
)

@Serializable
data class TrainerShiftRequest(
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String
)

/**
 * Sent as a single flat JSON object under `permissions` (no nested `effective` map).
 * [granular] holds fine-grained flags; they are merged into the same object as the legacy fields.
 */
@Serializable(with = TrainerPermissionsRequestSerializer::class)
data class TrainerPermissionsRequest(
    val dashboard: Boolean = false,
    val payments: Boolean = false,
    val members: Boolean = true,
    val admin: Boolean = false,
    val show_dashboard: Boolean = false,
    val show_payments: Boolean = false,
    val show_payment_in_details: Boolean = false,
    val add_clients: Boolean = true,
    val add_trainer: Boolean = false,
    val granular: Map<String, Boolean> = emptyMap()
)

object TrainerPermissionsRequestSerializer : KSerializer<TrainerPermissionsRequest> {

    private val legacyKeys = setOf(
        "dashboard",
        "payments",
        "members",
        "admin",
        "show_dashboard",
        "show_payments",
        "show_payment_in_details",
        "add_clients",
        "add_trainer"
    )

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("TrainerPermissionsRequest")

    override fun serialize(encoder: Encoder, value: TrainerPermissionsRequest) {
        val jsonEncoder = encoder as? JsonEncoder
            ?: throw SerializationException("TrainerPermissionsRequest requires JsonEncoder")
        val element = buildJsonObject {
            value.granular.forEach { (k, v) ->
                put(k, JsonPrimitive(v))
            }
            put("dashboard", JsonPrimitive(value.dashboard))
            put("payments", JsonPrimitive(value.payments))
            put("members", JsonPrimitive(value.members))
            put("admin", JsonPrimitive(value.admin))
            put("show_dashboard", JsonPrimitive(value.show_dashboard))
            put("show_payments", JsonPrimitive(value.show_payments))
            put("show_payment_in_details", JsonPrimitive(value.show_payment_in_details))
            put("add_clients", JsonPrimitive(value.add_clients))
            put("add_trainer", JsonPrimitive(value.add_trainer))
        }
        jsonEncoder.encodeJsonElement(element)
    }

    override fun deserialize(decoder: Decoder): TrainerPermissionsRequest {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("TrainerPermissionsRequest requires JsonDecoder")
        val obj = jsonDecoder.decodeJsonElement().jsonObject
        fun bool(key: String, default: Boolean = false): Boolean {
            val prim = obj[key]?.jsonPrimitive ?: return default
            return prim.booleanOrNull ?: default
        }
        val granular = mutableMapOf<String, Boolean>()
        obj.forEach { (k, v) ->
            if (k !in legacyKeys) {
                val p = v.jsonPrimitive
                p.booleanOrNull?.let { granular[k] = it }
            }
        }
        return TrainerPermissionsRequest(
            dashboard = bool("dashboard"),
            payments = bool("payments"),
            members = bool("members", default = true),
            admin = bool("admin"),
            show_dashboard = bool("show_dashboard"),
            show_payments = bool("show_payments"),
            show_payment_in_details = bool("show_payment_in_details"),
            add_clients = bool("add_clients", default = true),
            add_trainer = bool("add_trainer"),
            granular = granular
        )
    }
}

@Serializable
data class CreateTrainerSalaryPaymentRequest(
    val gymId: String,
    val trainer_id: String,
    val amount: Int,
    val payment_mode: String,
    val date: String
)

@Serializable
data class CreateTrainerResponse(
    val gymUserId: String = "",
    val userId: String = "",
    val isActive: Boolean = true
)

// ── Compat Create Trainer ─────────────────────────────────────────────────────

@Serializable
data class CreateTrainerCompatRequest(
    val gymId: String,
    val phone: String,
    val full_name: String,
    val dob: String? = null,
    val gender: String? = null,
    val experience: String? = null,
    val address: String? = null,
    val profile_image: String? = null,
    val salary: Int = 0,
    val salary_type: String = "monthly",
    val expertise: List<String> = emptyList(),
    val shifts: List<TrainerCompatShiftRequest>? = null,
    val credentials: TrainerCredentials? = null,
    /** Selected permission key names (same strings as dashboard / SessionManager.PermissionKeys). */
    val permissions: List<String> = emptyList(),
    val email: String,
    /** Gym role for the new user (`TRAINER` or `STAFF`). */
    val role: String = GymStaffListRole.TRAINER
)

@Serializable
data class TrainerCompatShiftRequest(
    val dayOfWeek: Int,
    val startTime: String,
    val endTime: String
)

@Serializable
data class TrainerCredentials(
    val trainer_id: String,
    val password: String
)

@Serializable
data class SetTrainerPasswordRequest(
    val password: String
)

// ── Image Upload ─────────────────────────────────────────────────────────────

@Serializable
data class UploadImageResponse(
    val name: String = "",
    val url: String = "",
    val relativePath: String? = null,
    val contentType: String? = null,
    val size: Long = 0
)

// ── Trainer Detail ───────────────────────────────────────────────────────────

@Serializable
data class TrainerDetailResponse(
    val tab: String? = null,
    val gymUserId: String = "",
    val role: String? = null,
    val userId: String = "",
    val isActive: Boolean = true,
    val joinedAt: String? = null,
    val user: TrainerUserDetailDTO? = null,
    val profile: TrainerProfileDetailDTO? = null,
    val expertise: List<String> = emptyList(),
    val shifts: List<TrainerShiftDetailDTO> = emptyList(),
    /** Granted permission key names (same strings as [gym.trak.studio.data.repository.SessionManager.PermissionKeys]). */
    val permissions: List<String> = emptyList(),
    val plans: TrainerPlansDTO? = null,
    val attendance: TrainerAttendanceDTO? = null,
    val payments: TrainerPaymentsDTO? = null,
    val salary: TrainerSalaryDetailsDTO? = null
)

@Serializable
data class TrainerPlansDTO(
    val totalActivePlans: Int = 0,
    val totalSubscribers: Int = 0,
    val items: List<TrainerPlanItemDTO> = emptyList()
)

@Serializable
data class TrainerPlanItemDTO(
    val type: String = "",
    val id: String = "",
    val name: String = "",

    val durationLabel: String = "",
    val activeClients: Int = 0,
    val price: Int = 0,
    val currency: String = "$",
    val billingCycle: String = ""
)

@Serializable
data class TrainerAttendanceDTO(
    val summary: TrainerAttendanceSummaryDTO? = null,
    val recentLogs: List<TrainerAttendanceLogDTO> = emptyList()
)

@Serializable
data class TrainerAttendanceLogDTO(
    val id: String = "",
    val title: String = "",
    @SerialName("checkedInAt")
    val checkedInAt: String = "",
    @SerialName("status")
    val status: String = "",
    val timestamp: String = "",
    val statusLabel: String = "",
    val statusColor: String? = null // Hex or color name
)

@Serializable
data class TrainerAttendanceSummaryDTO(
    val yearMonth: String = "",
    val daysPresentThisMonth: Int = 0,
    val daysInMonth: Int = 0,
    val lifetimeCheckIns: Int = 0
)

@Serializable
data class TrainerPaymentsDTO(
    val totalRevenue: Int = 0,
    val revenueChangePercent: Float? = null,
    val chart: List<TrainerRevenueChartItemDTO> = emptyList(),
    val items: List<TrainerPaymentItemDTO> = emptyList()
)

@Serializable
data class TrainerPaymentItemDTO(
    val id: String = "",
    val memberName: String = "",
    val memberAvatarUrl: String? = null,
    val subtitle: String? = null,
    val planName: String = "",
    val amount: Int = 0,
    val currency: String = "INR",
    val method: String = "",
    val createdAt: String = "",
)

@Serializable
data class TrainerRevenueChartItemDTO(
    val week: Int = 0,
    val revenue: Int = 0
)

@Serializable
data class TrainerSalaryDetailsDTO(
    val monthlySalary: Int = 0,
    val paidAmount: Int = 0,
    val pendingAmount: Int = 0,
    val paymentHistory: List<TrainerSalaryPaymentItemDTO> = emptyList()
)

@Serializable
data class TrainerSalaryPaymentItemDTO(
    val id: String = "",
    val title: String = "",
    val date: String = "",
    val amount: Int = 0,
    val currency: String = "",
    val method: String = ""
)

@Serializable
data class TrainerUserDetailDTO(
    val phone: String? = null,
    val email: String? = null,
    val fullName: String? = null,
    val username: String? = null,
    val avatarUrl: String? = null,
    val staffLoginEnabled: Boolean = false,
    val createdAt: String? = null
)

@Serializable
data class TrainerProfileDetailDTO(
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val salaryCents: Int = 0,
    val salaryPeriod: String? = null,
    val contractStartsAt: String? = null,
    val contractEndsAt: String? = null,
    val experience: String? = null,
    val address: String? = null,
    val notes: String? = null
)

@Serializable
data class TrainerShiftDetailDTO(
    val id: String = "",
    val dayOfWeek: Int = 0,
    val startTime: String = "",
    val endTime: String = ""
)

@Serializable
data class TrainerPermissionsDTO(
    val dashboard: Boolean = false,
    val payments: Boolean = false,
    val members: Boolean = false,
    val admin: Boolean = false,
    val show_dashboard: Boolean = false,
    val show_payments: Boolean = false,
    val show_payment_in_details: Boolean = false,
    val add_clients: Boolean = false,
    val add_trainer: Boolean = false,
    val effective: Map<String, Boolean> = emptyMap()
)

@Serializable
data class GenericSuccessResponse(
    val success: Boolean = false,
    val ok: Boolean = false,
    @kotlinx.serialization.SerialName("workout_id")
    val workoutId: String? = null,
    val id: String? = null,
)
