package `in`.gym.trak.studio.viewmodel.member

import cafe.adriel.voyager.core.model.screenModelScope
import `in`.gym.trak.studio.data.model.MemberOnboardingDraft
import `in`.gym.trak.studio.data.model.MemberOnboardingRequest
import `in`.gym.trak.studio.data.model.MemberOnboardingResponse
import `in`.gym.trak.studio.data.model.MemberWellnessPayload
import `in`.gym.trak.studio.data.repository.OwnerDashboardRepository
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.network.ApiResult
import `in`.gym.trak.studio.network.BaseScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MemberOnboardingScreenModel(
    private val repository: OwnerDashboardRepository = OwnerDashboardRepository()
) : BaseScreenModel() {

    private val _wellness = MutableStateFlow<MemberWellnessPayload?>(null)
    val wellness = _wellness.asStateFlow()

    private val _onboardingResponse = MutableStateFlow<MemberOnboardingResponse?>(null)
    val onboardingResponse = _onboardingResponse.asStateFlow()

    private val _submitDone = MutableStateFlow(false)
    val submitDone = _submitDone.asStateFlow()

    fun submitMemberProfile(draft: MemberOnboardingRequest, bearerToken: String) {
        screenModelScope.launch {
            _submitDone.value = false
            _wellness.value = null
            _onboardingResponse.value = null
            showLoader()
            clearError()
            val bearer = SessionManager.accessToken.takeIf { it.isNotBlank() } ?: bearerToken
            when (val result = repository.completeMemberProfile(bearer, draft)) {
                is ApiResult.Success -> {
                    hideLoader()
                    applySession(result.data)
                    _wellness.value = result.data.wellness
                    _onboardingResponse.value = result.data
                    _submitDone.value = true
                }
                is ApiResult.Error -> {
                    hideLoader()
                    showError(result.message)
                }
                else -> hideLoader()
            }
        }
    }

    private fun applySession(response: MemberOnboardingResponse) {
        val access = response.access_token ?: return
        SessionManager.accessToken = access
        SessionManager.refreshToken = response.refresh_token ?: ""
        SessionManager.gymId = response.gym_id ?: response.user?.gym_id ?: ""
        SessionManager.userId = response.user?.id ?: SessionManager.userId
        SessionManager.userRole = response.user?.role ?: "member"
        SessionManager.isLoggedIn = true
    }
}
