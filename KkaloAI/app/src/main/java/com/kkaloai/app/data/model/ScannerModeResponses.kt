package com.kkaloai.app.data.model

/** Response shapes for the 3 advanced scanner modes (recipe / menu / fridge). */

data class RecipeScanResponse(
    val recipeName: String,
    val servings: Int,
    val ingredients: List<String>,
    val perServing: FoodRecognitionResult,
    val totalCalories: Int,
    val summary: String
)

data class MenuItem(
    val name: String,
    val estimatedCalories: Int,
    val healthScore: Int, // 1-10
    val notes: String? = null
)

data class MenuScanResponse(
    val items: List<MenuItem>,
    val summary: String
)

data class FridgeRecipeSuggestion(
    val name: String,
    val whyGoodForUser: String,
    val ingredientsAvailable: List<String>,
    val ingredientsMissing: List<String>,
    val estimatedKcalPerServing: Int,
    val briefSteps: String
)

data class FridgeScanResponse(
    val detectedItems: List<String>,
    val suggestions: List<FridgeRecipeSuggestion>,
    val summary: String
)

data class PlanMeal(
    val name: String,
    val calories: Int,
    val proteins: Float,
    val carbs: Float,
    val fats: Float,
    val timeOfDay: String  // breakfast | lunch | dinner | snack
)

data class PlanDay(
    val dayLabel: String,
    val meals: List<PlanMeal>,
    val totalKcal: Int
)

data class WeeklyMealPlanResponse(
    val days: List<PlanDay>,
    val summary: String
)
