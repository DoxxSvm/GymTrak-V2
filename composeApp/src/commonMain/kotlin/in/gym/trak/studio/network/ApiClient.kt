package `in`.gym.trak.studio.network

import `in`.gym.trak.studio.data.repository.SessionInvalidationBus
import `in`.gym.trak.studio.data.repository.SessionManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json

/**
 * Central Base API Client for Ktor Network Layer.
 * It contains the configuration for JSON parsing, Logging (Request/Response),
 * and an isolated safe execution wrapper for APIs.
 */
object ApiClient {

    // Core Ktor Client Configuration
    val client = HttpClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
                explicitNulls = false
            })
        }

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("KTOR_LOG: $message")
                }
            }
            level = LogLevel.ALL
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 30000
            socketTimeoutMillis = 30000
        }

        defaultRequest {
            url(ApiEndpoints.BASE_URL)
            contentType(ContentType.Application.Json)
            // Example: Add your token dynamically here 
            // header("Authorization", "Bearer token_value")
        }
    }

    /**
     * A highly generic wrapper to safely parse APIs, avoid crashes and streamline standard HTTP error mappings.
     * @param apiCall Lambda wrapping your client response.
     */
    suspend inline fun <reified T> safeApiCall(
        crossinline apiCall: suspend HttpClient.() -> io.ktor.client.statement.HttpResponse
    ): ApiResult<T> {
        return try {
            val response = client.apiCall()

            when (response.status.value) {
                in 200..299 -> {
                    val data = response.body<T>()
                    ApiResult.Success(data)
                }

                else -> {
                    maybeInvalidateSessionOnAuthOrServerError(response)
                    val errorBody = try {
                        response.body<ErrorResponse>()
                    } catch (e: Exception) {
                        null
                    }

                    val statusCode = response.status.value
                    val message = statusMessageFromCode(
                        statusCode = statusCode,
                        apiMessage = errorBody?.message,
                        apiError = errorBody?.error
                    )

                    ApiResult.Error(message, statusCode)
                }
            }
//            when (response.status.value) {
//                in 200..299 -> {
//                    // Success Block
//                    val data = response.body<T>()
//                    ApiResult.Success(data)
//                }
//                401 -> ApiResult.Error("Unauthorized access. Please login again.", 401)
//                403 -> ApiResult.Error("You do not have permission to access this resource.", 403)
//                404 -> ApiResult.Error("Requested resource not found.", 404)
//                in 500..599 -> ApiResult.Error("Internal server error. Please try again later.", response.status.value)
//                else -> ApiResult.Error("Something went wrong. Status: ${response.status.value}", response.status.value)
//            }
        } catch (e: Exception) {
            // Rethrow cancellation to safely let Coroutines cancel jobs
            if (e is CancellationException) throw e

            // Log manually if needed (Ktor logging handles most of this automatically though)
            println("API_CRASH_PREVENTED: ${e.message}")

            // Map standard exceptions to human-readable UI messages
            val errorMessage = when (e) {
                is kotlinx.serialization.SerializationException -> "API Data mismatch or parsing error."
                is HttpRequestTimeoutException -> "Request timed out. Check your connection."
                else -> e.message ?: "An unexpected connectivity error occurred."
            }

            ApiResult.Error(errorMessage)
        }
    }

    /**
     * Single source of truth for user-visible HTTP error messages.
     * Message is derived from status code; API-provided detail is appended when available.
     */
    @PublishedApi
    internal fun statusMessageFromCode(
        statusCode: Int,
        apiMessage: String? = null,
        apiError: String? = null
    ): String {
        val base = when (statusCode) {
            400 -> "Bad request."
            401 -> "Unauthorized."
            403 -> "Forbidden."
            404 -> "Not found."
            409 -> ""
            422 -> "Validation failed."
            429 -> "Too many requests. Please try again later."
            in 500..599 -> "Server error. Please try again."
            else -> "Request failed (HTTP $statusCode)."
        }
        val detail = when {
            !apiMessage.isNullOrBlank() -> apiMessage.trim()
            !apiError.isNullOrBlank() -> apiError.trim()
            else -> null
        }
        return if (detail.isNullOrBlank()) base else "$base $detail"
    }
    @PublishedApi
    internal fun maybeInvalidateSessionOnAuthOrServerError(response: io.ktor.client.statement.HttpResponse) {
        val code = response.status.value
        if (!shouldForceLogoutForHttpStatus(code)) return
        if (!SessionManager.isLoggedIn) return
        val path = response.call.request.url.encodedPath
        if (path.contains(ApiEndpoints.App.CONFIG)) return
        SessionManager.clearSession()
        SessionInvalidationBus.notifySessionInvalidated()
    }

    private fun shouldForceLogoutForHttpStatus(code: Int): Boolean =
        code == 401 || code in 500..599
}

@kotlinx.serialization.Serializable
data class ErrorResponse(
    val message: String? = null,
    val error: String? = null,
    val statusCode: Int? = null
)
