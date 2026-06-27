import ActivityKit
import SwiftUI
import WidgetKit

private enum WorkoutLiveTheme {
    static let green = Color(red: 60 / 255, green: 140 / 255, blue: 74 / 255)
    static let card = Color(red: 24 / 255, green: 24 / 255, blue: 26 / 255)
    static let muted = Color.white.opacity(0.55)
}

private struct WorkoutLiveLockScreenView: View {
    let title: String
    let startedAt: Date
    let completedSets: Int
    let totalSets: Int
    let exerciseCount: Int

    private var progress: Double {
        guard totalSets > 0 else { return 0 }
        return min(1, Double(completedSets) / Double(totalSets))
    }

    private var exerciseLabel: String {
        switch exerciseCount {
        case 0: return "No exercises"
        case 1: return "1 exercise"
        default: return "\(exerciseCount) exercises"
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .center) {
                Image(systemName: "figure.strengthtraining.traditional")
                    .font(.body.weight(.semibold))
                    .foregroundStyle(WorkoutLiveTheme.green)
                Text(title)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(.white)
                    .lineLimit(1)
                Spacer(minLength: 8)
                Text("WORKOUT")
                    .font(.caption2.weight(.bold))
                    .foregroundStyle(WorkoutLiveTheme.muted)
                    .tracking(0.8)
            }

            HStack(alignment: .firstTextBaseline, spacing: 0) {
                statBlock(
                    value: "\(completedSets)/\(max(totalSets, 0))",
                    label: "SETS",
                )
                Spacer()
                statBlock(
                    value: nil,
                    label: "TIME",
                    timerFrom: startedAt,
                )
            }

            VStack(alignment: .leading, spacing: 6) {
                GeometryReader { geometry in
                    ZStack(alignment: .leading) {
                        Capsule()
                            .fill(Color.white.opacity(0.14))
                        Capsule()
                            .fill(WorkoutLiveTheme.green)
                            .frame(width: geometry.size.width * progress)
                    }
                }
                .frame(height: 5)

                Text(exerciseLabel)
                    .font(.caption)
                    .foregroundStyle(WorkoutLiveTheme.muted)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
    }

    @ViewBuilder
    private func statBlock(value: String?, label: String, timerFrom: Date? = nil) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            if let timerFrom {
                Text(timerFrom, style: .timer)
                    .font(.system(size: 34, weight: .bold, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(.white)
                    .multilineTextAlignment(.leading)
            } else if let value {
                Text(value)
                    .font(.system(size: 34, weight: .bold, design: .rounded))
                    .monospacedDigit()
                    .foregroundStyle(.white)
            }
            Text(label)
                .font(.caption2.weight(.semibold))
                .foregroundStyle(WorkoutLiveTheme.muted)
                .tracking(0.6)
        }
    }
}

struct WorkoutWidgetLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: WorkoutLiveAttributes.self) { context in
            WorkoutLiveLockScreenView(
                title: context.attributes.workoutTitle,
                startedAt: context.attributes.startedAt,
                completedSets: context.state.completedSets,
                totalSets: context.state.totalSets,
                exerciseCount: context.state.exerciseCount,
            )
            .activityBackgroundTint(WorkoutLiveTheme.card)
            .activitySystemActionForegroundColor(.white)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    VStack(alignment: .leading, spacing: 2) {
                        Text(context.attributes.workoutTitle)
                            .font(.caption.weight(.semibold))
                            .lineLimit(1)
                        Text("\(context.state.completedSets)/\(context.state.totalSets) sets")
                            .font(.caption2)
                            .foregroundStyle(WorkoutLiveTheme.muted)
                    }
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Text(context.attributes.startedAt, style: .timer)
                        .font(.title3.monospacedDigit().bold())
                        .multilineTextAlignment(.trailing)
                }
                DynamicIslandExpandedRegion(.bottom) {
                    ProgressView(
                        value: Double(context.state.completedSets),
                        total: Double(max(context.state.totalSets, 1)),
                    )
                    .tint(WorkoutLiveTheme.green)
                }
            } compactLeading: {
                Image(systemName: "figure.strengthtraining.traditional")
                    .foregroundStyle(WorkoutLiveTheme.green)
            } compactTrailing: {
                Text(context.attributes.startedAt, style: .timer)
                    .monospacedDigit()
                    .font(.caption2)
            } minimal: {
                Image(systemName: "figure.strengthtraining.traditional")
                    .foregroundStyle(WorkoutLiveTheme.green)
            }
        }
    }
}

@main
struct WorkoutWidgetBundle: WidgetBundle {
    var body: some Widget {
        WorkoutWidgetLiveActivity()
    }
}
