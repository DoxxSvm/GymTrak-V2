package `in`.gym.trak.studio.features.dashboard

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.gym_boy
import org.jetbrains.compose.resources.painterResource

@Composable
fun ImageAvatar(size: Dp = 36.dp, url: String? = null) {
    Surface(
        shape = CircleShape,
        modifier = Modifier.size(size),
        color = Color(0xFFE2E8F0)
    ) {
        if (!url.isNullOrBlank()) {
            Image(
                painter = rememberAsyncImagePainter(url),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(Res.drawable.gym_boy),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}
