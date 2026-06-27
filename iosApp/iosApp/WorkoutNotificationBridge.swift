import Foundation
import UIKit
import UserNotifications

#if canImport(ActivityKit)
import ActivityKit
#endif

/// Handles persistent workout notifications from Kotlin via NSNotificationCenter.
///
/// Lock-screen live timer uses Live Activity (`Text(startedAt, style: .timer)`), which the
/// system updates without the app running. The local notification is posted once per session
/// and only replaced when workout progress changes — never every second.
@objc public final class WorkoutNotificationBridge: NSObject {

    @objc public static let shared = WorkoutNotificationBridge()

    private let notificationId = "gym_trak_active_workout"
    private var isRegistered = false
    private var presentWorkoutBannerOnForeground = false
    private var lastPayload: Payload?
    private var activeSessionStartMillis: Double?
    private var lastPostedProgress: ProgressSnapshot?
    private var notificationsAuthorized = false
    private var isLiveActivityActive = false

    #if canImport(ActivityKit)
    private var liveActivityManagerStorage: Any?
    #endif

    @objc public func register() {
        guard !isRegistered else { return }
        isRegistered = true

        let center = NotificationCenter.default
        center.addObserver(self, selector: #selector(handleStart(_:)), name: Notification.Name("in.gym.trak.workout.live.start"), object: nil)
        center.addObserver(self, selector: #selector(handleUpdate(_:)), name: Notification.Name("in.gym.trak.workout.live.update"), object: nil)
        center.addObserver(self, selector: #selector(handleRestore(_:)), name: Notification.Name("in.gym.trak.workout.live.restore"), object: nil)
        center.addObserver(self, selector: #selector(handleStop(_:)), name: Notification.Name("in.gym.trak.workout.live.stop"), object: nil)
        center.addObserver(self, selector: #selector(handlePermissionGranted(_:)), name: Notification.Name("in.gym.trak.notifications.register"), object: nil)
        center.addObserver(self, selector: #selector(handleDidBecomeActive), name: UIApplication.didBecomeActiveNotification, object: nil)

        refreshNotificationAuthorization()
    }

    @objc public func consumePresentWorkoutBannerOnForeground() -> Bool {
        let shouldPresent = presentWorkoutBannerOnForeground
        presentWorkoutBannerOnForeground = false
        return shouldPresent
    }

    @objc public func refreshActiveWorkoutNotification() {
        guard let payload = lastPayload else { return }
        replaceWorkoutNotification(payload: payload, alertUser: false, force: true)
    }

    @objc private func handleStart(_ notification: Notification) {
        guard let payload = parsePayload(notification) else { return }
        let isSameSession = activeSessionStartMillis == payload.startTimeMillis
        lastPayload = payload

        if isSameSession {
            updateLiveActivity(payload: payload)
            replaceWorkoutNotification(payload: payload, alertUser: false, force: false)
            return
        }

        activeSessionStartMillis = payload.startTimeMillis
        lastPostedProgress = nil
        presentWorkoutBannerOnForeground = true
        clearActiveWorkoutNotifications()
        endLiveActivitySync()
        startLiveActivity(payload: payload) { [weak self] started in
            guard let self else { return }
            self.isLiveActivityActive = started
            self.replaceWorkoutNotification(payload: payload, alertUser: true, force: true)
        }
    }

    @objc private func handleUpdate(_ notification: Notification) {
        guard let payload = parsePayload(notification) else { return }
        lastPayload = payload
        updateLiveActivity(payload: payload)
        replaceWorkoutNotification(payload: payload, alertUser: false, force: false)
    }

    @objc private func handleRestore(_ notification: Notification) {
        guard let payload = parsePayload(notification) else { return }
        lastPayload = payload
        replaceWorkoutNotification(payload: payload, alertUser: false, force: true)
    }

    @objc private func handleStop(_ notification: Notification) {
        lastPayload = nil
        activeSessionStartMillis = nil
        lastPostedProgress = nil
        presentWorkoutBannerOnForeground = false
        isLiveActivityActive = false
        clearActiveWorkoutNotifications()
        endLiveActivitySync()
    }

    @objc private func handlePermissionGranted(_ notification: Notification) {
        refreshNotificationAuthorization { [weak self] in
            guard let self, let payload = self.lastPayload else { return }
            self.replaceWorkoutNotification(payload: payload, alertUser: false, force: true)
        }
    }

    @objc private func handleDidBecomeActive() {
        guard lastPayload != nil else { return }
        replaceWorkoutNotification(payload: lastPayload!, alertUser: false, force: true)
    }

    private struct Payload {
        let title: String
        let startTimeMillis: Double
        let durationLabel: String
        let exerciseCount: Int
        let completedSets: Int
        let totalSets: Int

        var setsLabel: String { "\(completedSets)/\(totalSets) sets" }
        var exerciseSummary: String {
            switch exerciseCount {
            case 0: return "No exercises"
            case 1: return "1 exercise"
            default: return "\(exerciseCount) exercises"
            }
        }
    }

    private struct ProgressSnapshot: Equatable {
        let title: String
        let exerciseCount: Int
        let completedSets: Int
        let totalSets: Int
    }

    private func progressSnapshot(from payload: Payload) -> ProgressSnapshot {
        ProgressSnapshot(
            title: payload.title,
            exerciseCount: payload.exerciseCount,
            completedSets: payload.completedSets,
            totalSets: payload.totalSets
        )
    }

    private func parsePayload(_ notification: Notification) -> Payload? {
        let userInfo = notification.userInfo ?? [:]
        let title = stringValue(userInfo, key: "title") ?? "Workout"
        let startMillis = numberValue(userInfo, key: "startTimeMillis") ?? Date().timeIntervalSince1970 * 1000
        let durationLabel = stringValue(userInfo, key: "durationLabel") ?? "0:00"
        let exerciseCount = Int(numberValue(userInfo, key: "exerciseCount") ?? 0)
        let completedSets = Int(numberValue(userInfo, key: "completedSets") ?? 0)
        let totalSets = Int(numberValue(userInfo, key: "totalSets") ?? 0)
        return Payload(
            title: title,
            startTimeMillis: startMillis,
            durationLabel: durationLabel,
            exerciseCount: exerciseCount,
            completedSets: completedSets,
            totalSets: totalSets
        )
    }

    private func stringValue(_ userInfo: [AnyHashable: Any], key: String) -> String? {
        if let value = userInfo[key] as? String { return value }
        if let value = userInfo[key as NSString] as? String { return value }
        if let value = userInfo[key] as? NSString { return value as String }
        return nil
    }

    private func numberValue(_ userInfo: [AnyHashable: Any], key: String) -> Double? {
        if let value = userInfo[key] as? NSNumber { return value.doubleValue }
        if let value = userInfo[key as NSString] as? NSNumber { return value.doubleValue }
        if let value = userInfo[key] as? Double { return value }
        if let value = userInfo[key] as? Int { return Double(value) }
        return nil
    }

    private func clearActiveWorkoutNotifications() {
        let center = UNUserNotificationCenter.current()
        center.removeDeliveredNotifications(withIdentifiers: [notificationId])
        center.removePendingNotificationRequests(withIdentifiers: [notificationId])
    }

    private func refreshNotificationAuthorization(completion: (() -> Void)? = nil) {
        UNUserNotificationCenter.current().getNotificationSettings { [weak self] settings in
            guard let self else { return }
            switch settings.authorizationStatus {
            case .authorized, .provisional, .ephemeral:
                self.notificationsAuthorized = true
            default:
                self.notificationsAuthorized = false
            }
            completion?()
        }
    }

    private func replaceWorkoutNotification(payload: Payload, alertUser: Bool, force: Bool) {
        let snapshot = progressSnapshot(from: payload)
        if !force, !alertUser, snapshot == lastPostedProgress {
            return
        }
        if force || alertUser || snapshot != lastPostedProgress {
            lastPostedProgress = snapshot
        }

        guard notificationsAuthorized else {
            refreshNotificationAuthorization { [weak self] in
                guard let self, self.notificationsAuthorized else { return }
                self.postLocalNotification(payload: payload, alertUser: alertUser)
            }
            return
        }
        postLocalNotification(payload: payload, alertUser: alertUser)
    }

    private func postLocalNotification(payload: Payload, alertUser: Bool) {
        let content = UNMutableNotificationContent()
        content.title = payload.title
        if isLiveActivityActive {
            content.subtitle = "Workout in progress"
            content.body = "\(payload.setsLabel) · \(payload.exerciseSummary)"
        } else {
            content.subtitle = "Workout in progress · \(payload.durationLabel)"
            content.body = "\(payload.setsLabel) · \(payload.exerciseSummary)"
        }
        content.sound = nil
        content.threadIdentifier = "active_workout"
        content.categoryIdentifier = "active_workout"
        if #available(iOS 15.0, *) {
            content.interruptionLevel = alertUser ? .timeSensitive : .passive
            content.relevanceScore = 1.0
        }

        let request = UNNotificationRequest(
            identifier: notificationId,
            content: content,
            trigger: nil
        )
        UNUserNotificationCenter.current().add(request) { [weak self] error in
            if error != nil {
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
                    guard let self, self.lastPayload != nil else { return }
                    UNUserNotificationCenter.current().add(request)
                }
            }
        }
    }

    private func startLiveActivity(payload: Payload, completion: @escaping (Bool) -> Void) {
        #if canImport(ActivityKit)
        if #available(iOS 16.2, *) {
            liveActivityManager().start(payload: payload, completion: completion)
            return
        }
        #endif
        completion(false)
    }

    private func updateLiveActivity(payload: Payload) {
        #if canImport(ActivityKit)
        if #available(iOS 16.2, *) {
            liveActivityManager().update(payload: payload)
        }
        #endif
    }

    private func endLiveActivitySync() {
        #if canImport(ActivityKit)
        if #available(iOS 16.2, *) {
            liveActivityManager().stop()
            liveActivityManagerStorage = nil
        }
        #endif
    }

    #if canImport(ActivityKit)
    @available(iOS 16.2, *)
    private func liveActivityManager() -> WorkoutLiveActivityManager {
        if let existing = liveActivityManagerStorage as? WorkoutLiveActivityManager {
            return existing
        }
        let manager = WorkoutLiveActivityManager()
        liveActivityManagerStorage = manager
        return manager
    }

    @available(iOS 16.2, *)
    private final class WorkoutLiveActivityManager {
        private var liveActivity: Activity<WorkoutLiveAttributes>?

        func start(payload: Payload, completion: @escaping (Bool) -> Void) {
            Task {
                await endAllActivities()
                guard ActivityAuthorizationInfo().areActivitiesEnabled else {
                    await MainActor.run { completion(false) }
                    return
                }
                let attributes = WorkoutLiveAttributes(
                    workoutTitle: payload.title,
                    startedAt: Date(timeIntervalSince1970: payload.startTimeMillis / 1000)
                )
                let state = WorkoutLiveAttributes.ContentState(
                    completedSets: payload.completedSets,
                    totalSets: payload.totalSets,
                    exerciseCount: payload.exerciseCount
                )
                do {
                    liveActivity = try Activity.request(
                        attributes: attributes,
                        content: ActivityContent(state: state, staleDate: nil),
                        pushType: nil
                    )
                    await MainActor.run { completion(liveActivity != nil) }
                } catch {
                    await MainActor.run { completion(false) }
                }
            }
        }

        func update(payload: Payload) {
            let state = WorkoutLiveAttributes.ContentState(
                completedSets: payload.completedSets,
                totalSets: payload.totalSets,
                exerciseCount: payload.exerciseCount
            )
            Task {
                if liveActivity == nil {
                    for activity in Activity<WorkoutLiveAttributes>.activities {
                        liveActivity = activity
                        break
                    }
                }
                await liveActivity?.update(ActivityContent(state: state, staleDate: nil))
            }
        }

        func stop() {
            Task {
                await endAllActivities()
            }
        }

        private func endAllActivities() async {
            if let liveActivity {
                await liveActivity.end(nil, dismissalPolicy: .immediate)
                self.liveActivity = nil
            }
            for activity in Activity<WorkoutLiveAttributes>.activities {
                await activity.end(nil, dismissalPolicy: .immediate)
            }
        }
    }
    #endif
}
