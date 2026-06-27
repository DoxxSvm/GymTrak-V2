package `in`.gym.trak.studio.data.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@Serializable
data class LoginRequest(
    val phone: String,
    val country_code: String = "+91"
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class LoginResponse(
    val success: Boolean,
    val isRegistered: Boolean,
    val phone: String,
    @JsonNames("temp_token", "tempToken")
    val temp_token: String? = null,
    @JsonNames("access_token", "accessToken")
    val access_token: String? = null,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class VerifyOtpRequest(
    val phone: String,
    val country_code: String = "+91",
    val otp: String,
    @JsonNames("temp_token", "tempToken")
    val temp_token: String? = null,
    @JsonNames("access_token", "accessToken")
    val access_token: String? = null,
)

@Serializable
data class VerifyOtpResponse(
    val success: Boolean,
    val isRegistered: Boolean,
    val tempToken: String? = null,
    val access_token: String? = null,
    val refresh_token: String? = null,
    val app_role: String? = null,
    val role: String? = null,
    val gym_id: String? = null,
    val phone: String? = null,
    val user: UserData? = null
)

@Serializable
data class UserData(
    val id: String,
    val name: String? = null,
    val phone: String? = null,
    val role: String? = null,
    val gym_id: String? = null
)

@Serializable
data class SetGymRequest(
    val gym_name: String,
    val owner_name: String
)

@Serializable
data class SetGymResponse(
    val success: Boolean,
    val gym_id: String? = null,
    val gym_name: String? = null,
    val access_token: String? = null,
    val refresh_token: String? = null
)

@Serializable
data class SelectRoleRequest(
    val role: String
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SelectRoleResponse(
    val success: Boolean,
    val role: String? = null,
    @JsonNames("refresh_token", "refreshToken")
    val refresh_token: String? = null,
    @JsonNames("access_token", "accessToken")
    val access_token: String? = null,
)

@Serializable
data class PasswordLoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class PasswordLoginResponse(
    val success: Boolean,
    val access_token: String? = null,
    val refresh_token: String? = null,
    val user: PasswordLoginUser? = null
)

@Serializable
data class PasswordLoginUser(
    val id: String,
    val name: String? = null,
    val role: String? = null
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val accessExpiresInSeconds: Int? = null,
    val refreshExpiresAt: String? = null,
    val gymId: String? = null
)

@Serializable
data class ResendOtpRequest(
    val phone: String,
    val country_code: String = "+91"
)

@Serializable
data class LogoutRequest(
    val refresh_token: String
)

@Serializable
data class SwitchToMemberRequest(
    val name: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val height: Int? = null,
    val weight: Int? = null
)

@Serializable
data class SwitchPersonaResponse(
    val success: Boolean,
    val message: String,
    val role: String,
    val access_token: String,
    val refresh_token: String
)

@Serializable
data class ProfileStatusResponse(
    val isSwitcheable: Boolean,
    val lastActiveRole: String? = null
)
