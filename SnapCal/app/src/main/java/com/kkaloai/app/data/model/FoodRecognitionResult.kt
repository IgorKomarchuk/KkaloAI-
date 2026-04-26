package com.kkaloai.app.data.model

data class FoodRecognitionResult(
    val id: String = "",
    val name: String,
    val calories: Int,
    val proteins: Float,
    val carbs: Float,
    val fats: Float,
    val confidence: Float,
    val cookingMethod: String? = null,
    val servingSize: String = "1 serving",
    val ingredients: List<String> = emptyList(),
    val volumeCm3: Float? = null
)

data class GeminiFoodResponse(
    val items: List<FoodRecognitionResult>,
    val totalCalories: Int,
    val totalVolumeCm3: Float? = null,
    val summary: String
)
