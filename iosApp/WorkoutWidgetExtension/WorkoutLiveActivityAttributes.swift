import ActivityKit
import Foundation

// Shared with the main app target — keep in sync with iosApp/WorkoutLiveActivityAttributes.swift
struct WorkoutLiveAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        var completedSets: Int
        var totalSets: Int
        var exerciseCount: Int
    }

    var workoutTitle: String
    var startedAt: Date
}
