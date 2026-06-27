package `in`.gym.trak.studio.data.repository

import `in`.gym.trak.studio.data.model.*
import `in`.gym.trak.studio.network.*
import `in`.gym.trak.studio.utils.PhoneNumberUtils
import io.ktor.client.request.*
import io.ktor.http.*

object AuthRepository {

    suspend fun login(phone: String): ApiResult<LoginResponse> {
        val normalizedPhone = PhoneNumberUtils.withIndiaCountryCodeForApiRequired(phone)
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Auth.LOGIN) {
                setBody(LoginRequest(phone = phone))
            }
        }
    }

    suspend fun passwordLogin(username: String, password: String): ApiResult<PasswordLoginResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Auth.LOGIN) {
                setBody(PasswordLoginRequest(username = username, password = password))
            }
        }
    }

    suspend fun verifyOtp(
        phone: String,
        otp: String,
        pendingTempToken: String? = null,
        pendingAccessToken: String? = null
    ): ApiResult<VerifyOtpResponse> {
        val temp = pendingTempToken?.takeIf { it.isNotBlank() }
        val access = pendingAccessToken?.takeIf { it.isNotBlank() }
        val bearer = access ?: temp
        val normalizedPhone = PhoneNumberUtils.withIndiaCountryCodeForApiRequired(phone)
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Auth.VERIFY_OTP) {
                if (bearer != null) {
                    header(HttpHeaders.Authorization, "Bearer $bearer")
                }
                setBody(
                    VerifyOtpRequest(
                        phone = phone,
                        otp = otp,
                        temp_token = temp,
                        access_token = access,
                    )
                )
            }
        }
    }

    suspend fun setGym(tempToken: String, gymName: String, ownerName: String): ApiResult<SetGymResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Gym.SET_GYM) {
                header(HttpHeaders.Authorization, "Bearer $tempToken")
                setBody(SetGymRequest(gym_name = gymName, owner_name = ownerName))
            }
        }
    }

    suspend fun selectRole(tempToken: String, role: String): ApiResult<SelectRoleResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.User.SELECT_ROLE) {
                header(HttpHeaders.Authorization, "Bearer $tempToken")
                setBody(SelectRoleRequest(role = role))
            }
        }
    }

    suspend fun refreshToken(refreshToken: String): ApiResult<RefreshTokenResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Auth.REFRESH_TOKEN) {
                setBody(RefreshTokenRequest(refreshToken = refreshToken))
            }
        }
    }

    suspend fun resendOtp(phone: String): ApiResult<LoginResponse> {
        val nationalPhone = PhoneNumberUtils.indianNationalDigitsForInput(phone)
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Auth.RESEND_OTP) {
                setBody(ResendOtpRequest(phone = nationalPhone))
            }
        }
    }

    suspend fun logout(refreshToken: String): ApiResult<Unit> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Auth.LOGOUT) {
                setBody(LogoutRequest(refresh_token = refreshToken))
            }
        }
    }

    suspend fun switchToMember(
        accessToken: String,
        request: SwitchToMemberRequest = SwitchToMemberRequest()
    ): ApiResult<SwitchPersonaResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Owner.SWITCH_TO_MEMBER) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                setBody(request)
            }
        }
    }

    suspend fun switchToOwner(accessToken: String): ApiResult<SwitchPersonaResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Owner.SWITCH_TO_OWNER) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }
    }

    suspend fun getProfileStatus(accessToken: String): ApiResult<ProfileStatusResponse> {
        return ApiClient.safeApiCall {
            get(ApiEndpoints.Owner.PROFILE_STATUS) {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }
    }
}

