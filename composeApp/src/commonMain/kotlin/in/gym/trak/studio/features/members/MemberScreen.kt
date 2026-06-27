package `in`.gym.trak.studio.features.members

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.navigator.LocalNavigator
import coil3.compose.rememberAsyncImagePainter
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.SearchBar
import `in`.gym.trak.studio.data.model.MemberDTO
import `in`.gym.trak.studio.data.model.MemberStats
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.PrimaryDarkColor
import `in`.gym.trak.studio.theme.White
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.gym_boy
import gym.composeapp.generated.resources.ic_active
import gym.composeapp.generated.resources.ic_active_unselected
import gym.composeapp.generated.resources.ic_all_member_selected
import gym.composeapp.generated.resources.ic_all_member_unselected
import gym.composeapp.generated.resources.ic_inactive
import gym.composeapp.generated.resources.ic_king
import gym.composeapp.generated.resources.ic_red_king
import gym.composeapp.generated.resources.ic_warning
import gym.composeapp.generated.resources.ic_warning_selected
import gym.composeapp.generated.resources.ic_warning_unselected
import gym.composeapp.generated.resources.no_member
import `in`.gym.trak.studio.components.LoadingScreenHandler
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun MemberScreen(screenModel: OwnerDashboardScreenModel) {
    val navigator = LocalNavigator.current

    val members by screenModel.members.collectAsState()
    val stats by screenModel.memberStats.collectAsState()
    val searchQuery by screenModel.searchQuery.collectAsState()
    val selectedFilter by screenModel.selectedFilter.collectAsState()
    val isLoading by screenModel.isLoading.collectAsState()
    val isLoadingMore by screenModel.isLoadingMore.collectAsState()
    var isPullRefreshRequested by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Refresh the list every time this screen is opened.
    LaunchedEffect(Unit) {
        screenModel.loadMembers(isRefresh = true)
    }

    LaunchedEffect(isLoading) {
        if (!isLoading) {
            isPullRefreshRequested = false
        }
    }

    // Pagination logic
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo }
            .collectLatest { visibleItems ->
                val lastVisibleItem = visibleItems.lastOrNull()
                if (lastVisibleItem != null && members.isNotEmpty()) {
                    if (lastVisibleItem.index >= members.size - 2) {
                        screenModel.loadMoreMembers()
                    }
                }
            }
    }

    Scaffold(
        floatingActionButton = {
            if (members.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        navigator?.push(
                            AddMemberScreen(
                                screenModel = screenModel,
                                onMemberAdded = { screenModel.loadMembers(isRefresh = true) }
                            )
                        )
                    },
                    containerColor = PrimaryColor,
                    contentColor = White,
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Member"
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Members",
                    style = AppTextTheme.bold.copy(fontSize = 24.sp)
                )
            }

            // Summary Card
            StaggeredEntranceItem(index = 0) {
                SummaryCard(stats)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar
            StaggeredEntranceItem(index = 1) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { screenModel.onSearchQueryChanged(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filters
            StaggeredEntranceItem(index = 2) {
                FilterSection(
                    selectedFilter = selectedFilter,
                    onFilterSelected = { screenModel.onFilterChanged(it) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            PullToRefreshBox(
                isRefreshing = isPullRefreshRequested && isLoading,
                onRefresh = {
                    isPullRefreshRequested = true
                    screenModel.loadMembers(isRefresh = true)
                },
                state = rememberPullToRefreshState(),
                indicator = {},
                modifier = Modifier.fillMaxSize()
            ) {
                // Content
                if (members.isEmpty() && !isLoading) {
                    StaggeredEntranceItem(index = 3) {
                        EmptyMembersView(
                            selectedFilter = selectedFilter,
                            searchQuery = searchQuery,
                            onAddClick = {
                                navigator?.push(
                                    AddMemberScreen(
                                        screenModel = screenModel,
                                        onMemberAdded = { screenModel.loadMembers(isRefresh = true) }
                                    )
                                )
                            }
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(members) { index, member ->
                            StaggeredEntranceItem(index = 3 + index) {
                                Box(modifier = Modifier.clickable {
                                    navigator?.push(
                                        MemberDetailScreen(
                                        memberId = member.id ?: "",
                                        onMemberDeleted = { screenModel.loadMembers(isRefresh = true) }
                                        )
                                    )
                                }) {
                                    MemberListItem(member)
                                }
                            }
                        }
                        if (isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = PrimaryColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                }
            }
        }
    }
}


@Composable
fun SummaryCard(stats: MemberStats?) {
    CommonCard(
        modifier = Modifier.fillMaxWidth(),
        content = {
            Box(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFE7F7F2))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SummaryItem(
                        title = "Active",
                        count = "${stats?.active ?: 0}",
                        icon = Res.drawable.ic_active
                    )
                    SummaryItem(
                        title = "Inactive",
                        count = "${stats?.inactive ?: 0}",
                        icon = Res.drawable.ic_inactive
                    )
                    SummaryItem(
                        title = "Expired",
                        count = "${stats?.expired ?: 0}",
                        icon = Res.drawable.ic_warning
                    )
                    val total =
                        (stats?.active ?: 0) + (stats?.inactive ?: 0) + (stats?.expired ?: 0)
                    SummaryItem(
                        title = "Total Members",
                        count = "$total",
                        icon = Res.drawable.ic_all_member_selected
                    )
                }
            }
        }
    )
}

@Composable
fun SummaryItem(title: String, count: String, icon: DrawableResource) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = count,
            style = AppTextTheme.bold.copy(fontSize = 18.sp),
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = title,
            style = AppTextTheme.regular.copy(fontSize = 11.sp, color = Gray),
            modifier = Modifier.padding(top = 0.dp)
        )
    }
}

@Composable
fun FilterSection(selectedFilter: String, onFilterSelected: (String) -> Unit) {
    val filters = listOf(
        FilterData(
            "All Member",
            Res.drawable.ic_all_member_selected,
            Res.drawable.ic_all_member_unselected
        ),
        FilterData("Active", Res.drawable.ic_active_unselected, Res.drawable.ic_active),
        FilterData("Expired", Res.drawable.ic_warning_unselected, Res.drawable.ic_warning_selected),
        FilterData("Inactive", Res.drawable.ic_inactive, Res.drawable.ic_inactive)
    )

    val listState = rememberLazyListState()

    // Scroll to selected filter when it changes
    LaunchedEffect(selectedFilter) {
        val index = filters.indexOfFirst { it.name == selectedFilter }
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    LazyRow(
        state = listState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(filters) { filter ->
            val isSelected = selectedFilter == filter.name
            FilterChip(
                label = filter.name,
                icon = if (isSelected) filter.selectedIcon else filter.unselectedIcon,
                isSelected = isSelected,
                onClick = { onFilterSelected(filter.name) }
            )
        }
    }
}

data class FilterData(
    val name: String,
    val selectedIcon: DrawableResource,
    val unselectedIcon: DrawableResource
)

@Composable
fun FilterChip(label: String, icon: DrawableResource, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) PrimaryColor else White,
        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFFE0E0E0)),
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = AppTextTheme.medium.copy(
                    fontSize = 14.sp,
                    color = if (isSelected) White else Black
                )
            )
        }
    }
}

@Composable
fun EmptyMembersView(
    selectedFilter: String,
    searchQuery: String,
    onAddClick: () -> Unit
) {
    val isSearch = searchQuery.isNotBlank()
    
    val title = when {
        isSearch -> "No Results Found"
        selectedFilter == "Active" -> "No Active Members"
        selectedFilter == "Expired" -> "No Expired Members"
        selectedFilter == "Inactive" -> "No Inactive Members"
        else -> "No Member Yet"
    }

    val subtitle = when {
        isSearch -> "We couldn't find any member matching \"$searchQuery\". Try a different search term."
        selectedFilter == "Active" -> "There are no members with an active subscription right now."
        selectedFilter == "Expired" -> "Great! No one has an expired subscription at the moment."
        selectedFilter == "Inactive" -> "All members seem to be engaged. No inactive records found."
        else -> "It Looks Like Your Gym Empty. Start Your Journey By Adding First Member!"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(if (isSearch) Res.drawable.ic_warning else Res.drawable.no_member),
            contentDescription = null,
            modifier = Modifier.size(140.dp),
            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            style = AppTextTheme.bold.copy(fontSize = 22.sp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subtitle,
            style = AppTextTheme.regular.copy(fontSize = 14.sp, color = Gray),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        if (!isSearch) {
            Spacer(modifier = Modifier.height(30.dp))
            CommonButton(
                onClick = onAddClick,
                text = "Add Member"
            )
        }
    }
}

@Composable
fun MemberListItem(member: MemberDTO) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Image with Badge
            Box {
                val profileImage =member.profile_image
                val painter = if (!profileImage.isNullOrEmpty())
                    rememberAsyncImagePainter(profileImage)
                else
                    painterResource(Res.drawable.gym_boy)
                // If the member object supported URL image loading, we could use KamelImage here.
                Image(
                    painter = painter, // Fallback
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .border(1.dp, Color.LightGray, CircleShape),
                    contentScale = ContentScale.Crop
                )
                // Small Crown Badge
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .align(Alignment.TopEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(
                            when {
                                member.status.equals("active", true) -> Res.drawable.ic_king
                                member.status.equals("inactive", true) -> Res.drawable.ic_inactive
                                else -> Res.drawable.ic_red_king
                            }
//                            if (member.status.equals(
//                                    "active",
//                                    true
//                                )
//                            ) Res.drawable.ic_king
//                            else if(member.status.equals(
//                                    "active",
//                                    true
//                                ))
//                            else Res.drawable.ic_red_king,
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Name and Phone
            Column(modifier = Modifier.weight(1f)) {
                val displayName = member.name.takeIf { it.isNotBlank() }
                    ?: "${member.first_name} ${member.last_name}"
                Text(
                    text = displayName.trim().takeIf { it.isNotBlank() } ?: "Unknown Member",
                    style = AppTextTheme.bold.copy(fontSize = 16.sp)
                )
                Text(
                    text = member.phone,
                    style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Gray)
                )
            }

            // Status and Days Left
            Column(horizontalAlignment = Alignment.End) {
                // Status Chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (member.status.lowercase()) {
                                "active" -> Color(0xFFE7F7F2)
                                "inactive" -> Color(0xFFFFF7E6)
                                else -> Color(0xFFFFF0F0)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            when (member.status.lowercase()) {
                                "active" -> Res.drawable.ic_active
                                "inactive" -> Res.drawable.ic_inactive
                                else -> Res.drawable.ic_warning
                            }
                        ),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = member.status.replaceFirstChar { it.uppercase() },
                        style = AppTextTheme.medium.copy(
                            fontSize = 11.sp,
                            color = when (member.status.lowercase()) {
                                "active" -> PrimaryDarkColor
                                "inactive" -> Color.Gray
                                else -> Color.Red
                            }
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Days Left/Overdue Chip
//                val daysLeftDisplay = member.daysLeft ?: "Unknown"
//                val isOverdue =
//                    daysLeftDisplay.lowercase() == "overdue" || member.status.lowercase() == "expired"
//                Box(
//                    modifier = Modifier
//                        .clip(RoundedCornerShape(8.dp))
//                        .background(
//                            if (isOverdue) Color(0xFFFFF0F0) else Color(0xFFF1F5F9)
//                        )
//                        .padding(horizontal = 8.dp, vertical = 2.dp)
//                ) {
//                    Text(
//                        text = daysLeftDisplay,
//                        style = AppTextTheme.medium.copy(
//                            fontSize = 10.sp,
//                            color = if (isOverdue) Color.Red else Gray
//                        )
//                    )
//                }
            }
        }
    }
}


