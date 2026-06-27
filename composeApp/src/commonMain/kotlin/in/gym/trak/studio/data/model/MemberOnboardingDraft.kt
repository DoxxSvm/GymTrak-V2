package `in`.gym.trak.studio.data.model

/**
 * Accumulates member onboarding answers across Voyager screens; used to build [MemberOnboardingRequest].
 */
data class MemberOnboardingDraft(
    val tempToken: String,
    val fullName: String = "",
    val ageYears: Int? = null,
    /** UI label from [gym.trak.studio.components.GenderSelector]: Male, Female, Other */
    val genderLabel: String = "",
    val heightCm: Int? = null,
    val weightKg: Int? = null,
    /** API values: HIGH, MODERATE, LOW */
    val activityLevel: String? = null,
    /** API values: LOSE_WEIGHT, BUILD_MUSCLE, STAY_FIT, IMPROVE_ENDURANCE */
    val fitnessGoal: String? = null,
)
