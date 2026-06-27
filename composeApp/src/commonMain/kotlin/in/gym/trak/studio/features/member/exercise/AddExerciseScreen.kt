package `in`.gym.trak.studio.features.member

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.rememberAsyncImagePainter
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.components.SearchBar
import `in`.gym.trak.studio.components.StaggeredEntranceItem
import `in`.gym.trak.studio.components.AppEmptyStateView
import `in`.gym.trak.studio.data.model.ExerciseRowDTO
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.GreenLightColor
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.gym_boy
import gym.composeapp.generated.resources.ic_add
import gym.composeapp.generated.resources.img_no_wrokout
import `in`.gym.trak.studio.features.member.exercise.CreateExerciseScreen
import org.jetbrains.compose.resources.painterResource

class AddExerciseScreen(
    val memberId: String = "",
    val onExerciseCreated: () -> Unit = {},
    val onExercisesSelected: ((List<ExerciseRowDTO>) -> Unit)? = null
) : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val exercises by screenModel.exercises.collectAsState()

        var searchQuery by remember { mutableStateOf("") }
        val selectedExercises = remember { mutableStateListOf<ExerciseRowDTO>() }

        LaunchedEffect(Unit) {
            screenModel.loadExercises()
        }

        LoadingScreenHandler(screenModel = screenModel) {
            Scaffold(
                topBar = {
                    GymAppBar(
                        title = "Add Exercise",
                        onBackClick = { navigator.pop() },
                        actions = {
                            TextButton(onClick = {
                                navigator.push(CreateExerciseScreen(onExerciseCreated = {
                                    screenModel.loadExercises()
                                }))
                            }) {
                                Text("Create", color = PrimaryColor, style = AppTextTheme.medium)
                            }
                        }
                    )
                },
                bottomBar = {
                    Box(modifier = Modifier.padding(24.dp)
                        .navigationBarsPadding()
                    ) {
                        CommonButton(
                            onClick = {
                                if (onExercisesSelected != null) {
                                    navigator.pop()
                                    onExercisesSelected.invoke(selectedExercises.toList())
                                } else {
                                    navigator.push(
                                        CreateWorkoutScreen(
                                            memberId,
                                            selectedExercises.toList()
                                        )
                                    )
                                }
                            },
                            text = if (selectedExercises.isEmpty()) "Add Exercise" else "Add Exercise (${selectedExercises.size})",
                            leftIcon = painterResource(Res.drawable.ic_add),
                            enabled = selectedExercises.isNotEmpty()
                        )
                    }
                },
                containerColor = Color.Transparent
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 24.dp)
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Search Bar
                    StaggeredEntranceItem(index = 0) {
                        SearchBar(
                            query = searchQuery,
                            onQueryChange = {
                                searchQuery = it
                                screenModel.loadExercises(search = it)
                            },
                            placeholder = "Search Exercises...",
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (exercises.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AppEmptyStateView(
                                image = Res.drawable.img_no_wrokout,
                                title = if (searchQuery.isBlank()) "No Exercises Yet" else "No Exercises Found",
                                subtitle = if (searchQuery.isBlank()) {
                                    "Exercises will appear here once you create or add them."
                                } else {
                                    "Try a different search term to find exercises."
                                }
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                StaggeredEntranceItem(index = 1) {
                                    SectionHeader("All Exercise")
                                }
                            }

                            items(exercises.size) { index ->
                                val exercise = exercises[index]
                                val isSelected = selectedExercises.any { it.id == exercise.id }
                                StaggeredEntranceItem(index = index + 2) {
                                    ExerciseItem(
                                        title = exercise.name,
                                        subtitle = exercise.primary_muscle ?: exercise.equipment ?: "",
                                        image = exercise.asset_url ?: "",
                                        isSelected = isSelected,
                                        onManageClick = { navigator.push(ExerciseDetailScreen(exercise.name)) },
                                        onSelectToggle = {
                                            if (isSelected) {
                                                selectedExercises.removeAll { it.id == exercise.id }
                                            } else {
                                                selectedExercises.add(exercise)
                                            }
                                        }
                                    )
                                }
                            }

                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = AppTextTheme.semiBold.copy(fontSize = 14.sp, color = Color.Black),
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun ExerciseItem(
    title: String,
    subtitle: String,
    image: String,
    isSelected: Boolean,
    onManageClick: () -> Unit = {},
    onSelectToggle: () -> Unit = {}
) {
    val painter = if (image.isNotEmpty())
        rememberAsyncImagePainter(image)
    else
        painterResource(Res.drawable.gym_boy)
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onSelectToggle() },
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) GreenLightColor.copy(alpha = 0.1f) else White,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) PrimaryColor else Color(0xFFE5E7EB)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter,
                    null,
                    tint = if (image.isNotEmpty()) Color.Unspecified else PrimaryColor,
                    modifier = Modifier.size(if (image.isNotEmpty()) 48.dp else 24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = AppTextTheme.bold.copy(fontSize = 16.sp))
                Text(subtitle, style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Gray))
            }
//            IconButton(onClick = onManageClick) {
//                Icon(
//                    painterResource(Res.drawable.manage),
//                    null,
//                    tint = Gray,
//                    modifier = Modifier.size(20.dp)
//                )
//            }
            IconButton(
                onClick = onSelectToggle,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isSelected) PrimaryColor else White
                ),
                modifier = Modifier
                    .size(24.dp)
            ) {
                Icon(
                    if (isSelected) Icons.Default.Done else Icons.Default.Add,
                    null,
                    tint = if (isSelected) White else PrimaryColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

