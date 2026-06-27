package `in`.gym.trak.studio.utils

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.messaging.messaging

internal actual fun startPlatformFcmTokenListener() {
    // Android receives token refreshes via FirebaseMessagingService if needed.
}

internal actual suspend fun fetchFcmToken(): String? {
    return Firebase.messaging.getToken()
}
