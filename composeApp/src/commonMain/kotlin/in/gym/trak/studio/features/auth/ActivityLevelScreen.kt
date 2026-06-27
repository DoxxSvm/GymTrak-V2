package `in`.gym.trak.studio.features.auth

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.components.AuthHeader
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.SelectionCard
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.PrimaryColor
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_high
import gym.composeapp.generated.resources.ic_low
import gym.composeapp.generated.resources.ic_moderate
import gym.composeapp.generated.resources.login_bg
import `in`.gym.trak.studio.data.model.MemberOnboardingDraft
import `in`.gym.trak.studio.viewmodel.member.MemberOnboardingValidation
import org.jetbrains.compose.resources.painterResource

data class ActivityLevelScreen(val draft: MemberOnboardingDraft) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var selectedLevel by remember { mutableStateOf<String?>(null) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { },
                    navigationIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 8.dp).clickable { navigator.pop() }
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back", style = AppTextTheme.medium.copy(fontSize = 16.sp, color = Color.Black))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                Box(modifier = Modifier.padding(24.dp)
                    .navigationBarsPadding()
                ) {
                    CommonButton(
                        onClick = {
                            val level = selectedLevel ?: return@CommonButton
                            val next = draft.copy(
                                activityLevel = MemberOnboardingValidation.activityToApi(level)
                            )
                            navigator.push(FitnessGoalScreen(next))
                        },
                        text = "Continue",
                        enabled = selectedLevel != null
                    )
                }
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                Image(
                    painter = painterResource(Res.drawable.login_bg),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                AppScrollableScreen(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    Spacer(modifier = Modifier.height(100.dp))
                    
                    AuthHeader(
                        title = "Activity Level",
                        subtitle = "How active are you daily ?"
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    SelectionCard(
                        icon = Res.drawable.ic_high,
                        title = "High",
                        subtitle = "Intense exercise 6-7 days/week",
                        isSelected = selectedLevel == "High",
                        onClick = { selectedLevel = "High" }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SelectionCard(
                       icon = Res.drawable.ic_moderate ,
                        title = "Moderate",
                        subtitle = "Exercise 3-5 days/week",
                        isSelected = selectedLevel == "Moderate",
                        onClick = { selectedLevel = "Moderate" }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SelectionCard(
                        icon = Res.drawable.ic_low,
                        title = "Low",
                        subtitle = "Little to no exercise",
                        isSelected = selectedLevel == "Low",
                        onClick = { selectedLevel = "Low" }
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
