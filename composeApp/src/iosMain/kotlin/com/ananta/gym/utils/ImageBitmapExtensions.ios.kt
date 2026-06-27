package `in`.gym.trak.studio.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image

actual fun ImageBitmap.encodeToPngBytes(quality: Int): ByteArray? {
    return runCatching {
        Image.makeFromBitmap(asSkiaBitmap())
            .encodeToData(EncodedImageFormat.PNG, quality.coerceIn(0, 100))
            ?.bytes
    }.getOrNull()
}
