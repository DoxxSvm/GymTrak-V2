package `in`.gym.trak.studio.features.member.workout

import `in`.gym.trak.studio.data.model.MemberWorkoutHistoryItemDTO
import `in`.gym.trak.studio.data.repository.OwnerDashboardRepository
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.network.BaseScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class MemberWorkoutHistoryScreenModel(
    private val repository: OwnerDashboardRepository = OwnerDashboardRepository(),
) : BaseScreenModel() {

    private val _items = MutableStateFlow<List<MemberWorkoutHistoryItemDTO>>(emptyList())
    val items = _items.asStateFlow()

    private val _total = MutableStateFlow(0)
    val total = _total.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore = _hasMore.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()

    /** Next 0-based page index to fetch (API: `GET workouts/history?page=…`). */
    private var nextPageIndex = 0
    private val pageSize = 20
    private var dateFrom: String? = null
    private var dateTo: String? = null

    fun setDateRange(from: String?, to: String?) {
        dateFrom = from
        dateTo = to
    }

    fun resetAndLoad(showFullLoader: Boolean = true) {
        nextPageIndex = 0
        _items.value = emptyList()
        _total.value = 0
        _hasMore.value = true
        loadPage(page = 0, append = false, showFullLoader = showFullLoader, isRefresh = false)
    }

    fun refresh() {
        nextPageIndex = 0
        _hasMore.value = true
        loadPage(page = 0, append = false, showFullLoader = false, isRefresh = true)
    }

    fun loadNextPage() {
        if (_isLoadingMore.value || !_hasMore.value) return
        loadPage(page = nextPageIndex, append = true, showFullLoader = false, isRefresh = false)
    }

    private fun loadPage(
        page: Int,
        append: Boolean,
        showFullLoader: Boolean,
        isRefresh: Boolean,
    ) {
        val token = SessionManager.accessToken
        if (token.isBlank()) return

        if (isRefresh) _isRefreshing.value = true
        if (append) _isLoadingMore.value = true

        val gymId = SessionManager.gymId.ifBlank { null }
        executeApi(
            apiCall = {
                repository.getWorkoutHistory(
                    accessToken = token,
                    gymId = gymId,
                    page = page,
                    limit = pageSize,
                    from = dateFrom,
                    to = dateTo,
                    completed = null,
                )
            },
            onSuccess = { response ->
                val newItems = if (append) _items.value + response.items else response.items
                _items.value = newItems
                _total.value = response.total
                when {
                    response.items.isEmpty() -> _hasMore.value = false
                    response.total > 0 -> _hasMore.value = newItems.size < response.total
                    else -> _hasMore.value = response.items.size >= pageSize
                }
                nextPageIndex = page + 1
                if (isRefresh) _isRefreshing.value = false
                if (append) _isLoadingMore.value = false
            },
            onError = {
                if (isRefresh) _isRefreshing.value = false
                if (append) _isLoadingMore.value = false
            },
            showGlobalLoader = showFullLoader,
        )
    }
}
