package `in`.gym.trak.studio.features.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Person

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.PrimaryColor
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_gym
import gym.composeapp.generated.resources.ic_user
import gym.composeapp.generated.resources.login_bg
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

import cafe.adriel.voyager.core.model.rememberScreenModel
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.viewmodel.auth.AuthScreenModel

class RoleSelectionScreen(private val tempToken: String = "") : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { AuthScreenModel() }

        LoadingScreenHandler(screenModel = screenModel) {
            RoleSelectionContent(
                onRoleSelected = { isOwner ->
                    val role = if (isOwner) "gym_owner" else "member"
                    screenModel.selectRole(tempToken, role) {
                        navigator.push(OnboardingScreen(isOwner, tempToken = tempToken))
                    }
                }
            )
        }
    }
}

@Composable
@Preview
fun RoleSelectionContentPreview() {
    RoleSelectionContent(onRoleSelected = {})
}
@Composable
fun RoleSelectionContent(
    onRoleSelected: (isOwner: Boolean) -> Unit
) {
    var selectedIsOwner by remember { mutableStateOf<Boolean?>(true) }

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Image(
            painter = painterResource(Res.drawable.login_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "Choose Your Role",
                style = AppTextTheme.bold.copy(
                    fontSize = 32.sp,
                    color = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Immerse Yourself In The Experience",
                style = AppTextTheme.regular.copy(
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )
            )

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                RoleCard(
                    modifier = Modifier.weight(1f),
                    title = "Gym Member",
                    description = "Track Workout ,Diet\nProgress And Connect .",
                    icon = Res.drawable.ic_user,
                    isSelected = selectedIsOwner == false,
                    onClick = { selectedIsOwner = false }
                )
                RoleCard(
                    modifier = Modifier.weight(1f),
                    title = "Gym Owner",
                    description = "I Manage\nFacilities & Staff",
                    icon = Res.drawable.ic_gym,
                    isSelected = selectedIsOwner == true,
                    onClick = { selectedIsOwner = true }
                )


            }

            Spacer(modifier = Modifier.weight(1f))

            CommonButton(
                onClick = { selectedIsOwner?.let { onRoleSelected(it) } },
                text = "Continue",
                enabled = selectedIsOwner != null
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "You Can Switch Later In Your Profile Setting If Needed.",
                style = AppTextTheme.regular.copy(
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun RoleCard(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    icon: DrawableResource,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE8F5E9) else Color.White
        ),
        border = BorderStroke(
            width = if (isSelected) 1.dp else 1.dp,
            color = if (isSelected) PrimaryColor else Color(0xFFE5E7EB)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                style = AppTextTheme.bold.copy(
                    fontSize = 16.sp,
                    color = Color.Black
                ),
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = description,
                style = AppTextTheme.regular.copy(
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280)
                ),
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}
