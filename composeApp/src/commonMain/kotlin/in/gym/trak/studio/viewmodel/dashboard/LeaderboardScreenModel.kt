package `in`.gym.trak.studio.viewmodel.dashboard

import cafe.adriel.voyager.core.model.screenModelScope
import `in`.gym.trak.studio.data.model.LeaderboardEntryDTO
import `in`.gym.trak.studio.data.repository.OwnerDashboardRepository
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.network.ApiResult
import `in`.gym.trak.studio.network.BaseScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LeaderboardScreenModel(
    private val repository: OwnerDashboardRepository = OwnerDashboardRepository()
) : BaseScreenModel() {

    private val _entries = MutableStateFlow<List<LeaderboardEntryDTO>>(emptyList())
    val entries = _entries.asStateFlow()

    private val _page = MutableStateFlow(1)
    val page = _page.asStateFlow()

    private val _limit = MutableStateFlow(20)
    val limit = _limit.asStateFlow()

    private val _total = MutableStateFlow(0)
    val total = _total.asStateFlow()

    private val _type = MutableStateFlow("attendance")
    val type = _type.asStateFlow()

    fun loadLeaderboard(
        type: String,
        page: Int = 1,
        limit: Int = 20,
        showFullLoader: Boolean = true
    ) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isBlank() || token.isBlank()) {
            showError("Missing gym session. Please login again.")
            return
        }

        screenModelScope.launch {
            if (showFullLoader) showLoader()
            clearError()
            try {
                when (
                    val result = repository.getLeaderboard(
                        gymId = gymId,
                        accessToken = token,
                        type = type,
                        page = page,
                        limit = limit
                    )
                ) {
                    is ApiResult.Success -> {
                        _entries.value = result.data.data
                        _page.value = result.data.page
                        _limit.value = result.data.limit
                        _total.value = result.data.total
                        _type.value = result.data.type
                    }

                    is ApiResult.Error -> showError(result.message)
                    else -> {}
                }
            } finally {
                if (showFullLoader) hideLoader()
            }
        }
    }
}
