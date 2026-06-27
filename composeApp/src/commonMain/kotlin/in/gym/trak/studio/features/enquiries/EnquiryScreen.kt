package `in`.gym.trak.studio.features.enquiries

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.rememberAsyncImagePainter
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.components.SearchBar
import `in`.gym.trak.studio.data.model.EnquiryDTO
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import `in`.gym.trak.studio.theme.*
import gym.composeapp.generated.resources.*
import `in`.gym.trak.studio.features.members.MemberDetailScreen
import org.jetbrains.compose.resources.painterResource
import `in`.gym.trak.studio.utils.DateUtils


class EnquiryScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = remember { OwnerDashboardScreenModel() }
        val enquiries by screenModel.enquiries.collectAsState()
        val stats by screenModel.enquiryStats.collectAsState()
        val searchQuery by screenModel.enquirySearchQuery.collectAsState()
        val selectedStatus by screenModel.selectedEnquiryStatus.collectAsState()


        LaunchedEffect(Unit) {
            if (enquiries.isEmpty()) {
                screenModel.loadEnquiries()
            }
        }

        LoadingScreenHandler(screenModel = screenModel) {
            Scaffold(
                topBar = {
                    GymAppBar(
                        title = "Enquiries",
                        onBackClick = { navigator?.pop() }
                    )
                },
                containerColor = Color.Transparent
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp)
                ) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { screenModel.onEnquirySearch(it) },
                        placeholder = "Search Enquiries...",
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    Text(
                        text = "Enquiry Overview",
                        style = AppTextTheme.bold.copy(fontSize = 18.sp, color = Black),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        EnquiryStatCard(
                            title = "Total Enquiry",
                            count = stats?.totalEnquiry ?: 0,
                            modifier = Modifier.weight(1f),
                            isSelected = selectedStatus == null,
                            onClick = { screenModel.onEnquiryStatusFilter(null) }
                        )
                        EnquiryStatCard(
                            title = "Converted",
                            count = stats?.converted ?: 0,
                            modifier = Modifier.weight(1f),
                            isSelected = selectedStatus == "CONVERTED",
                            onClick = { screenModel.onEnquiryStatusFilter("CONVERTED") }
                        )
                        EnquiryStatCard(
                            title = "Pending",
                            count = stats?.pending ?: 0,
                            modifier = Modifier.weight(1f),
                            isSelected = selectedStatus == "OPEN",
                            onClick = { screenModel.onEnquiryStatusFilter("OPEN") }
                        )

                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    CommonButton(
                        text = "Add New Enquiry",
                        onClick = { navigator?.push(AddEnquiryScreen(onResult = { screenModel.loadEnquiries() })) },
                        modifier = Modifier.fillMaxWidth(),
                        leftIcon =  painterResource(Res.drawable.ic_add)
                    )


                    Spacer(modifier = Modifier.height(24.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(enquiries) { enquiry ->
                            EnquiryListItem(
                                enquiry = enquiry,
                                onMarkAsLost = {
                                    screenModel.updateEnquiryStatus(enquiry.id, "LOST") {
                                        screenModel.loadEnquiries()
                                    }
                                },
                                onConvertToMember = {
                                    screenModel.convertEnquiry(enquiry.id) {
                                        screenModel.loadEnquiries()
                                    }
                                },
                                onClick = {
                                    if (enquiry.status == "CONVERTED" && !enquiry.convertedGymUserId.isNullOrEmpty()) {
                                        navigator?.push(MemberDetailScreen(memberId = enquiry.convertedGymUserId))
                                    } else {
                                        navigator?.push(AddEnquiryScreen(enquiry = enquiry, onResult = { screenModel.loadEnquiries() }))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EnquiryStatCard(
    title: String,
    count: Int,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = modifier.height(110.dp).clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) PrimaryColor else White,
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE8ECF4)) else null,
        shadowElevation = if (isSelected) 4.dp else 0.dp
    ) {

        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$count",
                style = AppTextTheme.bold.copy(
                    fontSize = 24.sp,
                    color = if (isSelected) White else PrimaryColor
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = AppTextTheme.medium.copy(
                    fontSize = 12.sp,
                    color = if (isSelected) White.copy(alpha = 0.7f) else Gray
                )
            )
        }
    }
}

@Composable
fun EnquiryListItem(
    enquiry: EnquiryDTO,
    onMarkAsLost: () -> Unit,
    onConvertToMember: () -> Unit,
    onClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    CommonCard(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {

        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Profile Image
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFF7E6))
                ) {
                    val painter = if (!enquiry.photoUrl.isNullOrEmpty())
                        rememberAsyncImagePainter(enquiry.photoUrl)
                    else painterResource(Res.drawable.gym_boy)
                    
                    Image(
                        painter = painter,
                        contentDescription = "Enquiry Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        enquiry.name ?: "${enquiry.firstName} ${enquiry.lastName}".trim(),
                        style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            DateUtils.formatEnquiryDate(enquiry.enquiryDate),
                            style = AppTextTheme.medium.copy(fontSize = 13.sp, color = Gray)
                        )
                        val timeStr = DateUtils.formatEnquiryTime(enquiry.enquiryDate)
                        if (timeStr.isNotEmpty()) {
                            Text(
                                timeStr,
                                style = AppTextTheme.medium.copy(fontSize = 13.sp, color = Gray)
                            )
                        }
                    }

                }

                IconButton(
                    onClick = {
                        enquiry.phone?.takeIf { it.isNotBlank() }?.let { phone ->
                            val cleanPhone = phone.filter { it.isDigit() }
                            if (cleanPhone.isNotEmpty()) {
                                uriHandler.openUri("https://wa.me/$cleanPhone")
                            }
                        }
                    },
                    enabled = !enquiry.phone.isNullOrBlank()
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_whatsapp),
                        contentDescription = "WhatsApp",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            if (enquiry.status != "LOST" && enquiry.status != "CONVERTED") {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onMarkAsLost,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(100.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, RedColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RedColor)
                    ) {
                        Text(
                            "Mark As Lost",
                            style = AppTextTheme.bold.copy(fontSize = 13.sp)
                        )
                    }

                    Button(
                        onClick = onConvertToMember,
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(100.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                    ) {
                        Text(
                            "Convert to Member",
                            style = AppTextTheme.bold.copy(fontSize = 13.sp, color = White)
                        )
                    }
                }
            }
        }
    }
}
