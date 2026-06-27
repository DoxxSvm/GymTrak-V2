package `in`.gym.trak.studio.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.DarkBlue
import `in`.gym.trak.studio.theme.GrayBorderColor
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_workout
import org.jetbrains.compose.resources.painterResource

/**
 * Reusable Product Card for the Shop grid.
 */
@Composable
fun ProductCard(
    name: String,
    price: String,
    strikethroughPrice: String? = null,
    modifier: Modifier = Modifier,
    imageUrl: String = "", // Placeholder for actual image loading
    isFavorite: Boolean = false,
    showLike: Boolean = false,
    onLikeClick: () -> Unit = {},
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(GrayBorderColor)
        ) {
            val painter = if (imageUrl.isNotBlank()) {
                rememberAsyncImagePainter(imageUrl)
            } else {
                painterResource(Res.drawable.ic_workout)
            }
            Image(
                painter = painter,
                contentDescription = name,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (imageUrl.isBlank()) Modifier.padding(16.dp) else Modifier),
                contentScale = if (imageUrl.isBlank()) ContentScale.Fit else ContentScale.Crop
            )
            if (showLike) {
                Box(
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(White.copy(alpha = 0.9f))
                        .clickable { onLikeClick() },
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = if (isFavorite) Color(0xFFEF4444) else DarkBlue
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = name,
            style = AppTextTheme.medium.copy(
                fontSize = 14.sp,
                color = DarkBlue,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = price,
                style = AppTextTheme.bold.copy(
                    fontSize = 16.sp,
                    color = PrimaryColor,
                    fontWeight = FontWeight.Bold
                )
            )
            if (!strikethroughPrice.isNullOrBlank()) {
                Text(
                    text = strikethroughPrice,
                    style = AppTextTheme.medium.copy(
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        textDecoration = TextDecoration.LineThrough
                    )
                )
            }
        }
    }
}
