package `in`.gym.trak.studio.tracking

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Shared KMP controller that owns business logic and drives LiveTrackingManager.
 *
 * Usage (from any screen or ScreenModel):
 *
 *     val tracker = LiveTrackingController()
 *
 *     // Start once a session begins
 *     tracker.startSession(memberId = "abc123", memberName = "Rahul", planName = "Morning Batch")
 *
 *     // Simulate or update progress from your API polling
 *     tracker.updateProgress(progress = 0.6f, eta = "12 min", status = "In Progress")
 *
 *     // End the session
 *     tracker.endSession()
 */
class LiveTrackingController {

    private val manager: LiveTrackingManager = createLiveTrackingManager()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var simulationJob: Job? = null

    // ─── Public API ──────────────────────────────────────────────────────────

    /** Start tracking for a member session. Shows lock screen notification immediately. */
    fun startSession(
        memberId: String,
        memberName: String,
        planName: String,
        initialEta: String = "Calculating..."
    ) {
        val data = TrackingData(
            title = planName,
            eta = initialEta,
            progress = 0f,
            status = "Starting",
            statusDetail = "Session started for $memberName",
            userName = memberName
        )
        manager.startTracking(data)
    }

    /** Push a real-time update to the lock screen notification / Live Activity. */
    fun updateProgress(
        progress: Float,
        eta: String,
        status: String = "In Progress",
        statusDetail: String = "",
        memberName: String? = null,
        planName: String = "Session"
    ) {
        val data = TrackingData(
            title = planName,
            eta = eta,
            progress = progress,
            status = status,
            statusDetail = statusDetail,
            userName = memberName
        )
        manager.updateTracking(data)
    }

    /** Stop tracking and dismiss the notification / Live Activity. */
    fun endSession() {
        simulationJob?.cancel()
        manager.stopTracking()
    }

    /**
     * Demo/test helper: simulates progress from 0 → 1 over [durationSeconds] seconds.
     * Call this if you want to see the notification update in real time without an API.
     */
    fun simulateProgress(
        planName: String = "Morning Batch",
        memberName: String = "Member",
        durationSeconds: Int = 60
    ) {
        val startData = TrackingData(
            title = planName,
            eta = "${durationSeconds}s",
            progress = 0f,
            status = "Starting",
            userName = memberName
        )
        manager.startTracking(startData)

        simulationJob?.cancel()
        simulationJob = scope.launch {
            val steps = durationSeconds
            for (i in 1..steps) {
                delay(1000L)
                val progress = i.toFloat() / steps
                val remainingSec = steps - i
                val eta = when {
                    remainingSec >= 60 -> "${remainingSec / 60}m ${remainingSec % 60}s"
                    else -> "${remainingSec}s"
                }
                val status = when {
                    progress < 0.3f -> "Just Started"
                    progress < 0.7f -> "In Progress"
                    progress < 0.95f -> "Almost Done"
                    else -> "Finishing Up"
                }
                val update = TrackingData(
                    title = planName,
                    eta = eta,
                    progress = progress,
                    status = status,
                    userName = memberName
                )
                manager.updateTracking(update)
            }
            // Auto-end when simulation completes
            manager.stopTracking()
        }
    }

    fun dispose() {
        scope.cancel()
    }
}
