package com.kkaloai.app.util

import com.kkaloai.app.data.local.ActivityLevel
import com.kkaloai.app.data.local.HealthGoal
import com.kkaloai.app.data.local.Sex
import kotlin.math.roundToInt

data class MacroGoals(val proteinG: Int, val carbsG: Int, val fatsG: Int)

object TdeeCalculator {

    private const val KCAL_PER_KG = 7700.0
    private const val KCAL_PER_G_PROTEIN = 4
    private const val KCAL_PER_G_CARBS = 4
    private const val KCAL_PER_G_FATS = 9

    fun calculateDailyCalories(
        sex: Sex,
        age: Int,
        heightCm: Int,
        weightKg: Float,
        activity: ActivityLevel,
        goal: HealthGoal,
        weeklyRateKg: Float
    ): Int {
        val bmr = when (sex) {
            Sex.MALE -> 10.0 * weightKg + 6.25 * heightCm - 5.0 * age + 5.0
            Sex.FEMALE -> 10.0 * weightKg + 6.25 * heightCm - 5.0 * age - 161.0
        }
        val tdee = bmr * activity.multiplier
        val dailyDelta = (weeklyRateKg * KCAL_PER_KG) / 7.0

        val target = when (goal) {
            HealthGoal.LOSS -> tdee - dailyDelta
            HealthGoal.MAINTAIN -> tdee
            HealthGoal.BULK -> tdee + dailyDelta.coerceAtLeast(250.0)
        }
        return target.roundToInt().coerceIn(1200, 4000)
    }

    /**
     * Goal-tuned macro split:
     *  - LOSS:    P 35% / C 35% / F 30% (high protein, moderate carbs)
     *  - MAINTAIN: P 25% / C 50% / F 25%
     *  - BULK:    P 30% / C 50% / F 20%
     */
    fun calculateMacroGoals(calorieGoal: Int, goal: HealthGoal): MacroGoals {
        val (pPct, cPct, fPct) = when (goal) {
            HealthGoal.LOSS -> Triple(0.35, 0.35, 0.30)
            HealthGoal.MAINTAIN -> Triple(0.25, 0.50, 0.25)
            HealthGoal.BULK -> Triple(0.30, 0.50, 0.20)
        }
        return MacroGoals(
            proteinG = ((calorieGoal * pPct) / KCAL_PER_G_PROTEIN).roundToInt(),
            carbsG   = ((calorieGoal * cPct) / KCAL_PER_G_CARBS).roundToInt(),
            fatsG    = ((calorieGoal * fPct) / KCAL_PER_G_FATS).roundToInt()
        )
    }
}
