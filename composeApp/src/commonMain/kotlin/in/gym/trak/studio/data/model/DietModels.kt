package `in`.gym.trak.studio.data.model

import kotlin.math.abs
import kotlin.math.round
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.math.round
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DietMealDTO(
    val id: String,
    val member_id: String,
    val name: String,
    val time: String,
    val meal_type: String,
    val repeat_enabled: Boolean = false,
    val repeat_days: List<Int> = emptyList(),
    val food_items: List<DietMealFoodItemDTO> = emptyList(),
    val created_at: String? = null,
    val updated_at: String? = null
)

@Serializable
data class DietMealFoodItemDTO(
    val id: String = "",
    val diet_food_id: String? = null,
    val name: String,
    val weight_kg: Double? = null,
    val calories: Int = 0,
    val quantity: Int = 1,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,
    val unit_type: String? = null,
    val image_url: String? = null,
)

@Serializable
data class CreateDietMealRequest(
    val member_id: String?= null,
    val name: String,
    @SerialName("created_by")
    val created_by: String,
    val time: String,
    val meal_type: String,
    val repeat_enabled: Boolean = false,
    val repeat_days: List<Int> = emptyList(),
    val food_items: List<CreateDietFoodItemRequest>
)

@Serializable
data class CreateDietFoodItemRequest(
    val diet_food_id: String? = null,
    val name: String? = null,
    val quantity: Int,
    val calories: Int? = null,
    val weight_kg: Double? = null,
    val protein: Int? = null,
    val carbs: Int? = null,
    val fat: Int? = null,
    val unit_type: String? = null,
)

@Serializable
data class DietCatalogFoodDTO(
    val id: String,
    val name: String,
    val weight_kg: Double? = null,
    val calories: Int? = null,
    val quantity: Int? = null,
    val image_url: String? = null,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,
    val unit_type: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
)

@Serializable
data class CreateDietCatalogFoodRequest(
    val name: String,
    val weight_kg: Double,
    val calories: Int,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0,
    val unit_type: String,
    val quantity: Int,
    val image_url: String? = null,
)

@Serializable
data class ConsumeDietFoodRequest(
    val meal_type: String,
    val consumed_on: String,
    val consumed_at: String,
    val items: List<ConsumeDietFoodItemRequest>
)

@Serializable
data class ConsumeDietFoodItemRequest(
    val diet_food_id: String? = null,
    val name: String? = null,
    val weight_kg: Double? = null,
    val calories: Int? = null,
    val quantity: Int = 1,
    val portion_label: String? = null,
    val protein_g: Double? = null,
    val carbs_g: Double? = null,
    val fat_g: Double? = null
)

@Serializable
data class ConsumeDietFoodResponse(
    val success: Boolean = false,
    val count: Int = 0,
    val ids: List<String> = emptyList()
)

@Serializable
data class DietHistoryResponse(
    val date: String,
    val user_id: String,
    val daily_summary: DietDailySummary = DietDailySummary(),
    val macros: DietMacroSummary = DietMacroSummary(),
    val meal_logs: List<DietHistoryMealLog> = emptyList(),
    val recurring_meals: List<DietRecurringMeal> = emptyList(),
    val recurring_summary: DietRecurringSummary? = null,
)

@Serializable
data class DietDailySummary(
    val target_kcal: Int = 0,
    val consumed_kcal: Int = 0,
    val remaining_kcal: Int = 0
)

@Serializable
data class DietMacroSummary(
    val protein_g: Double = 0.0,
    val carbs_g: Double = 0.0,
    val fat_g: Double = 0.0
)

@Serializable
data class DietHistoryMealLog(
    val meal_type: String,
    val meal_label: String,
    val time: String? = null,
    val total_calories: Int = 0,
    val items: List<DietHistoryFoodItem> = emptyList()
)

@Serializable
data class DietHistoryFoodItem(
    val id: String? = null,
    val diet_food_id: String? = null,
    val name: String,
    val amount_display: String? = null,
    val calories: Int = 0,
    val protein_g: Double? = null,
    val carbs_g: Double? = null,
    val fat_g: Double? = null,
    val image_url: String? = null,
)

@Serializable
data class DietRecurringMeal(
    val meal_id: String = "",
    val name: String = "",
    val time: String? = null,
    val meal_type: String = "",
    val meal_label: String = "",
    val repeat_enabled: Boolean = false,
    val repeat_days: List<Int> = emptyList(),
    val created_by: String? = null,
    @SerialName("created_by_role")
    val created_by_role: String? = null,
    val food_items: List<DietRecurringFoodItem> = emptyList(),
    val total_calories: Int = 0,
    val macros: DietMacroSummary? = null,
)

@Serializable
data class DietRecurringFoodItem(
    val id: String? = null,
    val diet_food_id: String? = null,
    val name: String = "",
    val weight_kg: Double? = null,
    val calories: Int = 0,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,
    val unit_type: String? = null,
    val quantity: Int = 1,
    val image_url: String? = null,
)

@Serializable
data class DietRecurringSummary(
    val repeat_day_index: Int? = null,
    val meal_count: Int = 0,
    val total_calories: Int = 0,
    val protein_g: Double = 0.0,
    val carbs_g: Double = 0.0,
    val fat_g: Double = 0.0,
)

fun DietRecurringMeal.toDietMealDTO(): DietMealDTO =
    DietMealDTO(
        id = meal_id,
        member_id = "",
        name = name.ifBlank { meal_label },
        time = time.orEmpty(),
        meal_type = meal_type,
        repeat_enabled = repeat_enabled,
        repeat_days = repeat_days,
        food_items = food_items.map { it.toDietMealFoodItemDTO() },
    )

fun DietRecurringFoodItem.toDietMealFoodItemDTO(): DietMealFoodItemDTO =
    DietMealFoodItemDTO(
        id = id.orEmpty(),
        diet_food_id = diet_food_id,
        name = name,
        weight_kg = weight_kg,
        calories = calories,
        quantity = quantity,
        protein = protein,
        carbs = carbs,
        fat = fat,
        unit_type = unit_type,
        image_url = image_url,
    )

fun DietRecurringMeal.toHistoryMealLog(): DietHistoryMealLog =
    DietHistoryMealLog(
        meal_type = meal_type,
        meal_label = name.ifBlank { meal_label },
        time = time,
        total_calories = total_calories,
        items = food_items.map { it.toHistoryFoodItem() },
    )

fun DietRecurringFoodItem.toHistoryFoodItem(): DietHistoryFoodItem {
    val weightAmount = weight_kg?.let { amount ->
        if (quantity > 1) amount * quantity else amount
    }
    val amountDisplay = formatDietFoodWeightShort(weightAmount, unit_type)
        .takeIf { it.isNotBlank() }
        ?: quantity.takeIf { it > 1 }?.let { "×$it" }
    return DietHistoryFoodItem(
        id = id,
        diet_food_id = diet_food_id,
        name = name,
        amount_display = amountDisplay,
        calories = calories,
        protein_g = protein,
        carbs_g = carbs,
        fat_g = fat,
        image_url = image_url,
    )
}

/** Weekday index used in [DietRecurringMeal.repeat_days]: 0 = Monday … 6 = Sunday. */
val DIET_WEEKDAY_LABELS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

fun LocalDate.toDietRepeatDayIndex(): Int = when (dayOfWeek) {
    kotlinx.datetime.DayOfWeek.MONDAY -> 0
    kotlinx.datetime.DayOfWeek.TUESDAY -> 1
    kotlinx.datetime.DayOfWeek.WEDNESDAY -> 2
    kotlinx.datetime.DayOfWeek.THURSDAY -> 3
    kotlinx.datetime.DayOfWeek.FRIDAY -> 4
    kotlinx.datetime.DayOfWeek.SATURDAY -> 5
    kotlinx.datetime.DayOfWeek.SUNDAY -> 6
    else -> 0
}

fun recurringMealsForDay(
    meals: List<DietRecurringMeal>,
    dayIndex: Int,
): List<DietRecurringMeal> =
    meals.filter { meal ->
        meal.repeat_enabled && dayIndex in meal.repeat_days
    }

@Serializable
data class MemberStatisticsResponse(
    val user_id: String? = null,
    val period: String = "week",
    val comparison_label: String? = null,
    val range: MemberStatisticsRange? = null,
    val previous_range: MemberStatisticsRange? = null,
    val summary: MemberStatisticsSummary = MemberStatisticsSummary(),
    val monthly_goal: MemberStatisticsMonthlyGoal? = null,
    val weekly_activity: MemberStatisticsWeeklyActivity? = null,
    val attendance: MemberStatisticsAttendance? = null
)

@Serializable
data class MemberStatisticsRange(
    val from: String? = null,
    val to: String? = null,
    val label: String? = null
)

@Serializable
data class MemberStatisticsSummary(
    val total_workouts: MemberStatisticsComparableInt = MemberStatisticsComparableInt(),
    val total_duration: MemberStatisticsDurationComparable = MemberStatisticsDurationComparable(),
    val active_calories: MemberStatisticsComparableDisplay = MemberStatisticsComparableDisplay(),
    val best_streak: MemberStatisticsBestStreak = MemberStatisticsBestStreak()
)

@Serializable
data class MemberStatisticsComparableInt(
    val value: Int = 0,
    val previous_value: Int? = null,
    val percent_change: Double? = null,
    val direction: String? = null,
    val comparison_label: String? = null
)

@Serializable
data class MemberStatisticsDurationComparable(
    val value_minutes: Int = 0,
    val display: String = "0h",
    val previous_value_minutes: Int? = null,
    val percent_change: Double? = null,
    val direction: String? = null,
    val comparison_label: String? = null
)

@Serializable
data class MemberStatisticsComparableDisplay(
    val value: Int = 0,
    val display: String = "0",
    val previous_value: Int? = null,
    val percent_change: Double? = null,
    val direction: String? = null,
    val comparison_label: String? = null,
    val estimate_note: String? = null
)

@Serializable
data class MemberStatisticsBestStreak(
    val value_days: Int = 0,
    val display: String = "0d",
    val source: String? = null
)

@Serializable
data class MemberStatisticsMonthlyGoal(
    val year: Int? = null,
    val month: Int? = null,
    val target_workouts: Int = 0,
    val workout_count: Int = 0,
    val duration_minutes: Int = 0,
    val duration_formatted: String? = null,
    val percent_change_vs_previous_month: Double? = null,
    val message: String? = null
)

@Serializable
data class MemberStatisticsWeeklyActivity(
    val week_start: String? = null,
    val week_end: String? = null,
    val by_metric: MemberStatisticsByMetric = MemberStatisticsByMetric()
)

@Serializable
data class MemberStatisticsByMetric(
    val active_calories: MemberStatisticsMetricSeries = MemberStatisticsMetricSeries(),
    val volume: MemberStatisticsMetricSeries = MemberStatisticsMetricSeries(),
    val sets: MemberStatisticsMetricSeries = MemberStatisticsMetricSeries(),
    val duration: MemberStatisticsMetricSeries = MemberStatisticsMetricSeries()
)

@Serializable
data class MemberStatisticsMetricSeries(
    val unit: String? = null,
    val total: Int = 0,
    val title_example: String? = null,
    val points: List<MemberStatisticsPoint> = emptyList()
)

@Serializable
data class MemberStatisticsPoint(
    val weekday: String,
    val weekday_index: Int? = null,
    val date: String? = null,
    val minutes: Int? = null,
    val volume: Int? = null,
    val sets: Int? = null,
    val calories: Int? = null,
    val value: Int = 0
)

@Serializable
data class MemberStatisticsAttendance(
    val year: Int? = null,
    val month: Int? = null,
    val days_with_activity: List<String> = emptyList(),
    val calendar: List<CalendarDayDTO> = emptyList(),
)

fun normalizeDietUnitType(unitType: String?): String = when (unitType?.uppercase()) {
    "G", "GRAM", "GRAMS" -> "GRAM"
    "KG", "KILOGRAM", "KILOGRAMS" -> "KG"
    "LITER", "LITRE", "L", "ML" -> "LITER"
    else -> unitType?.uppercase()?.takeIf { it.isNotBlank() } ?: "KG"
}

fun formatDietFoodAmount(value: Double): String {
    if (value % 1.0 == 0.0) return value.toInt().toString()
    val scaled = round(value * 10).toInt()
    return "${scaled / 10}.${abs(scaled % 10)}"
}

fun formatDietFoodWeightShort(weightAmount: Double?, unitType: String?): String {
    if (weightAmount == null) return ""
    val suffix = when (normalizeDietUnitType(unitType)) {
        "GRAM" -> "g"
        "LITER" -> "L"
        else -> "kg"
    }
    return "${formatDietFoodAmount(weightAmount)} $suffix"
}

fun formatDietFoodWeightLong(weightAmount: Double?, unitType: String?): String {
    if (weightAmount == null) return ""
    val label = when (normalizeDietUnitType(unitType)) {
        "GRAM" -> "Gram"
        "LITER" -> "Liter"
        else -> "Kg"
    }
    return "${formatDietFoodAmount(weightAmount)} $label"
}

fun formatDietMealFoodSummary(
    weightKg: Double?,
    unitType: String?,
    calories: Int,
    quantity: Int = 1,
    protein: Double? = null,
    carbs: Double? = null,
    fat: Double? = null,
): String {
    val parts = buildList {
        formatDietFoodWeightShort(weightKg, unitType).takeIf { it.isNotBlank() }?.let { add(it) }
        add("$calories kcal")
        if (quantity > 1) add("×$quantity")
        protein?.let { add("P: ${it.toInt()}g") }
        carbs?.let { add("C: ${it.toInt()}g") }
        fat?.let { add("F: ${it.toInt()}g") }
    }
    return parts.joinToString(" · ")
}
