package `in`.gym.trak.studio.features.member

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
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
import gym.composeapp.generated.resources.Res
import gym.composeapp.generated.resources.ic_breakfast
import gym.composeapp.generated.resources.ic_lunch
import `in`.gym.trak.studio.components.CommonButton
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.data.model.ConsumeDietFoodItemRequest
import `in`.gym.trak.studio.data.model.ConsumeDietFoodRequest
import `in`.gym.trak.studio.data.model.DietMealDTO
import `in`.gym.trak.studio.data.model.formatDietMealFoodSummary
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.GrayBorderColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.utils.DateUtils
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import org.jetbrains.compose.resources.painterResource

class ConsumeFoodScreen(
    private val meal: DietMealDTO,
    private val memberGymUserId: String?,
    /** `yyyy-MM-dd`; defaults to today when null. */
    private val consumedOnDate: String? = null,
    private val preselectAllItems: Boolean = false,
    private val onConsumed: () -> Unit
) : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val items = meal.food_items
        val selectedIndexes = remember(meal.id, items.size, preselectAllItems) {
            mutableStateListOf<Int>().apply {
                if (preselectAllItems) addAll(items.indices)
            }
        }

        val canConsume = selectedIndexes.isNotEmpty()

        LoadingScreenHandler(screenModel = screenModel) {
            Scaffold(
                topBar = {
                    GymAppBar(
                        title = meal.name,
                        onBackClick = { navigator.pop() }
                    )
                },
                containerColor = Color(0xFFFBFBFB),
                bottomBar = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        CommonButton(
                            text = "Consume Food",
                            onClick = {
                                val consumedAt = DateUtils.getCurrentDateIso()
                                val consumedOn = consumedOnDate?.takeIf { it.isNotBlank() }
                                    ?: consumedAt.take(10)
                                val request = ConsumeDietFoodRequest(
                                    meal_type = meal.meal_type,
                                    consumed_on = consumedOn,
                                    consumed_at = consumedAt,
                                    items = selectedIndexes.sorted().map { index ->
                                        val food = items[index]
                                        ConsumeDietFoodItemRequest(
                                            diet_food_id = food.diet_food_id,
                                            name = food.name,
                                            weight_kg = food.weight_kg,
                                            calories = food.calories,
                                            quantity = food.quantity
                                        )
                                    }
                                )
                                screenModel.consumeDietFood(
                                    memberGymUserId = memberGymUserId,
                                    request = request
                                ) {
                                    onConsumed()
                                    navigator.pop()
                                }
                            },
                            enabled = canConsume
                        )
                    }
                }
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color(0xFFFBFBFB)),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(items) { index, food ->
                        val selected = selectedIndexes.contains(index)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(White)
                                .border(1.dp, GrayBorderColor, RoundedCornerShape(14.dp))
                                .clickable {
                                    if (selected) selectedIndexes.remove(index)
                                    else selectedIndexes.add(index)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.Icon(
                                    painter = painterResource(mealTypeToIcon(meal.meal_type)),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = food.name,
                                        style = AppTextTheme.bold.copy(fontSize = 15.sp, color = Black)
                                    )
                                    Text(
                                        text = formatDietMealFoodSummary(
                                            weightKg = food.weight_kg,
                                            unitType = food.unit_type,
                                            calories = food.calories,
                                            quantity = food.quantity,
                                            protein = food.protein,
                                            carbs = food.carbs,
                                            fat = food.fat,
                                        ),
                                        style = AppTextTheme.regular.copy(fontSize = 12.sp, color = Gray)
                                    )
                                }
                            }
                            Checkbox(checked = selected, onCheckedChange = null)
                        }
                    }
                    item { Spacer(modifier = Modifier.height(90.dp)) }
                }
            }
        }
    }
}

private fun mealTypeToIcon(mealType: String) = when (mealType.uppercase()) {
    "BREAKFAST" -> Res.drawable.ic_breakfast
    "LUNCH" -> Res.drawable.ic_lunch
    else -> Res.drawable.ic_breakfast
}
