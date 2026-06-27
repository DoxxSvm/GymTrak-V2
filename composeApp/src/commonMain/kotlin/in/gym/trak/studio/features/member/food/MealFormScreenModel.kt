package `in`.gym.trak.studio.features.member.food

import androidx.compose.runtime.mutableStateListOf
import cafe.adriel.voyager.core.model.ScreenModel
import `in`.gym.trak.studio.data.model.DietCatalogFoodDTO
import `in`.gym.trak.studio.data.model.DietMealDTO
import `in`.gym.trak.studio.data.model.normalizeDietUnitType
import `in`.gym.trak.studio.features.member.LocalMealFood

class MealFormScreenModel(
    val initialMeal: DietMealDTO?,
    val newKeyGenerator: () -> String
) : ScreenModel {
    val localFoods = mutableStateListOf<LocalMealFood>()
    private var initialized = false

    fun init() {
        if (initialized) return
        if (initialMeal != null) {
            localFoods.addAll(
                initialMeal.food_items.map { fi ->
                    LocalMealFood(
                        key = fi.id.ifBlank { newKeyGenerator() },
                        diet_food_id = fi.diet_food_id,
                        name = fi.name,
                        quantity = fi.quantity,
                        calories = fi.calories,
                        weight_kg = fi.weight_kg,
                        protein = fi.protein,
                        carbs = fi.carbs,
                        fat = fi.fat,
                        unit_type = fi.unit_type?.let { normalizeDietUnitType(it) },
                        image_url = fi.image_url,
                    )
                }
            )
        }
        initialized = true
    }

    fun updateFoods(newFoods: List<DietCatalogFoodDTO>) {
        val currentMap = localFoods.associateBy { it.diet_food_id }

        val updatedList = newFoods.map { catalog ->
            val existing = currentMap[catalog.id]
            if (existing != null) {
                // Preserve key, update values from editor
                existing.copy(
                    name = catalog.name,
                    quantity = catalog.quantity ?: 1,
                    calories = catalog.calories ?: 0,
                    weight_kg = catalog.weight_kg,
                    protein = catalog.protein,
                    carbs = catalog.carbs,
                    fat = catalog.fat,
                    unit_type = catalog.unit_type,
                    image_url = catalog.image_url,
                )
            } else {
                // New item
                LocalMealFood(
                    key = newKeyGenerator(),
                    diet_food_id = catalog.id,
                    name = catalog.name,
                    quantity = catalog.quantity ?: 1,
                    calories = catalog.calories ?: 0,
                    weight_kg = catalog.weight_kg,
                    protein = catalog.protein,
                    carbs = catalog.carbs,
                    fat = catalog.fat,
                    unit_type = catalog.unit_type,
                    image_url = catalog.image_url,
                )
            }
        }

        localFoods.clear()
        localFoods.addAll(updatedList)
    }

    fun removeFood(key: String) {
        localFoods.removeAll { it.key == key }
    }

    fun updateQuantity(index: Int, quantity: Int) {
        if (index in localFoods.indices) {
            localFoods[index] = localFoods[index].copy(quantity = quantity)
        }
    }

    fun updateCalories(index: Int, calories: Int) {
        if (index in localFoods.indices) {
            localFoods[index] = localFoods[index].copy(calories = calories)
        }
    }
}
