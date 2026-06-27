package `in`.gym.trak.studio.viewmodel.member

import cafe.adriel.voyager.core.model.screenModelScope
import `in`.gym.trak.studio.data.model.MemberProfileDetailResponse
import `in`.gym.trak.studio.data.model.MemberProfileUpdateRequest
import `in`.gym.trak.studio.data.repository.AuthRepository
import `in`.gym.trak.studio.data.repository.OwnerDashboardRepository
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.network.ApiResult
import `in`.gym.trak.studio.network.BaseScreenModel
import `in`.gym.trak.studio.data.repository.WorkoutManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MemberProfileScreenModel(
    private val repository: OwnerDashboardRepository = OwnerDashboardRepository()
) : BaseScreenModel() {

    private val _memberDetail = MutableStateFlow<MemberProfileDetailResponse?>(null)
    val memberDetail = _memberDetail.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()
    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage = _isUploadingImage.asStateFlow()

    fun loadProfile(showFullLoader: Boolean = true) {
        val gymIdForQuery = SessionManager.effectiveGymIdForMemberApis()
        val memberIdForPath = SessionManager.effectiveMemberListingIdForApi(SessionManager.userId)
        val token = SessionManager.accessToken
        if (memberIdForPath.isBlank() || token.isBlank()) {
            showError("Session details missing. Please login again.")
            return
        }
        screenModelScope.launch {
            if (showFullLoader) showLoader() else _isRefreshing.value = true
            clearError()
            try {
                when (val result = repository.getMemberProfile(gymIdForQuery, memberIdForPath, token)) {
                    is ApiResult.Success -> _memberDetail.value = result.data
                    is ApiResult.Error -> showError(result.message)
                    else -> {}
                }
            } finally {
                if (showFullLoader) hideLoader() else _isRefreshing.value = false
            }
        }
    }

    fun refresh() {
        loadProfile(showFullLoader = false)
    }

    fun uploadImage(imageBytes: ByteArray, fileName: String, onResult: (String?) -> Unit) {
        val token = SessionManager.accessToken
        if (token.isBlank()) {
            onResult(null)
            return
        }
        _isUploadingImage.value = true
        executeApi(
            apiCall = { repository.uploadImage(token, imageBytes, fileName) },
            onSuccess = { response ->
                val finalUrl = if (response.url.contains("localhost")) {
                    response.url.replace("localhost", "192.168.1.73")
                } else {
                    response.url
                }
                _isUploadingImage.value = false
                onResult(finalUrl)
            },
            onError = {
                _isUploadingImage.value = false
                onResult(null)
            },
            showGlobalLoader = false,
        )
    }

    fun updateProfile(
        request: MemberProfileUpdateRequest,
        onSuccess: (() -> Unit)? = null,
    ) {
        val memberIdForPath = SessionManager.effectiveMemberListingIdForApi(SessionManager.userId)
        val token = SessionManager.accessToken
        if (memberIdForPath.isBlank() || token.isBlank()) {
            showError("Session details missing. Please login again.")
            return
        }

        screenModelScope.launch {
            showLoader()
            clearError()
            try {
                when (
                    val result = repository.updateMemberProfile(
                        memberId = memberIdForPath,
                        accessToken = token,
                        request = request,
                        gymId = SessionManager.effectiveGymIdForMemberApis().takeIf { it.isNotBlank() },
                    )
                ) {
                    is ApiResult.Success -> {
                        _memberDetail.value = result.data
                        showToast("Profile updated successfully")
                        onSuccess?.invoke()
                    }

                    is ApiResult.Error -> showError(result.message)
                    else -> {}
                }
            } finally {
                hideLoader()
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        val refreshToken = SessionManager.refreshToken
        if (refreshToken.isBlank()) {
            SessionManager.clearSession()
            onSuccess()
            return
        }

        screenModelScope.launch {
            showLoader()
            try {
                when (AuthRepository.logout(refreshToken)) {
                    is ApiResult.Success -> {
                        SessionManager.clearSession()
                        onSuccess()
                    }
                    is ApiResult.Error -> {
                        // Keep local logout resilient even when server logout fails.
                        SessionManager.clearSession()
                        onSuccess()
                    }
                    else -> {
                        SessionManager.clearSession()
                        onSuccess()
                    }
                }
            } finally {
                hideLoader()
            }
        }
    }

    fun switchToOwner(onSuccess: () -> Unit) {
        val token = SessionManager.accessToken
        if (token.isBlank()) {
            showError("Session expired. Please login again.")
            return
        }

        screenModelScope.launch {
            showLoader()
            clearError()
            try {
                when (val result = AuthRepository.switchToOwner(token)) {
                    is ApiResult.Success -> {
                        SessionManager.memberGymUserId = ""
                        SessionManager.memberDashboardGymId = ""
                        SessionManager.accessToken = result.data.access_token
                        SessionManager.refreshToken = result.data.refresh_token
                        SessionManager.userRole = "gym_owner"
                        val active = WorkoutManager.activeWorkout.value
                        if (active != null && !active.workoutId.isNullOrBlank()) {
                            // Non-blocking stop workout API call
                            screenModelScope.launch {
                                repository.stopWorkout(token, active.workoutId, SessionManager.gymId ?: "")
                            }
                        }
                        WorkoutManager.stopWorkout()
                        onSuccess()
                    }
                    is ApiResult.Error -> showError(result.message)
                    else -> {}
                }
            } finally {
                hideLoader()
            }
        }
    }
}
