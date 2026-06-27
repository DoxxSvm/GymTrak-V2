package `in`.gym.trak.studio.data.repository

import `in`.gym.trak.studio.data.model.AssignMemberPlanRequest
import `in`.gym.trak.studio.data.model.AssignMemberPlanResponse
import `in`.gym.trak.studio.data.model.AddBroadcastMembersRequest
import `in`.gym.trak.studio.data.model.AddMemberRequest
import `in`.gym.trak.studio.data.model.AddMemberResponse
import `in`.gym.trak.studio.data.model.ApiResponse
import `in`.gym.trak.studio.data.model.AttendancePunchRequest
import `in`.gym.trak.studio.data.model.AttendancePunchResponse
import `in`.gym.trak.studio.data.model.AttendanceQrResponse
import `in`.gym.trak.studio.data.model.BroadcastChannelDetailDTO
import `in`.gym.trak.studio.data.model.BroadcastChannelsResponse
import `in`.gym.trak.studio.data.model.BroadcastMemberDTO
import `in`.gym.trak.studio.data.model.BroadcastMembersResponse
import `in`.gym.trak.studio.data.model.BroadcastMessageDTO
import `in`.gym.trak.studio.data.model.BroadcastMessagesResponse
import `in`.gym.trak.studio.data.model.CompleteWorkoutRequest
import `in`.gym.trak.studio.data.model.CreateBroadcastChannelRequest
import `in`.gym.trak.studio.data.model.CreateBroadcastChannelResponse
import `in`.gym.trak.studio.data.model.CreateOwnedGymRequest
import `in`.gym.trak.studio.data.model.CreateOwnedGymResponse
import `in`.gym.trak.studio.data.model.UpdateGymResponse
import `in`.gym.trak.studio.data.model.ConsumeDietFoodRequest
import `in`.gym.trak.studio.data.model.ConsumeDietFoodResponse
import `in`.gym.trak.studio.data.model.CreateBroadcastMessageRequest
import `in`.gym.trak.studio.data.model.CreateDietCatalogFoodRequest
import `in`.gym.trak.studio.data.model.CreateDietMealRequest
import `in`.gym.trak.studio.data.model.CreateEnquiryRequest
import `in`.gym.trak.studio.data.model.CreateExerciseRequest
import `in`.gym.trak.studio.data.model.CreateExpenseRequest
import `in`.gym.trak.studio.data.model.CreateLeaveApiResponse
import `in`.gym.trak.studio.data.model.CreateLeaveRequest
import `in`.gym.trak.studio.data.model.CreatePlanCompatRequest
import `in`.gym.trak.studio.data.model.CreatePlanResponse
import `in`.gym.trak.studio.data.model.CreateProductApiResponse
import `in`.gym.trak.studio.data.model.CreateProductRequest
import `in`.gym.trak.studio.data.model.CreateSubscriptionCompatRequest
import `in`.gym.trak.studio.data.model.CreateSubscriptionWithBodyRequest
import `in`.gym.trak.studio.data.model.CreateTrainerCompatRequest
import `in`.gym.trak.studio.data.model.CreateTrainerResponse
import `in`.gym.trak.studio.data.model.CreateTrainerSalaryPaymentRequest
import `in`.gym.trak.studio.data.model.GymStaffListRole
import `in`.gym.trak.studio.data.model.CreateSetRequest
import `in`.gym.trak.studio.data.model.CreateWorkoutRequest
import `in`.gym.trak.studio.data.model.CurrentSubscriptionDTO
import `in`.gym.trak.studio.data.model.DeleteExpenseResponse
import `in`.gym.trak.studio.data.model.DeviceTokenRegisterResponse
import `in`.gym.trak.studio.data.model.DeviceTokenRequest
import `in`.gym.trak.studio.data.model.DietCatalogFoodDTO
import `in`.gym.trak.studio.data.model.DietHistoryResponse
import `in`.gym.trak.studio.data.model.DietMealDTO
import `in`.gym.trak.studio.data.model.MemberStatisticsResponse
import `in`.gym.trak.studio.data.model.EnquiryDTO
import `in`.gym.trak.studio.data.model.EnquiryListResponse
import `in`.gym.trak.studio.data.model.ExerciseRowDTO
import `in`.gym.trak.studio.data.model.ExpenseDTO
import `in`.gym.trak.studio.data.model.ExpenseResponse
import `in`.gym.trak.studio.data.model.GenericSuccessResponse
import `in`.gym.trak.studio.data.model.LeaveListResponse
import `in`.gym.trak.studio.data.model.LeaderboardResponse
import `in`.gym.trak.studio.data.model.MemberAttendanceSummaryResponse
import `in`.gym.trak.studio.data.model.MemberDashboardResponse
import `in`.gym.trak.studio.data.model.MemberDetailResponse
import `in`.gym.trak.studio.data.model.MemberListResponse
import `in`.gym.trak.studio.data.model.NotificationFeedResponse
import `in`.gym.trak.studio.data.model.NotificationMarkReadResponse
import `in`.gym.trak.studio.data.model.MemberOnboardingRequest
import `in`.gym.trak.studio.data.model.MemberOnboardingResponse
import `in`.gym.trak.studio.data.model.MemberProfileDetailResponse
import `in`.gym.trak.studio.data.model.MemberProfileGymDTO
import `in`.gym.trak.studio.data.model.MemberProfileUpdateRequest
import `in`.gym.trak.studio.data.model.MemberProfileWellnessDTO
import `in`.gym.trak.studio.data.model.MetadataResponse
import `in`.gym.trak.studio.data.model.OwnerDashboardNewResponse
import `in`.gym.trak.studio.data.model.PaymentAnalyticsResponse
import `in`.gym.trak.studio.data.model.PaymentListResponse
import `in`.gym.trak.studio.data.model.PlanEnrolledResponse
import `in`.gym.trak.studio.data.model.PlanListResponse
import `in`.gym.trak.studio.data.model.ProductDetailApiResponse
import `in`.gym.trak.studio.data.model.ProductListResponse
import `in`.gym.trak.studio.data.model.ProductMutationResponse
import `in`.gym.trak.studio.data.model.FavoritesListResponse
import `in`.gym.trak.studio.data.model.ProfileResponse
import `in`.gym.trak.studio.data.model.ExtendPlanPaymentRequest
import `in`.gym.trak.studio.data.model.FreezeSubscriptionRequest
import `in`.gym.trak.studio.data.model.ReceivePaymentRequest
import `in`.gym.trak.studio.data.model.ReceivePaymentResponse
import `in`.gym.trak.studio.data.model.RejectLeaveRequest
import `in`.gym.trak.studio.data.model.SetTrainerPasswordRequest
import `in`.gym.trak.studio.data.model.SubscriptionDTO
import `in`.gym.trak.studio.data.model.SubscriptionListResponse
import `in`.gym.trak.studio.data.model.SubscriptionStatsDTO
import `in`.gym.trak.studio.data.model.UnfreezeSubscriptionRequest
import `in`.gym.trak.studio.data.model.TrainerDetailResponse
import `in`.gym.trak.studio.data.model.TrainerListResponse
import `in`.gym.trak.studio.data.model.UpdateEnquiryRequest
import `in`.gym.trak.studio.data.model.UpdateExpenseRequest
import `in`.gym.trak.studio.data.model.UpdateOwnerProfileRequest
import `in`.gym.trak.studio.data.model.UpdateProductRequest
import `in`.gym.trak.studio.data.model.UpdateProfileRequest
import `in`.gym.trak.studio.data.model.UserGymsListResponse
import `in`.gym.trak.studio.data.model.UserOwnedGymDTO
import `in`.gym.trak.studio.data.model.UpdateSetRequest
import `in`.gym.trak.studio.data.model.UpdateWorkoutLegacyRequest
import `in`.gym.trak.studio.data.model.UpdateTrainerProfileRequest
import `in`.gym.trak.studio.data.model.UpdateTrainerRequest
import `in`.gym.trak.studio.data.model.UpdateBroadcastChannelRequest
import `in`.gym.trak.studio.data.model.UploadImageResponse
import `in`.gym.trak.studio.data.model.MemberWorkoutHistoryPageResponse
import `in`.gym.trak.studio.data.model.WorkoutDTO
import `in`.gym.trak.studio.data.model.WorkoutDetailResponse
import `in`.gym.trak.studio.data.model.WhatsAppAutomationResponse
import `in`.gym.trak.studio.data.model.UpdateWhatsAppAutomationRequest
import `in`.gym.trak.studio.data.model.WorkoutCompletionResponse
import `in`.gym.trak.studio.data.model.UpdateWorkoutCompletionRequest
import `in`.gym.trak.studio.data.model.WorkoutDetailSetDTO
import `in`.gym.trak.studio.network.ApiClient
import `in`.gym.trak.studio.network.ApiEndpoints
import `in`.gym.trak.studio.utils.PhoneNumberUtils
import `in`.gym.trak.studio.network.ApiResult
import `in`.gym.trak.studio.network.ApiResult.*
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.client.call.body
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

class OwnerDashboardRepository {
    private val broadcastJson = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    suspend fun completeMemberProfile(
        bearerToken: String,
        request: MemberOnboardingRequest
    ): ApiResult<MemberOnboardingResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.MemberOnboarding.COMPLETE_PROFILE) {
                header(HttpHeaders.Authorization, "Bearer $bearerToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun getMemberDashboard(
        accessToken: String,
        gymId: String?
    ): ApiResult<MemberDashboardResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Members.DASHBOARD) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (!gymId.isNullOrBlank()) {
                    parameter("gymId", gymId)
                }
            }
        }
    }

    suspend fun getMemberStatistics(
        accessToken: String,
        period: String = "week",
        date: String? = null,
        gymId: String? = null,
        calendarYear: String? = null,
        calendarMonth: String? = null
    ): ApiResult<MemberStatisticsResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Members.STATISTICS) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("period", period)
                if (!date.isNullOrBlank()) parameter("date", date)
                if (!gymId.isNullOrBlank()) parameter("gymId", gymId)
                if (!calendarYear.isNullOrBlank()) parameter("calendar_year", calendarYear)
                if (!calendarMonth.isNullOrBlank()) parameter("calendar_month", calendarMonth)
            }
        }
    }

    suspend fun getOwnerDashboardData(
        gymId: String,
        accessToken: String,
        day: String? = null
    ): ApiResult<OwnerDashboardNewResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Core.OWNER_DASHBOARD) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                if (!day.isNullOrBlank()) parameter("day", day)
            }
        }
    }

    suspend fun getLeaderboard(
        gymId: String,
        accessToken: String,
        type: String,
        page: Int = 1,
        limit: Int = 20
    ): ApiResult<LeaderboardResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Leaderboard.BASE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                parameter("type", type)
                parameter("page", page)
                parameter("limit", limit)
            }
        }
    }

    suspend fun registerDeviceToken(
        accessToken: String, request: DeviceTokenRequest
    ): ApiResult<DeviceTokenRegisterResponse> {
        return ApiClient.safeApiCall {
            put(ApiEndpoints.Notifications.DEVICE_TOKEN) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun getNotifications(
        accessToken: String,
        gymId: String,
        limit: Int = 20,
        cursor: String? = null,
    ): ApiResult<NotificationFeedResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Notifications.BASE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                parameter("limit", limit.coerceIn(1, 50))
                cursor?.takeIf { it.isNotBlank() }?.let { parameter("cursor", it) }
            }
        }
    }

    suspend fun markNotificationRead(
        accessToken: String,
        notificationId: String,
        gymId: String,
    ): ApiResult<NotificationMarkReadResponse> {
        return ApiClient.safeApiCall {
            patch(ApiEndpoints.Notifications.read(notificationId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
            }
        }
    }

    suspend fun generateAttendanceQr(
        gymId: String, accessToken: String
    ): ApiResult<AttendanceQrResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.AttendanceQr.GENERATE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
            }
        }
    }

    suspend fun getAttendanceQrStatic(
        gymId: String, accessToken: String
    ): ApiResult<AttendanceQrResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.AttendanceQr.GYM_STATIC) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
            }
        }
    }

    suspend fun getMyAttendanceQr(
        gymId: String, accessToken: String
    ): ApiResult<AttendanceQrResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.AttendanceQr.MY_QR) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
            }
        }
    }

    suspend fun regenerateAttendanceQr(
        gymId: String, accessToken: String
    ): ApiResult<AttendanceQrResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.AttendanceQr.REGENERATE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
            }
        }
    }

    suspend fun punchAttendance(
        token: String, accessToken: String

    ): ApiResult<AttendancePunchResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.AttendanceQr.PUNCH) {
                // As per API docs, this endpoint does not require Bearer token.
                header(HttpHeaders.Authorization, "Bearer $accessToken")

                contentType(ContentType.Application.Json)
                setBody(AttendancePunchRequest(token = token))
            }
        }
    }

    suspend fun punchTrainerAttendance(
        trainerId: String, gymId: String, accessToken: String
    ): ApiResult<AttendancePunchResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Trainers.attendancePunch(trainerId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
            }
        }
    }

//    suspend fun getDashboardData(gymId: String, accessToken: String): ApiResult<DashboardResponse> {
//        return ApiClient.safeApiCall {
//            get(ApiEndpoints.Core.DASHBOARD) {
//                header(HttpHeaders.Authorization, "Bearer $accessToken")
//                parameter("mobileView", "owner")
//                parameter("gymId", gymId)
//            }
//        }
//    }

    suspend fun getExercises(
        gymId: String,
        accessToken: String,
        search: String? = null,
        equipment: String? = null,
        muscle: String? = null
    ): ApiResult<List<ExerciseRowDTO>> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Core.EXERCISES) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (gymId.isNotEmpty()) parameter("gymId", gymId)
                if (!search.isNullOrBlank()) parameter("search", search)
                if (!equipment.isNullOrBlank() && equipment != "NONE") parameter(
                    "equipment", equipment
                )
                if (!muscle.isNullOrBlank()) parameter("muscle", muscle)
            }
        }
    }

    suspend fun createExercise(
        gymId: String,
        accessToken: String,
        request: CreateExerciseRequest
    ): ApiResult<Unit> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Core.EXERCISES) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (gymId.isNotEmpty()) parameter("gymId", gymId)
                setBody(request)
            }
        }
    }

    suspend fun getMetadata(): ApiResult<MetadataResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Core.META_ALL)
        }
    }

    suspend fun getMembers(
        gymId: String,
        accessToken: String,
        page: Int = 1,
        limit: Int = 20,
        status: String? = null,
        searchQuery: String? = null
    ): ApiResult<MemberListResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Members.GET_ALL) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                parameter("page", page)
                parameter("limit", limit)
                if (!status.isNullOrBlank() && status != "All Member") {
                    parameter("status", status.lowercase())
                }
                if (!searchQuery.isNullOrBlank()) {
                    parameter("search", searchQuery)
                }
            }
        }
    }

    suspend fun getMemberDetail(
        gymId: String, memberId: String, accessToken: String
    ): ApiResult<MemberDetailResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Members.detail(memberId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (gymId.isNotEmpty())
                    parameter("gymId", gymId)
            }
        }
    }


    suspend fun getMemberProfile(
        gymId: String,
        memberId: String,
        accessToken: String
    ): ApiResult<MemberProfileDetailResponse> {
        val rawResult: ApiResult<JsonElement> = ApiClient.safeApiCall {
            get(ApiEndpoints.Members.memberProfile(memberId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (gymId.isNotBlank()) {
                    parameter("gymId", gymId)
                }
            }
        }
        return when (rawResult) {
            is ApiResult.Success -> {
                runCatching {
                    parseMemberProfileCardResponse(rawResult.data)
                }.fold(
                    onSuccess = { ApiResult.Success(it) },
                    onFailure = {
                        ApiResult.Error(
                            it.message ?: "Unable to parse member profile response",
                        )
                    },
                )
            }

            is ApiResult.Error -> ApiResult.Error(rawResult.message, rawResult.code)
            ApiResult.Loading -> ApiResult.Loading
            ApiResult.Idle -> ApiResult.Idle
        }
    }

    suspend fun updateMemberProfile(
        memberId: String,
        accessToken: String,
        request: MemberProfileUpdateRequest,
        gymId: String? = null,
    ): ApiResult<MemberProfileDetailResponse> {
        val body = request.normalizedPhonesForApi()
        val rawResult: ApiResult<JsonElement> = ApiClient.safeApiCall {
            patch(ApiEndpoints.Members.memberProfile(memberId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (!gymId.isNullOrBlank()) {
                    parameter("gymId", gymId)
                }
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
        return when (rawResult) {
            is ApiResult.Success -> {
                runCatching {
                    parseMemberProfileCardResponse(rawResult.data)
                }.fold(
                    onSuccess = { ApiResult.Success(it) },
                    onFailure = {
                        ApiResult.Error(
                            it.message ?: "Unable to parse updated member profile response",
                        )
                    },
                )
            }

            is ApiResult.Error -> ApiResult.Error(rawResult.message, rawResult.code)
            ApiResult.Loading -> ApiResult.Loading
            ApiResult.Idle -> ApiResult.Idle
        }
    }

    suspend fun convertMember(
        gymId: String, memberId: String, accessToken: String
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Members.convert(memberId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
            }
        }
    }

    suspend fun getMemberAttendanceSummary(
        gymId: String,
        memberId: String,
        accessToken: String,
        month: String? = null,
        year: String? = null,
        date: String? = null,
    ): ApiResult<MemberAttendanceSummaryResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Members.attendanceSummary(memberId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                if (month != null) parameter("month", month)
                if (year != null) parameter("year", year)
                if (!date.isNullOrBlank()) parameter("date", date)
            }
        }
    }

    suspend fun getWorkouts(
        memberId: String, accessToken: String
    ): ApiResult<List<WorkoutDTO>> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Members.workouts(memberId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }
    }

    suspend fun getSubscriptions(
        gymId: String,
        accessToken: String,
        q: String? = null,
        tab: String = "active",
        limit: Int = 20,
        offset: Int = 0
    ): ApiResult<SubscriptionListResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Subscriptions.LIST) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                parameter("tab", tab)
                parameter("limit", limit)
                parameter("offset", offset)
                if (!q.isNullOrBlank()) parameter("q", q)
            }
        }
    }

    suspend fun receivePayment(
        accessToken: String, request: ReceivePaymentRequest
    ): ApiResult<ReceivePaymentResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Payments.BASE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun extendPlanPayment(
        accessToken: String,
        request: ExtendPlanPaymentRequest,
    ): ApiResult<ReceivePaymentResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Payments.BASE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun freezeSubscription(
        accessToken: String,
        request: FreezeSubscriptionRequest,
    ): ApiResult<SubscriptionDTO> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Plans.FREEZE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun unfreezeSubscription(
        accessToken: String,
        request: UnfreezeSubscriptionRequest,
    ): ApiResult<SubscriptionDTO> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Plans.UNFREEZE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun createTrainerSalaryPayment(
        accessToken: String, request: CreateTrainerSalaryPaymentRequest
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Payments.SALARY) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun createSubscriptionCompat(
        accessToken: String, request: CreateSubscriptionCompatRequest
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Subscriptions.LIST) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun assignMemberPlan(
        accessToken: String,
        request: AssignMemberPlanRequest,
        gymId: String? = null,
    ): ApiResult<AssignMemberPlanResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Plans.MEMBER_PLANS) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (!gymId.isNullOrBlank()) parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun getPaymentAnalytics(
        gymId: String,
        accessToken: String,
        range: String = "monthly",
        from: String? = null,
        to: String? = null
    ): ApiResult<PaymentAnalyticsResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Payments.ANALYTICS) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                parameter("range", range)
                if (from != null) parameter("from", from)
                if (to != null) parameter("to", to)
            }
        }
    }

    suspend fun getPayments(
        gymId: String,
        accessToken: String,
        status: String? = null,
        memberId: String? = null,
        search: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ): ApiResult<PaymentListResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Payments.BASE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                if (status != null) parameter("status", status)
                if (memberId != null) parameter("memberId", memberId)
                if (search != null) parameter("search", search)
                parameter("limit", limit)
                parameter("offset", offset)
            }
        }
    }

    suspend fun createSubscriptionWithBody(
        accessToken: String, request: CreateSubscriptionWithBodyRequest
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Subscriptions.LIST) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }


    suspend fun getProfile(accessToken: String): ApiResult<ApiResponse<ProfileResponse>> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Core.PROFILE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }
    }

    suspend fun listUserGyms(accessToken: String): ApiResult<UserGymsListResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Gyms.LIST) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }
    }

    suspend fun createOwnedGym(
        accessToken: String,
        request: CreateOwnedGymRequest,
    ): ApiResult<CreateOwnedGymResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Gyms.LIST) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun updateOwnedGym(
        accessToken: String,
        gymId: String,
        request: CreateOwnedGymRequest,
    ): ApiResult<UpdateGymResponse> {
        return ApiClient.safeApiCall {
            patch(ApiEndpoints.Gyms.detail(gymId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun deleteOwnedGym(
        accessToken: String,
        gymId: String,
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            delete(ApiEndpoints.Gyms.detail(gymId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }
    }

    // Member logic
    suspend fun addMember(
        accessToken: String, request: AddMemberRequest
    ): ApiResult<AddMemberResponse> {
        val body = request.copy(
            phone = PhoneNumberUtils.withIndiaCountryCodeForApiRequired(request.phone),
            emergencyContactPhone = PhoneNumberUtils.withIndiaCountryCodeForApi(request.emergencyContactPhone)
        )
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Members.ADD) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    suspend fun deleteMember(
        gymId: String, memberId: String, accessToken: String
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            delete(ApiEndpoints.Members.detail(memberId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
            }
        }
    }

    suspend fun updateMember(
        memberId: String,
        accessToken: String,
        request: AddMemberRequest
    ): ApiResult<AddMemberResponse> {
        val body = request.copy(
            phone = PhoneNumberUtils.withIndiaCountryCodeForApiRequired(request.phone),
            emergencyContactPhone = PhoneNumberUtils.withIndiaCountryCodeForApi(request.emergencyContactPhone)
        )
        return ApiClient.safeApiCall {
            put(ApiEndpoints.Members.detail(memberId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    // Trainer logic (Merged from TrainerRepository)
    suspend fun getTrainers(
        gymId: String,
        accessToken: String,
        query: String? = null,
        limit: Int = 20,
        offset: Int = 0,
        includeInactive: Boolean = false,
        role: String = GymStaffListRole.TRAINER
    ): ApiResult<TrainerListResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Trainers.LIST) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                parameter("limit", limit)
                parameter("offset", offset)
                if (!query.isNullOrBlank()) parameter("q", query)
                if (includeInactive) parameter("includeInactive", true)
                if (role.isNotBlank()) parameter("role", role)
            }
        }
    }



    suspend fun createTrainerCompat(
        accessToken: String, request: CreateTrainerCompatRequest
    ): ApiResult<CreateTrainerResponse> {
        val body = request.copy(
            phone = PhoneNumberUtils.withIndiaCountryCodeForApiRequired(request.phone)
        )
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Trainers.COMPAT_CREATE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    suspend fun getTrainerDetail(
        gymId: String,
        trainerId: String,
        accessToken: String,
        role: String = GymStaffListRole.TRAINER
    ): ApiResult<TrainerDetailResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Trainers.detail(trainerId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                if (role.isNotBlank()) parameter("role", role)
            }
        }
    }

    suspend fun setTrainerPassword(
        gymId: String, trainerId: String, accessToken: String, request: SetTrainerPasswordRequest
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            put(ApiEndpoints.Trainers.password(trainerId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun deleteTrainer(
        gymId: String,
        trainerId: String,
        accessToken: String,
        role: String = GymStaffListRole.TRAINER
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            delete(ApiEndpoints.Trainers.detail(trainerId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                if (role.isNotBlank()) parameter("role", role)
            }
        }
    }

    suspend fun updateTrainer(
        gymId: String,
        trainerId: String,
        accessToken: String,
        request: UpdateTrainerRequest,
        role: String = GymStaffListRole.TRAINER
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            patch(ApiEndpoints.Trainers.detail(trainerId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                if (role.isNotBlank()) parameter("role", role)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    // Shared Image Upload
    suspend fun uploadImage(
        accessToken: String,
        imageBytes: ByteArray,
        fileName: String,
        mimeType: String = "image/jpeg"
    ): ApiResult<UploadImageResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Uploads.IMAGE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append("file", imageBytes, Headers.build {
                                append(HttpHeaders.ContentType, mimeType)
                                append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                            })
                        })
                )
            }
        }
    }

    // Enquiry logic
    suspend fun getEnquiries(
        gymId: String,
        accessToken: String,
        status: String? = null,
        query: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ): ApiResult<EnquiryListResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Enquiries.BASE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                parameter("limit", limit)
                parameter("offset", offset)
                if (!status.isNullOrBlank()) parameter("status", status)
                if (!query.isNullOrBlank()) parameter("q", query)
            }
        }
    }

    suspend fun createEnquiry(
        accessToken: String, request: CreateEnquiryRequest
    ): ApiResult<EnquiryDTO> {
        val body = request.copy(
            phone = PhoneNumberUtils.withIndiaCountryCodeForApi(request.phone)
        )
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Enquiries.BASE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }

    suspend fun patchEnquiry(
        id: String, gymId: String, accessToken: String, request: UpdateEnquiryRequest
    ): ApiResult<EnquiryDTO> {
        val body = request.copy(
            phone = request.phone?.let { PhoneNumberUtils.withIndiaCountryCodeForApi(it) }
        )
        return ApiClient.safeApiCall {
            patch(ApiEndpoints.Enquiries.detail(id)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(body)
            }
        }
    }


    suspend fun getEnquiryDetail(
        gymId: String, enquiryId: String, accessToken: String
    ): ApiResult<EnquiryDTO> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Enquiries.detail(enquiryId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
            }
        }
    }

    suspend fun convertEnquiry(
        enquiryId: String, accessToken: String, gymId: String
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Enquiries.convert(enquiryId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
            }
        }
    }


    suspend fun createPlanCompat(
        accessToken: String, request: CreatePlanCompatRequest
    ): ApiResult<CreatePlanResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Plans.COMPAT_CREATE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun updatePlanCompat(
        accessToken: String, planId: String, request: CreatePlanCompatRequest
    ): ApiResult<CreatePlanResponse> {
        return ApiClient.safeApiCall {
            put(ApiEndpoints.Plans.detail(planId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", request.gymId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun getPlans(
        gymId: String, accessToken: String, limit: Int = 20, offset: Int = 0
    ): ApiResult<PlanListResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Plans.LIST) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                parameter("limit", limit)
                parameter("offset", offset)
            }
        }
    }

    suspend fun getPlanDetail(
        gymId: String, planId: String, accessToken: String
    ): ApiResult<CreatePlanResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Plans.detail(planId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
            }
        }
    }

    suspend fun getEnrolledMembers(
        gymId: String, planId: String, accessToken: String, limit: Int = 20, offset: Int = 0
    ): ApiResult<PlanEnrolledResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Plans.enrolled(planId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                parameter("limit", limit)
                parameter("offset", offset)
            }
        }
    }

    suspend fun deletePlan(
        gymId: String,
        planId: String,
        accessToken: String
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            delete(ApiEndpoints.Plans.detail(planId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
            }
        }
    }

    suspend fun updateProfile(
        accessToken: String, gymId: String, request: UpdateProfileRequest
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            put(ApiEndpoints.Core.PROFILE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun updateOwnerSelfProfile(
        accessToken: String, gymId: String, request: UpdateOwnerProfileRequest
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            put(ApiEndpoints.Core.UPDATE_OWNER_PROFILE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun updateTrainerSelfProfile(
        accessToken: String, gymId: String, request: UpdateTrainerProfileRequest
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            put(ApiEndpoints.Core.UPDATE_TRAINER_PROFILE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun deleteProfile(accessToken: String): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            delete(ApiEndpoints.Core.PROFILE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }
    }

    suspend fun createWorkout(
        accessToken: String, request: CreateWorkoutRequest
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Workouts.BASE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun getPersonalWorkouts(
        accessToken: String,
        gymId: String? = null,
        createdBy: String? = null,
    ): ApiResult<List<WorkoutDTO>> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Workouts.BASE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (!gymId.isNullOrBlank()) {
                    parameter("gymId", gymId)
                }
                parameter("created_by", createdBy?.takeIf { it.isNotBlank() } ?: "all")
            }
        }
    }

    suspend fun getWorkoutHistory(
        accessToken: String,
        gymId: String? = null,
        page: Int = 0,
        limit: Int = 20,
        from: String? = null,
        to: String? = null,
        completed: Boolean? = null,
    ): ApiResult<MemberWorkoutHistoryPageResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Workouts.HISTORY) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (!gymId.isNullOrBlank()) parameter("gymId", gymId)
                if (!from.isNullOrBlank()) parameter("from", from)
                if (!to.isNullOrBlank()) parameter("to", to)
                if (completed != null) parameter("completed", completed)
                parameter("page", page)
                parameter("limit", limit)
            }
        }
    }

    suspend fun getWorkoutDetail(
        accessToken: String, workoutId: String
    ): ApiResult<WorkoutDetailResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Workouts.detail(workoutId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }
    }

    suspend fun getWorkoutCompletion(
        accessToken: String,
        workoutId: String,
    ): ApiResult<WorkoutCompletionResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Workouts.memberCompletion(workoutId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }
    }

    suspend fun updateWorkoutCompletion(
        accessToken: String,
        workoutId: String,
        request: UpdateWorkoutCompletionRequest,
    ): ApiResult<WorkoutCompletionResponse> {
        return ApiClient.safeApiCall {
            patch(ApiEndpoints.Workouts.memberCompletion(workoutId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun completeWorkout(
        accessToken: String,
        workoutId: String
    ): ApiResult<WorkoutDetailResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Workouts.complete(workoutId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }
    }

    suspend fun deleteWorkout(
        accessToken: String, gymId: String, workoutId: String
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            delete(ApiEndpoints.Workouts.trainerDelete(workoutId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
            }
        }
    }

    suspend fun updateWorkoutLegacy(
        accessToken: String,
        request: UpdateWorkoutLegacyRequest,
        gymId: String? = null
    ): ApiResult<WorkoutDetailResponse> {
        return ApiClient.safeApiCall {
            patch(ApiEndpoints.Workouts.BASE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (!gymId.isNullOrBlank()) parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun updateWorkout(
        accessToken: String,
        request: CompleteWorkoutRequest,
        gymId: String? = null,
    ): ApiResult<WorkoutDetailResponse> {
        return ApiClient.safeApiCall {
            patch(ApiEndpoints.Workouts.BASE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (!gymId.isNullOrBlank()) parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun deleteWorkoutLegacy(
        accessToken: String,
        workoutId: String,
        gymId: String? = null
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            delete(ApiEndpoints.Workouts.BASE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (!gymId.isNullOrBlank()) parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(mapOf("workout_id" to workoutId))
            }
        }
    }

    suspend fun getDietMeals(
        gymId: String,
        accessToken: String,
        createdBy: String? = null,
    ): ApiResult<List<DietMealDTO>> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Diet.MEALS) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (gymId.isNotEmpty())
                    parameter("gymId", gymId)
                parameter("created_by", createdBy?.takeIf { it.isNotBlank() } ?: "all")
            }
        }
    }

    suspend fun getDietHistory(
        accessToken: String,
        date: String? = null,
        gymId: String? = null,
        targetKcal: Int? = null
    ): ApiResult<DietHistoryResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Diet.HISTORY) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (!date.isNullOrBlank()) parameter("date", date)
                if (!gymId.isNullOrBlank()) parameter("gymId", gymId)
                if (targetKcal != null) parameter("target_kcal", targetKcal)
            }
        }
    }

    suspend fun getBroadcastChannels(
        gymId: String,
        accessToken: String,
        page: Int = 1,
        limit: Int = 20,
        search: String? = null
    ): ApiResult<BroadcastChannelsResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Broadcast.CHANNELS) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                parameter("page", page)
                parameter("limit", limit)
                if (!search.isNullOrBlank()) parameter("search", search)
            }
        }
    }

    suspend fun createBroadcastChannel(
        accessToken: String, request: CreateBroadcastChannelRequest
    ): ApiResult<CreateBroadcastChannelResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Broadcast.CHANNELS) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun getBroadcastMembers(
        channelId: String,
        accessToken: String,
        page: Int = 1,
        limit: Int = 20,
        search: String? = null
    ): ApiResult<BroadcastMembersResponse> {
        val rawResult: ApiResult<JsonElement> = ApiClient.safeApiCall {
            get(ApiEndpoints.Broadcast.members(channelId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("page", page)
                parameter("limit", limit)
                if (!search.isNullOrBlank()) parameter("search", search)
            }
        }
        return when (rawResult) {
            is ApiResult.Success -> Success(parseBroadcastMembersResponse(rawResult.data))
            is ApiResult.Error -> Error(rawResult.message, rawResult.code)
            ApiResult.Loading -> ApiResult.Loading
            ApiResult.Idle -> TODO()
        }
    }

    suspend fun addBroadcastMembers(
        gymId: String,
        accessToken: String,
        search: String? = null
    ): ApiResult<List<BroadcastMemberDTO>> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Broadcast.MEMBERS_LIST) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                if (!search.isNullOrBlank()) parameter("search", search)
            }
        }
    }

    suspend fun addMembersToChannel(
        channelId: String,
        accessToken: String,
        gymUserIds: List<String>
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Broadcast.addMembers(channelId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(AddBroadcastMembersRequest(gymUserIds = gymUserIds))
            }
        }
    }

    suspend fun getBroadcastMessages(
        channelId: String,
        bearerToken: String,
        page: Int = 1,
        limit: Int = 20
    ): ApiResult<BroadcastMessagesResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Broadcast.messages(channelId)) {
                header(HttpHeaders.Authorization, "Bearer $bearerToken")
                parameter("page", page)
                parameter("limit", limit)
            }
        }
    }

    suspend fun sendMessage(
        channelId: String,
        accessToken: String,
        title: String,
        description: String? = null,
        imageUrl: String? = null
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Broadcast.messages(channelId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(
                    CreateBroadcastMessageRequest(
                        title = title,
                        description = description,
                        imageUrl = imageUrl
                    )
                )
            }
        }
    }

    suspend fun getBroadcastChannelDetail(
        channelId: String,
        accessToken: String
    ): ApiResult<BroadcastChannelDetailDTO> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Broadcast.detail(channelId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }
    }

    suspend fun deleteBroadcastChannel(
        channelId: String,
        accessToken: String
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            delete(ApiEndpoints.Broadcast.delete(channelId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }
    }

    /**
     * Backend currently returns multiple shapes for members:
     * 1) Standard paginated payload: { page, limit, total, data: [...] }
     * 2) Nested under imageUrl (legacy/buggy payload): { ..., imageUrl: { page, limit, total, data: [...] } }
     */
    private fun parseBroadcastMembersResponse(payload: JsonElement): BroadcastMembersResponse {
        val root = payload as? JsonObject ?: return BroadcastMembersResponse()
        val nested = root["imageUrl"] as? JsonObject
        val source = nested ?: root

        val members = decodeMembersArray(source["data"])
        val page = source["page"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: 1
        val limit = source["limit"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: members.size
        val total = source["total"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: members.size

        return BroadcastMembersResponse(
            page = page,
            limit = limit,
            total = total,
            data = members
        )
    }

    private fun decodeMembersArray(element: JsonElement?): List<BroadcastMemberDTO> {
        val array = element as? JsonArray ?: return emptyList()
        return array.mapNotNull { item ->
            runCatching { broadcastJson.decodeFromJsonElement<BroadcastMemberDTO>(item) }.getOrNull()
        }
    }

    /**
     * PATCH /members/{memberId}/profile currently returns multiple shapes:
     * 1) profile-card shape: { id, name, phone, ... }
     * 2) legacy detail shape: { gymUserId, user{...}, summary{...}, subscription{...}, ... }
     * This normalizes both into [MemberProfileDetailResponse].
     */
    private fun parseMemberProfileCardResponse(payload: JsonElement): MemberProfileDetailResponse {
        val root = payload as? JsonObject
            ?: throw IllegalArgumentException("Invalid profile payload")

        // If backend already returned the target shape, decode directly then merge nested subscription.
        if (root["id"] != null && root["name"] != null && root["phone"] != null) {
            val decoded = broadcastJson.decodeFromJsonElement<MemberProfileDetailResponse>(payload)
            return mergeProfileSubscriptionFields(decoded, root)
        }

        val summary = root["summary"] as? JsonObject
        val user = root["user"] as? JsonObject
        val subscription = root["subscription"] as? JsonObject
        val statsFromSubscription = subscription?.get("stats")
        val currentFromSubscription = subscription?.get("current_subscription")

        val id = root["id"]?.jsonPrimitive?.contentOrNull
            ?: user?.get("id")?.jsonPrimitive?.contentOrNull
            ?: root["gymUserId"]?.jsonPrimitive?.contentOrNull
            ?: ""
        val name = root["name"]?.jsonPrimitive?.contentOrNull
            ?: summary?.get("name")?.jsonPrimitive?.contentOrNull
            ?: user?.get("fullName")?.jsonPrimitive?.contentOrNull
            ?: ""
        val phone = root["phone"]?.jsonPrimitive?.contentOrNull
            ?: summary?.get("phone")?.jsonPrimitive?.contentOrNull
            ?: user?.get("phone")?.jsonPrimitive?.contentOrNull
            ?: ""

        val stats = runCatching {
            broadcastJson.decodeFromJsonElement<SubscriptionStatsDTO>(
                root["stats"] ?: statsFromSubscription ?: JsonObject(emptyMap()),
            )
        }.getOrDefault(SubscriptionStatsDTO())

        val currentSubscription = runCatching {
            val raw = root["current_subscription"] ?: currentFromSubscription ?: return@runCatching null
            broadcastJson.decodeFromJsonElement<CurrentSubscriptionDTO>(raw)
        }.getOrNull()

        val currentSubscriptions = parseProfileSubscriptionList(root, subscription, "current_subscriptions")
        val upcomingSubscriptions = parseProfileSubscriptionList(root, subscription, "upcoming_subscriptions")
        val expiredSubscriptions = parseProfileSubscriptionList(root, subscription, "expired_subscriptions")
        val pastSubscriptions = parseProfileSubscriptionList(root, subscription, "past_subscriptions")
        val freezeSubscriptions = parseProfileSubscriptionList(root, subscription, "freeze_subscriptions")

        val gym = runCatching {
            root["gym"]?.let { broadcastJson.decodeFromJsonElement<MemberProfileGymDTO>(it) }
        }.getOrNull()

        fun flexibleAgeString(element: JsonElement?): String? {
            val prim = element?.jsonPrimitive ?: return null
            if (prim.isString) return prim.content.trim().takeIf { it.isNotEmpty() }
            prim.intOrNull?.let { return it.toString() }
            prim.longOrNull?.let { return it.toString() }
            prim.doubleOrNull?.let { return it.toInt().toString() }
            return null
        }

        fun flexibleDouble(element: JsonElement?): Double? {
            val prim = element?.jsonPrimitive ?: return null
            if (prim.isString) return prim.content.trim().toDoubleOrNull()
            return prim.doubleOrNull ?: prim.intOrNull?.toDouble() ?: prim.longOrNull?.toDouble()
        }

        val wellnessParsed = root["wellness"]?.let { w ->
            runCatching { broadcastJson.decodeFromJsonElement<MemberProfileWellnessDTO>(w) }.getOrNull()
        }

        return MemberProfileDetailResponse(
            id = id,
            name = name,
            phone = phone,
            gender = root["gender"]?.jsonPrimitive?.contentOrNull,
            dob = root["dob"]?.jsonPrimitive?.contentOrNull
                ?: root["dateOfBirth"]?.jsonPrimitive?.contentOrNull,
            age = flexibleAgeString(root["age"]) ?: flexibleAgeString(summary?.get("age")),
            join_date = root["join_date"]?.jsonPrimitive?.contentOrNull
                ?: root["joinedAt"]?.jsonPrimitive?.contentOrNull,
            status = root["status"]?.jsonPrimitive?.contentOrNull
                ?: root["lifecycleStatus"]?.jsonPrimitive?.contentOrNull,
            profile_image = root["profile_image"]?.jsonPrimitive?.contentOrNull
                ?: summary?.get("profile_image")?.jsonPrimitive?.contentOrNull
                ?: user?.get("avatarUrl")?.jsonPrimitive?.contentOrNull,
            heightCm = flexibleDouble(root["heightCm"]),
            weightKg = flexibleDouble(root["weightKg"]),
            activityLevel = root["activityLevel"]?.jsonPrimitive?.contentOrNull,
            fitnessGoal = root["fitnessGoal"]?.jsonPrimitive?.contentOrNull,
            wellness = wellnessParsed,
            gym = gym,
            stats = stats,
            current_subscription = currentSubscription ?: currentSubscriptions.firstOrNull(),
            current_subscriptions = currentSubscriptions,
            upcoming_subscriptions = upcomingSubscriptions,
            expired_subscriptions = expiredSubscriptions,
            past_subscriptions = pastSubscriptions,
            freeze_subscriptions = freezeSubscriptions,
        )
    }

    private fun parseProfileSubscriptionList(
        root: JsonObject,
        subscription: JsonObject?,
        key: String,
    ): List<CurrentSubscriptionDTO> {
        val raw = root[key] ?: subscription?.get(key) ?: return emptyList()
        return runCatching {
            broadcastJson.decodeFromJsonElement<List<CurrentSubscriptionDTO>>(raw)
        }.getOrDefault(emptyList())
    }

    private fun mergeProfileSubscriptionFields(
        base: MemberProfileDetailResponse,
        root: JsonObject,
    ): MemberProfileDetailResponse {
        val subscription = root["subscription"] as? JsonObject

        val stats = base.stats ?: subscription?.get("stats")?.let {
            runCatching { broadcastJson.decodeFromJsonElement<SubscriptionStatsDTO>(it) }.getOrNull()
        }

        val currentSubscriptions = base.current_subscriptions.ifEmpty {
            parseProfileSubscriptionList(root, subscription, "current_subscriptions")
        }
        val currentSubscription = base.current_subscription
            ?: subscription?.get("current_subscription")?.let {
                runCatching { broadcastJson.decodeFromJsonElement<CurrentSubscriptionDTO>(it) }.getOrNull()
            }
            ?: currentSubscriptions.firstOrNull()

        return base.copy(
            gym = base.gym ?: root["gym"]?.let {
                runCatching { broadcastJson.decodeFromJsonElement<MemberProfileGymDTO>(it) }.getOrNull()
            },
            stats = stats,
            current_subscription = currentSubscription,
            current_subscriptions = currentSubscriptions,
            upcoming_subscriptions = base.upcoming_subscriptions.ifEmpty {
                parseProfileSubscriptionList(root, subscription, "upcoming_subscriptions")
            },
            expired_subscriptions = base.expired_subscriptions.ifEmpty {
                parseProfileSubscriptionList(root, subscription, "expired_subscriptions")
            },
            past_subscriptions = base.past_subscriptions.ifEmpty {
                parseProfileSubscriptionList(root, subscription, "past_subscriptions")
            },
            freeze_subscriptions = base.freeze_subscriptions.ifEmpty {
                parseProfileSubscriptionList(root, subscription, "freeze_subscriptions")
            },
        )
    }

    suspend fun updateBroadcastChannel(
        channelId: String,
        accessToken: String,
        request: UpdateBroadcastChannelRequest
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            patch(ApiEndpoints.Broadcast.detail(channelId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun getDietFoodCatalog(
        gymId: String, accessToken: String, search: String? = null
    ): ApiResult<List<DietCatalogFoodDTO>> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Diet.FOOD) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (gymId.isNotEmpty())
                    parameter("gymId", gymId)
                if (!search.isNullOrBlank()) parameter("search", search)
            }
        }
    }

    suspend fun createDietMeal(
        gymId: String, accessToken: String, request: CreateDietMealRequest
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Diet.MEALS) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (gymId.isNotEmpty())
                    parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun getExpenses(
        gymId: String,
        accessToken: String,
        month: String? = null,
        category: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ): ApiResult<ExpenseResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Expenses.BASE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                if (!month.isNullOrBlank()) parameter("filter", month)
                if (!category.isNullOrBlank()) parameter("category", category)
                if (!dateFrom.isNullOrBlank()) parameter("dateFrom", dateFrom)
                if (!dateTo.isNullOrBlank()) parameter("dateTo", dateTo)
                parameter("limit", limit)
                parameter("offset", offset)
            }
        }
    }

    suspend fun getLeaves(
        gymId: String,
        accessToken: String,
        status: String? = null,
        month: String? = null,
        dateFrom: String? = null,
        dateTo: String? = null,
        trainerId: String? = null,
        q: String? = null,
        limit: Int = 20,
        offset: Int = 0
    ): ApiResult<LeaveListResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Leaves.BASE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                if (!status.isNullOrBlank()) parameter("status", status)
                if (!month.isNullOrBlank()) parameter("month", month)
                if (!dateFrom.isNullOrBlank()) parameter("dateFrom", dateFrom)
                if (!dateTo.isNullOrBlank()) parameter("dateTo", dateTo)
                if (!trainerId.isNullOrBlank()) parameter("trainerId", trainerId)
                if (!q.isNullOrBlank()) parameter("q", q)
                parameter("limit", limit)
                parameter("offset", offset)
            }
        }
    }

    suspend fun approveLeave(
        gymId: String, leaveId: String, accessToken: String
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            patch(ApiEndpoints.Leaves.approve(leaveId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
            }
        }
    }

    suspend fun rejectLeave(
        gymId: String, leaveId: String, accessToken: String, request: RejectLeaveRequest
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            patch(ApiEndpoints.Leaves.reject(leaveId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun createExpense(
        gymId: String, accessToken: String, request: CreateExpenseRequest
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Expenses.BASE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun getExpenseById(
        id: String, gymId: String, accessToken: String
    ): ApiResult<ExpenseDTO> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Expenses.detail(id)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
            }
        }
    }

    suspend fun updateExpense(
        id: String, gymId: String, accessToken: String, request: UpdateExpenseRequest
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            patch(ApiEndpoints.Expenses.detail(id)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun deleteExpense(
        id: String, gymId: String, accessToken: String
    ): ApiResult<DeleteExpenseResponse> {
        return ApiClient.safeApiCall {
            delete(ApiEndpoints.Expenses.detail(id)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
            }
        }
    }

    suspend fun createDietFood(
        gymId: String, accessToken: String, request: CreateDietCatalogFoodRequest
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Diet.FOOD) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (gymId.isNotEmpty())
                    parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun updateDietMeal(
        gymId: String, mealId: String, accessToken: String, request: CreateDietMealRequest
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            patch(ApiEndpoints.Diet.mealDetail(mealId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (gymId.isNotEmpty())

                    parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun deleteDietMeal(
        gymId: String, mealId: String, accessToken: String
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            delete(ApiEndpoints.Diet.mealDetail(mealId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (gymId.isNotEmpty())

                    parameter("gymId", gymId)
            }
        }
    }

    suspend fun consumeDietFood(
        gymId: String?,
        memberId: String?,
        accessToken: String,
        request: ConsumeDietFoodRequest
    ): ApiResult<ConsumeDietFoodResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Diet.FOOD_CONSUME) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (!gymId.isNullOrBlank()) parameter("gymId", gymId)
//                if (!memberId.isNullOrBlank()) parameter("member_id", memberId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun createLeave(
        accessToken: String, request: CreateLeaveRequest
    ): ApiResult<CreateLeaveApiResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Leaves.BASE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun createProduct(
        accessToken: String, request: CreateProductRequest
    ): ApiResult<CreateProductApiResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Products.BASE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun getProducts(
        gymId: String,
        accessToken: String,
        page: Int = 1,
        limit: Int = 10,
        search: String? = null,
        category: String? = null,
        includeInactive: Boolean? = null
    ): ApiResult<ProductListResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Products.BASE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                parameter("gym_id", gymId)
                parameter("page", page.toString())
                parameter("limit", limit.toString())
                if (!search.isNullOrBlank()) parameter("search", search)
                if (!category.isNullOrBlank()) parameter("category", category)
                if (includeInactive != null) {
                    parameter("include_inactive", if (includeInactive) "true" else "false")
                }
            }
        }
    }

    suspend fun getProductDetail(
        productId: String, gymId: String, accessToken: String
    ): ApiResult<ProductDetailApiResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Products.detail(productId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                parameter("gym_id", gymId)
            }
        }
    }

    suspend fun updateProduct(
        productId: String, gymId: String, accessToken: String, request: UpdateProductRequest
    ): ApiResult<ProductMutationResponse> {
        return ApiClient.safeApiCall {
            patch(ApiEndpoints.Products.detail(productId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                parameter("gym_id", gymId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun deleteProduct(
        productId: String, gymId: String, accessToken: String
    ): ApiResult<ProductMutationResponse> {
        return ApiClient.safeApiCall {
            delete(ApiEndpoints.Products.detail(productId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                parameter("gym_id", gymId)
            }
        }
    }

    suspend fun getFavorites(
        gymId: String?,
        accessToken: String
    ): ApiResult<FavoritesListResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Favorites.BASE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (!gymId.isNullOrBlank()) {
                    parameter("gymId", gymId)
                    parameter("gym_id", gymId)
                }
            }
        }
    }

    suspend fun addFavorite(
        productId: String,
        gymId: String?,
        accessToken: String
    ): ApiResult<ProductMutationResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Favorites.detail(productId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (!gymId.isNullOrBlank()) {
                    parameter("gymId", gymId)
                    parameter("gym_id", gymId)
                }
            }
        }
    }

    suspend fun removeFavorite(
        productId: String,
        gymId: String?,
        accessToken: String
    ): ApiResult<ProductMutationResponse> {
        return ApiClient.safeApiCall {
            delete(ApiEndpoints.Favorites.detail(productId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (!gymId.isNullOrBlank()) {
                    parameter("gymId", gymId)
                    parameter("gym_id", gymId)
                }
            }
        }
    }

    suspend fun startWorkout(
        accessToken: String,
        workoutId: String,
        gymId: String? = null
    ): ApiResult<WorkoutDetailResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Workouts.START) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (!gymId.isNullOrBlank()) parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(mapOf("workout_id" to workoutId))
            }
        }
    }


    suspend fun createWorkoutSet(
        accessToken: String,
        request: CreateSetRequest,
        gymId: String? = null,
    ): ApiResult<WorkoutDetailSetDTO> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.ExerciseSets.BASE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (!gymId.isNullOrBlank()) parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun updateWorkoutSet(
        accessToken: String,
        setId: String,
        request: UpdateSetRequest,
        gymId: String? = null,
    ): ApiResult<WorkoutDetailSetDTO> {
        return ApiClient.safeApiCall {
            put(ApiEndpoints.Sets.detail(setId)) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (!gymId.isNullOrBlank()) parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun stopWorkout(
        accessToken: String,
        workoutId: String,
        gymId: String? = null
    ): ApiResult<WorkoutDetailResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Workouts.STOP) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (!gymId.isNullOrBlank()) parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(mapOf("workout_id" to workoutId))
            }
        }
    }

    suspend fun pauseWorkout(
        accessToken: String,
        workoutId: String,
        gymId: String? = null,
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Workouts.PAUSE) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (!gymId.isNullOrBlank()) parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(mapOf("workout_id" to workoutId))
            }
        }
    }

    suspend fun resumeWorkout(
        accessToken: String,
        workoutId: String,
        gymId: String? = null,
    ): ApiResult<GenericSuccessResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Workouts.RESUME) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (!gymId.isNullOrBlank()) parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(mapOf("workout_id" to workoutId))
            }
        }
    }

    suspend fun stopWorkout(
        accessToken: String,
        request: CompleteWorkoutRequest,
        gymId: String? = null
    ): ApiResult<WorkoutDetailResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Workouts.STOP) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                if (!gymId.isNullOrBlank()) parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    suspend fun getWhatsAppAutomation(
        gymId: String,
        accessToken: String,
    ): ApiResult<WhatsAppAutomationResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.WhatsApp.AUTOMATION) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
            }
        }
    }

    suspend fun updateWhatsAppAutomation(
        gymId: String,
        accessToken: String,
        request: UpdateWhatsAppAutomationRequest,
    ): ApiResult<WhatsAppAutomationResponse> {
        return ApiClient.safeApiCall {
            put(ApiEndpoints.WhatsApp.AUTOMATION) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                parameter("gymId", gymId)
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }
}

private fun MemberProfileUpdateRequest.normalizedPhonesForApi(): MemberProfileUpdateRequest {
    val rawEmergency = emergencyContactPhone ?: emergency_contact_phone
    val normalizedEmergency = PhoneNumberUtils.withIndiaCountryCodeForApi(rawEmergency)
    val normalizedPhone = phone?.let { PhoneNumberUtils.withIndiaCountryCodeForApiRequired(it) }
    return copy(
        phone = normalizedPhone,
        emergencyContactPhone = normalizedEmergency,
        emergency_contact_phone = normalizedEmergency
    )
}
