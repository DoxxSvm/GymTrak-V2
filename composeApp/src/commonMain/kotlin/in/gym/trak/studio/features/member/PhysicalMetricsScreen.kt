package `in`.gym.trak.studio.features.member

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.components.AuthHeader
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.CommonDropdown
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.components.SectionLabel
import `in`.gym.trak.studio.components.SelectionCard
import `in`.gym.trak.studio.data.model.MemberProfileUpdateRequest
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.viewmodel.member.MemberOnboardingValidation
import `in`.gym.trak.studio.viewmodel.member.MemberProfileScreenModel
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_build_muscle
import gym.composeapp.generated.resources.ic_high
import gym.composeapp.generated.resources.ic_improve_endurance
import gym.composeapp.generated.resources.ic_lose_weight
import gym.composeapp.generated.resources.ic_low
import gym.composeapp.generated.resources.ic_moderate
import gym.composeapp.generated.resources.ic_stay_fit
import gym.composeapp.generated.resources.login_bg
import `in`.gym.trak.studio.components.AppScrollDefaults
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.painterResource

/**
 * Fetches member profile, shows physical metrics from the API, and PATCHes updates via [MemberProfileScreenModel.updateProfile].
 */
class PhysicalMetricsScreen(
    private val onPhysicalMetricsUpdated: (() -> Unit)? = null,
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { MemberProfileScreenModel() }
        val profile by screenModel.memberDetail.collectAsState()

        var height by remember { mutableStateOf("") }
        var weight by remember { mutableStateOf("") }
        var heightUnit by remember { mutableStateOf("Cm") }
        var weightUnit by remember { mutableStateOf("Kg") }
        var selectedActivity by remember { mutableStateOf("Moderate") }
        var selectedGoal by remember { mutableStateOf("Stay Fit") }
        var maintenanceCalories by remember { mutableStateOf("") }
        var appliedProfileId by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            screenModel.loadProfile(showFullLoader = true)
        }

        LaunchedEffect(profile?.id) {
            val p = profile ?: return@LaunchedEffect
            if (appliedProfileId == p.id) return@LaunchedEffect
            appliedProfileId = p.id

            val hCm = p.heightCm
            val wKg = p.weightKg
            height = when {
                hCm == null -> ""
                heightUnit == "In" -> (hCm / 2.54).roundToInt().toString()
                else -> {
                    val v = hCm
                    if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
                }
            }
            weight = when {
                wKg == null -> ""
                weightUnit == "Lbs" -> (wKg / 0.45359237).roundToInt().toString()
                else -> {
                    val v = wKg
                    if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
                }
            }
            selectedActivity = MemberOnboardingValidation.activityFromApi(p.activityLevel)
            selectedGoal = MemberOnboardingValidation.fitnessGoalFromApi(p.fitnessGoal)
            maintenanceCalories = p.wellness?.maintenanceCalories?.let { cal ->
                if (cal % 1.0 == 0.0) cal.toInt().toString() else cal.toString()
            }.orEmpty()
        }

        val bmiLine = profile?.wellness?.let { w ->
            val parts = listOfNotNull(
                w.bmi?.let { b -> "BMI ${if (b % 1.0 == 0.0) b.toInt().toString() else b.toString()}" },
                w.bmiCategory?.takeIf { it.isNotBlank() }?.replaceFirstChar { c -> c.titlecase() },
            )
            if (parts.isEmpty()) null else parts.joinToString(" · ")
        }

        LoadingScreenHandler(screenModel = screenModel) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { },
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.Black,
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    )
                },
                containerColor = Color.Transparent,
            ) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    Image(
                        painter = painterResource(Res.drawable.login_bg),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )

                    AppScrollableScreen(
                        contentPadding = AppScrollDefaults.screenContentPadding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        AuthHeader(
                            title = "Physical Metrics",
                            subtitle = "Update your physical metrics to keep your training accurate.",
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        SectionLabel("Height")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Box(modifier = Modifier.weight(2.5f)) {
                                CommonTextField(
                                    value = height,
                                    onValueChange = { height = it },
                                    placeholder = "Height",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                CommonDropdown(
                                    options = listOf("Cm", "In"),
                                    selectedOption = heightUnit,
                                    onOptionSelected = { heightUnit = it },
                                    placeholder = "Unit",
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        SectionLabel("Weight")
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            Box(modifier = Modifier.weight(2.5f)) {
                                CommonTextField(
                                    value = weight,
                                    onValueChange = { weight = it },
                                    placeholder = "Weight",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                CommonDropdown(
                                    options = listOf("Kg", "Lbs"),
                                    selectedOption = weightUnit,
                                    onOptionSelected = { weightUnit = it },
                                    placeholder = "Unit",
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        SectionLabel("Activity Level")
                        Spacer(modifier = Modifier.height(16.dp))

                        SelectionCard(
                            icon = Res.drawable.ic_high,
                            title = "High",
                            subtitle = "Intense exercise 6–7 days/week",
                            isSelected = selectedActivity == "High",
                            onClick = { selectedActivity = "High" },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SelectionCard(
                            icon = Res.drawable.ic_moderate,
                            title = "Moderate",
                            subtitle = "Exercise 3–5 days/week",
                            isSelected = selectedActivity == "Moderate",
                            onClick = { selectedActivity = "Moderate" },
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SelectionCard(
                            icon = Res.drawable.ic_low,
                            title = "Low",
                            subtitle = "Little to no exercise",
                            isSelected = selectedActivity == "Low",
                            onClick = { selectedActivity = "Low" },
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        SectionLabel("Fitness Goal")
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            SelectionCard(
                                modifier = Modifier.weight(1f),
                                icon = Res.drawable.ic_lose_weight,
                                title = "Lose Weight",
                                subtitle = "Burn fat and get lean",
                                isSelected = selectedGoal == "Lose Weight",
                                isHorizontal = false,
                                onClick = { selectedGoal = "Lose Weight" },
                            )
                            SelectionCard(
                                modifier = Modifier.weight(1f),
                                icon = Res.drawable.ic_build_muscle,
                                title = "Build Muscle",
                                subtitle = "Gain strength and mass",
                                isSelected = selectedGoal == "Build Muscle",
                                isHorizontal = false,
                                onClick = { selectedGoal = "Build Muscle" },
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            SelectionCard(
                                modifier = Modifier.weight(1f),
                                icon = Res.drawable.ic_stay_fit,
                                title = "Stay Fit",
                                subtitle = "Maintain current physique",
                                isSelected = selectedGoal == "Stay Fit",
                                isHorizontal = false,
                                onClick = { selectedGoal = "Stay Fit" },
                            )
                            SelectionCard(
                                modifier = Modifier.weight(1f),
                                icon = Res.drawable.ic_improve_endurance,
                                title = "Improve Endurance",
                                subtitle = "Boost cardio and stamina",
                                isSelected = selectedGoal == "Improve Endurance",
                                isHorizontal = false,
                                onClick = { selectedGoal = "Improve Endurance" },
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        SectionLabel("Wellness")
                        Spacer(modifier = Modifier.height(12.dp))
                        CommonCard(
                            elevation = 1.dp,
                            borderColor = Color(0xFFF3F4F6),
                            backgroundColor = Color(0xFFF9FAFB),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                            ) {
                                if (!bmiLine.isNullOrBlank()) {
                                    Text(
                                        bmiLine,
                                        style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Color(0xFF374151)),
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                Text(
                                    "Maintenance calories",
                                    style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Color(0xFF6B7280)),
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                CommonTextField(
                                    value = maintenanceCalories,
                                    onValueChange = { maintenanceCalories = it.filter { ch -> ch.isDigit() } },
                                    placeholder = "Enter maintenance calories",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(40.dp))

                        CommonButton(
                            onClick = {
                                val (hErr, wErr) = MemberOnboardingValidation.validateHeightWeightOnly(
                                    height,
                                    heightUnit,
                                    weight,
                                    weightUnit,
                                )
                                when {
                                    hErr != null -> screenModel.showError(hErr)
                                    wErr != null -> screenModel.showError(wErr)
                                    else -> {
                                        val (heightCmInt, weightKgInt) =
                                            MemberOnboardingValidation.parsePhysicalMetrics(
                                                height,
                                                heightUnit,
                                                weight,
                                                weightUnit,
                                            )
                                        if (heightCmInt == null || weightKgInt == null) {
                                            screenModel.showError("Enter valid height and weight.")
                                        } else {
                                            val maintenanceValue = maintenanceCalories.trim()
                                                .toDoubleOrNull()
                                                ?.takeIf { it > 0.0 }
                                            screenModel.updateProfile(
                                                MemberProfileUpdateRequest(
                                                    heightCm = heightCmInt.toDouble(),
                                                    weightKg = weightKgInt.toDouble(),
                                                    activityLevel = MemberOnboardingValidation.activityToApi(
                                                        selectedActivity,
                                                    ),
                                                    fitnessGoal = MemberOnboardingValidation.fitnessGoalToApi(
                                                        selectedGoal,
                                                    ),
                                                    maintenanceCalories = maintenanceValue,
                                                ),
                                                onSuccess = {
                                                    onPhysicalMetricsUpdated?.invoke()
                                                    navigator.pop()
                                                },
                                            )
                                        }
                                    }
                                }
                            },
                            text = "Update Metrics",
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }
}
