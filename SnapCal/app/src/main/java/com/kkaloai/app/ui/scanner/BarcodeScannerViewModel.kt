package com.kkaloai.app.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kkaloai.app.data.model.FoodRecognitionResult
import com.kkaloai.app.data.model.GeminiFoodResponse
import com.kkaloai.app.data.remote.OpenFoodFactsService
import com.kkaloai.app.util.ScannerError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BarcodeScannerViewModel @Inject constructor(
    private val openFoodFactsService: OpenFoodFactsService
) : ViewModel() {

    private val _scanState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val scanState: StateFlow<ScannerUiState> = _scanState

    private var lastBarcode: String? = null

    fun onBarcodeScanned(barcode: String) {
        if (_scanState.value is ScannerUiState.Loading) return
        lastBarcode = barcode

        viewModelScope.launch {
            _scanState.value = ScannerUiState.Loading
            try {
                val response = openFoodFactsService.getProduct(barcode)
                if (response.status == 1 && response.product != null) {
                    val product = response.product
                    val result = FoodRecognitionResult(
                        name = product.product_name ?: "Unknown Product",
                        calories = (product.nutriments?.energy_100g ?: 0f).toInt(),
                        proteins = product.nutriments?.proteins_100g ?: 0f,
                        carbs = product.nutriments?.carbohydrates_100g ?: 0f,
                        fats = product.nutriments?.fat_100g ?: 0f,
                        confidence = 1.0f,
                        servingSize = "100g"
                    )
                    _scanState.value = ScannerUiState.Success(
                        GeminiFoodResponse(
                            items = listOf(result),
                            totalCalories = result.calories,
                            summary = "Barcode scan result for $barcode"
                        )
                    )
                } else {
                    _scanState.value = ScannerUiState.Error(ScannerError.NotFound)
                }
            } catch (e: Exception) {
                _scanState.value = ScannerUiState.Error(ScannerError.fromException(e))
            }
        }
    }

    fun retryLast() {
        lastBarcode?.let { onBarcodeScanned(it) } ?: resetState()
    }

    fun resetState() {
        _scanState.value = ScannerUiState.Idle
    }
}
