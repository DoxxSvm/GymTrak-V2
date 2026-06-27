package `in`.gym.trak.studio.features.management

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.data.model.EditableWhatsAppAutomationTemplate
import `in`.gym.trak.studio.data.model.UpdateWhatsAppAutomationRequest
import `in`.gym.trak.studio.data.model.toEditable
import `in`.gym.trak.studio.data.model.toUpdateItem
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel

private const val MAX_WELCOME_MESSAGE_LENGTH = 4000

/**
 * WhatsApp automation settings backed by GET/PUT `/whatsapp/automation`.
 */
class WhatsappAutomationScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val automation by screenModel.whatsAppAutomation.collectAsState()
        val isSaving by screenModel.whatsAppAutomationSaving.collectAsState()

        val templates = remember { mutableStateListOf<EditableWhatsAppAutomationTemplate>() }
        var appliedPayloadKey by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            screenModel.loadWhatsAppAutomation(showGlobalLoader = true)
        }

        LaunchedEffect(automation) {
            val payload = automation ?: return@LaunchedEffect
            val key = payload.templates.joinToString("|") { "${it.id}:${it.enabled}:${it.message.orEmpty()}" }
            if (appliedPayloadKey == key) return@LaunchedEffect
            appliedPayloadKey = key
            templates.clear()
            templates.addAll(payload.templates.map { it.toEditable() })
        }

        val screenTitle = automation?.screenTitle?.takeIf { it.isNotBlank() } ?: "Message Templates"
        val screenDescription = automation?.screenDescription?.takeIf { it.isNotBlank() }
            ?: "Configure automated WhatsApp messages for your members.\nThese messages will be sent automatically based on the triggers below."

        LoadingScreenHandler(screenModel = screenModel) {
            Scaffold(
                topBar = {
                    GymAppBar(
                        title = "Whatsapp Automation",
                        onBackClick = { navigator?.pop() },
                    )
                },
                containerColor = Color.Transparent,
            ) { padding ->
                AppScrollableScreen(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = screenTitle,
                        style = AppTextTheme.semiBold.copy(fontSize = 24.sp),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = screenDescription,
                        style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    templates.forEachIndexed { index, template ->
                        AutomationTriggerCard(
                            title = template.title,
                            description = template.description,
                            isEnabled = template.enabled,
                            onToggle = { enabled ->
                                templates[index] = template.copy(enabled = enabled)
                            },
                            showInput = template.supportsCustomMessage,
                            message = template.message,
                            onMessageChange = { message ->
                                templates[index] = template.copy(message = message.take(MAX_WELCOME_MESSAGE_LENGTH))
                            },
                            defaultMessage = template.defaultMessage,
                        )
                        if (index < templates.lastIndex) {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    CommonButton(
                        text = if (isSaving) "Saving..." else "Save Changes",
                        enabled = templates.isNotEmpty() && !isSaving,
                        onClick = {
                            if (templates.isEmpty()) return@CommonButton
                            val tooLong = templates.any {
                                it.supportsCustomMessage && it.message.length > MAX_WELCOME_MESSAGE_LENGTH
                            }
                            if (tooLong) {
                                screenModel.showError("Welcome message must be at most $MAX_WELCOME_MESSAGE_LENGTH characters.")
                                return@CommonButton
                            }
                            screenModel.saveWhatsAppAutomation(
                                request = UpdateWhatsAppAutomationRequest(
                                    templates = templates.map { it.toUpdateItem() },
                                ),
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }
}

@Composable
fun AutomationTriggerCard(
    title: String,
    description: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    showInput: Boolean = false,
    message: String = "",
    onMessageChange: (String) -> Unit = {},
    defaultMessage: String? = null,
) {
    CommonCard(
        content = {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = title, style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Black))
                        Text(text = description, style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray))
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = White,
                            checkedTrackColor = PrimaryColor,
                            uncheckedThumbColor = White,
                            uncheckedTrackColor = Color(0xFFE2E8F0),
                        ),
                    )
                }

                if (showInput && isEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    if (!defaultMessage.isNullOrBlank()) {
                        Text(
                            text = "Default: $defaultMessage",
                            style = AppTextTheme.regular.copy(fontSize = 11.sp, color = Gray),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    CommonTextField(
                        value = message,
                        onValueChange = onMessageChange,
                        placeholder = "Enter your welcome message",
                        modifier = Modifier.fillMaxWidth().height(120.dp),
                        borderRadius = 10.dp,
                        isMultiline = true,
                        singleLine = false,

                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    )
                }
            }
        },
    )
}
