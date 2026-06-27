package `in`.gym.trak.studio.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import `in`.gym.trak.studio.theme.*

@Composable
fun ConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    isDangerAction: Boolean = false
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .background(White, shape = RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = AppTextTheme.bold.copy(fontSize = 20.sp),
                    textAlign = TextAlign.Center,
                    color = DarkBlack
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = message,
                    style = AppTextTheme.medium.copy(fontSize = 14.sp),
                    textAlign = TextAlign.Center,
                    color = Gray
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CommonOutlineButton(
                        text = dismissText,
                        onClick = onDismissRequest,
                        modifier = Modifier.weight(1f),
                        borderColor = GrayBorderColor,
                        textColor = DarkBlack
                    )
                    
                    CommonButton(
                        text = confirmText,
                        onClick = {
                            onConfirm()
                            onDismissRequest()
                        },
                        modifier = Modifier.weight(1f),
                        color = if (isDangerAction) RedColor else PrimaryColor
                    )
                }
            }
        }
    }
}
