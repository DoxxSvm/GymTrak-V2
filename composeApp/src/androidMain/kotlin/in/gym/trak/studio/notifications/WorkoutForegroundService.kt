package `in`.gym.trak.studio.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.ananta.gym.MainActivity
import `in`.gym.trak.studio.R
import `in`.gym.trak.studio.appContext

private fun buildOpenActiveWorkoutIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra(MainActivity.EXTRA_OPEN_ACTIVE_WORKOUT, true)
    }

/**
 * Foreground service for an active workout session.
 * Keeps a persistent notification on the lock screen and in the shade while training.
 */
class WorkoutForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "workout_live_channel"
        const val NOTIFICATION_ID = 1002

        const val EXTRA_TITLE = "extra_workout_title"
        const val EXTRA_START_TIME_MILLIS = "extra_start_time_millis"
        const val EXTRA_DURATION_LABEL = "extra_duration_label"
        const val EXTRA_EXERCISE_COUNT = "extra_exercise_count"
        const val EXTRA_COMPLETED_SETS = "extra_completed_sets"
        const val EXTRA_TOTAL_SETS = "extra_total_sets"

        fun buildStartIntent(context: Context, data: WorkoutLiveNotificationData): Intent =
            Intent(context, WorkoutForegroundService::class.java).apply {
                putExtras(data.toExtras())
            }

        fun buildUpdateIntent(context: Context, data: WorkoutLiveNotificationData): Intent =
            Intent(context, WorkoutForegroundService::class.java).apply {
                putExtras(data.toExtras())
            }
    }

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val data = intent?.toWorkoutLiveNotificationData()
            ?: WorkoutLiveNotificationData(
                title = "Workout",
                startTimeMillis = System.currentTimeMillis(),
                durationLabel = "0:00",
                exerciseCount = 0,
                completedSets = 0,
                totalSets = 0,
            )

        val notification = buildNotification(data)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        notificationManager.cancel(NOTIFICATION_ID)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Live Workout",
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Shows elapsed time and progress while a workout is active"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(data: WorkoutLiveNotificationData): Notification {
        val tapIntent = buildOpenActiveWorkoutIntent(this)
        val tapPendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val detail = buildString {
            append(data.setsLabel)
            append(" · ")
            append(data.exerciseSummary)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_workout_notification)
            .setContentTitle(data.title)
            .setContentText(detail)
            .setSubText("Workout in progress")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setContentIntent(tapPendingIntent)
            .setUsesChronometer(true)
            .setChronometerCountDown(false)
            .setWhen(data.startTimeMillis)
            .setShowWhen(true)
            .build()
    }
}

private fun WorkoutLiveNotificationData.toExtras(): android.os.Bundle =
    android.os.Bundle().apply {
        putString(WorkoutForegroundService.EXTRA_TITLE, title)
        putLong(WorkoutForegroundService.EXTRA_START_TIME_MILLIS, startTimeMillis)
        putString(WorkoutForegroundService.EXTRA_DURATION_LABEL, durationLabel)
        putInt(WorkoutForegroundService.EXTRA_EXERCISE_COUNT, exerciseCount)
        putInt(WorkoutForegroundService.EXTRA_COMPLETED_SETS, completedSets)
        putInt(WorkoutForegroundService.EXTRA_TOTAL_SETS, totalSets)
    }

private fun Intent.toWorkoutLiveNotificationData(): WorkoutLiveNotificationData =
    WorkoutLiveNotificationData(
        title = getStringExtra(WorkoutForegroundService.EXTRA_TITLE) ?: "Workout",
        startTimeMillis = getLongExtra(WorkoutForegroundService.EXTRA_START_TIME_MILLIS, System.currentTimeMillis()),
        durationLabel = getStringExtra(WorkoutForegroundService.EXTRA_DURATION_LABEL) ?: "0:00",
        exerciseCount = getIntExtra(WorkoutForegroundService.EXTRA_EXERCISE_COUNT, 0),
        completedSets = getIntExtra(WorkoutForegroundService.EXTRA_COMPLETED_SETS, 0),
        totalSets = getIntExtra(WorkoutForegroundService.EXTRA_TOTAL_SETS, 0),
    )

fun updateWorkoutLiveNotification(data: WorkoutLiveNotificationData) {
    val context = appContext ?: return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    ensureWorkoutNotificationChannel(context, manager)

    val tapIntent = buildOpenActiveWorkoutIntent(context)
    val tapPendingIntent = PendingIntent.getActivity(
        context,
        WorkoutForegroundService.NOTIFICATION_ID,
        tapIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val detail = buildString {
        append(data.setsLabel)
        append(" · ")
        append(data.exerciseSummary)
    }

    val notification = NotificationCompat.Builder(context, WorkoutForegroundService.CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_workout_notification)
        .setContentTitle(data.title)
        .setContentText(detail)
        .setSubText("Workout in progress")
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .setAutoCancel(false)
        .setLocalOnly(false)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setCategory(NotificationCompat.CATEGORY_WORKOUT)
        .setContentIntent(tapPendingIntent)
        .setUsesChronometer(true)
        .setChronometerCountDown(false)
        .setWhen(data.chronometerBaseMillis)
        .setShowWhen(true)
        .build()

    manager.notify(WorkoutForegroundService.NOTIFICATION_ID, notification)
}

fun stopWorkoutLiveNotification() {
    val context = appContext ?: return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    manager.cancel(WorkoutForegroundService.NOTIFICATION_ID)
    context.stopService(Intent(context, WorkoutForegroundService::class.java))
}

private fun ensureWorkoutNotificationChannel(context: Context, manager: NotificationManager) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val existing = manager.getNotificationChannel(WorkoutForegroundService.CHANNEL_ID)
    if (existing != null) return
    val channel = NotificationChannel(
        WorkoutForegroundService.CHANNEL_ID,
        "Live Workout",
        NotificationManager.IMPORTANCE_LOW,
    ).apply {
        description = "Shows elapsed time and progress while a workout is active"
        setShowBadge(false)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
    }
    manager.createNotificationChannel(channel)
}
