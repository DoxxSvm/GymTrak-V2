package `in`.gym.trak.studio.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun AppEmptyStateView(
    image: DrawableResource,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    useCardContainer: Boolean = true
) {
    val content: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(image),
                contentDescription = title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = AppTextTheme.semiBold.copy(fontSize = 26.sp, color = Black),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray),
                textAlign = TextAlign.Center
            )
        }
    }

    if (useCardContainer) {
        Box(
            modifier = modifier,
            content = { content() }
        )
    } else {
        Column(modifier = modifier) {
            content()
        }
    }
}
