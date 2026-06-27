package `in`.gym.trak.studio.network.example

import `in`.gym.trak.studio.network.ApiClient
import `in`.gym.trak.studio.network.ApiEndpoints
import `in`.gym.trak.studio.network.ApiResult
import `in`.gym.trak.studio.network.BaseScreenModel
import io.ktor.client.request.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

// ============================================
// 1. DTOs (Data Transfer Objects)
// ============================================
@Serializable
data class LoginRequest(
    val phone: String,
    val pin: String
)

@Serializable
data class LoginResponse(
    val token: String,
    val userId: String,
    val name: String
)

// ============================================
// 2. Repository Layer
// Separates the business structure of network calls
// ============================================
class AuthRepository {
    
    // An end-to-end example implementing the network request
    suspend fun login(request: LoginRequest): ApiResult<LoginResponse> {
        return ApiClient.safeApiCall {
            post(ApiEndpoints.Auth.LOGIN) {
                setBody(request)
            }
        }
    }
}

// ============================================
// 3. Controller / ViewModel Layer
// Handling UI logic automatically using BaseScreenModel
// ============================================
class AuthScreenModel(
    private val authRepository: AuthRepository = AuthRepository()
) : BaseScreenModel() {

    // Manage a specific state unique to this controller
    private val _loginResult = MutableStateFlow<LoginResponse?>(null)
    val loginResult = _loginResult.asStateFlow()

    fun performLogin(phone: String, pin: String) {
        val request = LoginRequest(phone, pin)

        // Using the BaseScreenModel custom generic helper to automate loading and failure
        executeApi(
            apiCall = { authRepository.login(request) },
            onSuccess = { response ->
                // Loading is automatically dismissed. Error messages auto cleared.
                _loginResult.value = response
                
                // You can perform navigation or trigger UI actions using effect flows
                println("API_SUCCESS: User logged in: ${response.token}")
            },
            onError = { customErrorMessage ->
                // The BaseScreenModel automatically dismissed loaders here too.
                // We can override standard message logic. If `onError` is not provided here, 
                // the BaseScreenModel updates `errorMessage` automatically!
                println("API_ERROR_HANDLE_OVERRIDE: $customErrorMessage")
                
                // You can still manually bubble up errors
                showError(customErrorMessage)
            }
        )
    }
}
