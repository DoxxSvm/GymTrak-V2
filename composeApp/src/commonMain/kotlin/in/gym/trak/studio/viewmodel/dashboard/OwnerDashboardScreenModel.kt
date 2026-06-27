package `in`.gym.trak.studio.viewmodel.dashboard

import cafe.adriel.voyager.core.model.screenModelScope
import `in`.gym.trak.studio.data.model.AddMemberRequest
import `in`.gym.trak.studio.data.model.AttendancePunchResponse
import `in`.gym.trak.studio.data.model.AttendanceQrResponse
import `in`.gym.trak.studio.data.model.BroadcastChannelDTO
import `in`.gym.trak.studio.data.model.BroadcastChannelsResponse
import `in`.gym.trak.studio.data.model.CreateDietCatalogFoodRequest
import `in`.gym.trak.studio.data.model.CreateBroadcastChannelRequest
import `in`.gym.trak.studio.data.model.CreateBroadcastChannelResponse
import `in`.gym.trak.studio.data.model.CreateDietMealRequest
import `in`.gym.trak.studio.data.model.ConsumeDietFoodRequest
import `in`.gym.trak.studio.data.model.CreateEnquiryRequest
import `in`.gym.trak.studio.data.model.CreateExerciseRequest
import `in`.gym.trak.studio.data.model.CreateExpenseRequest
import `in`.gym.trak.studio.data.model.CreateLeaveRequest
import `in`.gym.trak.studio.data.model.AssignMemberPlanRequest
import `in`.gym.trak.studio.data.model.CreatePlanCompatRequest
import `in`.gym.trak.studio.data.model.CreatePlanResponse
import `in`.gym.trak.studio.data.model.CreateProductRequest
import `in`.gym.trak.studio.data.model.CreateSubscriptionCompatRequest
import `in`.gym.trak.studio.data.model.CreateSubscriptionWithBodyRequest
import `in`.gym.trak.studio.data.model.CreateTrainerCompatRequest
import `in`.gym.trak.studio.data.model.CreateTrainerRequest
import `in`.gym.trak.studio.data.model.CreateTrainerSalaryPaymentRequest
import `in`.gym.trak.studio.data.model.GymStaffListRole
import `in`.gym.trak.studio.data.model.CreateSetRequest
import `in`.gym.trak.studio.data.model.CreateWorkoutRequest
import `in`.gym.trak.studio.data.model.CompleteWorkoutRequest
import `in`.gym.trak.studio.data.model.GenericSuccessResponse
import `in`.gym.trak.studio.data.model.CreateOwnedGymRequest
import `in`.gym.trak.studio.data.model.CreateOwnedGymResponse
import `in`.gym.trak.studio.data.model.DashboardPermissions
import `in`.gym.trak.studio.data.model.DeviceTokenRequest
import `in`.gym.trak.studio.data.model.DietCatalogFoodDTO
import `in`.gym.trak.studio.data.model.DietHistoryResponse
import `in`.gym.trak.studio.data.model.DietMealDTO
import `in`.gym.trak.studio.data.model.EnquiryDTO
import `in`.gym.trak.studio.data.model.EnquiryStats
import `in`.gym.trak.studio.data.model.EnrolledSubscription
import `in`.gym.trak.studio.data.model.ExerciseRowDTO
import `in`.gym.trak.studio.data.model.ExpenseDTO
import `in`.gym.trak.studio.data.model.FreezeSubscriptionRequest
import `in`.gym.trak.studio.data.model.LeaveDTO
import `in`.gym.trak.studio.data.model.ExtendPlanPaymentRequest
import `in`.gym.trak.studio.data.model.MemberAttendanceSummaryResponse
import `in`.gym.trak.studio.data.model.MemberDTO
import `in`.gym.trak.studio.data.model.MemberDetailResponse
import `in`.gym.trak.studio.data.model.MemberProfileUpdateRequest
import `in`.gym.trak.studio.data.model.MemberStats
import `in`.gym.trak.studio.data.model.MemberStatisticsResponse
import `in`.gym.trak.studio.data.model.MetadataResponse
import `in`.gym.trak.studio.data.model.GymDetailsDTO
import `in`.gym.trak.studio.data.model.OwnerDashboardNewResponse
import `in`.gym.trak.studio.data.model.PersonalInfoDTO
import `in`.gym.trak.studio.data.model.PaymentAnalyticsResponse
import `in`.gym.trak.studio.data.model.PaymentItemDTO
import `in`.gym.trak.studio.data.model.PlanDTO
import `in`.gym.trak.studio.data.model.ProductDetailDTO
import `in`.gym.trak.studio.data.model.ProfileResponse
import `in`.gym.trak.studio.data.model.ReceivePaymentRequest
import `in`.gym.trak.studio.data.model.RejectLeaveRequest
import `in`.gym.trak.studio.data.model.SetTrainerPasswordRequest
import `in`.gym.trak.studio.data.model.ShopProductDTO
import `in`.gym.trak.studio.data.model.SubscriptionDTO
import `in`.gym.trak.studio.data.model.SwitchToMemberRequest
import `in`.gym.trak.studio.data.model.TrainerDTO
import `in`.gym.trak.studio.data.model.TrainerDetailResponse
import `in`.gym.trak.studio.data.model.UnfreezeSubscriptionRequest
import `in`.gym.trak.studio.data.model.UpdateEnquiryRequest
import `in`.gym.trak.studio.data.model.UpdateExpenseRequest
import `in`.gym.trak.studio.data.model.UpdateOwnerProfileRequest
import `in`.gym.trak.studio.data.model.UpdateProductRequest
import `in`.gym.trak.studio.data.model.UpdateProfileRequest
import `in`.gym.trak.studio.data.model.UpdateBroadcastChannelRequest
import `in`.gym.trak.studio.data.model.UpdateTrainerProfileRequest
import `in`.gym.trak.studio.data.model.UpdateTrainerRequest
import `in`.gym.trak.studio.data.model.UpdateSetRequest
import `in`.gym.trak.studio.data.model.UpdateWorkoutLegacyRequest
import `in`.gym.trak.studio.data.model.UserGymsListResponse
import `in`.gym.trak.studio.data.model.UserOwnedGymDTO
import `in`.gym.trak.studio.data.model.BroadcastMemberDTO
import `in`.gym.trak.studio.data.model.BroadcastMessageDTO
import `in`.gym.trak.studio.data.model.BroadcastChannelDetailDTO
import `in`.gym.trak.studio.data.model.WorkoutDTO
import `in`.gym.trak.studio.data.model.WorkoutDetailExerciseDTO
import `in`.gym.trak.studio.data.model.WorkoutCompletionResponse
import `in`.gym.trak.studio.data.model.WorkoutDetailResponse
import `in`.gym.trak.studio.data.model.UpdateWorkoutCompletionRequest
import `in`.gym.trak.studio.data.model.UpdateWhatsAppAutomationRequest
import `in`.gym.trak.studio.data.model.WhatsAppAutomationResponse
import `in`.gym.trak.studio.data.model.WorkoutDetailSetDTO
import `in`.gym.trak.studio.data.model.hasUsableQrData
import `in`.gym.trak.studio.data.repository.AuthRepository
import `in`.gym.trak.studio.data.repository.OwnerDashboardRepository
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.data.repository.WorkoutManager
import `in`.gym.trak.studio.features.shop.ProductRefreshBus
import `in`.gym.trak.studio.getCurrentTimeMillis
import `in`.gym.trak.studio.network.ApiResult
import `in`.gym.trak.studio.network.BaseScreenModel
import `in`.gym.trak.studio.network.NetworkMonitor
import `in`.gym.trak.studio.network.isNetworkAvailable
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OwnerDashboardScreenModel(
    private val repository:
    OwnerDashboardRepository = OwnerDashboardRepository()
) : BaseScreenModel() {

    /** Which owner profile image field is being uploaded (for UI loaders on Profile tab vs logo). */
    enum class ActiveImageUploadTarget {
        ProfilePhoto,
        GymLogo
    }
    private var dashboardBootstrapComplete = false
    private var dashboardBootstrapInFlight = false
    private var retryDashboardAfterRefresh = false
    private val pendingDashboardActions = mutableListOf<() -> Unit>()

    private fun runAfterDashboardReady(action: () -> Unit) {
        if (dashboardBootstrapComplete) {
            action()
            return
        }
        pendingDashboardActions.add(action)
        if (!dashboardBootstrapInFlight) {
            loadDashboardData()
        }
    }

    private fun flushPendingDashboardActions() {
        if (pendingDashboardActions.isEmpty()) return
        val queued = pendingDashboardActions.toList()
        pendingDashboardActions.clear()
        queued.forEach { it.invoke() }
    }

    private inline fun queueUntilDashboardReady(crossinline retry: () -> Unit): Boolean {
        if (dashboardBootstrapComplete) return false
        runAfterDashboardReady { retry() }
        return true
    }
//
//    private fun <T> executeApi(
//        apiCall: suspend () -> ApiResult<T>,
//        onSuccess: (T) -> Unit,
//        onError: ((String) -> Unit)? = null,
//        gateWithDashboard: Boolean = true
//    ) {
//        if (!gateWithDashboard) {
//            super.executeApi(apiCall, onSuccess, onError)
//            return
//        }
//        runAfterDashboardReady {
//            super.executeApi(apiCall, onSuccess, onError)
//        }
//    }

    private val _dashboardData = MutableStateFlow<OwnerDashboardNewResponse?>(null)
    val dashboardData = _dashboardData.asStateFlow()

    /** Bumped after owner permissions are persisted so UI recomposes (SessionManager is not observable). */
    private val _permissionsVersion = MutableStateFlow(0)
    val permissionsVersion = _permissionsVersion.asStateFlow()
    private val _selectedTab = MutableStateFlow("")
    val selectedTab = _selectedTab.asStateFlow()

    // Member list state
    private val _members = MutableStateFlow<List<MemberDTO>>(emptyList())
    val members = _members.asStateFlow()

    private val _memberStats = MutableStateFlow<MemberStats?>(null)
    val memberStats = _memberStats.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All Member")
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore = _isLoadingMore.asStateFlow()

    private var currentPage = 1
    private var canLoadMore = true
    private var searchJob: Job? = null
    private var tokenRefreshJob: Job? = null

    // Profile state
    private val _profileData = MutableStateFlow<ProfileResponse?>(null)
    val profileData = _profileData.asStateFlow()

    private val _userGyms = MutableStateFlow<List<UserOwnedGymDTO>>(emptyList())
    val userGyms = _userGyms.asStateFlow()

    private val _userGymsLoading = MutableStateFlow(false)
    val userGymsLoading = _userGymsLoading.asStateFlow()

    /** Mirrors [SessionManager.gymId] so Compose can recompose when the active gym changes. */
    private val _activeGymId = MutableStateFlow(SessionManager.gymId)
    val activeGymId = _activeGymId.asStateFlow()

    // Trainer state
    private val _trainers = MutableStateFlow<List<TrainerDTO>>(emptyList())
    val trainers = _trainers.asStateFlow()

    private val _trainersListLoading = MutableStateFlow(false)
    val trainersListLoading = _trainersListLoading.asStateFlow()

    private val _trainerSearchQuery = MutableStateFlow("")
    val trainerSearchQuery = _trainerSearchQuery.asStateFlow()

    /** Role filter for GET /trainers and refresh after mutations on this screen model. */
    private var trainersListRole: String = GymStaffListRole.TRAINER

    fun setTrainersListRole(role: String) {
        trainersListRole = role.ifBlank { GymStaffListRole.TRAINER }
    }

    private val _trainerDetail = MutableStateFlow<TrainerDetailResponse?>(null)
    val trainerDetail = _trainerDetail.asStateFlow()

    // Shared Upload state
    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage = _isUploadingImage.asStateFlow()

    private val _uploadedAvatarUrl = MutableStateFlow<String?>(null)
    val uploadedAvatarUrl = _uploadedAvatarUrl.asStateFlow()

    private val _activeImageUploadTarget = MutableStateFlow<ActiveImageUploadTarget?>(null)
    val activeImageUploadTarget = _activeImageUploadTarget.asStateFlow()

    // Enquiries state
    private val _enquiries = MutableStateFlow<List<EnquiryDTO>>(emptyList())
    val enquiries = _enquiries.asStateFlow()

    private val _enquiryStats = MutableStateFlow<EnquiryStats?>(null)
    val enquiryStats = _enquiryStats.asStateFlow()

    private val _enquirySearchQuery = MutableStateFlow("")
    val enquirySearchQuery = _enquirySearchQuery.asStateFlow()

    private val _selectedEnquiryStatus = MutableStateFlow<String?>(null)
    val selectedEnquiryStatus = _selectedEnquiryStatus.asStateFlow()

    private val _plans = MutableStateFlow<List<PlanDTO>>(emptyList())
    val plans = _plans.asStateFlow()
    private val _planSubscriptionCount = MutableStateFlow(0)
    val planSubscriptionCount = _planSubscriptionCount.asStateFlow()

    private val _plansLoading = MutableStateFlow(false)
    val plansLoading = _plansLoading.asStateFlow()

    private val _planDetail = MutableStateFlow<CreatePlanResponse?>(null)
    val planDetail = _planDetail.asStateFlow()

    private val _enrolledMembers = MutableStateFlow<List<EnrolledSubscription>>(emptyList())
    val enrolledMembers = _enrolledMembers.asStateFlow()

    private val _memberDetail = MutableStateFlow<MemberDetailResponse?>(null)
    val memberDetail = _memberDetail.asStateFlow()

    private val _memberAttendanceSummary = MutableStateFlow<MemberAttendanceSummaryResponse?>(null)
    val memberAttendanceSummary = _memberAttendanceSummary.asStateFlow()

    private val _attendanceQr = MutableStateFlow<AttendanceQrResponse?>(null)
    val attendanceQr = _attendanceQr.asStateFlow()

    private val _attendanceQrLoading = MutableStateFlow(false)
    val attendanceQrLoading = _attendanceQrLoading.asStateFlow()

    private val _attendancePunchLoading = MutableStateFlow(false)
    val attendancePunchLoading = _attendancePunchLoading.asStateFlow()

    private val _attendancePunchResult = MutableStateFlow<AttendancePunchResponse?>(null)
    val attendancePunchResult = _attendancePunchResult.asStateFlow()

    private val _broadcastChannels = MutableStateFlow<List<BroadcastChannelDTO>>(emptyList())
    val broadcastChannels = _broadcastChannels.asStateFlow()

    private val _broadcastChannelsLoading = MutableStateFlow(false)
    val broadcastChannelsLoading = _broadcastChannelsLoading.asStateFlow()

    private val _broadcastChannelCreating = MutableStateFlow(false)
    val broadcastChannelCreating = _broadcastChannelCreating.asStateFlow()

    private val _broadcastMembers = MutableStateFlow<List<BroadcastMemberDTO>>(emptyList())
    val broadcastMembers = _broadcastMembers.asStateFlow()

    private val _broadcastMembersLoading = MutableStateFlow(false)
    val broadcastMembersLoading = _broadcastMembersLoading.asStateFlow()

    private val _whatsAppAutomation = MutableStateFlow<WhatsAppAutomationResponse?>(null)
    val whatsAppAutomation = _whatsAppAutomation.asStateFlow()

    private val _whatsAppAutomationLoading = MutableStateFlow(false)
    val whatsAppAutomationLoading = _whatsAppAutomationLoading.asStateFlow()

    private val _whatsAppAutomationSaving = MutableStateFlow(false)
    val whatsAppAutomationSaving = _whatsAppAutomationSaving.asStateFlow()

    private val _subscriptions = MutableStateFlow<List<SubscriptionDTO>>(emptyList())
    val subscriptions = _subscriptions.asStateFlow()

    private val _paymentAnalytics = MutableStateFlow<PaymentAnalyticsResponse?>(null)
    val paymentAnalytics = _paymentAnalytics.asStateFlow()

    private val _paymentAnalyticsLoading = MutableStateFlow(false)
    val paymentAnalyticsLoading = _paymentAnalyticsLoading.asStateFlow()

    private val _paymentAnalyticsError = MutableStateFlow<String?>(null)
    val paymentAnalyticsError = _paymentAnalyticsError.asStateFlow()

    private var paymentAnalyticsRequestRange: String? = null

    private val _payments = MutableStateFlow<List<PaymentItemDTO>>(emptyList())
    val payments = _payments.asStateFlow()

    /** GET /payments scoped with [loadMemberPaymentHistory] `memberId` (does not mutate [payments]). */
    private val _memberPaymentHistoryItems = MutableStateFlow<List<PaymentItemDTO>>(emptyList())
    val memberPaymentHistoryItems = _memberPaymentHistoryItems.asStateFlow()

    private val _memberPaymentHistoryLoading = MutableStateFlow(false)
    val memberPaymentHistoryLoading = _memberPaymentHistoryLoading.asStateFlow()

    private val _workouts = MutableStateFlow<List<WorkoutDTO>>(emptyList())
    val workouts = _workouts.asStateFlow()

    private val _memberDietMeals = MutableStateFlow<List<DietMealDTO>>(emptyList())
    val memberDietMeals = _memberDietMeals.asStateFlow()

    private val _dietMealsLoading = MutableStateFlow(false)
    val dietMealsLoading = _dietMealsLoading.asStateFlow()

    private val _memberDietHistory = MutableStateFlow<DietHistoryResponse?>(null)
    val memberDietHistory = _memberDietHistory.asStateFlow()

    private val _dietHistoryLoading = MutableStateFlow(false)
    val dietHistoryLoading = _dietHistoryLoading.asStateFlow()

    private val _dietHistoryRefreshing = MutableStateFlow(false)
    val dietHistoryRefreshing = _dietHistoryRefreshing.asStateFlow()

    private val _memberStatistics = MutableStateFlow<MemberStatisticsResponse?>(null)
    val memberStatistics = _memberStatistics.asStateFlow()

    private val _memberStatisticsLoading = MutableStateFlow(false)
    val memberStatisticsLoading = _memberStatisticsLoading.asStateFlow()

    private val _memberStatisticsRefreshing = MutableStateFlow(false)
    val memberStatisticsRefreshing = _memberStatisticsRefreshing.asStateFlow()

    private val _dietCatalogFoods = MutableStateFlow<List<DietCatalogFoodDTO>>(emptyList())
    val dietCatalogFoods = _dietCatalogFoods.asStateFlow()

    private val _dietCatalogLoading = MutableStateFlow(false)
    val dietCatalogLoading = _dietCatalogLoading.asStateFlow()

    private val _expenses = MutableStateFlow<List<ExpenseDTO>>(emptyList())
    val expenses = _expenses.asStateFlow()

    private val _expensesLoading = MutableStateFlow(false)
    val expensesLoading = _expensesLoading.asStateFlow()

    private val _totalExpenseAmount = MutableStateFlow(0L)
    val totalExpenseAmount = _totalExpenseAmount.asStateFlow()
    private val _lastMonthPer = MutableStateFlow(0.0)
    val lastMonthPer = _lastMonthPer.asStateFlow()
    private val _exercises = MutableStateFlow<List<ExerciseRowDTO>>(emptyList())
    val exercises = _exercises.asStateFlow()

    private val _metadata = MutableStateFlow<MetadataResponse?>(null)
    val metadata = _metadata.asStateFlow()

    private val _shopProducts = MutableStateFlow<List<ShopProductDTO>>(emptyList())
    val shopProducts = _shopProducts.asStateFlow()
    private val _favoriteProductIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteProductIds = _favoriteProductIds.asStateFlow()

    private val _shopSearchQuery = MutableStateFlow("")
    val shopSearchQuery = _shopSearchQuery.asStateFlow()

    private val _shopLoadingMore = MutableStateFlow(false)
    val shopLoadingMore = _shopLoadingMore.asStateFlow()
    private val _shopRefreshing = MutableStateFlow(false)
    val shopRefreshing = _shopRefreshing.asStateFlow()

    private var shopPage = 1
    private var shopCanLoadMore = true
    private var shopSearchJob: Job? = null

    private val _productDetail = MutableStateFlow<ProductDetailDTO?>(null)
    val productDetail = _productDetail.asStateFlow()

    private val _leaves = MutableStateFlow<List<LeaveDTO>>(emptyList())
    val leaves = _leaves.asStateFlow()

    private val _leavesLoading = MutableStateFlow(false)
    val leavesLoading = _leavesLoading.asStateFlow()

    private var leavesOffset = 0
    private var canLoadMoreLeaves = true

    init {
        startTokenRefreshCycle()
    }

    // ─── Token Refresh ────────────────────────────────────────────────────────
    private fun startTokenRefreshCycle() {
        tokenRefreshJob?.cancel()
        tokenRefreshJob = screenModelScope.launch {
            // First call on fresh app open
            if (SessionManager.shouldRefreshToken()) {
                performTokenRefresh()
            }

            // Periodic polling every 10 minutes
            while (true) {
                delay(SessionManager.REFRESH_INTERVAL_MS)
                if (SessionManager.shouldRefreshToken()) {
                    performTokenRefresh()
                }
            }
        }
    }

    private suspend fun performTokenRefresh() {
        val storedRefreshToken = SessionManager.refreshToken
        if (storedRefreshToken.isEmpty()) return

        val result = AuthRepository.refreshToken(storedRefreshToken)
        when (result) {
            is ApiResult.Success -> {
                val data = result.data
                SessionManager.accessToken = data.accessToken
                SessionManager.refreshToken = data.refreshToken
                if (!data.gymId.isNullOrEmpty()) {
                    SessionManager.gymId = data.gymId
                    _activeGymId.value = data.gymId
                }
                SessionManager.lastRefreshTimestamp = getCurrentTimeMillis()
                SessionManager.sessionRefreshDoneForCurrentProcess = true
                println("TOKEN_REFRESH: Success — new token obtained.")

                if (!dashboardBootstrapComplete) {
                    if (dashboardBootstrapInFlight) {
                        // Current dashboard call might still be using stale token; retry once it ends.
                        retryDashboardAfterRefresh = true
                    } else if (pendingDashboardActions.isNotEmpty()) {
                        loadDashboardData()
                    }
                }
            }

            is ApiResult.Error -> {
                println("TOKEN_REFRESH: Failed — ${result.message}")
            }

            else -> {}
        }
    }

    // ─── Dashboard ────────────────────────────────────────────────────────────

    /**
     * Loads GET /gyms, persists a valid [SessionManager.gymId] (saved id if still in list, else first gym),
     * then loads the owner dashboard. Call on dashboard entry instead of [loadDashboardData] alone.
     */
    fun bootstrapDashboardEntry() {
        val token = SessionManager.accessToken
        if (token.isEmpty()) {
            showError("Please sign in again to access the dashboard.")
            return
        }
        _userGymsLoading.value = true
        executeApi(
            apiCall = { repository.listUserGyms(token) },
            onSuccess = { response ->
                _userGymsLoading.value = false
                applyUserGymsFromResponse(response)
                loadDashboardData()
            },
            onError = { message ->
                _userGymsLoading.value = false
                _activeGymId.value = SessionManager.gymId
                if (SessionManager.gymId.isNotBlank()) {
                    loadDashboardData()
                } else {
                    showError(message)
                }
            },
            showGlobalLoader = false
        )
    }

    /** Applies gyms list + syncs persisted gym selection; returns whether [SessionManager.gymId] changed. */
    private fun applyUserGymsFromResponse(response: UserGymsListResponse): Boolean {
        val list = response.gyms.takeIf { it.isNotEmpty() } ?: response.data ?: emptyList()
        _userGyms.value = list
        return syncPersistedGymSelection(list)
    }

    /**
     * Keeps [SessionManager.gymId] if it appears in [gyms]; otherwise selects the first gym.
     * Persists to storage via [SessionManager.gymId] setter.
     */
    private fun syncPersistedGymSelection(gyms: List<UserOwnedGymDTO>): Boolean {
        if (gyms.isEmpty()) {
            _activeGymId.value = SessionManager.gymId
            return false
        }
        val current = SessionManager.gymId
        val ids = gyms.mapNotNull { it.id.takeIf { id -> id.isNotBlank() } }.toSet()
        val resolved = when {
            current.isNotBlank() && current in ids -> current
            else -> gyms.first().id
        }
        val changed = resolved != current
        if (changed) {
            SessionManager.gymId = resolved
        }
        _activeGymId.value = resolved
        return changed
    }

    fun selectOwnerGym(gymId: String) {
        if (!SessionManager.userRole.equals("gym_owner", ignoreCase = true)) return
        if (gymId.isBlank()) return
        val gyms = _userGyms.value
        if (gyms.none { it.id == gymId }) return
        if (SessionManager.gymId == gymId) return
        SessionManager.gymId = gymId
        _activeGymId.value = gymId
        loadDashboardData()
        loadMembers(isRefresh = true)
    }

    private fun isOwnerSessionOrViewer(response: OwnerDashboardNewResponse): Boolean {
        if (SessionManager.userRole.equals("gym_owner", ignoreCase = true)) return true
        if (SessionManager.userRole.equals("trainer", ignoreCase = true)) return true
        return when (response.viewer.lowercase()) {
            "owner", "gym_owner", "trainer" -> true
            else -> false
        }
    }

    fun loadDashboardData(
        day: String? = null,
        showGlobalLoader: Boolean = true
    ) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken

        if (gymId.isEmpty() || token.isEmpty()) {
            showError("Please sign in again to access the dashboard.")
            return
        }
        if (day == null && dashboardBootstrapInFlight) return
        if (day == null) dashboardBootstrapInFlight = true

        executeApi(
            apiCall = { repository.getOwnerDashboardData(gymId, token, day) },
            onSuccess = { response ->
                // Sync the user role from the API's 'viewer' field. This fixes cases where the session 
                // might have an outdated role (e.g. from login) that doesn't match the dashboard view.
//                if (response.viewer.isNotBlank()) {
//                    SessionManager.userRole = response.viewer
//                }

                when {
                    response.permissions.isNotEmpty() -> {
                        SessionManager.saveOwnerDashboardPermissionKeys(response.permissions)
                        // If viewer is owner, ensure they have full access as a fallback.
                        if (isOwnerSessionOrViewer(response)) {
                            SessionManager.applyDefaultGymOwnerPermissions()
                        }
                    }

                    response.effectivePermissions.isNotEmpty() -> {
                        SessionManager.saveOwnerDashboardPermissions(
                            DashboardPermissions(
                                role = response.viewer,
                                effective = response.effectivePermissions
                            )
                        )
                    }

                    isOwnerSessionOrViewer(response) -> {
                        SessionManager.applyDefaultGymOwnerPermissions()
                    }
                }

                _dashboardData.value = response
                if (day == null) {
                    _permissionsVersion.value++
                    dashboardBootstrapComplete = true
                    retryDashboardAfterRefresh = false
                    flushPendingDashboardActions()
                }
                dashboardBootstrapInFlight = false
            },
            onError = { message ->
                dashboardBootstrapInFlight = false
                if (retryDashboardAfterRefresh) {
                    retryDashboardAfterRefresh = false
                    loadDashboardData()
                }
                else {
                    showError(message)
                }
            },
            showGlobalLoader = showGlobalLoader
//            gateWithDashboard = false
        )
    }

    /** PUT /notifications/device-token — does not toggle global loading; failures are logged only. */
    fun registerDeviceTokenWithBackend() {
        if (queueUntilDashboardReady { registerDeviceTokenWithBackend() }) return
        val token = SessionManager.fcmDeviceToken
        if (token.isEmpty()) return
        val accessToken = SessionManager.accessToken
        if (accessToken.isEmpty()) return

        executeApi(
            apiCall = {
                repository.registerDeviceToken(
                    accessToken,
                    DeviceTokenRequest(token)
                )
            },
            onSuccess = { response ->
                println("DEVICE_TOKEN: registered with backend (ok=${response.ok}).")
            },
            onError = { message ->
                println("DEVICE_TOKEN: registration failed — $message")
            },
            showGlobalLoader = false
        )
    }

    // Member logic
    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        searchJob = screenModelScope.launch {
            delay(500) // Debounce
            loadMembers(isRefresh = true)
        }
    }

    fun onFilterChanged(filter: String) {
        if (_selectedFilter.value != filter) {
            _selectedFilter.value = filter
            loadMembers(isRefresh = true)
        }
    }

    fun loadMoreMembers() {
        if (!canLoadMore || _isLoadingMore.value || isLoading.value) return
        _isLoadingMore.value = true
        loadMembers(isRefresh = false)
    }

    fun refresh() {
        loadDashboardData()
        loadMembers(isRefresh = true)
        loadProfile()
        loadUserGyms()
        loadPlans()
    }

    fun loadProfile() {
        if (queueUntilDashboardReady { loadProfile() }) return
        val token = SessionManager.accessToken
        if (token.isEmpty()) return

        executeApi(
            apiCall = { repository.getProfile(token) },
            onSuccess = { response ->
                SessionManager.userId = response.data?.id ?: ""
                _profileData.value = response.data
            }
        )
    }

    fun loadUserGyms() {
        val token = SessionManager.accessToken
        if (token.isEmpty()) return
        _userGymsLoading.value = true
        executeApi(
            apiCall = { repository.listUserGyms(token) },
            onSuccess = { response ->
                _userGymsLoading.value = false
                val selectionChanged = applyUserGymsFromResponse(response)
                if (selectionChanged && dashboardBootstrapComplete) {
                    loadDashboardData()
                    loadMembers(isRefresh = true)
                }
            },
            onError = {
                _userGymsLoading.value = false
                showToast(it)
            },
            showGlobalLoader = false
        )
    }

    fun createOwnedGym(
        request: CreateOwnedGymRequest,
        onSuccess: () -> Unit,
    ) {
        val token = SessionManager.accessToken
        if (token.isEmpty()) return
        executeApi(
            apiCall = { repository.createOwnedGym(token, request) },
            onSuccess = { body ->
                persistCreateOwnedGymAuth(body)
                loadUserGyms()
                refresh()
                showToast("Gym created successfully.")
                onSuccess()
            }
        )
    }

    fun updateOwnedGym(
        gymId: String,
        request: CreateOwnedGymRequest,
        onSuccess: () -> Unit,
    ) {
        val token = SessionManager.accessToken
        if (token.isEmpty() || gymId.isBlank()) return
        executeApi(
            apiCall = { repository.updateOwnedGym(token, gymId, request) },
            onSuccess = {
                loadUserGyms()
                loadProfile()
                refresh()
                showToast("Gym updated.")
                onSuccess()
            }
        )
    }

    fun deleteOwnedGym(
        gymId: String,
        onSuccess: () -> Unit,
    ) {
        val token = SessionManager.accessToken
        if (token.isEmpty() || gymId.isBlank()) return
        executeApi(
            apiCall = { repository.deleteOwnedGym(token, gymId) },
            onSuccess = {
                loadUserGyms()
                loadProfile()
                refresh()
                showToast("Gym deleted.")
                onSuccess()
            }
        )
    }

    private fun persistCreateOwnedGymAuth(response: CreateOwnedGymResponse) {
        val inner = response.data
        val access = response.access_token ?: inner?.access_token
        val refresh = response.refresh_token ?: inner?.refresh_token
        val temp = response.temp_token ?: inner?.temp_token
        val newGymId = response.gym_id
            ?: inner?.gym_id
            ?: inner?.gym?.id
            ?: response.gym?.id
        if (!access.isNullOrBlank()) SessionManager.accessToken = access
        if (!refresh.isNullOrBlank()) SessionManager.refreshToken = refresh
        if (!newGymId.isNullOrBlank()) {
            SessionManager.gymId = newGymId
            _activeGymId.value = newGymId
        }
        if (!temp.isNullOrBlank()) SessionManager.pendingOtpTempToken = temp
        SessionManager.lastRefreshTimestamp = getCurrentTimeMillis()
        SessionManager.sessionRefreshDoneForCurrentProcess = true
    }

    fun onTabSelected(tab: String) {
        _selectedTab.value = tab
    }

    /** Switches bottom nav to Members, applies the given filter, and reloads the members list. */
    fun openMembersWithFilter(filter: String) {
        _selectedTab.value = "Members"
        _selectedFilter.value = filter
        loadMembers(isRefresh = true)
    }

    fun loadMembers(isRefresh: Boolean) {
        if (queueUntilDashboardReady { loadMembers(isRefresh) }) return
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken

        if (gymId.isEmpty() || token.isEmpty()) return

        if (isRefresh) {
            currentPage = 1
            canLoadMore = true
        }

        val pageToLoad = currentPage

        executeApi(
            apiCall = {
                repository.getMembers(
                    gymId = gymId,
                    accessToken = token,
                    page = pageToLoad,
                    limit = 20,
                    status = if (_selectedFilter.value == "All Member") null else _selectedFilter.value,
                    searchQuery = _searchQuery.value
                )
            },
            onSuccess = { response ->
                val newMembers = response.members.takeIf { it.isNotEmpty() }
                    ?: response.items.takeIf { it.isNotEmpty() } ?: emptyList<MemberDTO>()

                if (isRefresh) {
                    _members.value = newMembers
                } else {
                    _members.value += newMembers
                }

                if (response.stats != null) {
                    _memberStats.value = response.stats
                }

                val totalPages = response.pagination?.total_pages ?: 1
                canLoadMore = currentPage < totalPages

                if (canLoadMore) {
                    currentPage++
                }
                _isLoadingMore.value = false
            },
            onError = {
                _isLoadingMore.value = false
            },
            showGlobalLoader = isRefresh
        )
    }

    fun loadMemberDetail(memberId: String) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        executeApi(
            apiCall = { repository.getMemberDetail(gymId, memberId, token) },
            onSuccess = { response ->
                _memberDetail.value = response
            }
        )
    }

    fun convertMember(id: String, onSuccess: () -> Unit) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        executeApi(
            apiCall = { repository.convertMember(gymId, id, token) },
            onSuccess = {
                loadMemberDetail(id)
                onSuccess()
            }
        )
    }

    fun loadMemberAttendanceSummary(
        memberId: String,
        month: String? = null,
        year: String? = null,
        date: String? = null,
    ) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        executeApi(
            apiCall = {
                repository.getMemberAttendanceSummary(
                    gymId,
                    memberId,
                    token,
                    month,
                    year,
                    date,
                )
            },
            onSuccess = { response ->
                _memberAttendanceSummary.value = response
            }
        )
    }

    fun generateAttendanceQr(onSuccess: (() -> Unit)? = null) {
        if (queueUntilDashboardReady { generateAttendanceQr(onSuccess) }) return
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) {
            showError("Please sign in again.")
            return
        }

        _attendanceQrLoading.value = true
        executeApi(
            apiCall = { repository.generateAttendanceQr(gymId, token) },
            onSuccess = { response ->
                _attendanceQr.value = response
                onSuccess?.invoke()
                _attendanceQrLoading.value = false
            },
            onError = {
                _attendanceQrLoading.value = false
            },
            showGlobalLoader = false
        )
    }

    fun loadAttendanceQrStaticOrGenerate(onSuccess: (() -> Unit)? = null) {
        if (queueUntilDashboardReady { loadAttendanceQrStaticOrGenerate(onSuccess) }) return
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) {
            showError("Please sign in again.")
            return
        }

        screenModelScope.launch {
            _attendanceQrLoading.value = true
            when (val staticResult = repository.getAttendanceQrStatic(gymId, token)) {
                is ApiResult.Success -> {
                    val staticQr = staticResult.data
                    if (staticQr.hasUsableQrData()) {
                        _attendanceQr.value = staticQr
                        onSuccess?.invoke()
                    } else {
                        when (val generateResult = repository.generateAttendanceQr(gymId, token)) {
                            is ApiResult.Success -> {
                                _attendanceQr.value = generateResult.data
                                onSuccess?.invoke()
                            }

                            is ApiResult.Error -> {
                                showError(generateResult.message)
                            }

                            else -> {}
                        }
                    }
                }

                is ApiResult.Error -> {
                    // If static QR is unavailable, fallback to generate API.
                    when (val generateResult = repository.generateAttendanceQr(gymId, token)) {
                        is ApiResult.Success -> {
                            _attendanceQr.value = generateResult.data
                            onSuccess?.invoke()
                        }

                        is ApiResult.Error -> {
                            showError(generateResult.message)
                        }

                        else -> {}
                    }
                }

                else -> {}
            }
            _attendanceQrLoading.value = false
        }
    }

    fun loadMyAttendanceQr(onSuccess: (() -> Unit)? = null) {
        if (queueUntilDashboardReady { loadMyAttendanceQr(onSuccess) }) return
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) {
            showError("Please sign in again.")
            return
        }

        _attendanceQrLoading.value = true
        executeApi(
            apiCall = { repository.getMyAttendanceQr(gymId, token) },
            onSuccess = { response ->
                _attendanceQr.value = response
                onSuccess?.invoke()
                _attendanceQrLoading.value = false
            },
            onError = {
                _attendanceQrLoading.value = false
            },
            showGlobalLoader = false
        )
    }

    fun regenerateAttendanceQr(onSuccess: (() -> Unit)? = null) {
        if (queueUntilDashboardReady { regenerateAttendanceQr(onSuccess) }) return
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken

        _attendanceQrLoading.value = true
        executeApi(
            apiCall = { repository.regenerateAttendanceQr(gymId, token) },
            onSuccess = { response ->
                _attendanceQr.value = response
                onSuccess?.invoke()
                _attendanceQrLoading.value = false
            },
            onError = {
                _attendanceQrLoading.value = false
            },
            showGlobalLoader = false
        )
    }

    fun punchAttendanceByToken(token: String, onComplete: (() -> Unit)? = null) {
        if (token.isBlank()) {
            showError("Invalid QR token.")
            return
        }

        val tokenn = SessionManager.accessToken


        _attendancePunchLoading.value = true
        executeApi(
            apiCall = { repository.punchAttendance(token, tokenn) },
            onSuccess = { response ->
                _attendancePunchResult.value = response
                onComplete?.invoke()
                _attendancePunchLoading.value = false
            },
            onError = { message ->
                _attendancePunchResult.value = AttendancePunchResponse(
                    success = false,
                    message = message,
                    statusCode = null
                )
                _attendancePunchLoading.value = false
            },
            showGlobalLoader = false
        )
    }

    fun punchTrainerAttendance(onComplete: (() -> Unit)? = null) {
        if (queueUntilDashboardReady { punchTrainerAttendance(onComplete) }) return
        val gymId = SessionManager.gymId
        val trainerId = SessionManager.userId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || trainerId.isEmpty() || token.isEmpty()) {
            showError("Missing trainer session data.")
            return
        }

        _attendancePunchLoading.value = true
        executeApi(
            apiCall = { repository.punchTrainerAttendance(trainerId, gymId, token) },
            onSuccess = { response ->
                _attendancePunchResult.value = response
                onComplete?.invoke()
                _attendancePunchLoading.value = false
            },
            onError = { message ->
                _attendancePunchResult.value = AttendancePunchResponse(
                    success = false,
                    message = message,
                    statusCode = null
                )
                _attendancePunchLoading.value = false
            },
            showGlobalLoader = false
        )
    }

    fun clearAttendancePunchResult() {
        _attendancePunchResult.value = null
    }

    fun loadWorkouts(memberId: String) {
        val token = SessionManager.accessToken
        if (token.isEmpty()) return

        executeApi(
            apiCall = { repository.getWorkouts(memberId, token) },
            onSuccess = { response ->
                _workouts.value = response
            }
        )
    }

    fun loadMemberDiet(
        @Suppress("UNUSED_PARAMETER") memberGymUserId: String,
        createdBy: String = "all",
        showGlobalLoader: Boolean = false,
    ) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken

        _dietMealsLoading.value = true
        executeApi(
            apiCall = {
                repository.getDietMeals(
                    gymId = gymId,
                    accessToken = token,
                    createdBy = createdBy,
                )
            },
            onSuccess = { response ->
                _memberDietMeals.value = response
                _dietMealsLoading.value = false
            },
//            onError = {
//                _memberDietMeals.value = emptyList()
//                _dietMealsLoading.value = false
//            },
            showGlobalLoader = showGlobalLoader
        )
    }

    fun loadMemberDietHistory(
        date: String? = null,
        targetKcal: Int? = null,
        isRefresh: Boolean = false
    ) {
        val token = SessionManager.accessToken
        if (token.isBlank()) {
            showError("Please sign in again.")
            return
        }
        if (isRefresh) _dietHistoryRefreshing.value = true else _dietHistoryLoading.value = true
        executeApi(
            apiCall = {
                repository.getDietHistory(
                    accessToken = token,
                    date = date,
                    gymId = SessionManager.gymId.takeIf { it.isNotBlank() },
                    targetKcal = targetKcal
                )
            },
            onSuccess = { response ->
                _memberDietHistory.value = response
                _dietHistoryLoading.value = false
                _dietHistoryRefreshing.value = false
            },
            onError = {
                _dietHistoryLoading.value = false
                _dietHistoryRefreshing.value = false
            },
            showGlobalLoader = false
        )
    }

    fun loadBroadcastChannels(
        page: Int = 1,
        limit: Int = 20,
        search: String? = null
    ) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isBlank() || token.isBlank()) {
            showError("Please sign in again.")
            return
        }
        _broadcastChannelsLoading.value = true
        executeApi(
            apiCall = { repository.getBroadcastChannels(gymId, token, page, limit, search) },
            onSuccess = { response: BroadcastChannelsResponse ->
                _broadcastChannels.value = response.channels
                    .takeIf { it.isNotEmpty() }
                    ?: response.items.takeIf { it.isNotEmpty() }
                    ?: response.data
                _broadcastChannelsLoading.value = false
            },
            onError = {
                _broadcastChannelsLoading.value = false
            },
            showGlobalLoader = false
        )
    }

    fun createBroadcastChannel(
        name: String,
        description: String?,
        imageUrl: String?,
        onSuccess: (CreateBroadcastChannelResponse) -> Unit
    ) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        val trimmedName = name.trim()
        val trimmedDescription = description?.trim().orEmpty()

        when {
            gymId.isBlank() || token.isBlank() -> {
                showError("Please sign in again.")
                return
            }
            trimmedName.isBlank() -> {
                showError("Channel name is required.")
                return
            }
            trimmedName.length < 3 -> {
                showError("Channel name must be at least 3 characters.")
                return
            }
            trimmedName.length > 60 -> {
                showError("Channel name must be at most 60 characters.")
                return
            }
            trimmedDescription.length > 300 -> {
                showError("Description must be at most 300 characters.")
                return
            }
        }

        _broadcastChannelCreating.value = true
        executeApi(
            apiCall = {
                repository.createBroadcastChannel(
                    accessToken = token,
                    request = CreateBroadcastChannelRequest(
                        gymId = gymId,
                        name = trimmedName,
                        description = trimmedDescription.ifBlank { null },
                        imageUrl = imageUrl
                    )
                )
            },
            onSuccess = { response ->
                _broadcastChannelCreating.value = false
                onSuccess(response)
                loadBroadcastChannels()
            },
            onError = {
                _broadcastChannelCreating.value = false
            },
            showGlobalLoader = false
        )
    }

    fun addMembersToChannel(channelId: String, gymUserIds: List<String>, onSuccess: () -> Unit) {
        val token = SessionManager.accessToken
        if (token.isBlank()) {
            showError("Please sign in again.")
            return
        }

        executeApi(
            apiCall = { repository.addMembersToChannel(channelId, token, gymUserIds) },
            onSuccess = {
                onSuccess()
            }
        )
    }

    fun loadAllBroadcastMembers(search: String? = null) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isBlank() || token.isBlank()) return

        _broadcastMembersLoading.value = true
        executeApi(
            apiCall = { repository.addBroadcastMembers(gymId, token, search) },
            onSuccess = { response ->
                _broadcastMembers.value = response
                _broadcastMembersLoading.value = false
            },
            onError = {
                _broadcastMembersLoading.value = false
            },
            showGlobalLoader = false
        )
    }

    private val _broadcastMessages = MutableStateFlow<List<BroadcastMessageDTO>>(emptyList())
    val broadcastMessages: StateFlow<List<BroadcastMessageDTO>> = _broadcastMessages

    private val _broadcastChannelDetail = MutableStateFlow<BroadcastChannelDetailDTO?>(null)
    val broadcastChannelDetail: StateFlow<BroadcastChannelDetailDTO?> = _broadcastChannelDetail

    fun loadBroadcastMessages(channelId: String, page: Int = 1, limit: Int = 20) {
        val token = SessionManager.accessToken
        if (token.isBlank()) return

        executeApi(
            apiCall = { repository.getBroadcastMessages(channelId, token, page, limit) },
            onSuccess = { response ->
                _broadcastMessages.value = response.data
            },
            showGlobalLoader = false
        )
    }

    fun sendBroadcastMessage(
        channelId: String,
        title: String,
        description: String? = null,
        imageUrl: String? = null,
        onResult: () -> Unit
    ) {
        val token = SessionManager.accessToken
        if (token.isBlank()) return
        if (title.isBlank()) {
            showToast("Please enter a message title")
            return
        }

        executeApi(
            apiCall = { repository.sendMessage(channelId, token, title, description, imageUrl) },
            onSuccess = {
                showToast("Message sent successfully")
                onResult()
            },
            showGlobalLoader = false
        )
    }



    fun loadBroadcastChannelDetail(channelId: String) {
        val token = SessionManager.accessToken
        if (token.isBlank()) return

        executeApi(
            apiCall = { repository.getBroadcastChannelDetail(channelId, token) },
            onSuccess = { response ->
                _broadcastChannelDetail.value = response
            },
            showGlobalLoader = false
        )
    }

//    private val _broadcastMembers = MutableStateFlow<List<BroadcastMemberDTO>>(emptyList())
//    val broadcastMembers: StateFlow<List<BroadcastMemberDTO>> = _broadcastMembers

    fun loadBroadcastMembers(channelId: String, page: Int = 1, limit: Int = 20, search: String? = null) {
        val token = SessionManager.accessToken
        if (token.isBlank()) return

        executeApi(
            apiCall = { repository.getBroadcastMembers(channelId, token, page, limit, search) },
            onSuccess = { response ->
                _broadcastMembers.value = response.data
            },
            showGlobalLoader = false
        )
    }

    fun deleteBroadcastChannel(channelId: String, onResult: () -> Unit) {
        val token = SessionManager.accessToken
        if (token.isBlank()) return

        executeApi(
            apiCall = { repository.deleteBroadcastChannel(channelId, token) },
            onSuccess = {
                onResult()
            }
        )
    }

    fun updateBroadcastChannel(
        channelId: String,
        name: String?,
        description: String?,
        imageUrl: String?,
        onResult: () -> Unit
    ) {
        val token = SessionManager.accessToken
        if (token.isBlank()) {
            showError("Please sign in again.")
            return
        }
        val trimmedName = name?.trim()
        val trimmedDescription = description?.trim()
        val request = UpdateBroadcastChannelRequest(
            name = trimmedName?.takeIf { it.isNotBlank() },
            description = trimmedDescription?.takeIf { it.isNotBlank() },
            imageUrl = imageUrl?.takeIf { it.isNotBlank() }
        )
        if (request.name == null && request.description == null && request.imageUrl == null) {
            showError("Please update at least one field.")
            return
        }

        executeApi(
            apiCall = { repository.updateBroadcastChannel(channelId, token, request) },
            onSuccess = {
                loadBroadcastChannels()
                loadBroadcastChannelDetail(channelId)
                onResult()
            }
        )
    }

    fun loadMemberStatistics(
        period: String = "week",
        date: String? = null,
        calendarYear: String? = null,
        calendarMonth: String? = null,
        isRefresh: Boolean = false
    ) {
        val token = SessionManager.accessToken
        if (token.isBlank()) {
            showError("Please sign in again.")
            return
        }
        if (isRefresh) _memberStatisticsRefreshing.value = true else _memberStatisticsLoading.value = true
        executeApi(
            apiCall = {
                repository.getMemberStatistics(
                    accessToken = token,
                    period = period,
                    date = date,
                    gymId = SessionManager.gymId.takeIf { it.isNotBlank() },
                    calendarYear = calendarYear,
                    calendarMonth = calendarMonth
                )
            },
            onSuccess = { response ->
                _memberStatistics.value = response
                _memberStatisticsLoading.value = false
                _memberStatisticsRefreshing.value = false
            },
            onError = {
                _memberStatisticsLoading.value = false
                _memberStatisticsRefreshing.value = false
            },
            showGlobalLoader = false
        )
    }

    fun createDietMeal(request: CreateDietMealRequest, onSuccess: () -> Unit) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken

        executeApi(
            apiCall = { repository.createDietMeal(gymId, token, request) },
            onSuccess = { onSuccess() }
        )
    }

    fun createDietFood(request: CreateDietCatalogFoodRequest, onSuccess: () -> Unit) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken

        executeApi(
            apiCall = { repository.createDietFood(gymId, token, request) },
            onSuccess = { onSuccess() }
        )
    }

    fun updateDietMeal(mealId: String, request: CreateDietMealRequest, onSuccess: () -> Unit) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken

        executeApi(
            apiCall = { repository.updateDietMeal(gymId, mealId, token, request) },
            onSuccess = { onSuccess() }
        )
    }

    fun deleteDietMeal(mealId: String, onSuccess: () -> Unit) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken

        executeApi(
            apiCall = { repository.deleteDietMeal(gymId, mealId, token) },
            onSuccess = { onSuccess() }
        )
    }

    fun consumeDietFood(
        memberGymUserId: String?,
        request: ConsumeDietFoodRequest,
        onSuccess: () -> Unit
    ) {
        val gymId = SessionManager.gymId.takeIf { it.isNotBlank() }
        val token = SessionManager.accessToken
        if (token.isBlank()) {
            showError("Please sign in again.")
            return
        }
        executeApi(
            apiCall = { repository.consumeDietFood(gymId, memberGymUserId, token, request) },
            onSuccess = { onSuccess() }
        )
    }

    fun loadDietFoodCatalog(search: String? = null) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken

        println("dsadsd")
        _dietCatalogLoading.value = true
        executeApi(
            apiCall = { repository.getDietFoodCatalog(gymId, token, search) },
            onSuccess = { response ->
                _dietCatalogFoods.value = response
                _dietCatalogLoading.value = false
            },
            onError = {
                _dietCatalogFoods.value = emptyList()
                _dietCatalogLoading.value = false
            },
            showGlobalLoader = false
        )
    }

    fun loadExpenses(month: String? = null, dateFrom: String? = null, dateTo: String? = null) {
        if (queueUntilDashboardReady { loadExpenses(month, dateFrom, dateTo) }) return
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return
        val formattedMonth = month?.toApiMonthFormat()
        _expensesLoading.value = true
        executeApi(
            apiCall = { repository.getExpenses(gymId, token, month = formattedMonth, dateFrom = dateFrom, dateTo = dateTo) },
            onSuccess = { response ->
                _expenses.value = response.items
                _totalExpenseAmount.value = response.totalAmountCents!!
                _lastMonthPer.value = response.percentageVsLastMonth!!
                _expensesLoading.value = false
            },
            onError = {
                _expenses.value = emptyList()
                _expensesLoading.value = false
            },
            showGlobalLoader = false
        )
    }

    fun String.toApiMonthFormat(): String {
        return this.trim()
            .lowercase()
            .replace(" ", "_")
    }

    fun createExpense(request: CreateExpenseRequest, onSuccess: () -> Unit) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken

        executeApi(
            apiCall = { repository.createExpense(gymId, token, request.copy(gymId = gymId)) },
            onSuccess = { onSuccess() }
        )
    }

    fun getExpenseById(id: String, onResult: (ExpenseDTO?) -> Unit) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken

        executeApi(
            apiCall = { repository.getExpenseById(id, gymId, token) },
            onSuccess = { onResult(it) },
            onError = { onResult(null) }
        )
    }

    fun updateExpense(id: String, request: UpdateExpenseRequest, onSuccess: () -> Unit) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken

        executeApi(
            apiCall = { repository.updateExpense(id, gymId, token, request) },
            onSuccess = { onSuccess() }
        )
    }

    fun deleteExpense(id: String, onSuccess: () -> Unit) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken

        executeApi(
            apiCall = { repository.deleteExpense(id, gymId, token) },
            onSuccess = { onSuccess() }
        )
    }

    private var exerciseSearchJob: Job? = null
    fun loadExercises(search: String? = null, equipment: String? = null, muscle: String? = null) {
//        if (queueUntilDashboardReady { loadExercises(search, equipment, muscle) }) return
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken

        exerciseSearchJob?.cancel()
        exerciseSearchJob = screenModelScope.launch {
            if (search != null) delay(500) // Debounce for search

            executeApi(
                apiCall = {
                    repository.getExercises(
                        gymId = gymId,
                        accessToken = token,
                        search = search,
                        equipment = equipment,
                        muscle = muscle
                    )
                },
                onSuccess = { result ->
                    _exercises.value = result
                },
                onError = {
                    _exercises.value = emptyList()
                }
            )
        }
    }

    fun createExercise(request: CreateExerciseRequest, onSuccess: () -> Unit) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken

        executeApi(
            apiCall = { repository.createExercise(gymId, token, request) },
            onSuccess = {
                onSuccess()
            }
        )
    }

    fun loadMetadata() {
        executeApi(
            apiCall = { repository.getMetadata() },
            onSuccess = { response ->
                _metadata.value = response
            }
        )
    }

    fun loadSubscriptions(q: String? = null, tab: String = "active") {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken

        executeApi(
            apiCall = { repository.getSubscriptions(gymId, token, q, tab) },
            onSuccess = { response ->
                _subscriptions.value = response.items
            }
        )
    }

    fun receivePayment(
        request: ReceivePaymentRequest,
        onSuccess: () -> Unit,
        showGlobalLoader: Boolean = true,
        onFinished: (() -> Unit)? = null
    ) {
        val token = SessionManager.accessToken

        executeApi(
            apiCall = { repository.receivePayment(token, request) },
            onSuccess = {
                onSuccess()
                onFinished?.invoke()
            },
            onError = { onFinished?.invoke() },
            showGlobalLoader = showGlobalLoader
        )
    }

    fun extendPlanPayment(
        request: ExtendPlanPaymentRequest,
        onSuccess: () -> Unit,
        showGlobalLoader: Boolean = true,
        onFinished: (() -> Unit)? = null,
    ) {
        val token = SessionManager.accessToken

        executeApi(
            apiCall = { repository.extendPlanPayment(token, request) },
            onSuccess = {
                onSuccess()
                onFinished?.invoke()
            },
            onError = { onFinished?.invoke() },
            showGlobalLoader = showGlobalLoader,
        )
    }

    fun freezeSubscription(
        request: FreezeSubscriptionRequest,
        onSuccess: () -> Unit,
        showGlobalLoader: Boolean = true,
        onFinished: (() -> Unit)? = null,
    ) {
        val token = SessionManager.accessToken

        executeApi(
            apiCall = { repository.freezeSubscription(token, request) },
            onSuccess = {
                onSuccess()
                onFinished?.invoke()
            },
            onError = { onFinished?.invoke() },
            showGlobalLoader = showGlobalLoader,
        )
    }

    fun unfreezeSubscription(
        request: UnfreezeSubscriptionRequest,
        onSuccess: () -> Unit,
        showGlobalLoader: Boolean = true,
        onFinished: (() -> Unit)? = null,
    ) {
        val token = SessionManager.accessToken

        executeApi(
            apiCall = { repository.unfreezeSubscription(token, request) },
            onSuccess = {
                onSuccess()
                onFinished?.invoke()
            },
            onError = { onFinished?.invoke() },
            showGlobalLoader = showGlobalLoader,
        )
    }

    fun userInfoToast(message: String) {
        showToast(message)
    }

    fun createSubscriptionCompat(
        request: CreateSubscriptionCompatRequest,
        onSuccess: () -> Unit,
        showGlobalLoader: Boolean = true,
        onAfterFailure: (() -> Unit)? = null,
    ) {
        val token = SessionManager.accessToken
        if (token.isEmpty()) return

        executeApi(
            apiCall = { repository.createSubscriptionCompat(token, request) },
            onSuccess = {
                onSuccess()
            },
            onError = { msg ->
                onAfterFailure?.invoke()
                showToast(msg)
            },
            showGlobalLoader = showGlobalLoader
        )
    }

    fun assignMemberPlan(
        request: AssignMemberPlanRequest,
        onSuccess: () -> Unit,
        showGlobalLoader: Boolean = false,
        onAfterFailure: (() -> Unit)? = null,
    ) {
        val token = SessionManager.accessToken
        val gymId = SessionManager.gymId
        if (token.isEmpty()) return

        executeApi(
            apiCall = { repository.assignMemberPlan(token, request, gymId.ifEmpty { null }) },
            onSuccess = { response ->
                if (!response.success) {
                    onAfterFailure?.invoke()
                    showToast(
                        response.message?.takeIf { it.isNotBlank() }
                            ?: "Could not assign plan. Please select a different date."
                    )
                    return@executeApi
                }
                onSuccess()
            },
            onError = { msg ->
                onAfterFailure?.invoke()
                showToast(msg)
            },
            showGlobalLoader = showGlobalLoader
        )
    }

    fun loadPaymentAnalytics(
        range: String = "monthly",
        from: String? = null,
        to: String? = null,
    ) {
        val token = SessionManager.accessToken
        val gymId = SessionManager.gymId
        if (token.isEmpty() || gymId.isEmpty()) return

        val normalizedRange = range.trim().lowercase()
        paymentAnalyticsRequestRange = normalizedRange
        _paymentAnalyticsLoading.value = true
        _paymentAnalyticsError.value = null

        executeApi(
            apiCall = { repository.getPaymentAnalytics(gymId, token, normalizedRange, from, to) },
            onSuccess = { response ->
                if (paymentAnalyticsRequestRange == normalizedRange) {
                    _paymentAnalytics.value = response
                    _paymentAnalyticsError.value = null
                }
                _paymentAnalyticsLoading.value = false
            },
            onError = { message ->
                if (paymentAnalyticsRequestRange == normalizedRange) {
                    _paymentAnalyticsError.value = message
                }
                _paymentAnalyticsLoading.value = false
            },
            showGlobalLoader = false,
        )
    }

    private var paymentsOffset = 0
    private var isPaymentsLoading = false
    private var canLoadMorePayments = true

    fun loadPayments(
        search: String? = null,
        status: String? = null,
        reset: Boolean = false
    ) {
        if (isPaymentsLoading) return
        if (reset) {
            paymentsOffset = 0
            canLoadMorePayments = true
            _payments.value = emptyList()
        }
        if (!canLoadMorePayments) return

        val token = SessionManager.accessToken
        val gymId = SessionManager.gymId
        if (token.isEmpty() || gymId.isEmpty()) return

        isPaymentsLoading = true
        executeApi(
            apiCall = {
                repository.getPayments(
                    gymId = gymId,
                    accessToken = token,
                    status = status,
                    search = search,
                    limit = 20,
                    offset = paymentsOffset
                )
            },
            onSuccess = {
                if (reset) {
                    _payments.value = it.items
                } else {
                    _payments.value += it.items
                }
                paymentsOffset += it.items.size
                canLoadMorePayments = it.items.size >= it.limit
                isPaymentsLoading = false
            },
            onError = {
                isPaymentsLoading = false
            }
        )
    }

    private var memberPaymentHistoryOffset = 0
    private var isMemberPaymentHistoryLoading = false
    private var canLoadMoreMemberPaymentHistory = true

    fun loadMemberPaymentHistory(
        memberId: String,
        search: String? = null,
        reset: Boolean = false,
    ) {
        if (isMemberPaymentHistoryLoading) return
        if (reset) {
            memberPaymentHistoryOffset = 0
            canLoadMoreMemberPaymentHistory = true
            _memberPaymentHistoryItems.value = emptyList()
        }
        if (!canLoadMoreMemberPaymentHistory) return

        val token = SessionManager.accessToken
        val gymIdForQuery = SessionManager.effectiveGymIdForMemberApis()
        val effectiveMemberId = SessionManager.effectiveMemberListingIdForApi(memberId)
        if (token.isEmpty() || gymIdForQuery.isEmpty() || effectiveMemberId.isBlank()) return

        isMemberPaymentHistoryLoading = true
        _memberPaymentHistoryLoading.value = true
        executeApi(
            apiCall = {
                repository.getPayments(
                    gymId = gymIdForQuery,
                    accessToken = token,
                    status = null,
                    memberId = effectiveMemberId,
                    search = search?.takeIf { it.isNotBlank() },
                    limit = 20,
                    offset = memberPaymentHistoryOffset,
                )
            },
            onSuccess = { resp ->
                val append = memberPaymentHistoryOffset > 0
                _memberPaymentHistoryItems.value =
                    if (append) _memberPaymentHistoryItems.value + resp.items else resp.items
                memberPaymentHistoryOffset += resp.items.size
                canLoadMoreMemberPaymentHistory = resp.items.size >= resp.limit
                isMemberPaymentHistoryLoading = false
                _memberPaymentHistoryLoading.value = false
            },
            onError = {
                isMemberPaymentHistoryLoading = false
                _memberPaymentHistoryLoading.value = false
            },
            showGlobalLoader = false,
        )
    }

    fun createSubscriptionWithBody(
        request: CreateSubscriptionWithBodyRequest,
        onSuccess: () -> Unit
    ) {
        val token = SessionManager.accessToken
        if (token.isEmpty()) return

        executeApi(
            apiCall = { repository.createSubscriptionWithBody(token, request) },
            onSuccess = {
                onSuccess()
            }
        )
    }


    // ─── Trainer Logic ────────────────────────────────────────────────────────
    fun loadTrainers(
        query: String? = null,
        showGlobalLoader: Boolean = true
    ) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        if (!showGlobalLoader) {
            _trainersListLoading.value = true
            _trainers.value = emptyList()
        }
        executeApi(
            apiCall = {
                repository.getTrainers(
                    gymId = gymId,
                    accessToken = token,
                    query = query,
                    role = trainersListRole
                )
            },
            onSuccess = { response ->
                _trainers.value = response.items
                _trainersListLoading.value = false
            },
            onError = { _trainersListLoading.value = false },
            showGlobalLoader = showGlobalLoader
        )
    }

    fun loadLeaves(
        status: String? = null,
        month: String? = null,
        trainerId: String? = null,
        q: String? = null,
        isRefresh: Boolean = true
    ) {
        if (queueUntilDashboardReady { loadLeaves(status, month, trainerId, q, isRefresh) }) return
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        if (isRefresh) {
            leavesOffset = 0
            canLoadMoreLeaves = true
        }

        if (!canLoadMoreLeaves && !isRefresh) return

        if (isRefresh) {
            _leavesLoading.value = true
        }
        executeApi(
            apiCall = {
                repository.getLeaves(
                    gymId = gymId,
                    accessToken = token,
                    status = status,
                    month = month,
                    trainerId = trainerId,
                    q = q,
                    limit = 20,
                    offset = leavesOffset
                )
            },
            onSuccess = { response ->
                val newLeaves = response.data
                if (isRefresh) {
                    _leaves.value = newLeaves
                } else {
                    _leaves.value += newLeaves
                }
                leavesOffset += newLeaves.size
                canLoadMoreLeaves = newLeaves.size >= response.limit
                _leavesLoading.value = false
            },
            onError = {
                _leavesLoading.value = false
            },
            showGlobalLoader = false
        )
    }

    fun approveLeave(leaveId: String, onSuccess: () -> Unit) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        executeApi(
            apiCall = { repository.approveLeave(gymId, leaveId, token) },
            onSuccess = { onSuccess() }
        )
    }

    fun rejectLeave(leaveId: String, reason: String, onSuccess: () -> Unit) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        executeApi(
            apiCall = { repository.rejectLeave(gymId, leaveId, token, RejectLeaveRequest(reason)) },
            onSuccess = { onSuccess() }
        )
    }


    fun onTrainerSearchQueryChanged(query: String) {
        _trainerSearchQuery.value = query
        loadTrainers(query = query.takeIf { it.isNotBlank() })
    }

    fun getTrainerDetail(
        trainerId: String,
        role: String = GymStaffListRole.TRAINER
    ) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        executeApi(
            apiCall = {
                repository.getTrainerDetail(
                    gymId = gymId,
                    trainerId = trainerId,
                    accessToken = token,
                    role = role
                )
            },
            onSuccess = { response ->
                _trainerDetail.value = response
            }
        )
    }

    fun setTrainerPassword(
        trainerId: String,
        newPassword: String,
        onSuccess: () -> Unit,
        onError: ((String) -> Unit)? = null
    ) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty() || trainerId.isBlank()) {
            val msg = "Please sign in again."
            if (onError != null) onError(msg) else showError(msg)
            return
        }
        executeApi(
            apiCall = {
                repository.setTrainerPassword(
                    gymId,
                    trainerId,
                    token,
                    SetTrainerPasswordRequest(password = newPassword)
                )
            },
            onSuccess = {
                showToast("Password updated successfully.")
                onSuccess()
            },
            onError = onError
        )
    }

    fun deleteTrainer(
        trainerId: String,
        onSuccess: () -> Unit,
        role: String = GymStaffListRole.TRAINER
    ) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        executeApi(
            apiCall = {
                repository.deleteTrainer(
                    gymId = gymId,
                    trainerId = trainerId,
                    accessToken = token,
                    role = role
                )
            },
            onSuccess = {
                loadTrainers()
                onSuccess()
            }
        )
    }

    fun updateTrainer(
        trainerId: String,
        request: UpdateTrainerRequest,
        onSuccess: () -> Unit,
        role: String = GymStaffListRole.TRAINER
    ) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        executeApi(
            apiCall = {
                repository.updateTrainer(
                    gymId = gymId,
                    trainerId = trainerId,
                    accessToken = token,
                    request = request,
                    role = role
                )
            },
            onSuccess = {
                loadTrainers()
                onSuccess()
            }
        )
    }



    fun createTrainerCompat(
        request: CreateTrainerCompatRequest,
        onSuccess: () -> Unit
    ) {
        val token = SessionManager.accessToken
        if (token.isEmpty()) return

        executeApi(
            apiCall = { repository.createTrainerCompat(token, request) },
            onSuccess = {
                loadTrainers()
                onSuccess()
            }
        )
    }

    fun createTrainerSalaryPayment(
        trainerId: String,
        amount: Int,
        paymentMode: String,
        date: String,
        onSuccess: () -> Unit
    ) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) {
            showError("Please sign in again.")
            return
        }
        if (trainerId.isBlank()) {
            showError("Trainer information is missing.")
            return
        }
        if (amount <= 0) {
            showError("Amount should be greater than zero.")
            return
        }

        val request = CreateTrainerSalaryPaymentRequest(
            gymId = gymId,
            trainer_id = trainerId,
            amount = amount,
            payment_mode = paymentMode.lowercase(),
            date = date
        )

        executeApi(
            apiCall = { repository.createTrainerSalaryPayment(token, request) },
            onSuccess = { onSuccess() }
        )
    }

    // ─── Member Creation ──────────────────────────────────────────────────────
    fun addMember(
        request: AddMemberRequest,
        onSuccess: () -> Unit
    ) {
        val token = SessionManager.accessToken
        if (token.isEmpty()) return
 
        executeApi(
            apiCall = { repository.addMember(token, request) },
            onSuccess = {
                loadMembers(isRefresh = true)
                onSuccess()
            }
        )
    }

    fun updateMember(
        memberId: String,
        request: AddMemberRequest,
        onSuccess: () -> Unit
    ) {
        val token = SessionManager.accessToken
        if (token.isEmpty()) return

        executeApi(
            apiCall = { repository.updateMember(memberId, token, request) },
            onSuccess = {
                loadMemberDetail(memberId)
                loadMembers(isRefresh = true)
                onSuccess()
            }
        )
    }

    /** Owner/staff: `PATCH members/{id}/profile` (same body as member self-service). */
    fun updateMemberProfile(
        memberId: String,
        request: MemberProfileUpdateRequest,
        onSuccess: () -> Unit
    ) {
        val token = SessionManager.accessToken
        val gymId = SessionManager.gymId
        if (token.isEmpty()) return

        executeApi(
            apiCall = {
                repository.updateMemberProfile(
                    memberId = memberId,
                    accessToken = token,
                    request = request,
                    gymId = gymId.takeIf { it.isNotBlank() }
                )
            },
            onSuccess = {
                loadMemberDetail(memberId)
                loadMembers(isRefresh = true)
                onSuccess()
            }
        )
    }

    fun deleteMember(
        memberId: String,
        onSuccess: () -> Unit
    ) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        executeApi(
            apiCall = { repository.deleteMember(gymId, memberId, token) },
            onSuccess = {
                loadMembers(isRefresh = true)
                onSuccess()
            }
        )
    }

    fun updateOwnerSelfProfile(
        request: UpdateOwnerProfileRequest,
        onSuccess: () -> Unit
    ) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        executeApi(
            apiCall = { repository.updateOwnerSelfProfile(token, gymId, request) },
            onSuccess = { onSuccess() }
        )
    }

    fun updateTrainerSelfProfile(
        request: UpdateTrainerProfileRequest,
        onSuccess: () -> Unit
    ) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        executeApi(
            apiCall = { repository.updateTrainerSelfProfile(token, gymId, request) },
            onSuccess = { onSuccess() }
        )
    }

    fun updateProfile(
        request: UpdateProfileRequest,
        onSuccess: () -> Unit
    ) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        executeApi(
            apiCall = { repository.updateProfile(token, gymId, request) },
            onSuccess = {
                onSuccess()
            }
        )
    }

    // ─── Shared Utils ─────────────────────────────────────────────────────────
    fun uploadImage(imageBytes: ByteArray, fileName: String, onResult: (String?) -> Unit) {
        val token = SessionManager.accessToken
        if (token.isEmpty()) {
            onResult(null); return
        }
        _activeImageUploadTarget.value = when {
            fileName.contains("gym_logo", ignoreCase = true) -> ActiveImageUploadTarget.GymLogo
            fileName.contains("profile_avatar", ignoreCase = true) -> ActiveImageUploadTarget.ProfilePhoto
            else -> null
        }
        _isUploadingImage.value = true
        executeApi(
            apiCall = { repository.uploadImage(token, imageBytes, fileName) },
            onSuccess = { response ->
                val rawUrl = response.url
                // Handle localhost replacement for local development
                val finalUrl = if (rawUrl.contains("localhost")) {
                    rawUrl.replace("localhost", "192.168.1.73")
                } else rawUrl

                applyUploadedImageToProfileCache(fileName, finalUrl)
                _uploadedAvatarUrl.value = finalUrl
                _isUploadingImage.value = false
                _activeImageUploadTarget.value = null
                onResult(finalUrl)
            },
            onError = {
                _isUploadingImage.value = false
                _activeImageUploadTarget.value = null
                onResult(null)
            },
            showGlobalLoader = false
        )
    }

    private fun applyUploadedImageToProfileCache(fileName: String, imageUrl: String) {
        val curr = _profileData.value ?: return
        when {
            fileName.contains("profile_avatar", ignoreCase = true) -> {
                val pi = curr.personalInfo
                _profileData.value = curr.copy(
                    personalInfo = (pi ?: PersonalInfoDTO()).copy(profileImage = imageUrl)
                )
            }

            fileName.contains("gym_logo", ignoreCase = true) -> {
                val gd = curr.gymDetails
                _profileData.value = curr.copy(
                    gymDetails = (gd ?: GymDetailsDTO()).copy(gymLogo = imageUrl)
                )
            }
        }
    }

    /**
     * Uploads multiple images sequentially (same URL normalization as [uploadImage]).
     */
    fun uploadImagesBatch(
        images: List<Pair<ByteArray, String>>,
        onComplete: (List<String>) -> Unit
    ) {
        if (images.isEmpty()) {
            onComplete(emptyList())
            return
        }
        val token = SessionManager.accessToken
        if (token.isEmpty()) {
            showError("Please sign in again.")
            onComplete(emptyList())
            return
        }
        _isUploadingImage.value = true
        screenModelScope.launch {
            val urls = mutableListOf<String>()
            for ((bytes, name) in images) {
                when (val result = repository.uploadImage(token, bytes, name)) {
                    is ApiResult.Success -> {
                        val rawUrl = result.data.url
                        val finalUrl = if (rawUrl.contains("localhost")) {
                            rawUrl.replace("localhost", "192.168.1.73")
                        } else {
                            rawUrl
                        }
                        urls.add(finalUrl)
                    }

                    else -> {}
                }
            }
            _isUploadingImage.value = false
            onComplete(urls)
        }
    }

    // ─── Enquiries Logic ──────────────────────────────────────────────────────
    fun loadEnquiries(query: String? = null, status: String? = null) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        executeApi(
            apiCall = {
                repository.getEnquiries(
                    gymId = gymId,
                    accessToken = token,
                    query = query,
                    status = status
                )
            },
            onSuccess = { response ->
                _enquiries.value = response.items
                // Only update stats if it's a non-filtered, non-search load to keep Overview accurate
                if (query == null && status == null) {
                    _enquiryStats.value = EnquiryStats(
                        totalEnquiry = response.total,
                        converted = response.items.count { it.status == "CONVERTED" },
                        pending = response.items.count { it.status == "OPEN" || it.status == "FOLLOW_UP" || it.status == "TRIAL" }
                    )
                }
            }
        )
    }


    fun onEnquirySearch(query: String) {
        _enquirySearchQuery.value = query
        loadEnquiries(
            query = query.takeIf { it.isNotBlank() },
            status = _selectedEnquiryStatus.value
        )
    }

    fun onEnquiryStatusFilter(status: String?) {
        _selectedEnquiryStatus.value = status
        loadEnquiries(query = _enquirySearchQuery.value.takeIf { it.isNotBlank() }, status = status)
    }

    fun createEnquiry(
        request: CreateEnquiryRequest,
        onSuccess: () -> Unit
    ) {
        val token = SessionManager.accessToken
        if (token.isEmpty()) return

        executeApi(
            apiCall = { repository.createEnquiry(token, request) },
            onSuccess = {
                loadEnquiries()
                onSuccess()
            }
        )
    }

    fun updateEnquiryStatus(id: String, status: String, onSuccess: () -> Unit) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        executeApi(
            apiCall = {
                repository.patchEnquiry(
                    id,
                    gymId,
                    token,
                    UpdateEnquiryRequest(status = status)
                )
            },
            onSuccess = {
                loadEnquiries()
                onSuccess()
            }
        )
    }


    fun getEnquiryDetail(id: String, onSuccess: (EnquiryDTO) -> Unit) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        executeApi(
            apiCall = { repository.getEnquiryDetail(gymId, id, token) },
            onSuccess = { response ->
                onSuccess(response)
            }
        )
    }

    fun updateEnquiry(id: String, request: UpdateEnquiryRequest, onSuccess: () -> Unit) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        executeApi(
            apiCall = { repository.patchEnquiry(id, gymId, token, request) },
            onSuccess = {
                loadEnquiries()
                onSuccess()
            }
        )
    }


    fun convertEnquiry(id: String, onSuccess: () -> Unit) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        executeApi(
            apiCall = { repository.convertEnquiry(id, token, gymId) },
            onSuccess = {
                loadEnquiries()
                onSuccess()
            }
        )
    }


    fun logout(onSuccess: () -> Unit) {
        val refreshToken = SessionManager.refreshToken
        if (refreshToken.isEmpty()) {
            SessionManager.clearSession()
            onSuccess()
            return
        }

        executeApi(
            apiCall = { AuthRepository.logout(refreshToken) },
            onSuccess = {
                SessionManager.clearSession()
                onSuccess()
            },
            onError = {
                // Even if API fails, we clear session locally
                SessionManager.clearSession()
                onSuccess()
            }
        )
    }

    fun switchToMember(
        onSuccess: () -> Unit,
        onNavigateToOnboarding: () -> Unit
    ) {
        val token = SessionManager.accessToken
        if (token.isBlank()) {
            showError("Session expired. Please login again.")
            return
        }

        executeApi(
            apiCall = { AuthRepository.getProfileStatus(token) },
            onSuccess = { status ->
                if (status.isSwitcheable) {
                    val fallbackName = profileData.value?.personalInfo?.fullName
                    val request = SwitchToMemberRequest(name = fallbackName)
                    executeApi(
                        apiCall = { AuthRepository.switchToMember(token, request) },
                        onSuccess = { response ->
                            SessionManager.accessToken = response.access_token
                            SessionManager.refreshToken = response.refresh_token
                            SessionManager.userRole = "member"
                            SessionManager.clearOwnerDashboardPermissions()
                            
                            val active = WorkoutManager.activeWorkout.value
                            if (active != null && !active.workoutId.isNullOrBlank()) {
                                stopWorkout(active.workoutId)
                            }
                            
                            WorkoutManager.stopWorkout()
                            onSuccess()
                        }
                    )
                } else {
                    onNavigateToOnboarding()
                }
            }
        )
    }

    fun createPlanCompat(
        request: CreatePlanCompatRequest,
        onSuccess: () -> Unit
    ) {
        val token = SessionManager.accessToken
        if (token.isEmpty()) return

        executeApi(
            apiCall = { repository.createPlanCompat(token, request) },
            onSuccess = {
                onSuccess()
            }
        )
    }

    fun updatePlanCompat(
        planId: String,
        request: CreatePlanCompatRequest,
        onSuccess: () -> Unit
    ) {
        val token = SessionManager.accessToken
        if (token.isEmpty() || planId.isBlank()) return

        executeApi(
            apiCall = { repository.updatePlanCompat(token, planId, request) },
            onSuccess = {
                onSuccess()
            }
        )
    }

    fun loadPlans(
        showGlobalLoader: Boolean = true,
        onFinished: (() -> Unit)? = null
    ) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) {
            _plansLoading.value = false
            onFinished?.invoke()
            return
        }

        _plansLoading.value = true
        executeApi(
            apiCall = { repository.getPlans(gymId, token) },
            onSuccess = { response ->
                _plans.value = response.items
                _planSubscriptionCount.value = response.subscriptionCount
                _plansLoading.value = false
                onFinished?.invoke()
            },
            onError = {
                _planSubscriptionCount.value = 0
                _plansLoading.value = false
                onFinished?.invoke()
            },
            showGlobalLoader = showGlobalLoader
        )
    }

    fun deletePlan(planId: String, onSuccess: () -> Unit) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty() || planId.isBlank()) return

        executeApi(
            apiCall = { repository.deletePlan(gymId, planId, token) },
            onSuccess = {
                _plans.value = _plans.value.filter { it.id != planId }
                showToast("Plan archived successfully")
                onSuccess()
            }
        )
    }

    fun getPlanDetail(planId: String) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        executeApi(
            apiCall = { repository.getPlanDetail(gymId, planId, token) },
            onSuccess = { response ->
                _planDetail.value = response
            }
        )
    }

    fun loadEnrolledMembers(planId: String) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        executeApi(
            apiCall = { repository.getEnrolledMembers(gymId, planId, token) },
            onSuccess = { response ->
                _enrolledMembers.value = response.items
            }
        )
    }

    fun deleteAccount(onSuccess: () -> Unit) {
        val token = SessionManager.accessToken
        if (token.isEmpty()) {
            SessionManager.clearSession()
            onSuccess()
            return
        }

        executeApi(
            apiCall = { repository.deleteProfile(token) },
            onSuccess = {
                SessionManager.clearSession()
                onSuccess()
            }
        )
    }

    fun createWorkout(
        request: CreateWorkoutRequest,
        onSuccess: (GenericSuccessResponse) -> Unit = {},
    ) {
        val token = SessionManager.accessToken
        if (token.isEmpty()) return
        executeApi(
            apiCall = { repository.createWorkout(token, request) },
            onSuccess = { response -> onSuccess(response) },
        )
    }

    fun createWorkoutAndResolveId(
        request: CreateWorkoutRequest,
        onCreated: (workoutId: String) -> Unit,
        onFailure: () -> Unit = {},
        showGlobalLoader: Boolean = true,
    ) {
        val token = SessionManager.accessToken
        val gymId = SessionManager.gymId.ifEmpty { null }
        if (token.isEmpty()) {
            onFailure()
            return
        }

        screenModelScope.launch {
            if (!isNetworkAvailable()) {
                NetworkMonitor.updateStatus(false)
                showToast("No internet connection")
                onFailure()
                return@launch
            }
            NetworkMonitor.updateStatus(true)
            if (showGlobalLoader) showLoader()
            clearError()
            clearToast()

            when (val createResult = repository.createWorkout(token, request)) {
                is ApiResult.Success -> {
                    var workoutId = createResult.data.workoutId?.takeIf { it.isNotBlank() }
                        ?: createResult.data.id?.takeIf { it.isNotBlank() }

                    if (workoutId.isNullOrBlank()) {
                        when (
                            val listResult = repository.getPersonalWorkouts(
                                accessToken = token,
                                gymId = gymId,
                                createdBy = "all",
                            )
                        ) {
                            is ApiResult.Success -> {
                                workoutId = resolveCreatedWorkoutId(listResult.data, request.title)
                            }
                            is ApiResult.Error -> showToast(listResult.message)
                            else -> Unit
                        }
                    }

                    if (showGlobalLoader) hideLoader()
                    if (workoutId.isNullOrBlank()) {
                        showToast("Workout created but could not resolve workout id.")
                        onFailure()
                    } else {
                        onCreated(workoutId)
                    }
                }
                is ApiResult.Error -> {
                    if (showGlobalLoader) hideLoader()
                    showToast(createResult.message)
                    onFailure()
                }
                else -> {
                    if (showGlobalLoader) hideLoader()
                    onFailure()
                }
            }
        }
    }

    fun createWorkoutThenStart(
        request: CreateWorkoutRequest,
        onStarted: (sessionWorkoutId: String, startResponse: WorkoutDetailResponse) -> Unit,
        onFailure: () -> Unit = {},
        showGlobalLoader: Boolean = true,
    ) {
        val token = SessionManager.accessToken
        val gymId = SessionManager.gymId.ifEmpty { null }
        if (token.isEmpty()) {
            onFailure()
            return
        }

        screenModelScope.launch {
            if (!isNetworkAvailable()) {
                NetworkMonitor.updateStatus(false)
                showToast("No internet connection")
                onFailure()
                return@launch
            }
            NetworkMonitor.updateStatus(true)
            if (showGlobalLoader) showLoader()
            clearError()
            clearToast()

            when (val createResult = repository.createWorkout(token, request)) {
                is ApiResult.Success -> {
                    var workoutId = createResult.data.workoutId?.takeIf { it.isNotBlank() }
                        ?: createResult.data.id?.takeIf { it.isNotBlank() }

                    if (workoutId.isNullOrBlank()) {
                        when (
                            val listResult = repository.getPersonalWorkouts(
                                accessToken = token,
                                gymId = gymId,
                                createdBy = "all",
                            )
                        ) {
                            is ApiResult.Success -> {
                                workoutId = resolveCreatedWorkoutId(listResult.data, request.title)
                            }
                            is ApiResult.Error -> showToast(listResult.message)
                            else -> Unit
                        }
                    }

                    if (workoutId.isNullOrBlank()) {
                        if (showGlobalLoader) hideLoader()
                        showToast("Workout created but could not start session.")
                        onFailure()
                        return@launch
                    }

                    when (val startResult = repository.startWorkout(token, workoutId, gymId)) {
                        is ApiResult.Success -> {
                            if (showGlobalLoader) hideLoader()
                            val sessionId = startResult.data.workoutId.ifBlank { workoutId }
                            onStarted(sessionId, startResult.data)
                        }
                        is ApiResult.Error -> {
                            if (showGlobalLoader) hideLoader()
                            showToast(startResult.message)
                            onFailure()
                        }
                        else -> {
                            if (showGlobalLoader) hideLoader()
                            onFailure()
                        }
                    }
                }
                is ApiResult.Error -> {
                    if (showGlobalLoader) hideLoader()
                    showToast(createResult.message)
                    onFailure()
                }
                else -> {
                    if (showGlobalLoader) hideLoader()
                    onFailure()
                }
            }
        }
    }

    private fun resolveCreatedWorkoutId(
        workouts: List<WorkoutDTO>,
        title: String,
    ): String? {
        val normalizedTitle = title.trim()
        val candidates = workouts.filter { workout ->
            workout.workoutId.isNotBlank() && !workout.completed
        }
        val titleMatches = if (normalizedTitle.isNotEmpty()) {
            candidates.filter { workout ->
                workout.title?.trim().equals(normalizedTitle, ignoreCase = true) == true
            }
        } else {
            candidates
        }
        return titleMatches.maxByOrNull { it.date.orEmpty() }?.workoutId
            ?: candidates.maxByOrNull { it.date.orEmpty() }?.workoutId
    }

    fun getWorkoutDetail(
        workoutId: String,
        onSuccess: (WorkoutDetailResponse) -> Unit
    ) {
        val token = SessionManager.accessToken
        if (token.isEmpty()) return
        executeApi(
            apiCall = { repository.getWorkoutDetail(token, workoutId) },
            onSuccess = { response -> onSuccess(response) }
        )
    }

    fun completeWorkout(
        workoutId: String,
        onSuccess: (WorkoutDetailResponse) -> Unit
    ) {
        val token = SessionManager.accessToken
        if (token.isEmpty()) return
        executeApi(
            apiCall = { repository.completeWorkout(token, workoutId) },
            onSuccess = { response -> onSuccess(response) }
        )
    }

    fun deleteWorkout(
        workoutId: String,
        onSuccess: () -> Unit
    ) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (token.isEmpty()) return
        executeApi(
            apiCall = { repository.deleteWorkout(token, gymId, workoutId) },
            onSuccess = { onSuccess() }
        )
    }

    fun createLeave(
        leaveTypeApi: String,
        startDateIso: String,
        endDateIso: String,
        reason: String,
        trainerId: String,
        onSuccess: () -> Unit
    ) {
        val token = SessionManager.accessToken
        val gymId = SessionManager.gymId
//        val trainerId = SessionManager.userId

        if (token.isEmpty()) {
            showError("Please sign in again.")
            return
        }
        if (gymId.isEmpty() || trainerId.isEmpty()) {
            showError("Missing gym or trainer information. Please sign in again.")
            return
        }

        val request = CreateLeaveRequest(
            gymId = gymId,
            trainerId = trainerId,
            leaveType = leaveTypeApi,
            startDate = startDateIso,
            endDate = endDateIso,
            reason = reason.trim()
        )

        executeApi(
            apiCall = { repository.createLeave(token, request) },
            onSuccess = { onSuccess() }
        )
    }

    fun createProduct(request: CreateProductRequest, onSuccess: () -> Unit) {
        val token = SessionManager.accessToken
        if (token.isEmpty()) {
            showError("Please sign in again.")
            return
        }
        if (request.gymId.isEmpty()) {
            showError("Missing gym information.")
            return
        }
        executeApi(
            apiCall = { repository.createProduct(token, request) },
            onSuccess = {
                ProductRefreshBus.bump()
                onSuccess()
            }
        )
    }

    fun clearProductDetail() {
        _productDetail.value = null
    }

    fun loadFavorites(showLoader: Boolean = false) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (token.isBlank()) return
        executeApi(
            apiCall = { repository.getFavorites(gymId.ifBlank { null }, token) },
            onSuccess = { response ->
                val ids = response.data.map { it.id }.toSet()
                _favoriteProductIds.value = ids
                if (_shopProducts.value.isNotEmpty()) {
                    _shopProducts.value = _shopProducts.value.map { it.copy(isFavorite = ids.contains(it.id)) }
                }
                _productDetail.value = _productDetail.value?.copy(
                    isFavorite = _productDetail.value?.id?.let { ids.contains(it) } == true
                )
            },
            showGlobalLoader = showLoader
        )
    }

    fun toggleFavorite(productId: String, currentlyFavorite: Boolean, onSuccess: (() -> Unit)? = null) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (productId.isBlank() || token.isBlank()) return
        executeApi(
            apiCall = {
                if (currentlyFavorite) {
                    repository.removeFavorite(productId, gymId.ifBlank { null }, token)
                } else {
                    repository.addFavorite(productId, gymId.ifBlank { null }, token)
                }
            },
            onSuccess = {
                val updated = _favoriteProductIds.value.toMutableSet().apply {
                    if (currentlyFavorite) remove(productId) else add(productId)
                }
                _favoriteProductIds.value = updated
                _shopProducts.value = _shopProducts.value.map { dto ->
                    if (dto.id == productId) dto.copy(isFavorite = !currentlyFavorite) else dto
                }
                _productDetail.value = _productDetail.value?.let { detail ->
                    if (detail.id == productId) detail.copy(isFavorite = !currentlyFavorite) else detail
                }
                onSuccess?.invoke()
            },
            showGlobalLoader = false
        )
    }

    fun updateProduct(productId: String, request: UpdateProductRequest, onSuccess: () -> Unit) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (productId.isBlank() || gymId.isEmpty() || token.isEmpty()) {
            showError("Unable to update product.")
            return
        }
        executeApi(
            apiCall = { repository.updateProduct(productId, gymId, token, request) },
            onSuccess = {
                ProductRefreshBus.bump()
                onSuccess()
            }
        )
    }

    fun deleteProduct(productId: String, onSuccess: () -> Unit) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (productId.isBlank() || gymId.isEmpty() || token.isEmpty()) {
            showError("Unable to delete product.")
            return
        }
        executeApi(
            apiCall = { repository.deleteProduct(productId, gymId, token) },
            onSuccess = {
                ProductRefreshBus.bump()
                _productDetail.value = null
                onSuccess()
            }
        )
    }

    fun onShopSearchQueryChanged(query: String) {
        _shopSearchQuery.value = query
        shopSearchJob?.cancel()
        shopSearchJob = screenModelScope.launch {
            delay(400)
            loadShopProducts(isRefresh = true)
        }
    }

    fun loadMoreShopProducts() {
        if (!shopCanLoadMore || _shopLoadingMore.value || isLoading.value) return
        loadShopProducts(isRefresh = false)
    }

    fun refreshShopProducts() {
        loadShopProducts(isRefresh = true, showFullLoader = false)
    }

    fun loadShopProducts(isRefresh: Boolean, showFullLoader: Boolean = true) {
//        if (queueUntilDashboardReady { loadShopProducts(isRefresh, showFullLoader) }) return
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isEmpty() || token.isEmpty()) return

        if (!isRefresh) {
            if (!shopCanLoadMore || _shopLoadingMore.value) return
        } else {
            shopPage = 1
            shopCanLoadMore = true
        }

        val pageToLoad = shopPage

        if (isRefresh && !showFullLoader) _shopRefreshing.value = true
        if (!isRefresh) _shopLoadingMore.value = true

        executeApi(
            apiCall = {
                repository.getProducts(
                    gymId = gymId,
                    accessToken = token,
                    page = pageToLoad,
                    limit = 10,
                    search = _shopSearchQuery.value.takeIf { it.isNotBlank() },
                    category = null,
                    includeInactive = null
                )
            },
            onSuccess = { response ->
                val items = response.data
                if (isRefresh) {
                    _shopProducts.value = items.map { dto ->
                        dto.copy(isFavorite = dto.isFavorite || _favoriteProductIds.value.contains(dto.id))
                    }
                } else {
                    _shopProducts.value += items.map { dto ->
                        dto.copy(isFavorite = dto.isFavorite || _favoriteProductIds.value.contains(dto.id))
                    }
                }

                val p = response.pagination
                shopCanLoadMore = when {
                    p != null && p.limit > 0 -> {
                        val totalPages =
                            kotlin.math.max(1, (p.total + p.limit - 1) / p.limit)
                        pageToLoad < totalPages
                    }

                    else -> items.size >= 10
                }
                if (shopCanLoadMore) {
                    shopPage++
                }
                _shopRefreshing.value = false
                _shopLoadingMore.value = false
            },
            onError = {
                _shopRefreshing.value = false
                _shopLoadingMore.value = false
            },
            showGlobalLoader = isRefresh && showFullLoader
        )
    }

    fun loadProductDetail(productId: String) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (productId.isBlank() || gymId.isEmpty() || token.isEmpty()) {
            showError("Unable to load product.")
            return
        }
        _productDetail.value = null
        executeApi(
            apiCall = { repository.getProductDetail(productId, gymId, token) },
            onSuccess = { response ->
                _productDetail.value = response.data?.let {
                    it.copy(isFavorite = it.isFavorite || _favoriteProductIds.value.contains(it.id))
                }
                if (response.data == null) {
                    showError("Product not found.")
                }
            }
        )
    }
    fun startWorkout(workoutId: String, onSuccess: (WorkoutDetailResponse) -> Unit = {}) {
        val token = SessionManager.accessToken
        val gymId = SessionManager.gymId
        if (token.isEmpty()) return

        executeApi(
            apiCall = { repository.startWorkout(token, workoutId, gymId.ifEmpty { null }) },
            onSuccess = { response ->
                onSuccess(response)
            }
        )
    }


    fun stopWorkout(workoutId: String, onSuccess: (WorkoutDetailResponse) -> Unit = {}) {
        val token = SessionManager.accessToken
        val gymId = SessionManager.gymId
        if (token.isEmpty()) return

        executeApi(
            apiCall = { repository.stopWorkout(token, workoutId, gymId.ifEmpty { null }) },
            onSuccess = { response ->
                onSuccess(response)
            }
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
            onSuccess = { response ->
                onSuccess(response)
            }
        )
    }

    fun loadWorkoutCompletion(
        workoutId: String,
        onSuccess: (WorkoutCompletionResponse) -> Unit,
        onError: () -> Unit = {},
    ) {
        val token = SessionManager.accessToken
        if (token.isEmpty() || workoutId.isBlank()) {
            onError()
            return
        }
        executeApi(
            apiCall = { repository.getWorkoutCompletion(token, workoutId) },
            onSuccess = onSuccess,
            onError = { onError() },
            showGlobalLoader = false,
        )
    }

    fun updateWorkoutCompletion(
        workoutId: String,
        durationMinutes: Int,
        onSuccess: (WorkoutCompletionResponse) -> Unit = {},
    ) {
        val token = SessionManager.accessToken
        if (token.isEmpty() || workoutId.isBlank()) return
        executeApi(
            apiCall = {
                repository.updateWorkoutCompletion(
                    token,
                    workoutId,
                    UpdateWorkoutCompletionRequest(duration_minutes = durationMinutes),
                )
            },
            onSuccess = onSuccess,
        )
    }

    fun createWorkoutSet(
        request: CreateSetRequest,
        onSuccess: (WorkoutDetailSetDTO) -> Unit = {},
        onFailure: () -> Unit = {},
    ) {
        val token = SessionManager.accessToken
        val gymId = SessionManager.gymId.ifEmpty { null }
        if (token.isEmpty()) {
            onFailure()
            return
        }

        executeApi(
            apiCall = { repository.createWorkoutSet(token, request, gymId) },
            showGlobalLoader = false,
            onSuccess = { response -> onSuccess(response) },
            onError = { _ -> onFailure() },
        )
    }

    fun updateWorkoutSet(
        setId: String,
        request: UpdateSetRequest,
        onSuccess: (WorkoutDetailSetDTO) -> Unit = {},
        onFailure: () -> Unit = {},
    ) {
        val token = SessionManager.accessToken
        val gymId = SessionManager.gymId.ifEmpty { null }
        if (token.isEmpty() || setId.isBlank()) {
            onFailure()
            return
        }

        executeApi(
            apiCall = { repository.updateWorkoutSet(token, setId, request, gymId) },
            showGlobalLoader = false,
            onSuccess = { response -> onSuccess(response) },
            onError = { _ -> onFailure() },
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

    fun updateWorkout(
        request: CompleteWorkoutRequest,
        onSuccess: (WorkoutDetailResponse) -> Unit = {},
    ) {
        val token = SessionManager.accessToken
        val gymId = SessionManager.gymId
        if (token.isBlank()) return

        executeApi(
            apiCall = { repository.updateWorkout(token, request, gymId.ifEmpty { null }) },
            onSuccess = onSuccess,
        )
    }

    fun loadWhatsAppAutomation(showGlobalLoader: Boolean = true) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isBlank() || token.isBlank()) {
            showError("Please sign in again.")
            return
        }
        if (!showGlobalLoader) _whatsAppAutomationLoading.value = true
        executeApi(
            apiCall = { repository.getWhatsAppAutomation(gymId, token) },
            onSuccess = { response ->
                _whatsAppAutomation.value = response
                _whatsAppAutomationLoading.value = false
            },
            onError = {
                _whatsAppAutomationLoading.value = false
            },
            showGlobalLoader = showGlobalLoader,
        )
    }

    fun saveWhatsAppAutomation(
        request: UpdateWhatsAppAutomationRequest,
        onSuccess: (WhatsAppAutomationResponse) -> Unit = {},
    ) {
        val gymId = SessionManager.gymId
        val token = SessionManager.accessToken
        if (gymId.isBlank() || token.isBlank()) {
            showError("Please sign in again.")
            return
        }
        _whatsAppAutomationSaving.value = true
        executeApi(
            apiCall = { repository.updateWhatsAppAutomation(gymId, token, request) },
            onSuccess = { response ->
                _whatsAppAutomation.value = response
                _whatsAppAutomationSaving.value = false
                showToast("Automation settings saved")
                onSuccess(response)
            },
            onError = {
                _whatsAppAutomationSaving.value = false
            },
            showGlobalLoader = false,
        )
    }
}
