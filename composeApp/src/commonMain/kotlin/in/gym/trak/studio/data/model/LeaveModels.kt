package `in`.gym.trak.studio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class CreateLeaveRequest(
    val gymId: String,
    val trainerId: String,
    val leaveType: String,
    val startDate: String,
    val endDate: String,
    val reason: String
)

@Serializable
data class CreateLeaveApiResponse(
    val success: Boolean = false,
    val message: String? = null,
    val data: CreateLeaveData? = null
)

@Serializable
data class CreateLeaveData(
    val id: String? = null,
    val status: String? = null
)

@Serializable
data class LeaveListResponse(
    val success: Boolean,
    val data: List<LeaveDTO>,
    val total: Int,
    val limit: Int,
    val offset: Int
)

@Serializable
data class LeaveDTO(
    val id: String,
    val trainer_id: String,
    val trainer_name: String,
    val leave_type: String,
    val start_date: String,
    val end_date: String,
    val days: Int,
    val status: String
)

@Serializable
data class RejectLeaveRequest(
    val reason: String
)
