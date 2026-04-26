package com.kkaloai.app.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkaloai.app.data.health.HealthConnectManager
import com.kkaloai.app.data.local.MealDao
import com.kkaloai.app.data.local.MealEntry
import com.kkaloai.app.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val mealDao: MealDao,
    private val userPreferences: UserPreferences,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    val favorites: StateFlow<List<MealEntry>> = mealDao.getFavorites().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val useImperial: StateFlow<Boolean> = userPreferences.useImperial.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun relogMeal(meal: MealEntry) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            mealDao.insertMeal(
                meal.copy(id = 0, timestamp = now, isFavorite = true)
            )
            userPreferences.updateStreak()
            runCatching {
                if (healthConnectManager.hasAllPermissions()) {
                    healthConnectManager.writeMeal(
                        name = meal.name,
                        calories = meal.calories,
                        proteins = meal.proteins,
                        carbs = meal.carbs,
                        fats = meal.fats,
                        startTime = Instant.ofEpochMilli(now)
                    )
                }
            }
        }
    }

    fun unfavorite(id: Int) {
        viewModelScope.launch { mealDao.setFavorite(id, false) }
    }
}
