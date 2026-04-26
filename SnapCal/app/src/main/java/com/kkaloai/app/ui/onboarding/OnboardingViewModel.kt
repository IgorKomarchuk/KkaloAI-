package com.kkaloai.app.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkaloai.app.data.billing.BillingManager
import com.android.billingclient.api.ProductDetails
import com.kkaloai.app.data.health.HealthConnectManager
import com.kkaloai.app.data.local.ActivityLevel
import com.kkaloai.app.data.local.HealthGoal
import com.kkaloai.app.data.local.Sex
import com.kkaloai.app.data.local.UserPreferences
import com.kkaloai.app.util.TdeeCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
    private val billingManager: BillingManager,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _healthPermissionsGranted = MutableStateFlow(false)
    val healthPermissionsGranted: StateFlow<Boolean> = _healthPermissionsGranted

    val healthConnectAvailability: com.kkaloai.app.data.health.HealthConnectManager.Availability
        get() = healthConnectManager.availability

    val products: StateFlow<List<ProductDetails>> = billingManager.products
    val isSubscribed: StateFlow<Boolean> = billingManager.isSubscribed

    private val _selectedGoal = MutableStateFlow(HealthGoal.LOSS)
    val selectedGoal: StateFlow<HealthGoal> = _selectedGoal

    private val _glp1 = MutableStateFlow(false)
    val glp1: StateFlow<Boolean> = _glp1

    private val _hormonal = MutableStateFlow(false)
    val hormonal: StateFlow<Boolean> = _hormonal

    private val _age = MutableStateFlow(28)
    val age: StateFlow<Int> = _age

    private val _sex = MutableStateFlow(Sex.FEMALE)
    val sex: StateFlow<Sex> = _sex

    private val _heightCm = MutableStateFlow(170)
    val heightCm: StateFlow<Int> = _heightCm

    private val _weightKg = MutableStateFlow(70f)
    val weightKg: StateFlow<Float> = _weightKg

    private val _activity = MutableStateFlow(ActivityLevel.LIGHT)
    val activity: StateFlow<ActivityLevel> = _activity

    private val _weeklyRate = MutableStateFlow(0.5f)
    val weeklyRate: StateFlow<Float> = _weeklyRate

    val computedCalories: Int
        get() = TdeeCalculator.calculateDailyCalories(
            sex = _sex.value,
            age = _age.value,
            heightCm = _heightCm.value,
            weightKg = _weightKg.value,
            activity = _activity.value,
            goal = _selectedGoal.value,
            weeklyRateKg = _weeklyRate.value
        )

    init {
        checkHealthPermissions()
    }

    fun checkHealthPermissions() {
        viewModelScope.launch {
            _healthPermissionsGranted.value = healthConnectManager.hasAllPermissions()
        }
    }

    val healthPermissions = healthConnectManager.permissions

    fun selectGoal(goal: HealthGoal) { _selectedGoal.value = goal }
    fun toggleGlp1(value: Boolean) { _glp1.value = value }
    fun toggleHormonal(value: Boolean) { _hormonal.value = value }

    fun setAge(value: Int) { _age.value = value.coerceIn(13, 100) }
    fun setSex(value: Sex) { _sex.value = value }
    fun setHeight(value: Int) { _heightCm.value = value.coerceIn(120, 230) }
    fun setWeight(value: Float) { _weightKg.value = value.coerceIn(35f, 250f) }
    fun setActivity(value: ActivityLevel) { _activity.value = value }
    fun setWeeklyRate(value: Float) { _weeklyRate.value = value.coerceIn(0.25f, 1f) }

    fun persistChoices() {
        val finalCalories = computedCalories
        viewModelScope.launch {
            userPreferences.setHealthGoal(_selectedGoal.value)
            userPreferences.setGlp1Mode(_glp1.value)
            userPreferences.setHormonalMode(_hormonal.value)
            userPreferences.setProfile(
                age = _age.value,
                sex = _sex.value,
                heightCm = _heightCm.value,
                weightKg = _weightKg.value,
                activity = _activity.value,
                weeklyRate = _weeklyRate.value
            )
            userPreferences.setCalorieGoal(finalCalories)
            userPreferences.setOnboardingComplete(true)
        }
    }
}
