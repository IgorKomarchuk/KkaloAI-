package com.kkaloai.app.ui.weight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkaloai.app.data.health.HealthConnectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeightEntryViewModel @Inject constructor(
    private val healthConnectManager: HealthConnectManager,
    private val achievementEngine: com.kkaloai.app.util.AchievementEngine
) : ViewModel() {

    private val _latestWeight = MutableStateFlow<Double?>(null)
    val latestWeight: StateFlow<Double?> = _latestWeight

    private val _status = MutableStateFlow<Status>(Status.Idle)
    val status: StateFlow<Status> = _status

    init {
        refreshLatest()
    }

    fun refreshLatest() {
        viewModelScope.launch {
            runCatching { healthConnectManager.readLatestWeight() }
                .onSuccess { _latestWeight.value = it }
        }
    }

    fun saveWeight(weightKg: Double) {
        viewModelScope.launch {
            _status.value = Status.Saving
            val result = runCatching {
                if (!healthConnectManager.hasAllPermissions()) {
                    throw IllegalStateException("Health Connect permission not granted")
                }
                healthConnectManager.writeWeight(weightKg)
            }
            _status.value = result.fold(
                onSuccess = {
                    _latestWeight.value = weightKg
                    achievementEngine.onWeightLogged()
                    Status.Saved
                },
                onFailure = { Status.Error(it.message ?: "Failed to save") }
            )
        }
    }

    fun clearStatus() { _status.value = Status.Idle }

    sealed class Status {
        object Idle : Status()
        object Saving : Status()
        object Saved : Status()
        data class Error(val message: String) : Status()
    }
}
