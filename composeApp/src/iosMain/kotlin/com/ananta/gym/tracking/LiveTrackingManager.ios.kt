package `in`.gym.trak.studio.tracking

/**
 * iOS actual implementation of LiveTrackingManager.
 *
 * Live Activities (ActivityKit) require native Swift code in the Xcode project.
 * This Kotlin bridge calls into the Swift layer via the expect/actual pattern.
 *
 * ──────────────────────────────────────────────────────────────
 * SWIFT SIDE — add these files to your Xcode target:
 *
 * 1) GymTrackingAttributes.swift
 * ────────────────────────────────
 * import ActivityKit
 * struct GymTrackingAttributes: ActivityAttributes {
 *     public typealias ContentState = TrackingState
 *     let gymName: String
 *     struct TrackingState: Codable, Hashable {
 *         var eta: String
 *         var progress: Double
 *         var status: String
 *         var statusDetail: String
 *     }
 * }
 *
 * 2) LiveActivityBridge.swift
 * ────────────────────────────
 * import ActivityKit, Foundation
 * @objc public class LiveActivityBridge: NSObject {
 *     private var activity: Activity<GymTrackingAttributes>?
 *     @objc public func startTracking(title: String, eta: String, progress: Float,
 *         status: String, statusDetail: String, userName: String?) {
 *         let state = GymTrackingAttributes.ContentState(
 *             eta: eta, progress: Double(progress), status: status, statusDetail: statusDetail)
 *         activity = try? Activity.request(
 *             attributes: GymTrackingAttributes(gymName: title),
 *             content: ActivityContent(state: state, staleDate: nil), pushType: nil)
 *     }
 *     @objc public func updateTracking(eta: String, progress: Float, status: String, statusDetail: String) {
 *         let state = GymTrackingAttributes.ContentState(
 *             eta: eta, progress: Double(progress), status: status, statusDetail: statusDetail)
 *         Task { await activity?.update(ActivityContent(state: state, staleDate: nil)) }
 *     }
 *     @objc public func stopTracking() {
 *         Task { await activity?.end(ActivityContent(state: activity!.content.state, staleDate: nil),
 *             dismissalPolicy: .immediate) }
 *     }
 * }
 *
 * 3) GymTrackingWidget.swift  (Lock Screen + Dynamic Island)
 * ───────────────────────────────────────────────────────────
 * import SwiftUI, ActivityKit, WidgetKit
 * struct GymTrackingLiveActivity: Widget {
 *     var body: some WidgetConfiguration {
 *         ActivityConfiguration(for: GymTrackingAttributes.self) { context in
 *             VStack(alignment: .leading, spacing: 6) {
 *                 HStack {
 *                     Text(context.attributes.gymName).font(.headline)
 *                     Spacer()
 *                     Text("ETA: \(context.state.eta)").font(.subheadline)
 *                 }
 *                 ProgressView(value: context.state.progress).tint(.green)
 *                 Text(context.state.status).font(.caption).foregroundColor(.secondary)
 *             }.padding()
 *         } dynamicIsland: { context in
 *             DynamicIsland {
 *                 DynamicIslandExpandedRegion(.leading) { Text(context.attributes.gymName).font(.caption) }
 *                 DynamicIslandExpandedRegion(.trailing) { Text(context.state.eta).font(.caption.bold()) }
 *                 DynamicIslandExpandedRegion(.bottom) { ProgressView(value: context.state.progress).tint(.green) }
 *             } compactLeading: { Image(systemName: "figure.run") }
 *             compactTrailing: { Text(context.state.eta).font(.caption2) }
 *             minimal: { Image(systemName: "figure.run") }
 *         }
 *     }
 * }
 * ──────────────────────────────────────────────────────────────
 */
private class IOSLiveTrackingManager : LiveTrackingManager {
    // Uncomment after adding Swift target:
    // private val bridge = LiveActivityBridge()

    override fun startTracking(data: TrackingData) {
        // bridge.startTracking(title=data.title, eta=data.eta, progress=data.progress, ...)
        println("[LiveTracking/iOS] startTracking: ${data.title} – ETA ${data.eta}")
    }

    override fun updateTracking(data: TrackingData) {
        // bridge.updateTracking(eta=data.eta, progress=data.progress, ...)
        println("[LiveTracking/iOS] updateTracking: ${data.status} – ${(data.progress * 100).toInt()}%")
    }

    override fun stopTracking() {
        // bridge.stopTracking()
        println("[LiveTracking/iOS] stopTracking")
    }
}

actual fun createLiveTrackingManager(): LiveTrackingManager = IOSLiveTrackingManager()
