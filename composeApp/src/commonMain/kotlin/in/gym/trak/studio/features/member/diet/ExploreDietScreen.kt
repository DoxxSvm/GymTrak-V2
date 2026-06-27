package `in`.gym.trak.studio.features.member

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.CommonCard
import `in`.gym.trak.studio.components.AppEmptyStateView
import `in`.gym.trak.studio.components.CommonOutlineButton
import `in`.gym.trak.studio.components.ConfirmationDialog
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.data.model.DietMealDTO
import `in`.gym.trak.studio.data.model.formatDietMealFoodSummary
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.DarkBlack
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.GrayBorderColor
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.TextFiledColor
import `in`.gym.trak.studio.theme.White
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_all
import gym.composeapp.generated.resources.ic_breakfast
import gym.composeapp.generated.resources.ic_delete_outline
import gym.composeapp.generated.resources.ic_edit_outline
import gym.composeapp.generated.resources.ic_lunch
import gym.composeapp.generated.resources.ic_recommended
import gym.composeapp.generated.resources.ic_trainer
import gym.composeapp.generated.resources.ic_workout
import gym.composeapp.generated.resources.img_no_wrokout
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import org.jetbrains.compose.resources.painterResource

private fun dietFilterLabelToCreatedBy(label: String): String = when (label) {
    "My Meal" -> "member"
    "Trainer" -> "trainer"
    "Recommended", "All" -> "all"
    else -> "all"
}

class ExploreDietScreen : Screen {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val memberMeals by screenModel.memberDietMeals.collectAsState()
        var selectedFilter by remember { mutableStateOf("All") }
        val filters = listOf("All", "My Meal", "Trainer", "Recommended")

        var showDeleteDialog by remember { mutableStateOf(false) }
        var mealToDelete by remember { mutableStateOf<DietMealDTO?>(null) }
        val memberGymUserId = SessionManager.userId

        LaunchedEffect(memberGymUserId, selectedFilter) {
            if (memberGymUserId.isNotBlank()) {
                screenModel.loadMemberDiet(
                    memberGymUserId,
                    dietFilterLabelToCreatedBy(selectedFilter),
                    showGlobalLoader = true,
                )
            }
        }

        if (showDeleteDialog) {
            ConfirmationDialog(
                onDismissRequest = { 
                    showDeleteDialog = false
                    mealToDelete = null
                },
                onConfirm = {
                    val mealId = mealToDelete?.id
                    if (!mealId.isNullOrBlank()) {
                        screenModel.deleteDietMeal(mealId) {
                            if (memberGymUserId.isNotBlank()) {
                                screenModel.loadMemberDiet(
                                    memberGymUserId,
                                    dietFilterLabelToCreatedBy(selectedFilter),
                                )
                            }
                        }
                    }
                    showDeleteDialog = false
                    mealToDelete = null
                },
                title = "Delete Meal log",
                message = "Are you sure you want to delete '${mealToDelete?.name}' from your meal logs?",
                confirmText = "Delete",
                isDangerAction = true
            )
        }

        LoadingScreenHandler(screenModel = screenModel) {
            Scaffold(
                topBar = {
         GymAppBar(
                        title = "Explore Diet",
                        onBackClick = { navigator.pop() }
                    )
                },
                containerColor = Color.Transparent
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        CommonButton(
                            onClick = {
                                navigator.push(
                                    CreateMealScreen(
                                        readOnly = false,
                                        memberGymUserId = memberGymUserId,
                                        onMealCreated = {
                                            if (memberGymUserId.isNotBlank()) {
                                                screenModel.loadMemberDiet(
                                                    memberGymUserId,
                                                    dietFilterLabelToCreatedBy(selectedFilter),
                                                )
                                            }
                                        }
                                    )
                                )
                            },
                            text = "Create New Meal Routine",
                            color = PrimaryColor
                        )
                    }

                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(filters) { filter ->
                                FilterChip(
                                    label = filter,
                                    isSelected = selectedFilter == filter,
                                    onClick = { selectedFilter = filter }
                                )
                            }
                        }
                    }

                    item {
                        Text("Meal Logs", style = AppTextTheme.bold.copy(fontSize = 18.sp, color = DarkBlack))
                    }

                    if (memberMeals.isEmpty()) {
                        item {
                            AppEmptyStateView(
                                image = Res.drawable.img_no_wrokout,
                                title = "No meal routines yet",
                                subtitle = "Create a new meal routine to get started."
                            )
                        }
                    } else {
                        items(memberMeals) { meal ->
                            MealLogCard(
                                meal = meal,
                                onEdit = {
                                    navigator.push(
                                        CreateMealScreen(
                                            memberGymUserId = memberGymUserId,
                                            dietMeal = meal,
                                            readOnly = false,
                                            onMealCreated = {
                                                if (memberGymUserId.isNotBlank()) {
                                                    screenModel.loadMemberDiet(
                                                        memberGymUserId,
                                                        dietFilterLabelToCreatedBy(selectedFilter),
                                                    )
                                                }
                                            }
                                        )
                                    )
                                },
                                onDelete = {
                                    mealToDelete = meal
                                    showDeleteDialog = true
                            },
                            onConsume = {
                                navigator.push(
                                    ConsumeFoodScreen(
                                        meal = meal,
                                        memberGymUserId = memberGymUserId,
                                        onConsumed = {
                                            if (memberGymUserId.isNotBlank()) {
                                                screenModel.loadMemberDiet(
                                                    memberGymUserId,
                                                    dietFilterLabelToCreatedBy(selectedFilter),
                                                )
                                            }
                                        }
                                    )
                                )
                                }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(20.dp)) }
                }
            }
        }
    }
}

@Composable
fun MealLogCard(
    meal: DietMealDTO,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onConsume: () -> Unit
) {
    CommonCard(
        modifier = Modifier.fillMaxWidth(),
        elevation = 2.dp,
        borderColor = GrayBorderColor
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Icon, Name, Kcal, Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painterResource(mealTypeToIcon(meal.meal_type)),
                    null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(meal.name, style = AppTextTheme.bold.copy(fontSize = 18.sp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${meal.food_items.sumOf { it.calories * it.quantity }} kcal ",
                            style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
                        )
                        Text(meal.time, style = AppTextTheme.medium.copy(fontSize = 12.sp, color = PrimaryColor))
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, GrayBorderColor, CircleShape)
                            .clickable { onEdit() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_edit_outline),
                            contentDescription = "Edit",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .border(1.dp, GrayBorderColor, CircleShape)
                            .clickable {
                                onDelete()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_delete_outline),
                            contentDescription = "Delete",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Food Item Details with Light Gray Background
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = TextFiledColor,
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    meal.food_items.forEach { food ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                food.name,
                                style = AppTextTheme.medium.copy(fontSize = 14.sp),
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                formatDietMealFoodSummary(
                                    weightKg = food.weight_kg,
                                    unitType = food.unit_type,
                                    calories = food.calories,
                                    quantity = food.quantity,
                                    protein = food.protein,
                                    carbs = food.carbs,
                                    fat = food.fat,
                                ),
                                style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray),
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Consume Button (Outline)
            CommonOutlineButton(
                onClick = onConsume,
                text = "Consume Food",
                textColor = Black,
                color = TextFiledColor,
                borderColor = GrayBorderColor
            )
        }
    }
}

private fun mealTypeToIcon(mealType: String) = when (mealType.uppercase()) {
    "BREAKFAST" -> Res.drawable.ic_breakfast
    "LUNCH" -> Res.drawable.ic_lunch
    else -> Res.drawable.ic_breakfast
}

// Legacy model kept for compatibility with old edit flow constructor in CreateMealScreen.
data class MealData(
    val name: String,
    val icon: org.jetbrains.compose.resources.DrawableResource,
    val kcal: Int,
    val trainerName: String,
    val items: List<FoodItem>
)

data class FoodItem(
    val name: String,
    val weight: Int,
    val kcal: Int
)

@Composable
fun FilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) PrimaryColor else White,
        shape = RoundedCornerShape(100.dp),
        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (label == "All") Icon(painterResource(Res.drawable.ic_all), null, tint = if (isSelected) White else Black, modifier = Modifier.size(16.dp))
            else if (label == "My Workout" || label == "My Meal") Icon(painterResource(Res.drawable.ic_workout), null, tint = Color.Unspecified, modifier = Modifier.size(16.dp))
            else if (label == "Trainer") Icon(painterResource(Res.drawable.ic_trainer), null, tint = Color.Unspecified, modifier = Modifier.size(16.dp))
            else if (label == "Recommended") Icon(painterResource(Res.drawable.ic_recommended), null, tint =Color.Unspecified, modifier = Modifier.size(16.dp))

            Spacer(modifier = Modifier.width(8.dp))
            Text(label, style = AppTextTheme.medium.copy(fontSize = 14.sp, color = if (isSelected) White else Color.Black))
        }
    }
}
