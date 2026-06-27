package `in`.gym.trak.studio.utils

import `in`.gym.trak.studio.data.repository.WorkoutManager
import platform.Foundation.NSNotificationCenter
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter

private const val REGISTER_REMOTE_NOTIFICATIONS = "in.gym.trak.notifications.register"

actual fun requestNotificationPermission() {
    val center = UNUserNotificationCenter.currentNotificationCenter()
    center.requestAuthorizationWithOptions(
        UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound
    ) { granted, error ->
        if (granted) {
            println("Notification permission granted")
            NSNotificationCenter.defaultCenter.postNotificationName(
                aName = REGISTER_REMOTE_NOTIFICATIONS,
                `object` = null,
            )
            WorkoutManager.touchActiveWorkoutClock()
        } else {
            val reason = error?.localizedDescription ?: "user denied or notifications unavailable (e.g. Simulator)"
            println("Notification permission denied: $reason")
        }
    }
}
