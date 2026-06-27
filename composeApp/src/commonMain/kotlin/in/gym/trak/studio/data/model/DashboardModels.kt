package `in`.gym.trak.studio.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonNames

@Serializable
data class DashboardResponse(
    val greeting: String,
    val gym_name: String,
    val owner_name: String,
    val stats: DashboardStats,
    val revenue: DashboardRevenue,
    val recent_payments: List<RecentPaymentDTO> = emptyList(),
    val weekly_attendance: List<WeeklyAttendanceDTO> = emptyList(),
    val enquiry_summary: EnquirySummaryDTO? = null
)

@Serializable
data class EnquirySummaryDTO(
    val total: Int = 0,
    val converted: Int = 0,
    val pending: Int = 0
)

@Serializable
data class DashboardStats(
    val active_members: Int,
    val inactive_members: Int,
    val expired_members: Int,
    val total_members: Int
)

@Serializable
data class DashboardRevenue(
    val monthly: Int,
    val pending: Int
)

@Serializable
data class RecentPaymentDTO(
    val name: String,
    val time: String,
    val plan: String,
    val amount: String,
    val imageUrl: String? = null
)

@Serializable
data class WeeklyAttendanceDTO(
    val day: String,
    val count: Int
)

@Serializable
data class ApiResponse<T>(
    val success: Boolean = false,
    val data: T? = null
)

@Serializable
data class ProfileResponse(
    val id: String? = null,
    val role: String? = null,
    val personalInfo: PersonalInfoDTO? = null,
    val gymDetails: GymDetailsDTO? = null,
    val trainerDetails: TrainerDetailsDTO? = null
)

@Serializable
data class PersonalInfoDTO(
    val fullName: String? = null,
    val profileImage: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val address: String? = null
)

@Serializable
data class GymDetailsDTO(
    val gymName: String? = null,
    val gymAddress: String? = null,
    val gstNumber: String? = null,
    val gymLogo: String? = null
)

@Serializable
data class TrainerDetailsDTO(
    val experience: String? = null,
    val salary: Int? = null,
    val salaryDuration: String? = null,
    val expertise: List<String> = emptyList()
)
@Serializable
data class OwnerDashboardNewResponse(
    val viewer: String = "",
    val gyms: List<GymDTO> = emptyList(),
    val selectedGymId: String = "",
    val gym: GymDTO = GymDTO(),
    @JsonNames("effective_permissions")
    @SerialName("effectivePermissions")
    val effectivePermissions: Map<String, Boolean> = emptyMap(),
    /** Granted permission key names (same strings as [gym.trak.studio.data.repository.SessionManager.PermissionKeys]). */
    @JsonNames("permission_keys", "granted_permissions")
    val permissions: List<String> = emptyList(),
    val owner_name: String = "",
    val owner_image: String? = null,
    val total_enquiry: Int = 0,
    val converted: Int = 0,
    val pending: Int = 0,
    val health: HealthStats = HealthStats(),
    val payments: PaymentStats = PaymentStats(),
    val attendance: AttendanceStats = AttendanceStats(),
    @SerialName("traffic_trend")
    val trafficTrend: TrafficTrendStats = TrafficTrendStats(),
    val members: MemberStatsResponse = MemberStatsResponse(),
    val expiryAlerts: ExpiryAlerts = ExpiryAlerts(),
    val enquiries: EnquiryStatsResponse = EnquiryStatsResponse(),
    val notifications: NotificationStats = NotificationStats()
)

@Serializable
data class DashboardPermissions(
    val role: String = "",
    val effective: Map<String, Boolean> = emptyMap(),
    val roleDefaults: RoleDefaultPermissions = RoleDefaultPermissions()
)

@Serializable
data class RoleDefaultPermissions(
    val trainer: RoleDefaultPermissionSet? = null,
    val staff: RoleDefaultPermissionSet? = null
)

@Serializable
data class RoleDefaultPermissionSet(
    val gymRole: String = "",
    val permissionCodes: List<String> = emptyList(),
    val details: List<RoleDefaultPermissionDetail> = emptyList()
)

@Serializable
data class RoleDefaultPermissionDetail(
    val code: String = "",
    val description: String = ""
)

@Serializable
data class GymDTO(
    val id: String = "",
    val name: String = "",
    val slug: String = "",
    val timezone: String = ""
)

@Serializable
data class HealthStats(
    val score: Int = 0,
    val factors: HealthFactors = HealthFactors()
)

@Serializable
data class HealthFactors(
    val payments: Int = 0,
    val attendance: Int = 0,
    val membership: Int = 0
)

@Serializable
data class PaymentStats(
    val receivedLast30DaysCents: Long = 0L,
    val pendingCents: Long = 0L,
    val currency: String = "",
    val recent: List<RecentPaymentNewDTO> = emptyList()
)

@Serializable
data class RecentPaymentMemberUserDTO(
    val id: String? = null,
    val fullName: String? = null,
    val phone: String? = null
)

@Serializable
data class RecentPaymentNewDTO(
    val id: String? = null,
    val amountCents: Long? = null,
    val currency: String? = null,
    val status: String? = null,
    val createdAt: String? = null,
    val memberName: String? = null,
    val memberImage: String? = null,
    val memberUser: RecentPaymentMemberUserDTO? = null
)

@Serializable
data class AttendanceStats(
    val todayCount: Int = 0,
    val date: String = ""
)

@Serializable
data class TrafficTrendStats(
    @SerialName("today_count")
    val todayCount: Int = 0,
    val capacity: Int = 0,
    @SerialName("selected_day")
    val selectedDay: String = "",
    val weekly: List<TrafficTrendWeeklyDTO> = emptyList()
)

@Serializable
data class TrafficTrendHourlyDTO(
    val label: String = "",
    val count: Int = 0
)

@Serializable
data class TrafficTrendWeeklyDTO(
    val day: String = "",
    val count: Int = 0,
    val hourly: List<TrafficTrendHourlyDTO> = emptyList()
)

@Serializable
data class MemberStatsResponse(
    val total: Int = 0,
    val active: Int = 0,
    val inactive: Int = 0,
    val expired: Int = 0
)

@Serializable
data class ExpiryAlerts(
    val days1to3: Int = 0,
    val days4to7: Int = 0
)

@Serializable
data class EnquiryStatsResponse(
    val total: Int = 0,
    val open: Int = 0,
    val converted: Int = 0,
    val pending: Int = 0
)

@Serializable
data class NotificationStats(
    val unreadCount: Int = 0
)

@Serializable
data class LeaderboardResponse(
    val type: String = "attendance",
    val data: List<LeaderboardEntryDTO> = emptyList(),
    val page: Int = 1,
    val limit: Int = 20,
    val total: Int = 0
)

@Serializable
data class LeaderboardEntryDTO(
    val rank: Int = 0,
    val userId: String = "",
    val name: String = "",
    val profileImage: String? = null,
    val points: Int = 0,
    val isTopThree: Boolean = false,
    val isCurrentUser: Boolean = false
)
