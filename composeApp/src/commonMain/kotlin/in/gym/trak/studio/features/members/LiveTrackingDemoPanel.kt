package `in`.gym.trak.studio.features.members

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.theme.*
import `in`.gym.trak.studio.tracking.LiveTrackingController

@Composable
fun LiveTrackingDemoPanel(
    memberName: String = "Member",
    planName: String = "Session"
) {
    // One controller per composition — auto-disposed on leave
    val tracker = remember { LiveTrackingController() }
    var isTracking by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            tracker.endSession()
            tracker.dispose()
        }
    }

    CommonCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🏋️ Live Session Tracking",
                style = AppTextTheme.semiBold.copy(fontSize = 16.sp)
            )
            Text(
                text = if (isTracking)
                    "Notification active on lock screen. Updates every second."
                else
                    "Tap Start to push a live notification to the lock screen.",
                style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Gray)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // ── Start ──────────────────────────────────────────────────
                Button(
                    onClick = {
                        isTracking = true
                        tracker.simulateProgress(
                            planName   = planName,
                            memberName = memberName,
                            durationSeconds = 60
                        )
                    },
                    enabled = !isTracking,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Start")
                }

                // ── Stop ───────────────────────────────────────────────────
                Button(
                    onClick = {
                        isTracking = false
                        tracker.endSession()
                    },
                    enabled = isTracking,
                    colors = ButtonDefaults.buttonColors(containerColor = RedColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Stop")
                }
            }

            // Status indicator
            if (isTracking) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .let {
                                it // pulse effect would go here
                            }
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = PrimaryGreenColor,
                            modifier = Modifier.size(8.dp)
                        ) {}
                    }
                    Text(
                        "Live — check your lock screen",
                        style = AppTextTheme.medium.copy(fontSize = 12.sp, color = PrimaryGreenColor)
                    )
                }
            }
        }
    }
}
