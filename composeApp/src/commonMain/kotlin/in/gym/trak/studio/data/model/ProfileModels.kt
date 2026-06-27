package `in`.gym.trak.studio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    val role: String, // gym_owner or trainer
    val personalInfo: PersonalInfoUpdate? = null,
    val gymDetails: GymDetailsUpdate? = null,
    val trainerDetails: TrainerDetailsUpdate? = null
)

@Serializable
data class PersonalInfoUpdate(
    val firstName: String? = null,
    val lastName: String? = null,
    val fullName: String? = null,
    val profileImage: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null, // "male", "female", etc.
    val address: String? = null
)

@Serializable
data class GymDetailsUpdate(
    val gymName: String? = null,
    val gymAddress: String? = null,
    val gstNumber: String? = null,
    val gymLogo: String? = null
)

@Serializable
data class TrainerDetailsUpdate(
    val experience: String? = null,
    val salary: Int? = null,
    val salaryDuration: String? = "month", // "month", "week", etc.
    val expertise: List<String> = emptyList(),
    val shifts: List<TrainerShiftUpdate> = emptyList()
)

@Serializable
data class TrainerShiftUpdate(
    val name: String? = null,
    val dayOfWeek: Int, // 0-6
    val startTime: String, // HH:mm
    val endTime: String // HH:mm
)

@Serializable
data class UpdateOwnerProfileRequest(
    val fullName: String? = null,
    val avatarUrl: String? = null,
    val profile_image: String? = null,
    val gymName: String? = null,
    val gymAddress: String? = null,
    val gymGstNumber: String? = null,
    val gymLogoUrl: String? = null
)

@Serializable
data class UpdateTrainerProfileRequest(
    val avatarUrl: String? = null,
    val profile_image: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val fullName: String? = null,
    val dateOfBirth: String? = null,
    val gender: String? = null,
    val experience: String? = null,
    val address: String? = null,
    val salary: Int? = null,
    val salaryCents: Int? = null,
    val salaryPeriod: String? = null, // HOURLY, WEEKLY, MONTHLY, YEARLY
    val expertise: List<String> = emptyList(),
    val shifts: List<TrainerShiftUpdate> = emptyList(),
    val role: String? = null
)
