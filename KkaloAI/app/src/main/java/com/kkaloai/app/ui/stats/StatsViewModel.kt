package com.kkaloai.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkaloai.app.data.health.HealthConnectManager
import com.kkaloai.app.data.local.MealDao
import com.kkaloai.app.data.local.MealEntry
import com.kkaloai.app.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Calendar
import javax.inject.Inject

data class DailyKcal(val day: LocalDate, val kcal: Int)
data class WeightPoint(val day: LocalDate, val kg: Float)
data class MacroSplit(val proteinG: Int, val carbsG: Int, val fatsG: Int)

data class StatsState(
    val isLoading: Boolean = true,
    val rangeDays: Int = 30,
    val avgKcal: Int = 0,
    val calorieGoal: Int = UserPreferences.DEFAULT_CALORIE_GOAL,
    val dailyKcal: List<DailyKcal> = emptyList(),
    val weightHistory: List<WeightPoint> = emptyList(),
    val macroSplit: MacroSplit = MacroSplit(0, 0, 0),
    val hasNoData: Boolean = false
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val mealDao: MealDao,
    private val userPreferences: UserPreferences,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val _state = MutableStateFlow(StatsState())
    val state: StateFlow<StatsState> = _state

    init {
        loadRange(30)
    }

    fun loadRange(days: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, rangeDays = days)
            val now = System.currentTimeMillis()
            val start = Calendar.getInstance().apply {
                timeInMillis = now
                add(Calendar.DAY_OF_YEAR, -days)
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val meals = mealDao.getRangeForStats(start, now)
            val dailyKcal = bucketDaily(meals, days, now)
            val avg = if (meals.isEmpty()) 0 else meals.sumOf { it.calories } / days
            val macros = MacroSplit(
                proteinG = meals.sumOf { it.proteins.toDouble() }.toInt(),
                carbsG = meals.sumOf { it.carbs.toDouble() }.toInt(),
                fatsG = meals.sumOf { it.fats.toDouble() }.toInt()
            )

            val weightHistory = readWeightHistory(days)
            val goal = userPreferences.calorieGoal.first()

            _state.value = StatsState(
                isLoading = false,
                rangeDays = days,
                avgKcal = avg,
                calorieGoal = goal,
                dailyKcal = dailyKcal,
                weightHistory = weightHistory,
                macroSplit = macros,
                hasNoData = meals.isEmpty() && weightHistory.isEmpty()
            )
        }
    }

    private fun bucketDaily(meals: List<MealEntry>, days: Int, nowMs: Long): List<DailyKcal> {
        val zone = ZoneId.systemDefault()
        val end = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val byDay = meals.groupBy {
            Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate()
        }.mapValues { (_, entries) -> entries.sumOf { it.calories } }

        return (0 until days).map { offset ->
            val day = end.minusDays((days - 1 - offset).toLong())
            DailyKcal(day, byDay[day] ?: 0)
        }
    }

    private suspend fun readWeightHistory(days: Int): List<WeightPoint> {
        if (!healthConnectManager.isAvailable || !healthConnectManager.hasAllPermissions()) {
            return emptyList()
        }
        return runCatching {
            val end = Instant.now()
            val start = end.minus(days.toLong(), ChronoUnit.DAYS)
            healthConnectManager.readWeightSeries(start, end)
        }.getOrDefault(emptyList())
    }
}
