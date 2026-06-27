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
import gym.composeapp.generated.resources.ic_build_muscle
import gym.composeapp.generated.resources.ic_improve_endurance
import gym.composeapp.generated.resources.ic_lose_weight
import gym.composeapp.generated.resources.ic_low
import gym.composeapp.generated.resources.ic_stay_fit
import gym.composeapp.generated.resources.login_bg
import `in`.gym.trak.studio.data.model.MemberOnboardingDraft
import `in`.gym.trak.studio.viewmodel.member.MemberOnboardingValidation
import org.jetbrains.compose.resources.painterResource

data class FitnessGoalScreen(val draft: MemberOnboardingDraft) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var selectedGoal by remember { mutableStateOf<String?>(null) }

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
                            val goal = selectedGoal ?: return@CommonButton
                            val next = draft.copy(
                                fitnessGoal = MemberOnboardingValidation.fitnessGoalToApi(goal)
                            )
                            navigator.push(MemberResultsScreen(next))
                        },
                        text = "See Result",
                        enabled = selectedGoal != null
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
                        title = "Fitness Goal",
                        subtitle = "What do you want to achieve?"
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SelectionCard(
                            modifier = Modifier.weight(1f),
                            icon = Res.drawable.ic_lose_weight,
                            title = "Lose Weight",
                            subtitle = "Burn fat and get lean",
                            isSelected = selectedGoal == "Lose Weight",
                            isHorizontal = false,
                            onClick = { selectedGoal = "Lose Weight" }
                        )

                        SelectionCard(
                            modifier = Modifier.weight(1f),
                            icon = Res.drawable.ic_build_muscle,
                            title = "Build Muscle",
                            subtitle = "Gain Strength and mass",
                            isSelected = selectedGoal == "Build Muscle",
                            isHorizontal = false,
                            onClick = { selectedGoal = "Build Muscle" }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        SelectionCard(
                            modifier = Modifier.weight(1f),
                            icon = Res.drawable.ic_stay_fit,
                            title = "Stay Fit",
                            subtitle = "Maintain Current Physique",
                            isSelected = selectedGoal == "Stay Fit",
                            isHorizontal = false,
                            onClick = { selectedGoal = "Stay Fit" }
                        )

                        SelectionCard(
                            modifier = Modifier.weight(1f),
                            icon = Res.drawable.ic_improve_endurance,
                            title = "Improve Endurance",
                            subtitle = "Boost cardio and stamina",
                            isSelected = selectedGoal == "Improve Endurance",
                            isHorizontal = false,
                            onClick = { selectedGoal = "Improve Endurance" }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
