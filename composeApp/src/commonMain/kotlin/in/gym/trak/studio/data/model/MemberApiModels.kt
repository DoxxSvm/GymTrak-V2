package `in`.gym.trak.studio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MemberOnboardingRequest(
    val fullName: String,
    val heightCm: Int,
    val weightKg: Int,
    val ageYears: Int,
    val gender: String,
    val activityLevel: String,
    val fitnessGoal: String,
)

@Serializable
data class MemberWellnessPayload(
    val bmi: Double? = null,
    val bmiCategory: String? = null,
    val maintenanceCalories: Int? = null,
)

@Serializable
data class MemberOnboardingResponse(
    val success: Boolean? = null,
    val access_token: String? = null,
    val refresh_token: String? = null,
    val gym_id: String? = null,
    val user: UserData? = null,
    val wellness: MemberWellnessPayload? = null,
)

@Serializable
data class MemberDashboardResponse(
    val gymId: String? = null,
    /** Gym membership row id — use for member profile path and payments `memberId` when present. */
    val gymUserId: String? = null,
    /** Auth user id (may differ from [gymUserId]). */
    val userId: String? = null,
    val user: MemberDashboardUser? = null,
    val membership: MemberDashboardMembership? = null,
    val todayWorkout: MemberDashboardTodayWorkout? = null,
    val stats: MemberDashboardStats? = null,
    val nutrition: MemberDashboardNutrition? = null,
    val attendance: MemberDashboardAttendance? = null,
)

@Serializable
data class MemberDashboardUser(
    val id: String? = null,
    val firstName: String? = null,
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val greeting: String? = null,
    val tagline: String? = null,
    val unreadNotifications: Int = 0,
)

@Serializable
data class MemberDashboardMembership(
    val expiresAt: String? = null,
    val daysRemaining: Int? = null,
    val totalDays: Int? = null,
    val percentRemaining: Double? = null,
    val statusLabel: String? = null,
    val progressLabel: String? = null,
)

@Serializable
data class MemberDashboardTodayWorkout(
    val id: String? = null,
    val title: String? = null,
    val exerciseCount: Int? = null,
    val tags: List<String> = emptyList(),
    val imageUrl: String? = null,
    val completed: Boolean? = null,
)

@Serializable
data class MemberDashboardStats(
    val sessionsThisWeek: Int? = null,
    val streakDays: Int? = null,
    val totalHours: Double? = null,
)

@Serializable
data class MemberDashboardNutrition(
    val caloriesConsumed: Int? = null,
    val calorieGoal: Int? = null,
    val proteinKcal: Int? = null,
    val carbsKcal: Int? = null,
    val fatKcal: Int? = null,
    val fatPending: Boolean? = null,
)

@Serializable
data class MemberDashboardAttendance(
    val daysAttended: Int? = null,
    val periodDays: Int? = null,
    val lifetimeCheckIns: Int? = null,
    val percentileAmongMembers: Double? = null,
    val insightLabel: String? = null,
)
