package `in`.gym.trak.studio.components

import androidx.compose.runtime.Composable

@Composable
actual fun rememberExitApplication(): () -> Unit {
    // No-op on iOS. Programmatically exiting is discouraged by platform guidelines.
    return {}
}

