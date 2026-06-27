package `in`.gym.trak.studio.viewmodel.auth

import `in`.gym.trak.studio.data.model.SwitchToMemberRequest
import `in`.gym.trak.studio.data.repository.AuthRepository
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.network.BaseScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthScreenModel : BaseScreenModel() {

    private val _isRegistered = MutableStateFlow<Boolean?>(null)
    val isRegistered = _isRegistered.asStateFlow()

    private val _tempToken = MutableStateFlow<String?>(null)
    val tempToken = _tempToken.asStateFlow()

    fun login(phone: String, onSuccess: () -> Unit) {
        executeApi(
            apiCall = { AuthRepository.login(phone) },
            onSuccess = { response ->
                _isRegistered.value = response.isRegistered
                SessionManager.clearPendingOtpTokens()
                response.temp_token?.takeIf { it.isNotBlank() }?.let { SessionManager.pendingOtpTempToken = it }
                response.access_token?.takeIf { it.isNotBlank() }?.let { SessionManager.pendingOtpAccessToken = it }
                onSuccess()
            }
        )
    }

    fun verifyOtp(phone: String, otp: String, onSuccess: (Boolean, String?, String?) -> Unit) {
        val pendingTemp = SessionManager.pendingOtpTempToken.takeIf { it.isNotBlank() }
        val pendingAccess = SessionManager.pendingOtpAccessToken.takeIf { it.isNotBlank() }
        executeApi(
            apiCall = {
                AuthRepository.verifyOtp(
                    phone = phone,
                    otp = otp,
                    pendingTempToken = pendingTemp,
                    pendingAccessToken = pendingAccess,
                )
            },
            onSuccess = { response ->
                SessionManager.clearPendingOtpTokens()
                _isRegistered.value = response.isRegistered
                _tempToken.value = response.tempToken

                if (response.isRegistered) {
                    SessionManager.accessToken = response.access_token ?: ""
                    SessionManager.refreshToken = response.refresh_token ?: ""
                    SessionManager.gymId = response.gym_id ?: response.user?.gym_id ?: ""
                    SessionManager.userId = response.user?.id ?: ""
                    SessionManager.userRole =
                        response.app_role ?: response.user?.role ?: response.role ?: ""
                    SessionManager.isLoggedIn = true
                }
                else {
                    SessionManager.accessToken = response.access_token ?: ""
                    SessionManager.refreshToken = response.refresh_token ?: ""
                }

                onSuccess(
                    response.isRegistered,
                    response.tempToken ?:response.access_token ,
                    response.app_role ?: response.user?.role ?: response.role
                )
            }
        )
    }

    fun setGym(tempToken: String, gymName: String, ownerName: String, onSuccess: () -> Unit) {
        executeApi(
            apiCall = { AuthRepository.setGym(tempToken, gymName, ownerName) },
            onSuccess = { response ->
                SessionManager.accessToken = response.access_token ?: ""
                SessionManager.refreshToken = response.refresh_token ?: ""
                SessionManager.gymId = response.gym_id ?: ""
                if (SessionManager.userRole.isEmpty()) {
                    SessionManager.userRole = "gym_owner"
                }
                SessionManager.isLoggedIn = true
                onSuccess()
            }
        )
    }

    fun selectRole(tempToken: String, role: String, onSuccess: () -> Unit) {
        executeApi(
            apiCall = { AuthRepository.selectRole(tempToken, role) },
            onSuccess = { response ->
                response.access_token?.takeIf { it.isNotBlank() }?.let { SessionManager.accessToken = it }
                response.refresh_token?.takeIf { it.isNotBlank() }?.let { SessionManager.refreshToken = it }
                onSuccess()
            }
        )
    }

    fun resendOtp(phone: String, onSuccess: () -> Unit) {
        executeApi(
            apiCall = { AuthRepository.resendOtp(phone) },
            onSuccess = { response ->
                SessionManager.clearPendingOtpTokens()
                response.temp_token?.takeIf { it.isNotBlank() }?.let { SessionManager.pendingOtpTempToken = it }
                response.access_token?.takeIf { it.isNotBlank() }?.let { SessionManager.pendingOtpAccessToken = it }
                onSuccess()
            }
        )
    }

    fun passwordLogin(username: String, password: String, onSuccess: (role: String) -> Unit) {
        executeApi(
            apiCall = { AuthRepository.passwordLogin(username, password) },
            onSuccess = { response ->
                print("user role ===> ${response.user?.role }")
                SessionManager.accessToken = response.access_token ?: ""
                SessionManager.refreshToken = response.refresh_token ?: ""
                SessionManager.userRole = response.user?.role ?: ""
                SessionManager.isLoggedIn = true
                onSuccess(response.user?.role ?: "")
            }
        )
    }

    fun switchToMember(
        name: String,
        age: Int,
        gender: String,
        height: Int,
        weight: Int,
        onSuccess: (role: String) -> Unit
    ) {
        val token = SessionManager.accessToken
        if (token.isBlank()) {
            showError("Session expired. Please login again.")
            return
        }

        val request = SwitchToMemberRequest(
            name = name,
            age = age,
            gender = gender.lowercase(),
            height = height,
            weight = weight
        )

        executeApi(
            apiCall = { AuthRepository.switchToMember(token, request) },
            onSuccess = { response ->
                SessionManager.accessToken = response.access_token
                SessionManager.refreshToken = response.refresh_token
                SessionManager.userRole = response.role
                SessionManager.isLoggedIn = true
                onSuccess(response.role)
            }
        )
    }
}
