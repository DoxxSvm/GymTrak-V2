package `in`.gym.trak.studio.features.member

import `in`.gym.trak.studio.components.AppScrollableScreen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.BlueLightColor
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.GrayBorderColor
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_all
import gym.composeapp.generated.resources.ic_recent
import gym.composeapp.generated.resources.ic_share
import gym.composeapp.generated.resources.ic_workout
import gym.composeapp.generated.resources.img_dummy_product
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.utils.ShareService
import org.jetbrains.compose.resources.painterResource

class ExerciseDetailScreen(val exerciseName: String = "Bench Press (Barbell)") : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var selectedTab by remember { mutableStateOf("Summary") }

        Scaffold(
            topBar = {
                GymAppBar(
                    title = exerciseName,
                    onBackClick = { navigator.pop() },
                    actions = {
                        IconButton(onClick = { ShareService.shareExercise(exerciseName) }) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_share),
                                contentDescription = "Share",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            AppScrollableScreen(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(horizontal = 24.dp),
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Image Card
                CommonCard(
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Image(
                        painter = painterResource(Res.drawable.img_dummy_product), // Placeholder for exercise illustration
                        contentDescription = "Exercise Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title and Category
                Text(
                    text = exerciseName,
                    style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Color.Black)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Chest",
                    style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
                )

                Spacer(modifier = Modifier.height(15.dp))

                // Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TabChip(
                        title = "Summary",
                        icon = Res.drawable.ic_all,
                        isSelected = selectedTab == "Summary",
                        onClick = { selectedTab = "Summary" }
                    )
                    TabChip(
                        title = "History",
                        icon = Res.drawable.ic_recent,
                        isSelected = selectedTab == "History",
                        onClick = { selectedTab = "History" }
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tab Content
                if (selectedTab == "Summary") {
                    SummaryTabContent()
                } else {
                    HistoryTabContent()
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
fun TabChip(
    title: String,
    icon: org.jetbrains.compose.resources.DrawableResource,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = if (isSelected) PrimaryColor else White,
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
            1.dp,
            Color(0xFFE5E7EB)
        ),
        modifier = Modifier.height(44.dp).defaultMinSize(minWidth = 110.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (isSelected) White else Color.Black,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = AppTextTheme.medium.copy(
                    fontSize = 14.sp,
                    color = if (isSelected) White else Color.Black
                )
            )
        }
    }
}

@Composable
fun SummaryTabContent() {
    Column {
        // Log Item Header
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BlueLightColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_workout),
                    contentDescription = null,
                    tint = PrimaryColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Morning Workout",
                    style = AppTextTheme.semiBold.copy(fontSize = 14.sp, color = Color.Black)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "25 Feb 2026, 10:36",
                    style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Table Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "SET", style = AppTextTheme.regular.copy(
                    textAlign = TextAlign.Center,

                    fontSize = 10.sp, color = Gray
                ), modifier = Modifier.weight(0.5f)
            )
            Text(
                "KG", style = AppTextTheme.regular.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 10.sp, color = Gray
                ), modifier = Modifier.weight(1f)
            )
            Text(
                "REPS", style = AppTextTheme.regular.copy(
                    textAlign = TextAlign.Center,

                    fontSize = 10.sp, color = Gray
                ), modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Table Rows
        WorkoutLogRow(set = "1", kg = "60", reps = "10")
        Spacer(modifier = Modifier.height(8.dp))
        WorkoutLogRow(set = "2", kg = "60", reps = "10")
    }
}

@Composable
fun WorkoutLogRow(set: String, kg: String, reps: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = set,
            style = AppTextTheme.regular.copy(
                textAlign = TextAlign.Center,

                fontSize = 16.sp, color = Color.Black),
            modifier = Modifier.weight(0.5f)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(BlueLightColor)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = kg,
                style = AppTextTheme.regular.copy(
                    textAlign = TextAlign.Center,

                    fontSize = 16.sp, color = Color.Black)
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(BlueLightColor)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = reps,
                style = AppTextTheme.regular.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp, color = Color.Black)
            )
        }
    }
}

@Composable
fun HistoryTabContent() {
    Column {
        Text(
            text = "How to log barbell exercises",
            style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Color.Black)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Chart Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "55 Kg",
                    style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Color.Black)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Feb 24",
                    style = AppTextTheme.medium.copy(fontSize = 14.sp, color = PrimaryColor)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Last 3months",
                    style = AppTextTheme.medium.copy(fontSize = 14.sp, color = PrimaryColor)
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = PrimaryColor,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Dummy Bar Chart
        Row(
            modifier = Modifier.fillMaxWidth().height(120.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Y-Axis labels
            Column(
                modifier = Modifier.fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text("55 Kg", style = AppTextTheme.regular.copy(fontSize = 10.sp, color = Gray))
                Text("53 Kg", style = AppTextTheme.regular.copy(fontSize = 10.sp, color = Gray))
                Text("52 Kg", style = AppTextTheme.regular.copy(fontSize = 10.sp, color = Gray))
                Text("51 Kg", style = AppTextTheme.regular.copy(fontSize = 10.sp, color = Gray))
            }

            // Bars
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier.width(28.dp).height(50.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(PrimaryColor)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Jan", style = AppTextTheme.regular.copy(fontSize = 11.sp, color = Gray))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier.width(28.dp).height(60.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(PrimaryColor)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Feb", style = AppTextTheme.regular.copy(fontSize = 11.sp, color = Gray))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier.width(28.dp).height(90.dp)
                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(PrimaryColor)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Mar", style = AppTextTheme.regular.copy(fontSize = 11.sp, color = Gray))
            }
            Spacer(modifier = Modifier.width(8.dp)) // padding for right end
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Filters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterBadge(title = "Heaviest weight", isSelected = true)
            FilterBadge(title = "One Rep max", isSelected = false)
            FilterBadge(title = "Best Set Volume", isSelected = false)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Personal Records
        CommonCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Personal Records",
                    style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Color.Black),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                RecordItem(title = "Heaviest weight", value = "111kg")
                Spacer(modifier = Modifier.height(12.dp))
                RecordItem(title = "Best 1RM", value = "124.75kg")
                Spacer(modifier = Modifier.height(12.dp))
                RecordItem(title = "Best Set Volume", value = "111kg x 5")
                Spacer(modifier = Modifier.height(12.dp))
                RecordItem(title = "Best Session Volume", value = "555kg")
            }
        }
    }
}

@Composable
fun FilterBadge(title: String, isSelected: Boolean) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = if (isSelected) PrimaryColor else White,
        border = if (isSelected) null else BorderStroke(1.dp, GrayBorderColor)
    ) {
        Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
            Text(
                text = title,
                style = AppTextTheme.regular.copy(
                    fontSize = 12.sp,
                    color = if (isSelected) White else Gray
                )
            )
        }
    }
}

@Composable
fun RecordItem(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, PrimaryColor.copy(0.1f), RoundedCornerShape(12.dp))
            .background(PrimaryColor.copy(0.05f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Color.Black)
        )
        Text(
            text = value,
            style = AppTextTheme.medium.copy(fontSize = 14.sp, color = PrimaryColor)
        )
    }
}
