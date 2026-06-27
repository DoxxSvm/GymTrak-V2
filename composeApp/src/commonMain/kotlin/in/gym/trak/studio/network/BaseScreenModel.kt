package `in`.gym.trak.studio.network

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Base ScreenModel (ViewModel) that centralizes Loading, Error, and Success tracking logic.
 * Every ScreenModel in your app should inherit this to have automated state properties.
 */
abstract class BaseScreenModel : ScreenModel {
    
    // Globally shareable loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Globally shareable error message state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Shown like [errorMessage] but not cleared when [executeApi] starts (e.g. success after a follow-up refresh). */
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    protected fun showLoader() { _isLoading.value = true }
    protected fun hideLoader() { _isLoading.value = false }
    
    public fun showError(message: String) {
        _toastMessage.value = null
        _errorMessage.value = message
    }
    fun clearError() { _errorMessage.value = null }

    protected fun showToast(message: String) {
        _errorMessage.value = null
        _toastMessage.value = message
    }

    fun showSuccessToast(message: String) = showToast(message)

    fun clearToast() { _toastMessage.value = null }

    protected fun <T> executeApi(
        apiCall: suspend () -> ApiResult<T>,
        onSuccess: (T) -> Unit,
        onError: ((String) -> Unit)? = null,
        showGlobalLoader: Boolean = true
    ) {
        screenModelScope.launch {
            if (!isNetworkAvailable()) {
                NetworkMonitor.updateStatus(false)
                if (onError != null) {
                    onError("No internet connection")
                } else {
                    showToast("No internet connection")
                }
                return@launch
            }
            NetworkMonitor.updateStatus(true)
            
            if (showGlobalLoader) showLoader()
            clearError()
            clearToast()
            
            when (val result = apiCall()) {
                is ApiResult.Success -> {
                    if (showGlobalLoader) hideLoader()
                    println("API_SUCCESS: Successfully received data.")
                    onSuccess(result.data)
                }
                is ApiResult.Error -> {
                    if (showGlobalLoader) hideLoader()
                    println("API_ERROR: Message: ${result.message}, Code: ${result.code}")
                    if (onError != null) {
                        onError(result.message) // Handle custom error logic locally
                    } else {
                        showToast(result.message) // Standard approach (error toast path)
                    }
                }
                is ApiResult.Loading -> { } // Handled automatically before match
                is ApiResult.Idle -> { }
            }
        }
    }
}
