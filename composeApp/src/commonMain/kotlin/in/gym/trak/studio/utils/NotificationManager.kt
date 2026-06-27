package `in`.gym.trak.studio.utils

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.messaging.messaging
import `in`.gym.trak.studio.data.repository.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

object NotificationManager {
    private val _fcmToken = MutableStateFlow<String?>(null)
    val fcmToken: StateFlow<String?> = _fcmToken

    fun clearFcmState() {
        _fcmToken.value = null
    }

    fun initialize() {
        startPlatformFcmTokenListener()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val token = fetchFcmToken()
                if (!token.isNullOrBlank()) {
                    storeFcmToken(token)
                }
                Firebase.messaging.subscribeToTopic("all_users")
            } catch (e: Exception) {
                println("Firebase initialization failed: ${e.message}")
            }
        }
    }

    internal fun storeFcmToken(token: String) {
        SessionManager.fcmDeviceToken = token
        _fcmToken.value = token
        println("FCM Token: $token")
    }
}

/** Platform-specific FCM token fetch (iOS waits for APNS) and native token callbacks. */
internal expect fun startPlatformFcmTokenListener()

internal expect suspend fun fetchFcmToken(): String?
