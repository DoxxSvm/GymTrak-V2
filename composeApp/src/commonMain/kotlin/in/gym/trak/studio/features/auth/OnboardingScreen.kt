package `in`.gym.trak.studio.features.auth

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.autofill.AutofillType

import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.components.AuthHeader
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonDropdown
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.CustomTextField
import `in`.gym.trak.studio.components.GenderSelector
import `in`.gym.trak.studio.components.SectionLabel
import `in`.gym.trak.studio.features.dashboard.OwnerDashboardScreen
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import gym.composeapp.generated.resources.*
import `in`.gym.trak.studio.features.member.MemberDashboardScreen
import org.jetbrains.compose.resources.painterResource

import cafe.adriel.voyager.core.model.rememberScreenModel
import `in`.gym.trak.studio.components.GymAppBar

import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.data.model.MemberOnboardingDraft
import `in`.gym.trak.studio.viewmodel.auth.AuthScreenModel
import `in`.gym.trak.studio.viewmodel.member.MemberOnboardingValidation

data class OnboardingScreen(
    val isOwner: Boolean,
    val tempToken: String = "",
    val isSwitchingFromOwner: Boolean = false,
    val initialName: String? = null
) : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { AuthScreenModel() }

        LoadingScreenHandler(screenModel = screenModel) {
            OnboardingContent(
                isOwner = isOwner,
                tempToken = tempToken,
                initialName = initialName,
                onOnboardingComplete = { gymName, ownerName ->
                    if (isOwner) {
                        screenModel.setGym(tempToken, gymName ?: "", ownerName ?: "") {
                            navigator.replaceAll(OwnerDashboardScreen())
                        }
                    }
                },
                onMemberProfileBasicsComplete = { draft ->
                    if (isSwitchingFromOwner) {
                        screenModel.switchToMember(
                            name = draft.fullName,
                            age = draft.ageYears ?: 0,
                            gender = draft.genderLabel,
                            height = draft.heightCm?.toInt() ?: 0,
                            weight = draft.weightKg?.toInt() ?: 0,
                            onSuccess = { role ->
                                if (role.equals("member", ignoreCase = true)) {
                                    navigator.replaceAll(MemberDashboardScreen())
                                } else {
                                    navigator.replaceAll(OwnerDashboardScreen())
                                }
                            }
                        )
                    } else {
                        navigator.push(ActivityLevelScreen(draft))
                    }
                }
            )
        }
    }
}

@Composable
@Preview
fun OnboardingContentPreview() {
    OnboardingContent(
        isOwner = false,
        tempToken = "",
        onOnboardingComplete = { _, _ -> },
        onMemberProfileBasicsComplete = {}
    )
}

@Composable
fun OnboardingContent(
    isOwner: Boolean,
    tempToken: String = "",
    initialName: String? = null,
    onOnboardingComplete: (gymName: String?, ownerName: String?) -> Unit,
    onMemberProfileBasicsComplete: (MemberOnboardingDraft) -> Unit = {}
) {
    if (isOwner) {
        OwnerOnboardingContent { g, o -> onOnboardingComplete(g, o) }
    } else {
        MemberOnboardingContent(
            tempToken = tempToken,
            initialName = initialName,
            onContinue = onMemberProfileBasicsComplete
        )
    }
}

@Composable
fun OwnerOnboardingContent(onOnboardingComplete: (String, String) -> Unit) {
    var gymName by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize().background(Color.White)) {
        // Background from OTP design reference
        Image(
            painter = painterResource(Res.drawable.login_bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        val focusManager = LocalFocusManager.current
        
        AppScrollableScreen(
            scrollState = scrollState,
            contentPadding = PaddingValues(24.dp),
            dismissKeyboardOnTap = true,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            AuthHeader(
                title = "Set Your Gym",
                subtitle = "Let's Get Started With The Basics",
                horizontalAlignment = Alignment.CenterHorizontally,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            // Gym Name Field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = buildAnnotatedString {
                        append("Gym Name ")
                        withStyle(SpanStyle(color = Color.Red)) {
                            append("*")
                        }
                    },
                    style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Color.Black),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                CustomTextField(
                    value = gymName,
                    onValueChange = { gymName = it },
                    placeholder = "Enter Your Gym Name",
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.ic_driving_licence),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.Unspecified
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Full Name Field
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = buildAnnotatedString {
                        append("Full Name ")
                        withStyle(SpanStyle(color = Color.Red)) {
                            append("*")
                        }
                    },
                    style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Color.Black),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                CustomTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Enter Your Full Name",
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.userIcon),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.Unspecified
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    autofillType = AutofillType.PersonFullName
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
            
            CommonButton(
                onClick = { onOnboardingComplete(gymName, name) },
                text = "Continue",
                enabled = gymName.isNotBlank() && name.isNotBlank()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberOnboardingContent(
    tempToken: String,
    initialName: String? = null,
    onContinue: (MemberOnboardingDraft) -> Unit
) {
    val navigator = LocalNavigator.currentOrThrow
    var name by remember { mutableStateOf(initialName ?: "") }
    var age by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var heightUnit by remember { mutableStateOf("Cm") }
    var weightUnit by remember { mutableStateOf("Kg") }
    var fieldErrors by remember { mutableStateOf(MemberOnboardingValidation.FieldErrors()) }

    val heightUnits = listOf("Cm", "In")
    val weightUnits = listOf("Kg", "Lbs")

    Scaffold(
        topBar = {

            GymAppBar(
                title = "",
                onBackClick = {navigator.pop()}
            )
        },
        bottomBar = {
            Box(modifier = Modifier.padding(24.dp)
                .navigationBarsPadding()) {
                CommonButton(
                    onClick = {
                        val errors = MemberOnboardingValidation.validateBasics(
                            fullName = name,
                            ageText = age,
                            genderLabel = gender,
                            heightRaw = height,
                            heightUnit = heightUnit,
                            weightRaw = weight,
                            weightUnit = weightUnit
                        )
                        if (errors.hasErrors) {
                            fieldErrors = errors
                            return@CommonButton
                        }
                        fieldErrors = MemberOnboardingValidation.FieldErrors()
                        val (hCm, wKg) = MemberOnboardingValidation.parsePhysicalMetrics(
                            height, heightUnit, weight, weightUnit
                        )
                        onContinue(
                            MemberOnboardingDraft(
                                tempToken = tempToken,
                                fullName = name.trim(),
                                ageYears = age.trim().toIntOrNull(),
                                genderLabel = gender,
                                heightCm = hCm,
                                weightKg = wKg
                            )
                        )
                    },
                    text = "Next",
                    enabled = name.isNotBlank() && age.isNotBlank() && height.isNotBlank() && weight.isNotBlank()
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

            val focusManager = LocalFocusManager.current
            
            AppScrollableScreen(
                contentPadding = PaddingValues(horizontal = 24.dp),
                dismissKeyboardOnTap = true,
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(modifier = Modifier.height(100.dp))

                AuthHeader(
                    title = "Let's Get Started !",
                    subtitle = "Tell us about yourself"
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Name
                SectionLabel("Name")
                Spacer(modifier = Modifier.height(8.dp))
                CommonTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        fieldErrors = fieldErrors.copy(fullName = null)
                    },
                    placeholder = "Enter Your Username",
                    leadingIconDrawable = Res.drawable.userIcon,
                    autofillType = AutofillType.PersonFullName
                )
                fieldErrors.fullName?.let {
                    Text(it, color = Color(0xFFB91C1C), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Age
                SectionLabel("Age")
                Spacer(modifier = Modifier.height(8.dp))
                CommonTextField(
                    value = age,
                    onValueChange = {
                        age = it
                        fieldErrors = fieldErrors.copy(age = null)
                    },
                    placeholder = "Enter your age",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIconDrawable = Res.drawable.userIcon
                )
                fieldErrors.age?.let {
                    Text(it, color = Color(0xFFB91C1C), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Gender
                SectionLabel("Gender")
                Spacer(modifier = Modifier.height(8.dp))
                GenderSelector(
                    selectedGender = gender,
                    onGenderSelected = {
                        gender = it
                        fieldErrors = fieldErrors.copy(gender = null)
                    }
                )
                fieldErrors.gender?.let {
                    Text(it, color = Color(0xFFB91C1C), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Height
                SectionLabel("Height")
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.weight(2.5f)) {
                        CommonTextField(
                            value = height,
                            onValueChange = {
                                height = it
                                fieldErrors = fieldErrors.copy(height = null)
                            },
                            placeholder = "Height",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CommonDropdown(
                            options = heightUnits,
                            selectedOption = heightUnit,
                            onOptionSelected = { heightUnit = it },
                            placeholder = "Unit"
                        )
                    }
                }
                fieldErrors.height?.let {
                    Text(it, color = Color(0xFFB91C1C), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Weight
                SectionLabel("Weight")
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(modifier = Modifier.weight(2.5f)) {
                        CommonTextField(
                            value = weight,
                            onValueChange = {
                                weight = it
                                fieldErrors = fieldErrors.copy(weight = null)
                            },
                            placeholder = "Weight",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CommonDropdown(
                            options = weightUnits,
                            selectedOption = weightUnit,
                            onOptionSelected = { weightUnit = it },
                            placeholder = "Unit"
                        )
                    }
                }
                fieldErrors.weight?.let {
                    Text(it, color = Color(0xFFB91C1C), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
