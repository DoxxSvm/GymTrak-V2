package `in`.gym.trak.studio.features.location

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import `in`.gym.trak.studio.components.LocationResult
import `in`.gym.trak.studio.components.SearchBar
import `in`.gym.trak.studio.theme.*
import gym.composeapp.generated.resources.*
import `in`.gym.trak.studio.components.SearchResult
import `in`.gym.trak.studio.components.rememberLocationProvider
import `in`.gym.trak.studio.components.rememberLocationSearchService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * Screen for selecting a gym location using Google Maps integration.
 * Includes a fixed marker at the center. As the user drags the map, 
 * the location updates based on the center.
 */
class SelectGymLocationScreen(
    private val gymAddress: String = "",
    val onLocationSelected: (PickedGymLocation) -> Unit
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        var searchQuery by remember { mutableStateOf("") }
        
        // Dynamic location state
        var centerAddressTitle by remember { mutableStateOf("Elite Fitness Downtown") }
        var centerAddressSubtitle by remember { mutableStateOf("Bhaktinagar main road.Rajkot") }

        // Search logic
        val searchService = rememberLocationSearchService()
        var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
        val locationProvider = rememberLocationProvider()
        
        LaunchedEffect(searchQuery) {
            if (searchQuery.length >= 2) {
                delay(350)
                searchResults = searchService.search(searchQuery)
            } else {
                searchResults = emptyList()
            }
        }
        
        // In a real implementation with real Google Maps:
        var centerLocation by remember { mutableStateOf(Pair(22.3039, 70.8022)) }
        var targetLocation by remember { mutableStateOf<Pair<Double, Double>?>(null) }

        // [GoogleMap] may only report the center when the camera stops moving; keep this in sync
        // with [targetLocation] so "Confirm" has correct lat/lng even before the animation finishes.
        LaunchedEffect(targetLocation) {
            targetLocation?.let { centerLocation = it }
        }

        LaunchedEffect(gymAddress.trim()) {
            val trimmed = gymAddress.trim()
            if (trimmed.isBlank()) {
                when (val result = locationProvider.getCurrentLocation()) {
                    is LocationResult.Success -> targetLocation = Pair(result.latitude, result.longitude)
                    else -> Unit
                }
                return@LaunchedEffect
            }

            val result = searchService.search(trimmed).firstOrNull()
            if (result != null) {
                val coords = searchService.resolveCoordinates(result)
                if (coords != null) {
                    targetLocation = coords
                    centerLocation = coords
                }
                centerAddressTitle = result.name
                centerAddressSubtitle = result.address
            } else {
                when (val locationResult = locationProvider.getCurrentLocation()) {
                    is LocationResult.Success -> targetLocation = Pair(locationResult.latitude, locationResult.longitude)
                    else -> Unit
                }
            }
        }
        
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                Column(
                    modifier = Modifier.background(White).padding(bottom = 8.dp)
                ) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                "Select Gym Location",
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
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        placeholder = "Search Location",
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                // Real Google Map Integration
               GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    targetLocation = targetLocation,
                    zoom = 17f,
                    tilt = 35f,
                    bearing = 0f,
                    onLocationUpdate = { lat, lng ->
                        centerLocation = Pair(lat, lng)
                        // Real reverse geocoding to update address
                        scope.launch {
                            val fullAddress = searchService.reverseGeocode(lat, lng)
                            if (fullAddress != null) {
                                // Try to split address: first part as title, rest as subtitle
                                val parts = fullAddress.split(",")
                                if (parts.isNotEmpty()) {
                                    centerAddressTitle = parts[0].trim()
                                    centerAddressSubtitle = if (parts.size > 1) {
                                        parts.drop(1).joinToString(",").trim()
                                    } else {
                                        ""
                                    }
                                }
                            }
                        }
                    }
                )

                // Search Results List (Over Map)
                if (searchResults.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp, start = 24.dp, end = 24.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(White)
                            .border(1.dp, Gray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    ) {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.heightIn(max = 250.dp)
                        ) {
                            items(searchResults.size) { index ->
                                val result = searchResults[index]
                               ListItem(
                                    headlineContent = { 
                                        Text(
                                            result.name, 
                                            style = AppTextTheme.bold.copy(fontSize = 14.sp, color = Black)
                                        ) 
                                    },
                                    supportingContent = { 
                                        Text(
                                            result.address, 
                                            style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
                                        ) 
                                    },
                                    modifier = Modifier.clickable {
                                        scope.launch {
                                            val coords = searchService.resolveCoordinates(result)
                                            if (coords != null) {
                                                targetLocation = coords
                                                centerLocation = coords
                                            }
                                            // Combined into a single line when selected
                                            centerAddressTitle = result.name
                                            centerAddressSubtitle = result.address
                                            searchQuery = ""
                                            searchResults = emptyList()
                                        }
                                    }
                                )
                                if (index < searchResults.size - 1) {
                                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Gray.copy(alpha = 0.05f))
                                }
                            }
                        }
                    }
                }

                // Fixed Marker at the Center (Remains on top of the map)
                Column(
                   horizontalAlignment = Alignment.CenterHorizontally,
                   modifier = Modifier.offset(y = (-30).dp) // Adjust for marker height offset
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(PrimaryColor, CircleShape)
                            .border(4.dp, White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_location),
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    // Marker pin tip simulation
                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(8.dp)
                            .background(PrimaryColor)
                    )
                    // Marker shadow
                    Box(
                        modifier = Modifier
                            .size(12.dp, 4.dp)
                            .background(Color.Black.copy(alpha = 0.1f), CircleShape)
                    )
                }

                // Use Current Location Button (Floating above map bottom card)
                Surface(
                    onClick = { 
                        scope.launch {
                            when (val result = locationProvider.getCurrentLocation()) {
                                is LocationResult.Success -> {
                                    val loc = Pair(result.latitude, result.longitude)
                                    targetLocation = loc
                                    centerLocation = loc
                                }
                                LocationResult.PermissionDenied -> {
                                    snackbarHostState.showSnackbar("Location permission denied. Please allow location access.")
                                }
                                LocationResult.ServiceDisabled -> {
                                    snackbarHostState.showSnackbar("Location services are off. Please enable GPS.")
                                }
                                LocationResult.Unavailable -> {
                                    snackbarHostState.showSnackbar("Unable to fetch current location. Try again outdoors or on a real device.")
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(100.dp),
                    color = PrimaryColor,
                    modifier = Modifier
                        .padding(bottom = 220.dp) // Offset above the bottom sheet
                        .align(Alignment.BottomCenter)
                        .height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Use Current Location",
                            style = AppTextTheme.bold.copy(fontSize = 15.sp, color = White)
                        )
                    }
                }

                // Bottom location details card
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(White)
                        .padding(24.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Gym Icon
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(Color(0xFFF3FBF9), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_dumble),
                                    contentDescription = null,
                                    tint = PrimaryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    centerAddressTitle,
                                    style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black),
                                    maxLines = 1
                                )
                                Text(
                                    centerAddressSubtitle,
                                    style = AppTextTheme.medium.copy(fontSize = 13.sp, color = Gray),
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = { /* Edit address logic */ }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Black)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        CommonButton(
                            text = "Confirm Location",
                            onClick = {
                                val address =
                                    if (centerAddressSubtitle.isEmpty()) centerAddressTitle
                                    else "${centerAddressTitle}, ${centerAddressSubtitle}"
                                onLocationSelected(
                                    PickedGymLocation(
                                        address = address,
                                        latitude = centerLocation.first,
                                        longitude = centerLocation.second
                                    )
                                )
                                navigator.pop()
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}
