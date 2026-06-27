package `in`.gym.trak.studio.utils

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.ananta.gym.MainActivity
import `in`.gym.trak.studio.data.repository.WorkoutManager

actual fun requestNotificationPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val activity = MainActivity.instance ?: return
        if (ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            WorkoutManager.touchActiveWorkoutClock()
        } else {
            activity.requestPostNotificationsPermission()
        }
    }
}
