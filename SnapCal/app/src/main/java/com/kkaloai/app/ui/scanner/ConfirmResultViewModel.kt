package com.kkaloai.app.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkaloai.app.data.health.HealthConnectManager
import com.kkaloai.app.data.local.MealDao
import com.kkaloai.app.data.local.MealEntry
import com.kkaloai.app.data.model.GeminiFoodResponse
import com.kkaloai.app.util.FileLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

import com.kkaloai.app.data.auth.AuthManager
import com.kkaloai.app.data.local.UserPreferences
import com.kkaloai.app.data.remote.FirestoreRepository
import com.kkaloai.app.util.AnalyticsManager

@HiltViewModel
class ConfirmResultViewModel @Inject constructor(
    private val mealDao: MealDao,
    private val healthConnectManager: HealthConnectManager,
    private val authManager: AuthManager,
    private val firestoreRepository: FirestoreRepository,
    private val analyticsManager: AnalyticsManager,
    private val userPreferences: UserPreferences
) : ViewModel() {

    fun saveMeal(response: GeminiFoodResponse, onDone: () -> Unit) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            response.items.forEach { item ->
                val entry = MealEntry(
                    name = item.name,
                    calories = item.calories,
                    proteins = item.proteins,
                    carbs = item.carbs,
                    fats = item.fats,
                    timestamp = now
                )
                mealDao.insertMeal(entry)
                
                // Sync to Cloud
                authManager.getUserId()?.let { userId ->
                    firestoreRepository.syncMeal(userId, entry)
                }
            }
            userPreferences.updateStreak()
            FileLogger.d("ConfirmResultViewModel", "Saved ${response.items.size} meal(s), total ${response.totalCalories} kcal and synced to cloud")

            // Log to Analytics
            analyticsManager.logMealSaved(response.totalCalories, response.items.size)

            runCatching {
                if (healthConnectManager.hasAllPermissions()) {
                    val startInstant = Instant.ofEpochMilli(now)
                    response.items.forEach { item ->
                        healthConnectManager.writeMeal(
                            name = item.name,
                            calories = item.calories,
                            proteins = item.proteins,
                            carbs = item.carbs,
                            fats = item.fats,
                            startTime = startInstant
                        )
                    }
                    FileLogger.d("ConfirmResultViewModel", "Health Connect sync OK")
                } else {
                    FileLogger.d("ConfirmResultViewModel", "Health Connect permission missing, skipping sync")
                }
            }.onFailure { FileLogger.e("ConfirmResultViewModel", "Health Connect write failed: ${it.message}", it) }

            onDone()
        }
    }
}
