package `in`.gym.trak.studio.features.location

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.GoogleMap
import `in`.gym.trak.studio.theme.*
import gym.composeapp.generated.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

class ConfirmLocationScreen(
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val onConfirm: (PickedGymLocation) -> Unit
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        
        // Split address for bold title + subtitle display
        val parts = address.split(",")
        val title = parts.getOrNull(0)?.trim() ?: address
        val subtitle = if (parts.size > 1) parts.drop(1).joinToString(",").trim() else ""
        
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Confirm Location",
                            style = AppTextTheme.bold.copy(fontSize = 18.sp)
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            AppScrollableScreen(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFE5E7EB)),
                    contentAlignment = Alignment.Center
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        targetLocation = Pair(latitude, longitude),
                        zoom = 17f,
                        onLocationUpdate = { _, _ -> }
                    )
                    
                    // Fixed marker overlay
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(PrimaryColor, CircleShape)
                            .border(3.dp, White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_location),
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Location Details",
                        style = AppTextTheme.bold.copy(fontSize = 18.sp, color = Black)
                    )
                    Text(
                        "Is This The Correct Site For Your New Facility?",
                        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Detailed Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
                    color = White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(PrimaryColor, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_dumble),
                                    contentDescription = null,
                                    tint = White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    title,
                                    style = AppTextTheme.bold.copy(fontSize = 18.sp, color = Black)
                                )
                                if (subtitle.isNotEmpty()) {
                                    Text(
                                        subtitle,
                                        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        AddressItem(
                            icon = Res.drawable.ic_full_address,
                            label = "Full Address",
                            value = address
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth()) {
                            AddressItem(
                                icon = Res.drawable.ic_city, // or some building icon
                                label = "City",
                                value = "Rajkot",
                                modifier = Modifier.weight(1f)
                            )
                            AddressItem(
                                icon = Res.drawable.ic_state, // Placeholder for state icon
                                label = "State",
                                value = "Gujarat",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        AddressItem(
                           icon = Res.drawable.ic_pincode,
                           label = "Pincode",
                           value = "360001"
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(40.dp))
                
                CommonButton(
                    text = "Use This Location",
                    onClick = {
                        onConfirm(PickedGymLocation(address = address, latitude = latitude, longitude = longitude))
                        navigator.pop() // Pop ConfirmLocationScreen
//                        navigator.pop() // Pop SelectGymLocationScreen
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun AddressItem(
    icon: DrawableResource,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Black,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                label,
                style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray)
            )
            Text(
                value,
                style = AppTextTheme.bold.copy(fontSize = 15.sp, color = Black)
            )
        }
    }
}
