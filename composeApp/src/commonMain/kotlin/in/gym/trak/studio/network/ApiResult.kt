package `in`.gym.trak.studio.network

/**
 * A generic wrapper class around data requests.
 * It strictly returns Success, Error (with details), Loading, or Idle states.
 */
sealed class ApiResult<out T> {
    data class Success<out T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    object Loading : ApiResult<Nothing>()
    object Idle : ApiResult<Nothing>()
}
