package `in`.gym.trak.studio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class EnquiryListResponse(
    val total: Int = 0,
    val limit: Int = 20,
    val offset: Int = 0,
    val items: List<EnquiryDTO> = emptyList()
)

@Serializable
data class EnquiryDTO(
    val id: String,
    val gymId: String,
    val name: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val gender: String? = null,
    val address: String? = null,
    val message: String? = null,
    val source: String? = null,
    val medium: String? = null,
    val interestedIn: String? = null,
    val notes: String? = null,
    val status: String? = "OPEN", // OPEN, CONTACTED, QUALIFIED, TRIAL, FOLLOW_UP, CONVERTED, CLOSED, LOST
    val enquiryDate: String? = null,
    val followUpAt: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val convertedAt: String? = null,
    val assignedTo: AssignedToDTO? = null,
    val convertedGymUserId: String? = null,
    val convertedMemberId: String? = null
)

@Serializable
data class AssignedToDTO(
    val id: String,
    val fullName: String,
    val phone: String
)

@Serializable
data class CreateEnquiryRequest(
    val gymId: String,
    val name: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val gender: String? = "male",
    val address: String? = null,
    val message: String? = null,
    val source: String? = null,
    val medium: String? = null,
    val interestedIn: String? = null,
    val notes: String? = null,
    val assignedToUserId: String? = null,
    val enquiryDate: String? = null,
    val followUpAt: String? = null
)

@Serializable
data class UpdateEnquiryRequest(
    val name: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val gender: String? = null,
    val address: String? = null,
    val message: String? = null,
    val source: String? = null,
    val medium: String? = null,
    val interestedIn: String? = null,
    val notes: String? = null,
    val assignedToUserId: String? = null,
    val enquiryDate: String? = null,
    val followUpAt: String? = null,
    val status: String? = null,
    val gymId: String? = null
)


@Serializable
data class EnquiryStats(
    val totalEnquiry: Int = 0,
    val converted: Int = 0,
    val pending: Int = 0
)
