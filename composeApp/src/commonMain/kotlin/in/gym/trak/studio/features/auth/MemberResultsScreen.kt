package `in`.gym.trak.studio.features.auth

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.data.model.MemberOnboardingDraft
import `in`.gym.trak.studio.features.member.MemberDashboardScreen
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.viewmodel.member.MemberOnboardingScreenModel
import `in`.gym.trak.studio.viewmodel.member.MemberOnboardingValidation
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_ai_diet_plan
import gym.composeapp.generated.resources.ic_ai_workout_plan
import gym.composeapp.generated.resources.login_bg
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

data class MemberResultsScreen(val draft: MemberOnboardingDraft) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { MemberOnboardingScreenModel() }
        val wellness by screenModel.wellness.collectAsState()
        val submitDone by screenModel.submitDone.collectAsState()
        val isLoading by screenModel.isLoading.collectAsState()
        val errorMessage by screenModel.errorMessage.collectAsState()

        LaunchedEffect(draft) {
            val body = MemberOnboardingValidation.toRequest(draft) ?: return@LaunchedEffect
            screenModel.submitMemberProfile(body, draft.tempToken)
        }

        Scaffold(
            topBar = { },
            bottomBar = {
                Box(
                    modifier = Modifier
                        .padding(24.dp)
                        .navigationBarsPadding()
                ) {
                    CommonButton(
                        onClick = { navigator.replaceAll(MemberDashboardScreen()) },
                        text = "Get Started",
                        enabled = submitDone && !isLoading
                    )
                }
            },
            containerColor = Color.Transparent
        ) { _ ->
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(Res.drawable.login_bg),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                AppScrollableScreen(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Spacer(modifier = Modifier.height(100.dp))

                    Text(
                        text = "Your Results!",
                        style = AppTextTheme.semiBold.copy(
                            fontSize = 24.sp,
                            color = Color.Black
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Based on your information",
                        style = AppTextTheme.regular.copy(
                            fontSize = 12.sp,
                            color = Black.copy(alpha = 0.80f)
                        )
                    )

                    errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = msg,
                            color = Color(0xFFB91C1C),
                            style = AppTextTheme.regular.copy(fontSize = 14.sp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CommonButton(
                            onClick = {
                                screenModel.clearError()
                                MemberOnboardingValidation.toRequest(draft)?.let {
                                    screenModel.submitMemberProfile(it, draft.tempToken)
                                }
                            },
                            text = "Retry",
                            enabled = MemberOnboardingValidation.toRequest(draft) != null && !isLoading
                        )
                    }

                    if (MemberOnboardingValidation.toRequest(draft) == null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Something is missing from your profile. Go back and check your details.",
                            style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Color(0xFFB91C1C))
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    val bmiValue = wellness?.bmi ?: 0.0
                    val bmiText = wellness?.bmi?.let { ((it * 10).toInt() / 10.0).toString() } ?: "—"
                    val bmiCategory = wellness?.bmiCategory ?: "—"
                    val maintenance = wellness?.maintenanceCalories

                    CommonCard(content = {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .border(4.dp, PrimaryColor, CircleShape)
                                    .background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        bmiText,
                                        style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Color.Black)
                                    )
                                    Text(
                                        "BMI",
                                        style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Color(0xFF6B7280))
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            Text(
                                text = bmiCategory,
                                style = AppTextTheme.bold.copy(fontSize = 24.sp, color = Color.Black)
                            )
                        }
                    })

                    Spacer(modifier = Modifier.height(16.dp))

                    if (maintenance != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp)
                            ) {
                                Text(
                                    text = "Maintenance Calories",
                                    style = AppTextTheme.semiBold.copy(fontSize = 16.sp, color = Color.Black)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "$maintenance Cal",
                                    style = AppTextTheme.semiBold.copy(fontSize = 24.sp, color = PrimaryColor)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "This is your estimated daily calorie need to maintain current weight.",
                                    style = AppTextTheme.regular.copy(
                                        fontSize = 12.sp,
                                        color = Black.copy(alpha = 0.60f),
                                        lineHeight = 18.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Ready To Start ?",
                        style = AppTextTheme.semiBold.copy(
                            fontSize = 16.sp,
                            color = Color.Black
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ActionCard(
                        icon = Res.drawable.ic_ai_diet_plan,
                        title = "AI Diet Plan",
                        subtitle = "Personalized nutrition"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ActionCard(
                        icon = Res.drawable.ic_ai_workout_plan,
                        title = "AI Workout Plan",
                        subtitle = "Personalized training"
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }

                if (isLoading && wellness == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryColor)
                    }
                }
            }
        }
    }

    @Composable
    private fun ActionCard(
        icon: DrawableResource,
        title: String,
        subtitle: String
    ) {
        CommonCard(content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Color.Black)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Color(0xFF6B7280))
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(20.dp)
                )
            }
        })
    }
}
