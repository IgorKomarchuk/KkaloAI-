package com.kkaloai.app.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkaloai.app.data.local.MealDao
import com.kkaloai.app.data.model.WeeklyReport
import com.kkaloai.app.data.remote.GeminiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import com.google.gson.Gson
import com.kkaloai.app.data.local.UserPreferences

sealed class WeeklyReportState {
    object Idle : WeeklyReportState()
    object Loading : WeeklyReportState()
    object Empty : WeeklyReportState()
    data class Success(val report: WeeklyReport) : WeeklyReportState()
    data class Error(val message: String) : WeeklyReportState()
}

@HiltViewModel
class WeeklyReportViewModel @Inject constructor(
    private val mealDao: MealDao,
    private val userPreferences: UserPreferences,
    private val gson: Gson
) : ViewModel() {

    private val _uiState = MutableStateFlow<WeeklyReportState>(WeeklyReportState.Idle)
    val uiState: StateFlow<WeeklyReportState> = _uiState

    fun generateReport() {
        viewModelScope.launch {
            _uiState.value = WeeklyReportState.Loading
            
            val json = userPreferences.latestWeeklyReportJson.first()
            if (json.isNullOrEmpty()) {
                _uiState.value = WeeklyReportState.Empty
                return@launch
            }
            
            try {
                val report = gson.fromJson(json, WeeklyReport::class.java)
                _uiState.value = WeeklyReportState.Success(report)
            } catch (e: Exception) {
                _uiState.value = WeeklyReportState.Error("Failed to parse report.")
            }
        }
    }
}
