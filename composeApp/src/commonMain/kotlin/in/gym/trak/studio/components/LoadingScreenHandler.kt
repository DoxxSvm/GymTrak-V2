package `in`.gym.trak.studio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import `in`.gym.trak.studio.network.BaseScreenModel

/**
 * A reusable wrapper that reacts to the isLoading state of a BaseScreenModel.
 * It automatically dims the background and shows a loading indicator while isLoading is true.
 */
@Composable
    fun LoadingScreenHandler(
    screenModel: BaseScreenModel,
    showLoadingOverlay: Boolean = true,
    content: @Composable () -> Unit
) {
    val isLoading by screenModel.isLoading.collectAsState()
    val errorMessage by screenModel.errorMessage.collectAsState()
    val toastMessage by screenModel.toastMessage.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        
        if (isLoading && showLoadingOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    // Block click events while loading
                    .clickable(enabled = false) { }
            ) {
                CommonProgressOverlay()
            }
        }

        when {
            errorMessage != null -> ToastComponent(
                message = errorMessage!!,
                onDismiss = { screenModel.clearError() },
                isSuccess = false
            )
            toastMessage != null -> ToastComponent(
                message = toastMessage!!,
                onDismiss = { screenModel.clearToast() },
                isSuccess = true
            )
        }
    }
}
