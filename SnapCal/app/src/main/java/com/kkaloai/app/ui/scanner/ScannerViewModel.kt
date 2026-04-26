package com.kkaloai.app.ui.scanner

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkaloai.app.data.model.GeminiFoodResponse
import com.kkaloai.app.data.remote.GeminiRepository
import com.kkaloai.app.util.AnalyticsManager
import com.kkaloai.app.util.FileLogger
import com.kkaloai.app.util.ScannerError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ScannerUiState {
    object Idle : ScannerUiState()
    object Loading : ScannerUiState()
    data class Success(val foodResponse: GeminiFoodResponse) : ScannerUiState()
    data class Error(val error: ScannerError) : ScannerUiState()
}

@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val geminiRepository: GeminiRepository,
    private val analyticsManager: AnalyticsManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState: StateFlow<ScannerUiState> = _uiState

    private val _userHint = MutableStateFlow<String?>(null)
    val userHint: StateFlow<String?> = _userHint

    private var lastImage: Bitmap? = null
    private var lastVoiceText: String? = null

    fun updateUserHint(hint: String?) {
        _userHint.value = hint
    }

    fun onImageCaptured(bitmap: Bitmap) {
        FileLogger.d("ScannerViewModel", "onImageCaptured triggered. Hint: ${_userHint.value}")
        lastImage = bitmap
        lastVoiceText = null
        viewModelScope.launch {
            _uiState.value = ScannerUiState.Loading
            val result = geminiRepository.recognizeFood(bitmap, _userHint.value)
            result.onSuccess { response ->
                FileLogger.d("ScannerViewModel", "Gemini image SUCCESS: ${response.totalCalories} kcal")
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
                FileLogger.e("ScannerViewModel", "Gemini FAILURE: ${error.message}", error)
                val mapped = ScannerError.fromException(error)
                _uiState.value = ScannerUiState.Error(mapped)
                analyticsManager.logScannerError("gemini_failure", mapped::class.simpleName ?: "Unknown")
            }
        }
    }

    fun onVoiceInput(spokenText: String) {
        if (spokenText.isBlank()) return
        FileLogger.d("ScannerViewModel", "onVoiceInput: $spokenText")
        lastVoiceText = spokenText
        lastImage = null
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
