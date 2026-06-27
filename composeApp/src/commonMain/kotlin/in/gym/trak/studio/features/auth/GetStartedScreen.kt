package `in`.gym.trak.studio.features.auth

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.theme.AppTextTheme
import gym.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

import `in`.gym.trak.studio.data.repository.SessionManager

class GetStartedScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        GetStartedContent(
            onGetStarted = {
                SessionManager.isFirstTimeUser = false
                navigator.replace(LoginScreen())
            }
        )
    }
}

@Composable
@Preview
fun GetStartedPreview() {
    GetStartedContent(onGetStarted = {})
}

@Composable
fun GetStartedContent(
    onGetStarted: () -> Unit
) {

    Box(
        modifier = Modifier.fillMaxSize()
    ) {

        // Background Image
        Image(
            painter = painterResource(Res.drawable.splash_screen),
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(25.dp),
            contentScale = ContentScale.Crop
        )

        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xAA000000),
                            Color(0xDD000000)
                        )
                    )
                )
        )

        // Main Scrollable Content
        AppScrollableScreen(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            Spacer(modifier = Modifier.height(60.dp))

            // Title
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White)) {
                        append("Gym ")
                    }
                    withStyle(SpanStyle(color = Color(0xFF3ACB7A))) {
                        append("Trak")
                    }
                },
                style = AppTextTheme.semiBold.copy(fontSize = 36.sp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Your Complete Fitness Partner",
                color = Color.LightGray,
                style = AppTextTheme.regular.copy(fontSize = 12.sp)


            )

            Spacer(modifier = Modifier.height(30.dp))

            // Hero Image
            Image(
                painter = painterResource(Res.drawable.gym_boy),
                contentDescription = null,
                modifier = Modifier.height(280.dp)
            )

            // Community Card
            CommunityFeatureCard(
                icon = Res.drawable.community,
                title = "Engage With Your Fitness Community"
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Res.drawable.manage,
                    title = "Manage Your\nGYM"
                )

                FeatureCard(
                    modifier = Modifier.weight(1f),
                    icon = Res.drawable.trackdiets,
                    title = "Track Diets And\nWorkout"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White)) {
                        append("Transform Your ")
                    }
                    withStyle(SpanStyle(color = Color(0xFF3ACB7A))) {
                        append("Fitness\nJourney")
                    }
                },
                textAlign = TextAlign.Center,
                style = AppTextTheme.semiBold.copy(fontSize = 24.sp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            CommonButton(
                onClick = onGetStarted,
                text = "Get Started"
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun CommunityFeatureCard(
    icon: DrawableResource,
    title: String
) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
    ) {

        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(40.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White.copy(alpha = 0.18f)
            ),

            elevation = CardDefaults.cardElevation(0.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Image(
                    painter = painterResource(icon),
                    contentDescription = null,
                    modifier = Modifier.size(38.dp)
                )

                Spacer(modifier = Modifier.width(14.dp))

                Text(
                    text = title,
                    color = Color.White,
                    style = AppTextTheme.semiBold.copy(fontSize = 15.sp)
                )
            }
        }
    }
}

@Composable
fun FeatureCard(
    modifier: Modifier,
    icon: DrawableResource,
    title: String
) {

    Card(
        modifier = modifier.height(120.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2A2A2A)
        )
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
