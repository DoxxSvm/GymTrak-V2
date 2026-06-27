import Foundation

#if canImport(ActivityKit)
import ActivityKit

struct WorkoutLiveAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        var completedSets: Int
        var totalSets: Int
        var exerciseCount: Int
    }

    var workoutTitle: String
    var startedAt: Date
}
#endif
