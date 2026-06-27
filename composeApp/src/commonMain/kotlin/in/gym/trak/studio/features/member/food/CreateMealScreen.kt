package `in`.gym.trak.studio.features.member

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import `in`.gym.trak.studio.components.CommonOutlineButton
import `in`.gym.trak.studio.components.CommonTextField
import `in`.gym.trak.studio.components.ConfirmationDialog
import `in`.gym.trak.studio.components.GymAppBar
import `in`.gym.trak.studio.components.LoadingScreenHandler
import `in`.gym.trak.studio.data.model.CreateDietFoodItemRequest
import `in`.gym.trak.studio.data.model.formatDietMealFoodSummary
import `in`.gym.trak.studio.data.model.CreateDietMealRequest
import `in`.gym.trak.studio.data.model.DietMealDTO
import `in`.gym.trak.studio.data.model.DietMealFoodItemDTO
import `in`.gym.trak.studio.getCurrentTimeMillis
import `in`.gym.trak.studio.components.DaySelector
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.saveable.rememberSaveable
import `in`.gym.trak.studio.theme.AppTextTheme
import `in`.gym.trak.studio.theme.Black
import `in`.gym.trak.studio.theme.Gray
import `in`.gym.trak.studio.theme.GrayBorderColor
import `in`.gym.trak.studio.theme.GreenColor
import `in`.gym.trak.studio.theme.PrimaryColor
import `in`.gym.trak.studio.theme.White
import `in`.gym.trak.studio.viewmodel.dashboard.OwnerDashboardScreenModel
import gym.composeapp.generated.resources.Res
import `in`.gym.trak.studio.components.formatTime
import `in`.gym.trak.studio.components.TimePickerModal
import `in`.gym.trak.studio.theme.RedColor
import gym.composeapp.generated.resources.ic_add_meal
import gym.composeapp.generated.resources.ic_breakfast_outline
import gym.composeapp.generated.resources.ic_clock
import gym.composeapp.generated.resources.ic_delete_outline
import gym.composeapp.generated.resources.ic_dinner
import gym.composeapp.generated.resources.ic_dinner_outline
import gym.composeapp.generated.resources.ic_lunch_outline
import gym.composeapp.generated.resources.ic_snack_outline
import gym.composeapp.generated.resources.ic_thunder
import `in`.gym.trak.studio.data.repository.SessionManager
import `in`.gym.trak.studio.features.member.food.AddFoodScreen
import `in`.gym.trak.studio.features.member.food.MealFormScreenModel
import kotlin.random.Random
import org.jetbrains.compose.resources.painterResource

/** Tight label → control; consistent gaps between form blocks; extra scroll space above IME. */
private val MealFormLabelBelowSpacing = 6.dp
private val MealFormSectionBelowSpacing = 18.dp
private val MealFormScrollBottomExtra = 120.dp

data class LocalMealFood(
    val key: String,
    val diet_food_id: String?,
    val name: String,
    val quantity: Int,
    val calories: Int,
    val weight_kg: Double?,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,
    val unit_type: String? = null,
    val image_url: String? = null,
)

class CreateMealScreen(
    val mealData: MealData? = null,
    val memberGymUserId: String? = null,
    val dietMeal: DietMealDTO? = null,
    val readOnly: Boolean = false,
    val hideRepeat: Boolean = false,
    /** After creating a meal, open consume-food with all items pre-selected. */
    val consumeAfterCreate: Boolean = false,
    /** `yyyy-MM-dd` passed to consume screen when [consumeAfterCreate] is true. */
    val consumedOnDate: String? = null,
    val onMealCreated: (() -> Unit)? = null,
    val onConsumed: (() -> Unit)? = null,
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        if (mealData != null) {
            LegacyExploreCreateMealContent(mealData)
            return
        }

        val screenModel = rememberScreenModel { OwnerDashboardScreenModel() }
        val formScreenModel = rememberScreenModel {
            MealFormScreenModel(dietMeal, { newFoodKey() })
        }
        LaunchedEffect(Unit) {
            formScreenModel.init()
        }

        DietApiMealFormContent(
            screenModel = screenModel,
            formScreenModel = formScreenModel,
            memberGymUserId = memberGymUserId,
            dietMeal = dietMeal,
            readOnly = readOnly,
            hideRepeat = hideRepeat,
            consumeAfterCreate = consumeAfterCreate,
            consumedOnDate = consumedOnDate,
            onMealCreated = onMealCreated,
            onConsumed = onConsumed,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DietApiMealFormContent(
    screenModel: OwnerDashboardScreenModel,
    formScreenModel: MealFormScreenModel,
    memberGymUserId: String?,
    dietMeal: DietMealDTO?,
    readOnly: Boolean,
    hideRepeat: Boolean,
    consumeAfterCreate: Boolean,
    consumedOnDate: String?,
    onMealCreated: (() -> Unit)?,
    onConsumed: (() -> Unit)?,
) {
    val navigator = LocalNavigator.currentOrThrow
    val isLoading by screenModel.isLoading.collectAsState()

    var mealName by rememberSaveable(dietMeal?.id) { mutableStateOf(dietMeal?.name ?: "") }
    var mealTime by rememberSaveable(dietMeal?.id) { mutableStateOf(dietMeal?.time ?: "07:30 AM") }

    var startHour by rememberSaveable { mutableIntStateOf(7) }
    var startMinute by rememberSaveable { mutableIntStateOf(30) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }

    var repeatEnabled by rememberSaveable(dietMeal?.id) {
        mutableStateOf(
            dietMeal?.repeat_enabled ?: false
        )
    }
    var selectedDays by rememberSaveable(dietMeal?.id) {
        val daysStrings = listOf("M", "T", "W", "T", "F", "S", "S")
        val initial = dietMeal?.repeat_days?.mapNotNull { idx ->
            daysStrings.getOrNull(idx)?.let { "$it$idx" }
        }?.toSet() ?: emptySet()
        mutableStateOf(initial)
    }

    var selectedType by rememberSaveable(dietMeal?.id) {
        mutableStateOf(apiMealTypeToUiLabel(dietMeal?.meal_type ?: "BREAKFAST"))
    }

    val localFoods = formScreenModel.localFoods

    val mealTypes = listOf(
        Triple("Breakfast", Res.drawable.ic_breakfast_outline, "Breakfast"),
        Triple("Lunch", Res.drawable.ic_lunch_outline, "Lunch"),
        Triple("Dinner", Res.drawable.ic_dinner_outline, "Dinner"),
        Triple("Snack", Res.drawable.ic_snack_outline, "Snack")
    )

    var showDeleteDialog by remember { mutableStateOf(false) }
    var foodKeyPendingDelete by remember { mutableStateOf<String?>(null) }
    var showDeleteMealDialog by remember { mutableStateOf(false) }
    var mealNameError by remember { mutableStateOf<String?>(null) }
    var repeatDaysError by remember { mutableStateOf<String?>(null) }
    var foodItemsError by remember { mutableStateOf<String?>(null) }
    var memberError by remember { mutableStateOf<String?>(null) }

    val isEditMode = dietMeal != null && !readOnly

    if (showTimePicker) {
        TimePickerModal(
            title = "Meal time",
            initialHour = startHour,
            initialMinute = startMinute,
            onClose = { showTimePicker = false },
            onSelect = { h, m ->
                startHour = h
                startMinute = m
                mealTime = formatTime(h, m)
                showTimePicker = false
            })
    }

    if (showDeleteMealDialog && dietMeal != null) {
        ConfirmationDialog(
            onDismissRequest = { showDeleteMealDialog = false },
            onConfirm = {
                screenModel.deleteDietMeal(dietMeal.id) {
                    onMealCreated?.invoke()
                    navigator.pop()
                }
                showDeleteMealDialog = false
            },
            title = "Delete Meal",
            message = "Are you sure you want to delete this meal?",
            confirmText = "Delete",
            isDangerAction = true
        )
    }

    if (showDeleteDialog && foodKeyPendingDelete != null) {
        val key = foodKeyPendingDelete!!
        val foodName = localFoods.find { it.key == key }?.name ?: ""
        ConfirmationDialog(
            onDismissRequest = {
                showDeleteDialog = false
                foodKeyPendingDelete = null
            },
            onConfirm = {
                formScreenModel.removeFood(key)
                foodItemsError = null
                showDeleteDialog = false
                foodKeyPendingDelete = null
            },
            title = "Remove food item",
            message = "Remove \"$foodName\" from this meal?",
            confirmText = "Remove",
            isDangerAction = true
        )
    }

    val canSubmit = !readOnly && !memberGymUserId.isNullOrBlank()

    LoadingScreenHandler(screenModel = screenModel) {
        Scaffold(topBar = {
            GymAppBar(
                title = when {
                    readOnly -> "Meal details"
                    isEditMode -> "Edit Meal"
                    else -> "Create New Meal"
                }, onBackClick = { navigator.pop() }, actions = {
                    if (isEditMode) {
                        IconButton(onClick = { showDeleteMealDialog = true }) {
                            Icon(
                                painter = painterResource(Res.drawable.ic_delete_outline),
                                contentDescription = "Delete Meal",
                                tint = RedColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    if (readOnly) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_dinner),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(40.dp).padding(end = 8.dp)
                        )
                    }
                })
        }, containerColor = White,
//            bottomBar = {
//
//        }
        )

        { padding ->
            val mealScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .imePadding()
                    .verticalScroll(mealScrollState)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = MealFormScrollBottomExtra)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                MealFormSectionLabel("Meal Name")
                Spacer(modifier = Modifier.height(MealFormLabelBelowSpacing))
                CommonTextField(
                    value = mealName,
                    onValueChange = {
                        mealName = it
                        mealNameError = null
                    },
                    placeholder = "e.g. Post-workout meal",
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = readOnly,
                    errorText = mealNameError
                )

                Spacer(modifier = Modifier.height(MealFormSectionBelowSpacing))

                MealFormSectionLabel("Meal Time")
                Spacer(modifier = Modifier.height(MealFormLabelBelowSpacing))
                Box(modifier = Modifier.clickable(enabled = !readOnly) { showTimePicker = true }) {
                    CommonTextField(
                        value = mealTime,
                        onValueChange = { if (!readOnly) mealTime = it },
                        placeholder = "07:30 PM",
                        modifier = Modifier.fillMaxWidth(),
                        leadingIconDrawable = Res.drawable.ic_clock,
                        readOnly = true,
                        enabled = false
                    )
                }

                Spacer(modifier = Modifier.height(MealFormSectionBelowSpacing))

                MealFormSectionLabel("Meal type")
                Spacer(modifier = Modifier.height(MealFormLabelBelowSpacing))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    mealTypes.forEach { (label, icon, value) ->
                        MealTypeItem(
                            modifier = Modifier.weight(1f),
                            label = label,
                            icon = icon,
                            isSelected = selectedType == value,
                            onClick = { if (!readOnly) selectedType = value },
                            enabled = !readOnly
                        )
                    }
                }

                Spacer(modifier = Modifier.height(MealFormSectionBelowSpacing))


                if(!hideRepeat)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MealFormSectionLabel("Repeat On")
                    Switch(
                        checked = repeatEnabled,
                        onCheckedChange = { if (!readOnly) repeatEnabled = it },
                        enabled = !readOnly,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = White,
                            checkedTrackColor = PrimaryColor,
                            uncheckedThumbColor = Gray,
                            uncheckedTrackColor = Gray.copy(alpha = 0.2f)
                        )
                    )
                }

                if (repeatEnabled) {
                    Spacer(modifier = Modifier.height(10.dp))
                    DaySelector(
                        selectedDays = selectedDays, onDayToggle = { day ->
                            if (!readOnly) {
                                selectedDays = if (selectedDays.contains(day)) {
                                    selectedDays - day
                                } else {
                                    selectedDays + day
                                }
                                repeatDaysError = null
                            }
                        })
                    repeatDaysError?.let {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = it,
                            style = AppTextTheme.regular.copy(fontSize = 12.sp, color = RedColor),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }

                if (readOnly && dietMeal != null && dietMeal.repeat_enabled && dietMeal.repeat_days.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Repeats on: ${formatRepeatDays(dietMeal.repeat_days)}",
                        style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Gray)
                    )
                }

                Spacer(modifier = Modifier.height(MealFormSectionBelowSpacing))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MealFormSectionLabel("Food items")
                    Text(
                        "${localFoods.size} items",
                        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = GreenColor)
                    )
                }
                foodItemsError?.let {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = it,
                        style = AppTextTheme.regular.copy(fontSize = 12.sp, color = RedColor),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(MealFormLabelBelowSpacing))

                CommonCard(
                    content = {
                        Box(
                            modifier = Modifier.fillMaxWidth()
                                .background(PrimaryColor.copy(alpha = 0.06f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,

                                ) {
                                if (localFoods.isEmpty()) {
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Box(
                                        modifier = Modifier.size(64.dp).clip(CircleShape)
                                            .background(PrimaryColor.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(Res.drawable.ic_add_meal), // Cloche placeholder
                                            contentDescription = null,
                                            tint = Black,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        "No Food Item added yet", style = AppTextTheme.bold.copy(
                                            fontSize = 16.sp, color = Black
                                        )
                                    )
                                    Text(
                                        "Add items to calculate Calories",
                                        style = AppTextTheme.medium.copy(
                                            fontSize = 13.sp, color = GreenColor
                                        )
                                    )
                                } else {
                                    localFoods.forEachIndexed { index, food ->
                                        EditableFoodRow(
                                            food = food,
                                            readOnly = readOnly,
                                            onQuantityChange = { q ->
                                                formScreenModel.updateQuantity(index, q)
                                            },
                                            onCaloriesChange = { c ->
                                                formScreenModel.updateCalories(index, c)
                                            },
                                            onDelete = {
                                                foodKeyPendingDelete = food.key
                                                showDeleteDialog = true
                                            })
                                        if (index < localFoods.lastIndex) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(vertical = 16.dp),
                                                color = Color(0xFFE5E7EB),
                                                thickness = 0.5.dp
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(24.dp))

                                if (!readOnly) {
                                    Surface(
                                        onClick = {
                                            navigator.push(
                                                AddFoodScreen(
                                                currentFoods = localFoods,
                                                onFoodsSelected = { foods ->
                                                    formScreenModel.updateFoods(foods)
                                                    foodItemsError = null
                                                }))
                                        },
                                        shape = RoundedCornerShape(100.dp),
                                        color = PrimaryColor,
                                        modifier = Modifier.fillMaxWidth(0.7f).height(44.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.Add,
                                                null,
                                                tint = White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Add Food Item", style = AppTextTheme.bold.copy(
                                                    fontSize = 14.sp, color = White
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    })

                Spacer(modifier = Modifier.height(24.dp))
                if (!readOnly) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(top = 12.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CommonButton(
                            text = if (isEditMode) "Update Meal" else "Create Meal", onClick = {
                                val mid = memberGymUserId
                                val normalizedMealName = mealName.trim()
                                mealNameError = when {
                                    normalizedMealName.isBlank() -> "Meal name is required."
                                    normalizedMealName.length < 2 -> "Meal name must be at least 2 characters."
                                    else -> null
                                }
                                repeatDaysError = if (repeatEnabled && selectedDays.isEmpty()) {
                                    "Select at least one repeat day."
                                } else {
                                    null
                                }
                                foodItemsError = if (localFoods.isEmpty()) {
                                    "Add at least one food item."
                                } else {
                                    null
                                }
                                memberError = if (mid.isNullOrBlank()) {
                                    "Unable to identify member. Please reopen this screen."
                                } else {
                                    null
                                }

                                if (mealNameError != null || repeatDaysError != null || foodItemsError != null || memberError != null || mid == null) return@CommonButton

                                val items = localFoods.map { f ->
                                    CreateDietFoodItemRequest(
                                        diet_food_id = f.diet_food_id,
                                        name = f.name,
                                        quantity = f.quantity,
                                        calories = f.calories,
                                        weight_kg = f.weight_kg,
                                        protein = f.protein?.toInt(),
                                        carbs = f.carbs?.toInt(),
                                        fat = f.fat?.toInt(),
                                        unit_type = f.unit_type,
                                    )
                                }
                                val selectedIndices = selectedDays.mapNotNull { key ->
                                    if (key.length >= 2) key.drop(1).toIntOrNull() else null
                                }.sorted()

                                val request = CreateDietMealRequest(

                                    member_id = if (SessionManager.userRole == "gym_owner" || SessionManager.userRole == "trainer") mid else null,
                                    name = normalizedMealName,
                                    time = mealTime.trim(),
                                    created_by = if (SessionManager.userRole == "gym_owner" || SessionManager.userRole == "trainer") "trainer" else "member",
                                    meal_type = uiLabelToMealTypeApi(selectedType),
                                    repeat_enabled = repeatEnabled,
                                    repeat_days = selectedIndices,
                                    food_items = items
                                )
                                if (isEditMode && dietMeal != null) {
                                    screenModel.updateDietMeal(dietMeal.id, request) {
                                        onMealCreated?.invoke()
                                        navigator.pop()
                                    }
                                } else {
                                    screenModel.createDietMeal(request) {
                                        onMealCreated?.invoke()
                                        navigator.pop()
                                        if (consumeAfterCreate) {
                                            val createdMeal = buildDietMealFromForm(
                                                memberId = mid,
                                                request = request,
                                                localFoods = localFoods,
                                            )
                                            navigator.push(
                                                ConsumeFoodScreen(
                                                    meal = createdMeal,
                                                    memberGymUserId = memberGymUserId,
                                                    consumedOnDate = consumedOnDate,
                                                    preselectAllItems = true,
                                                    onConsumed = {
                                                        onConsumed?.invoke()
                                                    },
                                                )
                                            )
                                        }
                                    }
                                }
                            }, enabled = canSubmit && !isLoading
                        )
                        memberError?.let {
                            Text(
                                text = it, style = AppTextTheme.regular.copy(
                                    fontSize = 12.sp, color = RedColor
                                )
                            )
                        }
                        CommonOutlineButton(
                            text = "Cancel",
                            onClick = { navigator.pop() },
                            borderColor = GrayBorderColor,
                            textColor = Black
                        )
                    }
                }
            }
        }
    }
}

private fun newFoodKey(): String = "${getCurrentTimeMillis()}_${Random.nextInt(99999)}"

private fun buildDietMealFromForm(
    memberId: String,
    request: CreateDietMealRequest,
    localFoods: List<LocalMealFood>,
): DietMealDTO = DietMealDTO(
    id = "",
    member_id = memberId,
    name = request.name,
    time = request.time,
    meal_type = request.meal_type,
    repeat_enabled = request.repeat_enabled,
    repeat_days = request.repeat_days,
    food_items = localFoods.map { food ->
        DietMealFoodItemDTO(
            diet_food_id = food.diet_food_id,
            name = food.name,
            weight_kg = food.weight_kg,
            calories = food.calories,
            quantity = food.quantity,
            protein = food.protein,
            carbs = food.carbs,
            fat = food.fat,
            unit_type = food.unit_type,
            image_url = food.image_url,
        )
    },
)

private fun apiMealTypeToUiLabel(api: String): String = when (api.uppercase()) {
    "BREAKFAST" -> "Breakfast"
    "LUNCH" -> "Lunch"
    "DINNER" -> "Dinner"
    "SNACK" -> "Snack"
    else -> "Breakfast"
}

private fun uiLabelToMealTypeApi(label: String): String = when (label) {
    "Breakfast" -> "BREAKFAST"
    "Lunch" -> "LUNCH"
    "Dinner" -> "DINNER"
    "Snack" -> "SNACK"
    else -> "BREAKFAST"
}

private val dayLetters = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private fun formatRepeatDays(days: List<Int>): String =
    days.sorted().mapNotNull { d -> dayLetters.getOrNull(d) }.joinToString(", ")

@Composable
private fun EditableFoodRow(
    food: LocalMealFood,
    readOnly: Boolean,
    onQuantityChange: (Int) -> Unit,
    onCaloriesChange: (Int) -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_thunder),
                contentDescription = null,
                modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(food.name, style = AppTextTheme.bold.copy(fontSize = 14.sp))
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
                    style = AppTextTheme.medium.copy(fontSize = 11.sp, color = Gray)
                )
            }
            if (!readOnly) {
                Box(
                    modifier = Modifier.size(32.dp).clip(CircleShape).clickable { onDelete() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_delete_outline),
                        contentDescription = "Remove",
                        tint = Color.Red,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        if (!readOnly) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Quantity", style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    CommonTextField(
                        value = food.quantity.toString(), onValueChange = {
                            onQuantityChange(
                                it.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            )
                        }, placeholder = "1", singleLine = true
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Calories", style = AppTextTheme.medium.copy(fontSize = 12.sp, color = Gray)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    CommonTextField(
                        value = food.calories.toString(), onValueChange = {
                            onCaloriesChange(
                                it.toIntOrNull()?.coerceAtLeast(0) ?: 0
                            )
                        }, placeholder = "0", singleLine = true
                    )
                }
            }
        }
    }
}

@Composable
private fun MealFormSectionLabel(text: String) {
    Text(
        text = text,
        style = AppTextTheme.medium.copy(fontSize = 14.sp, color = Color.Black),
    )
}

@Composable
private fun MealTypeItem(
    modifier: Modifier = Modifier,
    label: String,
    icon: org.jetbrains.compose.resources.DrawableResource,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier
            .fillMaxHeight()
            .clickable(enabled = enabled) { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF9FAFB),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp, color = if (isSelected) PrimaryColor else Color.Transparent
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = if (isSelected) PrimaryColor else Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label, style = AppTextTheme.bold.copy(
                    fontSize = 10.sp, color = if (isSelected) PrimaryColor else Gray
                )
            )
        }
    }
}

@Composable
private fun LegacyExploreCreateMealContent(mealData: MealData) {
    val navigator = LocalNavigator.currentOrThrow
    var mealName by remember { mutableStateOf(mealData.name) }
    var mealTime by remember { mutableStateOf("07:30 PM") }
    var selectedType by remember { mutableStateOf("Breakfast") }

    val mealTypes = listOf(
        Triple("Breakfast", Res.drawable.ic_breakfast_outline, "Breakfast"),
        Triple("Lunch", Res.drawable.ic_lunch_outline, "Lunch"),
        Triple("Dinner", Res.drawable.ic_dinner_outline, "Dinner"),
        Triple("Snack", Res.drawable.ic_snack_outline, "Snack")
    )

    Scaffold(topBar = {
        GymAppBar(title = "Edit Meal", onBackClick = { navigator.pop() }, actions = {
            Icon(
                painter = painterResource(Res.drawable.ic_dinner),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(40.dp)
            )
        })
    }, containerColor = White, bottomBar = {
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CommonButton(
                text = "Update Meal Changes", onClick = { navigator.pop() })
            CommonOutlineButton(
                text = "Cancel",
                onClick = { navigator.pop() },
                borderColor = GrayBorderColor,
                textColor = Black
            )
        }
    }) { padding ->
        val legacyScrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(legacyScrollState)
                .padding(horizontal = 20.dp)
                .padding(bottom = MealFormScrollBottomExtra)
        ) {
            Spacer(modifier = Modifier.height(4.dp))
            MealFormSectionLabel("Meal Name")
            Spacer(modifier = Modifier.height(MealFormLabelBelowSpacing))
            CommonTextField(
                value = mealName,
                onValueChange = { mealName = it },
                placeholder = "e.g. Post-Workout meal",
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(MealFormSectionBelowSpacing))
            MealFormSectionLabel("Meal Time")
            Spacer(modifier = Modifier.height(MealFormLabelBelowSpacing))
            CommonTextField(
                value = mealTime,
                onValueChange = { mealTime = it },
                placeholder = "07:30 PM",
                modifier = Modifier.fillMaxWidth(),
                leadingIconDrawable = Res.drawable.ic_clock
            )
            Spacer(modifier = Modifier.height(MealFormSectionBelowSpacing))
            MealFormSectionLabel("Meal Icon / Type")
            Spacer(modifier = Modifier.height(MealFormLabelBelowSpacing))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                mealTypes.forEach { (label, icon, value) ->
                    MealTypeItem(
                        modifier = Modifier.weight(1f),
                        label = label,
                        icon = icon,
                        isSelected = selectedType == value,
                        onClick = { selectedType = value })
                }
            }
            Spacer(modifier = Modifier.height(MealFormSectionBelowSpacing))
            Text(
                "Explore diet preview — member meals use the gym diet APIs from the member profile.",
                style = AppTextTheme.regular.copy(fontSize = 13.sp, color = Gray)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
