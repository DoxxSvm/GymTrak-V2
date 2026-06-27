//package `in`.gym.trak.studio.components
//
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.rememberCoroutineScope
//import dev.icerock.moko.permissions.Permission
//import dev.icerock.moko.permissions.PermissionsController
//import dev.icerock.moko.permissions.compose.BindEffect
//import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
//import kotlinx.coroutines.launch
//
//@Composable
//fun rememberMediaPermissionHandler(
//    onCameraGranted: () -> Unit,
//    onGalleryGranted: () -> Unit
//): MediaPermissionHandler {
//    val factory = rememberPermissionsControllerFactory()
//    val controller = remember(factory) { factory.createPermissionsController() }
//    val coroutineScope = rememberCoroutineScope()
//    BindEffect(controller)
//
//    return remember(controller, coroutineScope, onCameraGranted, onGalleryGranted) {
//        MediaPermissionHandler(controller, coroutineScope, onCameraGranted, onGalleryGranted)
//    }
//}
//
//class MediaPermissionHandler(
//    private val controller: PermissionsController,
//    private val scope: kotlinx.coroutines.CoroutineScope,
//    private val onCameraGranted: () -> Unit,
//    private val onGalleryGranted: () -> Unit
//) {
//    fun requestCamera() {
//        scope.launch {
//            try {
//                controller.providePermission(Permission.CAMERA)
//                onCameraGranted()
//            } catch (e: Exception) {
//                // Permission denied explicitly
//            }
//        }
//    }
//
//    fun requestGallery() {
//        scope.launch {
//            try {
//                controller.providePermission(Permission.GALLERY)
//                onGalleryGranted()
//            } catch (e: Exception) {
//                // Permission denied explicitly
//            }
//        }
//    }
//}
