package com.kkaloai.app.data.model

data class WeeklyReport(
    val summary: String,
    val insights: List<String>,
    val recommendations: List<String>,
    val personalizedRecipes: List<PersonalizedRecipe>
)

data class PersonalizedRecipe(
    val name: String,
    val reason: String,
    val macros: String,
    val briefInstructions: String
)
