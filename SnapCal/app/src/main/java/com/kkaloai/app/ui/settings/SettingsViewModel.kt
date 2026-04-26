package com.kkaloai.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkaloai.app.data.local.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    val calorieGoal: StateFlow<Int> = userPreferences.calorieGoal.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences.DEFAULT_CALORIE_GOAL
    )

    val isGlp1Mode: StateFlow<Boolean> = userPreferences.isGlp1Mode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val useImperial: StateFlow<Boolean> = userPreferences.useImperial.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun setCalorieGoal(value: Int) {
        viewModelScope.launch { userPreferences.setCalorieGoal(value) }
    }

    fun setGlp1Mode(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setGlp1Mode(enabled) }
    }

    fun setUseImperial(enabled: Boolean) {
        viewModelScope.launch { userPreferences.setUseImperial(enabled) }
    }
}
