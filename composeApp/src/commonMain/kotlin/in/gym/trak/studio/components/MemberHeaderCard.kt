package `in`.gym.trak.studio.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.rememberAsyncImagePainter
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.PrimaryDarkColor
import `in`.gym.trak.studio.theme.White
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.gym_boy
import gym.composeapp.generated.resources.ic_edit
import gym.composeapp.generated.resources.ic_king
import gym.composeapp.generated.resources.ic_outline_whatsapp
import gym.composeapp.generated.resources.ic_phone
import gym.composeapp.generated.resources.ic_whatsapp
import org.jetbrains.compose.resources.painterResource

@Composable
fun MemberHeaderCard(
    name: String,
    imageUrl: String,
    membershipType: String,
    onCallClick: () -> Unit = {},
    onMessageClick: () -> Unit = {},
    onEditClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .border(
                width = 1.dp,
                color = PrimaryDarkColor.copy(alpha = 0.40f),
                shape = RoundedCornerShape(20.dp)
            ),

        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryDarkColor.copy(alpha = 0.10f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.clickable { onEditClick() }
            ) {
                val painter = if (imageUrl.isNotEmpty())
                    rememberAsyncImagePainter(imageUrl)
                else
                    painterResource(Res.drawable.gym_boy)
                Image(
                    painter = painter,

                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(White)
                        .border(2.dp, White, CircleShape),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.TopEnd)
                        .clip(CircleShape)
                        .background(PrimaryDarkColor)
                        .border(1.dp, White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_edit),
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = AppTextTheme.bold.copy(fontSize = 18.sp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_king),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = membershipType,
                        style = AppTextTheme.regular.copy(
                            fontSize = 13.sp,
                            color = Gray
                        )
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {


                IconButton(
                    onClick = onCallClick,
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(
                            elevation = 2.dp,
                            shape = CircleShape,
                            clip = true
                        )
                        .clip(CircleShape)
                        .background(White)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_phone),
                        contentDescription = "Call",
                        tint = PrimaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }


                IconButton(
                    onClick = onMessageClick,
                    modifier = Modifier
                        .size(36.dp)
                        .shadow(
                            elevation = 2.dp,
                            shape = CircleShape,
                            clip = true
                        )
                        .clip(CircleShape)
                        .background(White)
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_outline_whatsapp),
                        contentDescription = "WhatsApp",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
