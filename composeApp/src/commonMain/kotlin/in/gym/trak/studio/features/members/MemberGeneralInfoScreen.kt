package `in`.gym.trak.studio.features.members

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import `in`.gym.trak.studio.components.AppScrollableScreen
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonDropdown
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.SectionLabel
import `in`.gym.trak.studio.theme.*
import gym.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource

/**
 * Screen for managing a member's general information (Height, Weight, Phone, etc.)
 * DESIGN: Based on ProfilePage.kt (Centered Avatar and clean layout)
 */
class MemberGeneralInfoScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        
        var phone by remember { mutableStateOf("+91 56555 24444") }
        var height by remember { mutableStateOf("175") }
        var weight by remember { mutableStateOf("78") }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "General Information",
                            style = AppTextTheme.bold.copy(fontSize = 18.sp, color = DarkBlue)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator?.pop() }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            AppScrollableScreen(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Profile Avatar (Matching ProfilePage.kt)
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(White)
                        .border(2.dp, PrimaryColor.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(Res.drawable.gym_boy),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(92.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "John Den",
                    style = AppTextTheme.bold.copy(fontSize = 20.sp),
                    color = Black
                )
                
                Spacer(modifier = Modifier.height(32.dp))

                // Information Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        SectionLabel("Phone Number")
                        Spacer(modifier = Modifier.height(8.dp))
                        CommonTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            placeholder = "Phone",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                SectionLabel("Height (cm)")
                                Spacer(modifier = Modifier.height(8.dp))
                                CommonTextField(
                                    value = height,
                                    onValueChange = { height = it },
                                    placeholder = "Height",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                SectionLabel("Weight (kg)")
                                Spacer(modifier = Modifier.height(8.dp))
                                CommonTextField(
                                    value = weight,
                                    onValueChange = { weight = it },
                                    placeholder = "Weight",
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        SectionLabel("Gender")
                        Spacer(modifier = Modifier.height(8.dp))
                        CommonDropdown(
                            options = listOf("Male", "Female", "Other"),
                            selectedOption = "Male",
                            onOptionSelected = { }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))

                CommonButton(
                    onClick = { navigator?.pop() },
                    text = "Save Changes",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
