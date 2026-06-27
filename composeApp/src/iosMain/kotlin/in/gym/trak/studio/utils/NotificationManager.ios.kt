package `in`.gym.trak.studio.utils

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.messaging.messaging
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSString

private const val FCM_TOKEN_NOTIFICATION = "in.gym.trak.fcm.token"

@OptIn(ExperimentalForeignApi::class)
internal actual fun startPlatformFcmTokenListener() {
    NSNotificationCenter.defaultCenter.addObserverForName(
        name = FCM_TOKEN_NOTIFICATION,
        `object` = null,
        queue = NSOperationQueue.mainQueue,
    ) { notification ->
        val token = notification?.userInfo?.get("token") as? NSString ?: return@addObserverForName
        NotificationManager.storeFcmToken(token.toString())
    }
}

internal actual suspend fun fetchFcmToken(): String? {
    // APNS token is set asynchronously after permission + registerForRemoteNotifications.
    repeat(30) { attempt ->
        try {
            val token = Firebase.messaging.getToken()
            if (token.isNotBlank()) return token
        } catch (_: Exception) {
            // Expected until Messaging.messaging().apnsToken is set in AppDelegate.
        }
        if (attempt < 29) delay(500)
    }
    return null
}
