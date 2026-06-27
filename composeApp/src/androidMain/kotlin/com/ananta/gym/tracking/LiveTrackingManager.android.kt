package `in`.gym.trak.studio.tracking

import android.content.Intent
import android.os.Build
import `in`.gym.trak.studio.appContext

/** Android implementation — delegates to TrackingForegroundService. */
private class AndroidLiveTrackingManager : LiveTrackingManager {

    override fun startTracking(data: TrackingData) {
        val context = appContext ?: return
        val intent = TrackingForegroundService.buildStartIntent(context, data)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    override fun updateTracking(data: TrackingData) {
        updateTrackingNotification(data)
    }

    override fun stopTracking() {
        val context = appContext ?: return
        val intent = Intent(context, TrackingForegroundService::class.java).apply {
            action = TrackingForegroundService.ACTION_STOP
        }
        context.stopService(intent)
    }
}

actual fun createLiveTrackingManager(): LiveTrackingManager = AndroidLiveTrackingManager()
