package `in`.gym.trak.studio.tracking

/**
 * KMP bridge — platform-specific live tracking notification manager.
 * Android → Foreground Service + Notification
 * iOS     → Live Activities (ActivityKit)
 */
interface LiveTrackingManager {
    fun startTracking(data: TrackingData)
    fun updateTracking(data: TrackingData)
    fun stopTracking()
}

expect fun createLiveTrackingManager(): LiveTrackingManager
