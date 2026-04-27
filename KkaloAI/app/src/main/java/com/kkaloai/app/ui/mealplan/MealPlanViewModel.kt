package com.kkaloai.app.ui.mealplan

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkaloai.app.data.local.MealDao
import com.kkaloai.app.data.local.MealEntry
import com.kkaloai.app.data.model.WeeklyMealPlanResponse
import com.kkaloai.app.data.remote.GeminiRepository
import com.kkaloai.app.notif.PlannedMealNotifier
import com.kkaloai.app.util.PdfExporter
import com.kkaloai.app.util.ScannerError
import com.kkaloai.app.util.SharingUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

sealed class MealPlanUiState {
    object Idle : MealPlanUiState()
    object Loading : MealPlanUiState()
    data class Loaded(val plan: WeeklyMealPlanResponse) : MealPlanUiState()
    data class Error(val error: ScannerError) : MealPlanUiState()
}

@HiltViewModel
class MealPlanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val geminiRepository: GeminiRepository,
    private val mealDao: MealDao
) : ViewModel() {

    private val _state = MutableStateFlow<MealPlanUiState>(MealPlanUiState.Idle)
    val state: StateFlow<MealPlanUiState> = _state

    fun generate() {
        _state.value = MealPlanUiState.Loading
        viewModelScope.launch {
            geminiRepository.generateWeeklyMealPlan()
                .onSuccess { _state.value = MealPlanUiState.Loaded(it) }
                .onFailure { _state.value = MealPlanUiState.Error(ScannerError.fromException(it)) }
        }
    }

    fun saveToDashboardAndSchedule() {
        val current = (_state.value as? MealPlanUiState.Loaded)?.plan ?: return
        viewModelScope.launch {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 8); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            current.days.forEachIndexed { dayIdx, day ->
                day.meals.forEachIndexed { mealIdx, meal ->
                    val ts = cal.timeInMillis + dayIdx * 86_400_000L + mealHourOffset(meal.timeOfDay) * 3_600_000L
                    val entry = MealEntry(
                        name = meal.name,
                        calories = meal.calories,
                        proteins = meal.proteins,
                        carbs = meal.carbs,
                        fats = meal.fats,
                        timestamp = ts,
                        isPlanned = true
                    )
                    mealDao.insertMeal(entry)
                    PlannedMealNotifier.schedule(
                        context, (ts / 1000).toInt(), meal.name, ts
                    )
                }
            }
        }
    }

    private fun mealHourOffset(timeOfDay: String): Int = when (timeOfDay.lowercase()) {
        "breakfast" -> 0
        "lunch" -> 5
        "snack" -> 8
        "dinner" -> 11
        else -> 0
    }

    fun exportPdf() {
        val current = (_state.value as? MealPlanUiState.Loaded)?.plan ?: return
        viewModelScope.launch {
            val uri = PdfExporter.exportWeeklyPlan(context, current)
            SharingUtils.sharePdf(context, uri, "My weekly KkaloAI plan 📄")
        }
    }
}
