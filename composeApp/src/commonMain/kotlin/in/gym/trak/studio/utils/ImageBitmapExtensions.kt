package `in`.gym.trak.studio.utils

import androidx.compose.ui.graphics.ImageBitmap

expect fun ImageBitmap.encodeToPngBytes(quality: Int = 100): ByteArray?
