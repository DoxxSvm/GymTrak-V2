package `in`.gym.trak.studio.tracking

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ananta.gym.MainActivity
import `in`.gym.trak.studio.appContext

/**
 * Android Foreground Service for lock screen / notification tracking.
 * Shows a live notification with progress, ETA and status — stays alive in background.
 */
class TrackingForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "gym_tracking_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "gym.trak.studio.ACTION_STOP_TRACKING"

        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_ETA = "extra_eta"
        const val EXTRA_PROGRESS = "extra_progress"
        const val EXTRA_STATUS = "extra_status"
        const val EXTRA_STATUS_DETAIL = "extra_status_detail"
        const val EXTRA_USER_NAME = "extra_user_name"

        fun buildStartIntent(context: Context, data: TrackingData): Intent =
            Intent(context, TrackingForegroundService::class.java).apply {
                putExtra(EXTRA_TITLE, data.title)
                putExtra(EXTRA_ETA, data.eta)
                putExtra(EXTRA_PROGRESS, data.progress)
                putExtra(EXTRA_STATUS, data.status)
                putExtra(EXTRA_STATUS_DETAIL, data.statusDetail)
                putExtra(EXTRA_USER_NAME, data.userName)
            }
    }

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        val data = intent?.toTrackingData() ?: TrackingData(
            title = "Tracking Active",
            eta = "--",
            progress = 0f,
            status = "In Progress"
        )

        val notification = buildNotification(data)
        startForeground(NOTIFICATION_ID, notification)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        notificationManager.cancel(NOTIFICATION_ID)
    }

    // ─── Notification ───────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Tracking",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time gym session tracking"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(data: TrackingData): Notification {
        val progressInt = (data.progress * 100).toInt().coerceIn(0, 100)

        // Tap opens app
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val tapPendingIntent = PendingIntent.getActivity(
            this,
            0,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop action
        val stopIntent = Intent(this, TrackingForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(data.title)
            .setContentText("${data.status} · ETA: ${data.eta}")
            .setSubText(data.userName)
            .setProgress(100, progressInt, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setContentIntent(tapPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                data.actionLabel,
                stopPendingIntent
            )
            .build()
    }

    // ─── Helper ─────────────────────────────────────────────────────────────

    private fun Intent.toTrackingData() = TrackingData(
        title = getStringExtra(EXTRA_TITLE) ?: "Tracking",
        eta = getStringExtra(EXTRA_ETA) ?: "--",
        progress = getFloatExtra(EXTRA_PROGRESS, 0f),
        status = getStringExtra(EXTRA_STATUS) ?: "",
        statusDetail = getStringExtra(EXTRA_STATUS_DETAIL) ?: "",
        userName = getStringExtra(EXTRA_USER_NAME)
    )
}

// ─── Companion update function (called from actual) ─────────────────────────

fun updateTrackingNotification(data: TrackingData) {
    val context = appContext ?: return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    // Reuse service's buildNotification via a temp instance approach:
    val svc = TrackingForegroundService()
    // reflection-free: just rebuild via NotificationCompat directly
    val progressInt = (data.progress * 100).toInt().coerceIn(0, 100)

    val tapIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val tapPendingIntent = PendingIntent.getActivity(
        context, 0, tapIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val stopIntent = Intent(context, TrackingForegroundService::class.java).apply {
        action = TrackingForegroundService.ACTION_STOP
    }
    val stopPendingIntent = PendingIntent.getService(
        context, 1, stopIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, TrackingForegroundService.CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_menu_compass)
        .setContentTitle(data.title)
        .setContentText("${data.status} · ETA: ${data.eta}")
        .setSubText(data.userName)
        .setProgress(100, progressInt, false)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setContentIntent(tapPendingIntent)
        .addAction(
            android.R.drawable.ic_menu_close_clear_cancel,
            data.actionLabel,
            stopPendingIntent
        )
        .build()

    manager.notify(TrackingForegroundService.NOTIFICATION_ID, notification)
}
