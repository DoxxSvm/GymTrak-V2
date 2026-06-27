package `in`.gym.trak.studio.features.member

import `in`.gym.trak.studio.data.model.CompleteWorkoutRequest
import `in`.gym.trak.studio.data.model.UpdateWorkoutLegacyRequest
import `in`.gym.trak.studio.data.model.WorkoutDTO
import `in`.gym.trak.studio.data.model.WorkoutDetailResponse
import `in`.gym.trak.studio.data.repository.OwnerDashboardRepository
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.network.BaseScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MemberWorkoutScreenModel(
    private val repository: OwnerDashboardRepository = OwnerDashboardRepository()
) : BaseScreenModel() {

    private val _workouts = MutableStateFlow<List<WorkoutDTO>>(emptyList())
    val workouts = _workouts.asStateFlow()

    private val _workoutDetail = MutableStateFlow<WorkoutDetailResponse?>(null)
    val workoutDetail = _workoutDetail.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private var lastWorkoutCreatedBy: String = "all"

    fun loadWorkouts(showFullLoader: Boolean = true, createdBy: String? = null) {
        val token = SessionManager.accessToken
        if (token.isBlank()) return

        if (createdBy != null) lastWorkoutCreatedBy = createdBy
        val gymId = SessionManager.gymId.ifBlank { null }

        if (!showFullLoader) _isRefreshing.value = true
        executeApi(
            apiCall = {
                repository.getPersonalWorkouts(
                    accessToken = token,
                    gymId = gymId,
                    createdBy = lastWorkoutCreatedBy,
                )
            },
            onSuccess = { response ->
                _workouts.value = response
                _isRefreshing.value = false
            },
            onError = { _isRefreshing.value = false },
            showGlobalLoader = showFullLoader
        )
    }

    fun refresh() = loadWorkouts(showFullLoader = false)

    fun loadWorkoutDetail(
        workoutId: String,
        showGlobalLoader: Boolean = true,
        onSuccess: ((WorkoutDetailResponse) -> Unit)? = null,
    ) {
        val token = SessionManager.accessToken
        if (token.isBlank() || workoutId.isBlank()) return
        executeApi(
            apiCall = { repository.getWorkoutDetail(token, workoutId) },
            onSuccess = {
                _workoutDetail.value = it
                onSuccess?.invoke(it)
            },
            showGlobalLoader = showGlobalLoader
        )
    }

    fun clearWorkoutDetail() {
        _workoutDetail.value = null
    }

    fun startWorkout(workoutId: String, onSuccess: (WorkoutDetailResponse) -> Unit = {}) {
        val token = SessionManager.accessToken
        val gymId = SessionManager.gymId
        if (token.isEmpty()) return

        executeApi(
            apiCall = { repository.startWorkout(token, workoutId, gymId.ifEmpty { null }) },
            onSuccess = { response -> onSuccess(response) }
        )
    }



    fun stopWorkout(workoutId: String, onSuccess: (WorkoutDetailResponse) -> Unit = {}) {
        val token = SessionManager.accessToken
        val gymId = SessionManager.gymId
        if (token.isEmpty()) return

        executeApi(
            apiCall = { repository.stopWorkout(token, workoutId, gymId.ifEmpty { null }) },
            onSuccess = { response -> onSuccess(response) }
        )
    }

    fun pauseWorkout(
        workoutId: String,
        onSuccess: () -> Unit = {},
        onError: () -> Unit = {},
    ) {
        val token = SessionManager.accessToken
        val gymId = SessionManager.gymId
        if (token.isEmpty() || workoutId.isBlank()) return

        executeApi(
            apiCall = { repository.pauseWorkout(token, workoutId, gymId.ifEmpty { null }) },
            onSuccess = { onSuccess() },
            onError = { onError() },
            showGlobalLoader = false,
        )
    }

    fun resumeWorkout(
        workoutId: String,
        onSuccess: () -> Unit = {},
        onError: () -> Unit = {},
    ) {
        val token = SessionManager.accessToken
        val gymId = SessionManager.gymId
        if (token.isEmpty() || workoutId.isBlank()) return

        executeApi(
            apiCall = { repository.resumeWorkout(token, workoutId, gymId.ifEmpty { null }) },
            onSuccess = { onSuccess() },
            onError = { onError() },
            showGlobalLoader = false,
        )
    }

    fun stopWorkout(request: CompleteWorkoutRequest, onSuccess: (WorkoutDetailResponse) -> Unit = {}) {
        val token = SessionManager.accessToken
        val gymId = SessionManager.gymId
        if (token.isEmpty()) return

        executeApi(
            apiCall = { repository.stopWorkout(token, request, gymId.ifEmpty { null }) },
            onSuccess = { response -> onSuccess(response) }
        )
    }

    fun updateWorkoutLegacy(
        request: UpdateWorkoutLegacyRequest,
        onSuccess: (WorkoutDetailResponse) -> Unit = {}
    ) {
        val token = SessionManager.accessToken
        val gymId = SessionManager.gymId
        if (token.isBlank()) return

        executeApi(
            apiCall = { repository.updateWorkoutLegacy(token, request, gymId.ifEmpty { null }) },
            onSuccess = onSuccess
        )
    }

    fun deleteWorkoutLegacy(
        workoutId: String,
        onSuccess: () -> Unit = {}
    ) {
        val token = SessionManager.accessToken
        val gymId = SessionManager.gymId
        if (token.isBlank()) return

        executeApi(
            apiCall = { repository.deleteWorkoutLegacy(token, workoutId, gymId.ifEmpty { null }) },
            onSuccess = { onSuccess() }
        )
    }
}
