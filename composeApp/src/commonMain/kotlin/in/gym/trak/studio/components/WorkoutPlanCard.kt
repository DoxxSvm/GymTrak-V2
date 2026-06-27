package `in`.gym.trak.studio.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.RedColor
import `in`.gym.trak.studio.theme.White
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.gym_boy
import gym.composeapp.generated.resources.ic_delete
import gym.composeapp.generated.resources.ic_delete_outline
import gym.composeapp.generated.resources.ic_dumble
import gym.composeapp.generated.resources.ic_edit
import gym.composeapp.generated.resources.ic_edit_outline
import gym.composeapp.generated.resources.ic_workout
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun WorkoutPlanCard(
    title: String,
    count: String,
    tag: String,
    modifier: Modifier = Modifier,
    imageRes: DrawableResource = Res.drawable.gym_boy, // Default image
    createdBy: String? = null,
    onCardClick: () -> Unit = {},
    onEditClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onCardClick)
        ) {
            // Background Image
            Image(
                painter = painterResource(imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient at the bottom for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.9f)
                            ),
                            startY = 300f // Adjust fade point
                        )
                    )
            )

            // Top-left Tag
//            Box(
//                modifier = Modifier
//                    .padding(16.dp)
//                    .background(
//                        color = White.copy(alpha = 0.3f), // Semi-transparent white
//                        shape = RoundedCornerShape(20.dp)
//                    )
//                    .padding(horizontal = 12.dp, vertical = 6.dp)
//            ) {
//                Text(
//                    text = tag,
//                    style = AppTextTheme.semiBold.copy(fontSize = 14.sp, color = White)
//                )
//            }

            // Bottom Content
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Title and Workout Count
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = AppTextTheme.bold.copy(fontSize = 16.sp, color = White),
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_dumble),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = count,
                            style = AppTextTheme.regular.copy(fontSize = 12.sp, color = White)
                        )
                    }
                    createdBy?.takeIf { it.isNotBlank() }?.let { author ->
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Created by $author",
                            style = AppTextTheme.regular.copy(
                                fontSize = 11.sp,
                                color = White.copy(alpha = 0.85f),
                            ),
                            maxLines = 1,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Action Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val editInteraction = remember { MutableInteractionSource() }
                    val deleteInteraction = remember { MutableInteractionSource() }
                    // Edit Button
//                    Surface(
//                        modifier = Modifier
//                            .size(36.dp)
//                            .clip(CircleShape)
//                            .clickable(
//                                onClick = onEditClick,
//                                indication = null,
//                                interactionSource = editInteraction
//                            ),
//                        color = White,
//                        shape = CircleShape
//                    ) {
//                        Box(contentAlignment = Alignment.Center) {
//                            Icon(
//                                painter = painterResource(Res.drawable.ic_edit_outline),
//                                contentDescription = "Edit",
//                                tint = Color.Black,
//                                modifier = Modifier.size(16.dp)
//                            )
//                        }
//                    }

                    // Delete Button
                    Surface(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(
                                onClick = onDeleteClick,
                                indication = null,
                                interactionSource = deleteInteraction
                            ),
                        color = White,
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_delete_outline),
                                contentDescription = "Delete",
                                tint = RedColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
