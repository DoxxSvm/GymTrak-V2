package `in`.gym.trak.studio.viewmodel.dashboard

import cafe.adriel.voyager.core.model.screenModelScope
import `in`.gym.trak.studio.data.model.NotificationDTO
import `in`.gym.trak.studio.data.repository.OwnerDashboardRepository
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.getCurrentTimeMillis
import `in`.gym.trak.studio.network.ApiResult
import `in`.gym.trak.studio.network.BaseScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class NotificationScreenModel(
    private val repository: OwnerDashboardRepository = OwnerDashboardRepository(),
) : BaseScreenModel() {

    private val _notifications = MutableStateFlow<List<NotificationDTO>>(emptyList())
    val notifications = _notifications.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore = _loadingMore.asStateFlow()

    private val _markingReadIds = MutableStateFlow<Set<String>>(emptySet())
    val markingReadIds = _markingReadIds.asStateFlow()

    private var nextCursor: String? = null
    private var canLoadMore = true

    fun loadNotifications(refresh: Boolean = true, showFullLoader: Boolean = true) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isBlank() || token.isBlank()) {
            showError("Please sign in again to view notifications.")
            return
        }

        if (!refresh) {
            if (!canLoadMore || _loadingMore.value || isLoading.value) return
            _loadingMore.value = true
        } else {
            nextCursor = null
            canLoadMore = true
        }

        val cursorToLoad = if (refresh) null else nextCursor

        screenModelScope.launch {
            if (showFullLoader && refresh) showLoader()
            clearError()
            try {
                when (
                    val result = repository.getNotifications(
                        accessToken = token,
                        gymId = gymId,
                        limit = 20,
                        cursor = cursorToLoad,
                    )
                ) {
                    is ApiResult.Success -> {
                        val pageItems = result.data.items
                        _notifications.value = if (refresh) {
                            pageItems
                        } else {
                            _notifications.value + pageItems
                        }
                        nextCursor = result.data.nextCursor
                        canLoadMore = !result.data.nextCursor.isNullOrBlank()
                    }

                    is ApiResult.Error -> showError(result.message)
                    else -> {}
                }
            } finally {
                if (showFullLoader && refresh) hideLoader()
                _loadingMore.value = false
            }
        }
    }

    fun loadMoreNotifications() {
        loadNotifications(refresh = false, showFullLoader = false)
    }

    fun refresh() {
        loadNotifications(refresh = true, showFullLoader = false)
    }

    fun markNotificationRead(
        notificationId: String,
        onSuccess: () -> Unit = {},
    ) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isBlank() || token.isBlank() || notificationId.isBlank()) return

        val existing = _notifications.value.find { it.id == notificationId }
        if (existing?.readAt != null) {
            onSuccess()
            return
        }

        if (_markingReadIds.value.contains(notificationId)) return
        _markingReadIds.value = _markingReadIds.value + notificationId

        val readAtIso = Instant.fromEpochMilliseconds(getCurrentTimeMillis())
            .toLocalDateTime(TimeZone.UTC)
            .let { "${it.year}-${it.monthNumber.toString().padStart(2, '0')}-${it.dayOfMonth.toString().padStart(2, '0')}T${it.hour.toString().padStart(2, '0')}:${it.minute.toString().padStart(2, '0')}:${it.second.toString().padStart(2, '0')}Z" }

        _notifications.value = _notifications.value.map { item ->
            if (item.id == notificationId) item.copy(readAt = readAtIso) else item
        }

        screenModelScope.launch {
            try {
                when (
                    val result = repository.markNotificationRead(
                        accessToken = token,
                        notificationId = notificationId,
                        gymId = gymId,
                    )
                ) {
                    is ApiResult.Success -> onSuccess()
                    is ApiResult.Error -> {
                        _notifications.value = _notifications.value.map { item ->
                            if (item.id == notificationId) item.copy(readAt = null) else item
                        }
                        showError(result.message)
                    }
                    else -> {}
                }
            } finally {
                _markingReadIds.value = _markingReadIds.value - notificationId
            }
        }
    }
}
