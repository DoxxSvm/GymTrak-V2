package `in`.gym.trak.studio.tracking

/**
 * Shared data model for live tracking state.
 * Passed between shared business logic and platform-specific implementations.
 */
data class TrackingData(
    val title: String,
    val eta: String,
    val progress: Float,          // 0.0 to 1.0
    val status: String,
    val statusDetail: String = "",
    val userName: String? = null,
    val actionLabel: String = "Cancel"
)
