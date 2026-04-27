package com.kkaloai.app.ui.scanner

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkaloai.app.data.model.GeminiFoodResponse
import com.kkaloai.app.data.remote.GeminiRepository
import com.kkaloai.app.util.AchievementEngine
import com.kkaloai.app.util.AnalyticsManager
import com.kkaloai.app.util.FileLogger
import com.kkaloai.app.util.ScannerError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ScannerMode { FOOD, RECIPE, MENU, FRIDGE }

sealed class ScannerUiState {
    object Idle : ScannerUiState()
    object Loading : ScannerUiState()
    data class Success(val foodResponse: GeminiFoodResponse) : ScannerUiState()
    data class RecipeSuccess(val response: com.kkaloai.app.data.model.RecipeScanResponse) : ScannerUiState()
    data class MenuSuccess(val response: com.kkaloai.app.data.model.MenuScanResponse) : ScannerUiState()
    data class FridgeSuccess(val response: com.kkaloai.app.data.model.FridgeScanResponse) : ScannerUiState()
    data class Error(val error: ScannerError) : ScannerUiState()
}

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val geminiRepository: GeminiRepository,
    private val analyticsManager: AnalyticsManager,
    private val achievementEngine: AchievementEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState: StateFlow<ScannerUiState> = _uiState

    private val _userHint = MutableStateFlow<String?>(null)
    val userHint: StateFlow<String?> = _userHint

    private val _mode = MutableStateFlow(ScannerMode.FOOD)
    val mode: StateFlow<ScannerMode> = _mode

    private var lastImage: Bitmap? = null
    private var lastVoiceText: String? = null

    fun setMode(m: ScannerMode) { _mode.value = m }

    fun updateUserHint(hint: String?) {
        _userHint.value = hint
    }

    fun onImageCaptured(bitmap: Bitmap) {
        FileLogger.d("ScannerViewModel", "onImageCaptured mode=${_mode.value} hint=${_userHint.value}")
        lastImage = bitmap
        lastVoiceText = null
        viewModelScope.launch {
            _uiState.value = ScannerUiState.Loading
            val outcome = when (_mode.value) {
                ScannerMode.FOOD -> geminiRepository.recognizeFood(bitmap, _userHint.value).map { it as Any }
                ScannerMode.RECIPE -> geminiRepository.recognizeRecipe(bitmap).map { it as Any }
                ScannerMode.MENU -> geminiRepository.recognizeMenu(bitmap).map { it as Any }
                ScannerMode.FRIDGE -> geminiRepository.suggestFromFridge(bitmap).map { it as Any }
            }
            outcome.onSuccess { response ->
                _uiState.value = when (response) {
                    is GeminiFoodResponse -> {
                        response.items.firstOrNull()?.let { item ->
                            analyticsManager.logFoodRecognized(item.name, item.calories, item.proteins, item.carbs, item.fats)
                        }
                        ScannerUiState.Success(response)
                    }
                    is com.kkaloai.app.data.model.RecipeScanResponse -> ScannerUiState.RecipeSuccess(response)
                    is com.kkaloai.app.data.model.MenuScanResponse -> ScannerUiState.MenuSuccess(response)
                    is com.kkaloai.app.data.model.FridgeScanResponse -> ScannerUiState.FridgeSuccess(response)
                    else -> ScannerUiState.Error(ScannerError.Unknown)
                }
            }.onFailure { error ->
                FileLogger.e("ScannerViewModel", "Scan FAILURE: ${error.message}", error)
                val mapped = ScannerError.fromException(error)
                _uiState.value = ScannerUiState.Error(mapped)
                analyticsManager.logScannerError("scan_failure_${_mode.value}", mapped::class.simpleName ?: "Unknown")
            }
        }
    }

    fun onVoiceInput(spokenText: String) {
        if (spokenText.isBlank()) return
        FileLogger.d("ScannerViewModel", "onVoiceInput: $spokenText")
        lastVoiceText = spokenText
        lastImage = null
        achievementEngine.onVoiceScan()
        viewModelScope.launch {
            _uiState.value = ScannerUiState.Loading
            val result = geminiRepository.recognizeFoodFromText(spokenText)
            result.onSuccess { response ->
                _uiState.value = ScannerUiState.Success(response)
                response.items.firstOrNull()?.let { item ->
                    analyticsManager.logFoodRecognized(
                        foodName = item.name,
                        calories = item.calories,
                        proteins = item.proteins,
                        carbs = item.carbs,
                        fats = item.fats
                    )
                }
            }.onFailure { error ->
                FileLogger.e("ScannerViewModel", "Voice parse FAILURE: ${error.message}", error)
                val mapped = ScannerError.fromException(error)
                _uiState.value = ScannerUiState.Error(mapped)
                analyticsManager.logScannerError("voice_parse_failure", mapped::class.simpleName ?: "Unknown")
            }
        }
    }

    fun retryLast() {
        lastImage?.let { onImageCaptured(it); return }
        lastVoiceText?.let { onVoiceInput(it); return }
        resetState()
    }

    fun resetState() {
        _uiState.value = ScannerUiState.Idle
    }
}
