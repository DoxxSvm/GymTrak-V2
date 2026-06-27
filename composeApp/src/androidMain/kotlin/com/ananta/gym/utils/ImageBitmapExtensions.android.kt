package `in`.gym.trak.studio.utils

import android.graphics.Bitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import java.io.ByteArrayOutputStream

actual fun ImageBitmap.encodeToPngBytes(quality: Int): ByteArray? {
    return runCatching {
        ByteArrayOutputStream().use { stream ->
            asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, quality.coerceIn(0, 100), stream)
            stream.toByteArray()
        }
    }.getOrNull()
}
