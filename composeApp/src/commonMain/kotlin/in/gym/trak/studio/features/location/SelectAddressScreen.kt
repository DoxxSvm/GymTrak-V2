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

private const val DEFAULT_LATITUDE = 22.3039
private const val DEFAULT_LONGITUDE = 70.8022

class SelectAddressScreen(
    private val initialAddress: String = "",
    private val onAddressSelected: (PickedGymLocation) -> Unit
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val snackbarHostState = remember { SnackbarHostState() }
        var searchQuery by remember { mutableStateOf("") }
        
        // Dynamic location state
        var centerAddress by remember { mutableStateOf("Select Location") }
        var centerGymName by remember { mutableStateOf("Map Location") }

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
        
        var targetLocation by remember { mutableStateOf<Pair<Double, Double>?>(Pair(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)) }
        /** Map crosshair / latest camera center — returned with the confirmed address. */
        var mapCenterLatLng by remember { mutableStateOf(Pair(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)) }

        LaunchedEffect(targetLocation) {
            targetLocation?.let { mapCenterLatLng = it }
        }

        LaunchedEffect(initialAddress.trim()) {
            val trimmed = initialAddress.trim()
            if (trimmed.isBlank()) {
                targetLocation = Pair(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
                return@LaunchedEffect
            }

            val result = searchService.search(trimmed).firstOrNull()
            if (result != null) {
                val coords = searchService.resolveCoordinates(result)
                if (coords != null) {
                    targetLocation = coords
                    mapCenterLatLng = coords
                }
                centerAddress = result.address
                centerGymName = result.name
            } else {
                targetLocation = Pair(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
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
                                "Select Address",
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
                        placeholder = "Search for a place",
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
                        mapCenterLatLng = Pair(lat, lng)
                        scope.launch {
                            val address = searchService.reverseGeocode(lat, lng)
                            if (address != null) {
                                centerAddress = address
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
                                androidx.compose.material3.ListItem(
                                    headlineContent = { Text(result.name, style = AppTextTheme.medium.copy(fontSize = 14.sp)) },
                                    supportingContent = { Text(result.address, style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)) },
                                    modifier = Modifier.clickable {
                                        scope.launch {
                                            val coords = searchService.resolveCoordinates(result)
                                            if (coords != null) {
                                                targetLocation = coords
                                                mapCenterLatLng = coords
                                            }
                                            centerAddress = result.address
                                            centerGymName = result.name
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

                // Fixed Marker
                Column(
                   horizontalAlignment = Alignment.CenterHorizontally,
                   modifier = Modifier.offset(y = (-30).dp)
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
                    Box(modifier = Modifier.width(2.dp).height(8.dp).background(PrimaryColor))
                }

                // Use Current Location
                Surface(
                    onClick = { 
                        scope.launch {
                            when (val result = locationProvider.getCurrentLocation()) {
                                is LocationResult.Success -> {
                                    val loc = Pair(result.latitude, result.longitude)
                                    mapCenterLatLng = loc
                                    // Force camera recenter even if the same coordinates are returned.
                                    targetLocation = null
                                    delay(50)
                                    targetLocation = loc
                                    val address = searchService.reverseGeocode(loc.first, loc.second)
                                    if (!address.isNullOrBlank()) {
                                        centerAddress = address
                                        centerGymName = "Current Location"
                                    }
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
                        .padding(bottom = 220.dp)
                        .align(Alignment.BottomCenter)
                        .height(48.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.MyLocation, contentDescription = null, tint = White, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Current Location", style = AppTextTheme.bold.copy(fontSize = 15.sp, color = White))
                    }
                }

                // Bottom card
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
                            Box(
                                modifier = Modifier.size(48.dp).background(Color(0xFFF3FBF9), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(painter = painterResource(Res.drawable.ic_location), contentDescription = null, tint = PrimaryColor, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Selected Address", style = AppTextTheme.bold.copy(fontSize = 14.sp, color = Gray))
                                Text(centerAddress, style = AppTextTheme.medium.copy(fontSize = 16.sp, color = Black), maxLines = 2)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        CommonButton(
                            text = "Confirm Address",
                            onClick = {
                                onAddressSelected(
                                    PickedGymLocation(
                                        address = centerAddress,
                                        latitude = mapCenterLatLng.first,
                                        longitude = mapCenterLatLng.second
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
