package com.kkaloai.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkaloai.app.data.health.HealthConnectManager
import com.kkaloai.app.data.local.BiofeedbackDao
import com.kkaloai.app.data.local.BiofeedbackEntry
import com.kkaloai.app.data.local.HealthGoal
import com.kkaloai.app.data.local.MealDao
import com.kkaloai.app.data.local.MealEntry
import com.kkaloai.app.data.local.UserPreferences
import com.kkaloai.app.data.local.WaterDao
import com.kkaloai.app.data.local.WaterEntry
import com.kkaloai.app.util.TdeeCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import javax.inject.Inject

data class DashboardState(
    val meals: List<MealEntry> = emptyList(),
    val plannedMeals: List<MealEntry> = emptyList(),
    val totalCalories: Int = 0,
    val calorieGoal: Int = UserPreferences.DEFAULT_CALORIE_GOAL,
    val totalProtein: Float = 0f,
    val totalCarbs: Float = 0f,
    val totalFats: Float = 0f,
    val proteinGoalG: Int = 100,
    val carbsGoalG: Int = 250,
    val fatsGoalG: Int = 65,
    val isGlp1Mode: Boolean = false,
    val steps: Long = 0,
    val burnedCalories: Int = 0,
    val currentWeightKg: Float? = null,
    val currentStreak: Int = 0,
    val waterMl: Int = 0,
    val waterGoalMl: Int = UserPreferences.DEFAULT_WATER_GOAL_ML,
    val useImperial: Boolean = false,
    val todayBiofeedback: BiofeedbackEntry? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val mealDao: MealDao,
    private val waterDao: WaterDao,
    private val biofeedbackDao: BiofeedbackDao,
    private val userPreferences: UserPreferences,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val startOfDay: Long
        get() = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private val _biofeedbackHC = MutableStateFlow(Triple(0L, 0, null as Float?))

    val uiState: StateFlow<DashboardState> = combine(
        listOf(
            mealDao.getMealsForDay(startOfDay),
            mealDao.getPlannedForDay(startOfDay),
            userPreferences.calorieGoal,
            userPreferences.isGlp1Mode,
            userPreferences.currentStreak,
            _biofeedbackHC,
            waterDao.getTotalForDay(startOfDay),
            userPreferences.waterGoalMl,
            userPreferences.useImperial,
            biofeedbackDao.getLatestForDay(startOfDay),
            userPreferences.healthGoal
        )
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val meals = values[0] as List<MealEntry>
        @Suppress("UNCHECKED_CAST")
        val planned = values[1] as List<MealEntry>
        val goal = values[2] as Int
        val isGlp1 = values[3] as Boolean
        val streak = values[4] as Int
        @Suppress("UNCHECKED_CAST")
        val bio = values[5] as Triple<Long, Int, Float?>
        val water = values[6] as Int
        val waterGoal = values[7] as Int
        val imperial = values[8] as Boolean
        val biofeedback = values[9] as BiofeedbackEntry?
        val healthGoal = values[10] as HealthGoal
        val macros = TdeeCalculator.calculateMacroGoals(goal, healthGoal)
        DashboardState(
            meals = meals,
            plannedMeals = planned,
            totalCalories = meals.sumOf { it.calories },
            calorieGoal = goal,
            totalProtein = meals.sumOf { it.proteins.toDouble() }.toFloat(),
            totalCarbs = meals.sumOf { it.carbs.toDouble() }.toFloat(),
            totalFats = meals.sumOf { it.fats.toDouble() }.toFloat(),
            proteinGoalG = macros.proteinG,
            carbsGoalG = macros.carbsG,
            fatsGoalG = macros.fatsG,
            isGlp1Mode = isGlp1,
            steps = bio.first,
            burnedCalories = bio.second,
            currentWeightKg = bio.third,
            currentStreak = streak,
            waterMl = water,
            waterGoalMl = waterGoal,
            useImperial = imperial,
            todayBiofeedback = biofeedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardState()
    )

    init {
        refreshBiofeedback()
    }

    fun refreshBiofeedback() {
        viewModelScope.launch {
            if (healthConnectManager.hasAllPermissions()) {
                val now = Instant.now()
                val start = Instant.now().truncatedTo(ChronoUnit.DAYS)

                val steps = healthConnectManager.readSteps(start, now)
                val burns = healthConnectManager.readCaloriesBurned(start, now).toInt()
                val weight = healthConnectManager.readLatestWeight()?.toFloat()

                _biofeedbackHC.value = Triple(steps, burns, weight)
            }
        }
    }

    fun addWater(amountMl: Int) {
        viewModelScope.launch {
            waterDao.insert(WaterEntry(amountMl = amountMl))
            runCatching {
                if (healthConnectManager.hasAllPermissions()) {
                    healthConnectManager.writeWater(amountMl)
                }
            }
        }
    }

    fun toggleFavorite(mealId: Int, favorite: Boolean) {
        viewModelScope.launch {
            mealDao.setFavorite(mealId, favorite)
        }
    }

    fun markPlannedEaten(mealId: Int) {
        viewModelScope.launch {
            mealDao.markPlannedEaten(mealId, System.currentTimeMillis())
            userPreferences.updateStreak()
        }
    }

    fun saveDailyCheckin(energy: Int, mood: String, symptoms: Set<String>) {
        viewModelScope.launch {
            biofeedbackDao.insert(
                BiofeedbackEntry(
                    energyLevel = energy,
                    mood = mood,
                    symptoms = symptoms.joinToString(",")
                )
            )
        }
    }
}
