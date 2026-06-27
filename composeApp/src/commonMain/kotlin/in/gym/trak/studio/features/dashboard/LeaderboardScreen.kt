package `in`.gym.trak.studio.features.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.data.model.LeaderboardEntryDTO
import `in`.gym.trak.studio.theme.*
import gym.composeapp.generated.resources.*
import `in`.gym.trak.studio.viewmodel.dashboard.LeaderboardScreenModel
import coil3.compose.rememberAsyncImagePainter
import org.jetbrains.compose.resources.painterResource

/**
 * Screen for "My Leaderboard" replicating the provided design.
 */
class LeaderboardScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val screenModel = rememberScreenModel { LeaderboardScreenModel() }
        var selectedTab by remember { mutableStateOf("Attendance") }
        val entries by screenModel.entries.collectAsState()
        val total by screenModel.total.collectAsState()
        val tabs = listOf("Attendance", "Workout", "Challenges")

        val leaderboardType = when (selectedTab) {
            "Workout" -> "workout"
            else -> "attendance"
        }

        LaunchedEffect(selectedTab) {
            if (selectedTab != "Challenges") {
                screenModel.loadLeaderboard(type = leaderboardType, showFullLoader = true)
            }
        }

        val performers = remember(entries) { entries.filter { it.rank in 1..3 }.sortedBy { it.rank } }
        val sortedEntries = remember(entries) { entries.sortedBy { it.rank } }
        /** Ranks 4+ when present; otherwise full list (so a single #1 still appears in the vertical list). */
        val listRows = remember(sortedEntries) {
            val beyondPodium = sortedEntries.filter { it.rank > 3 }
            if (beyondPodium.isNotEmpty()) beyondPodium else sortedEntries
        }
        val unitLabel = if (leaderboardType == "workout") "workouts" else "days"

        LoadingScreenHandler(screenModel = screenModel) {
            Scaffold(
            topBar = {
                GymAppBar(
                    title = "My Leaderboard",
                    onBackClick = { navigator?.pop() }
                )
            },
            containerColor = Color.Transparent
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item {
                        LeaderboardTabBar(
                            tabs = tabs,
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }

                    if (selectedTab != "Challenges" && performers.isNotEmpty()) {
                        item { PodiumSection(performers) }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }

                    if (selectedTab == "Challenges") {
                        item { ChallengeCard() }
                        item { Spacer(modifier = Modifier.height(24.dp)) }
                    }

                    item {
                        Text(
                            text = if (selectedTab == "Challenges") {
                                "Other Competitors"
                            } else {
                                "Rankings"
                            },
                            style = AppTextTheme.bold.copy(fontSize = 18.sp, color = Black),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            textAlign = TextAlign.Start
                        )
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    if (selectedTab == "Challenges") {
                        item {
                            CompetitorItem(
                                competitor = LeaderboardEntryDTO(
                                    rank = 4,
                                    name = "Marcus",
                                    points = 1200
                                ),
                                unitLabel = unitLabel
                            )
                        }
                    } else if (listRows.isEmpty()) {
                        item {
                            Text(
                                text = "No leaderboard data available.",
                                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp)
                            )
                        }
                    } else {
                        items(
                            items = listRows,
                            key = { "${it.userId}_${it.rank}_${leaderboardType}" }
                        ) { competitor ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CompetitorItem(competitor = competitor, unitLabel = unitLabel)
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }

                    item {
                        OutlinedButton(
                            onClick = { /* View Full List */ },
                            shape = RoundedCornerShape(50.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.padding(bottom = 32.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (selectedTab == "Challenges") "View Full List" else "Total: $total",
                                    style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardTabBar(tabs: List<String>, selectedTab: String, onTabSelected: (String) -> Unit) {
    CommonCard (
        modifier =  Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .height(56.dp),
        content = {
            Row(
                modifier = Modifier.padding(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) PrimaryColor else Color.Transparent)
                            .clickable { onTabSelected(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            style = AppTextTheme.medium.copy(
                                fontSize = 14.sp,
                                color = if (isSelected) White else Color(0xFF64748B)
                            )
                        )
                    }
                }
            }
    })
}

@Composable
fun PodiumSection(users: List<LeaderboardEntryDTO>) {
    val user2 = users.find { it.rank == 2 }
    val user1 = users.find { it.rank == 1 }
    val user3 = users.find { it.rank == 3 }

    if (user1 == null && user2 == null && user3 == null) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        user2?.let { PodiumUserItem(it, modifier = Modifier.weight(1f), imageSize = 90.dp) }

        Spacer(modifier = Modifier.width(16.dp))

        user1?.let {
            PodiumUserItem(it, modifier = Modifier.weight(1.2f), imageSize = 120.dp, isWinner = true)
        }

        Spacer(modifier = Modifier.width(16.dp))

        user3?.let { PodiumUserItem(it, modifier = Modifier.weight(1f), imageSize = 80.dp) }
    }
}

@Composable
fun PodiumUserItem(
    user: LeaderboardEntryDTO,
    modifier: Modifier = Modifier,
    imageSize: Dp,
    isWinner: Boolean = false
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 🔥 Trophy Icon (moved down)
        if (isWinner) {
            Icon(
                painter = painterResource(Res.drawable.ic_tropy),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .offset(y = 6.dp),
                tint = Color.Unspecified
            )
        }

        Box(contentAlignment = Alignment.BottomCenter) {

            Box(
                modifier = Modifier
                    .size(imageSize)
                    .shadow(
                        elevation = 10.dp,
                        shape = CircleShape,
                        ambientColor = Color.Black.copy(alpha = 0.2f),
                        spotColor = Color.Black.copy(alpha = 0.3f)
                    )
                    .clip(CircleShape)
                    .border(
                        if (isWinner) 4.dp else 2.dp,
                        when (user.rank) {
                            1 -> PrimaryColor
                            2 -> LightBlueColor
                            else -> OrangeColor
                        },
//                        if (isWinner) PrimaryColor else Color(0xFFE2E8F0),
                        CircleShape
                    )
            ) {
                Image(
                    painter = if (!user.profileImage.isNullOrBlank()) {
                        rememberAsyncImagePainter(user.profileImage)
                    } else {
                        painterResource(Res.drawable.gym_boy)
                    },
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }


            Box(
                modifier = Modifier
                    .size(24.dp)
                    .offset(y = 6.dp)
                    .clip(CircleShape)
                    .background(
                        when (user.rank) {
                            1 -> PrimaryColor
                            2 -> LightBlueColor
                            else -> OrangeColor
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.rank.toString(),
                    style = AppTextTheme.bold.copy(
                        fontSize = 14.sp,
                        color =
                            when (user.rank) {
                                2 ->   Color(0xff334155)
                                else ->   White
                            }
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = user.name,
            style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black),
            textAlign = TextAlign.Center
        )

        Text(
            text = "${user.points} pts",
            style =
                when (user.rank) {
                    1 ->   AppTextTheme.bold.copy(fontSize = 14.sp, color = PrimaryColor)
                    else ->   AppTextTheme.medium.copy(fontSize = 12.sp, color = PrimaryColor)
                }
              ,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ChallengeCard() {
    CommonCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "30 Day Squat Challenge",
                style = AppTextTheme.bold.copy(fontSize = 18.sp, color = Black)
            )
            Text(
                text = "You are on track! keep it up.",
                style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Gray)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Progress", style = AppTextTheme.medium.copy(fontSize = 12.sp, color = PrimaryColor))
                Text(text = "20/30 Days", style = AppTextTheme.bold.copy(fontSize = 12.sp, color = Black))
            }

            Spacer(modifier = Modifier.height(8.dp))

            LinearProgressIndicator(
                progress = { 0.66f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = PrimaryColor,
                trackColor = Color(0xFFF1F5F9)
            )
        }
    }
}

@Composable
fun CompetitorItem(competitor: Competitor, unitLabel: String) {
    val bgColor = if (competitor.isCurrentUser) Color(0xFFE7F7F2) else White
    val borderColor = if (competitor.isCurrentUser) PrimaryColor else Color(0xFFF1F5F9)

    Surface(
        modifier = Modifier
            .padding(horizontal = 24.dp)
            .fillMaxWidth()
            .height(84.dp),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = competitor.rank.toString(),
                style = AppTextTheme.bold.copy(fontSize = 24.sp, color = Black),
                modifier = Modifier.width(32.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Image(
                painter = if (!competitor.profileImage.isNullOrBlank()) {
                    rememberAsyncImagePainter(competitor.profileImage)
                } else {
                    painterResource(Res.drawable.gym_boy)
                },
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color.White, CircleShape),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = competitor.name,
                    style = AppTextTheme.bold.copy(fontSize = 16.sp, color = Black)
                )
                Text(
                    text = "${competitor.points} $unitLabel",
                    style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = competitor.rank.toString(),
                    style = AppTextTheme.bold.copy(fontSize = 18.sp, color = Black)
                )
                Text(
                    text = unitLabel,
                    style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray)
                )
            }
        }
    }
}

private typealias Competitor = LeaderboardEntryDTO
