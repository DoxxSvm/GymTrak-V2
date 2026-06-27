package com.ananta.gym

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import `in`.gym.trak.studio.App
import `in`.gym.trak.studio.appContext
import `in`.gym.trak.studio.billing.BillingModule
import `in`.gym.trak.studio.data.repository.WorkoutManager
import `in`.gym.trak.studio.notifications.WorkoutNavigationBus

class MainActivity : ComponentActivity() {
    companion object {
        const val EXTRA_OPEN_ACTIVE_WORKOUT = "extra_open_active_workout"

        var instance: MainActivity? = null
            private set
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            WorkoutManager.touchActiveWorkoutClock()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        instance = this
        appContext = this
        BillingModule.iapManager.setContext(context = this, activity = this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        handleWorkoutNotificationIntent(intent)

        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWorkoutNotificationIntent(intent)
    }

    fun requestPostNotificationsPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            WorkoutManager.touchActiveWorkoutClock()
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun handleWorkoutNotificationIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_ACTIVE_WORKOUT, false) == true) {
            WorkoutNavigationBus.requestOpenActiveWorkout()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
