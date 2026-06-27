import UIKit
import Firebase
import FirebaseMessaging

/// Posted when Firebase Messaging provides an FCM registration token (Kotlin listens).
let kGymTrakFcmTokenNotification = Notification.Name("in.gym.trak.fcm.token")
/// Posted from Kotlin after the user grants notification permission.
let kGymTrakRegisterRemoteNotifications = Notification.Name("in.gym.trak.notifications.register")

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        FirebaseApp.configure()
        UNUserNotificationCenter.current().delegate = self
        Messaging.messaging().delegate = self
        WorkoutNotificationBridge.shared.register()

        NotificationCenter.default.addObserver(
            forName: kGymTrakRegisterRemoteNotifications,
            object: nil,
            queue: .main
        ) { _ in
            UIApplication.shared.registerForRemoteNotifications()
        }

        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("APNS registration failed: \(error.localizedDescription)")
    }

    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let fcmToken else { return }
        print("Firebase registration token: \(fcmToken)")
        NotificationCenter.default.post(
            name: kGymTrakFcmTokenNotification,
            object: nil,
            userInfo: ["token": fcmToken]
        )
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        if notification.request.identifier == "gym_trak_active_workout" {
            if WorkoutNotificationBridge.shared.consumePresentWorkoutBannerOnForeground() {
                completionHandler([.banner, .list])
            } else {
                completionHandler([.list])
            }
            return
        }
        completionHandler([.banner, .sound])
    }

    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        if response.notification.request.identifier == "gym_trak_active_workout" {
            // iOS clears the notification on tap — immediately re-post so it stays on lock screen.
            WorkoutNotificationBridge.shared.refreshActiveWorkoutNotification()
            NotificationCenter.default.post(
                name: Notification.Name("in.gym.trak.workout.open"),
                object: nil
            )
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.4) {
                WorkoutNotificationBridge.shared.refreshActiveWorkoutNotification()
            }
        }
        completionHandler()
    }
}
