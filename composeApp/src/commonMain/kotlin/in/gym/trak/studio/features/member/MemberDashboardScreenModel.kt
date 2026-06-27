package `in`.gym.trak.studio.features.member

import cafe.adriel.voyager.core.model.screenModelScope
import `in`.gym.trak.studio.data.model.MemberDashboardResponse
import `in`.gym.trak.studio.data.repository.OwnerDashboardRepository
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.network.ApiResult
import `in`.gym.trak.studio.network.BaseScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MemberDashboardScreenModel(
    private val repository: OwnerDashboardRepository = OwnerDashboardRepository()
) : BaseScreenModel() {

    private val _selectedTab = MutableStateFlow("Home")
    val selectedTab = _selectedTab.asStateFlow()

    private val _memberDashboard = MutableStateFlow<MemberDashboardResponse?>(null)
    val memberDashboard = _memberDashboard.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    init {
        loadMemberDashboard()
    }

    fun onTabSelected(tab: String) {
        _selectedTab.value = tab
        if (tab == "Home" || tab == "Profile") {
            loadMemberDashboard(showFullLoader = tab == "Home")
        }
    }

    fun loadMemberDashboard(showFullLoader: Boolean = true) {
        val token = SessionManager.accessToken
        if (token.isEmpty()) return

        if (!showFullLoader) _isRefreshing.value = true
        val gymId = SessionManager.gymId.takeIf { it.isNotBlank() }
        executeApi(
            apiCall = { repository.getMemberDashboard(token, gymId) },
            onSuccess = { response ->
                _memberDashboard.value = response
                val dashGymId = response.gymId?.takeIf { it.isNotBlank() }.orEmpty()
                val dashGymUserId = response.gymUserId?.takeIf { it.isNotBlank() }
                    ?: response.userId?.takeIf { it.isNotBlank() }
                    ?: ""
                SessionManager.memberDashboardGymId = dashGymId
                SessionManager.memberGymUserId = dashGymUserId
                _isRefreshing.value = false
            },
            onError = { _isRefreshing.value = false },
            showGlobalLoader = showFullLoader
        )
    }

    fun refreshMemberDashboard() = loadMemberDashboard(showFullLoader = false)
}
