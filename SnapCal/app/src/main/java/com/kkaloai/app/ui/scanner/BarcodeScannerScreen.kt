package com.kkaloai.app.ui.scanner

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.kkaloai.app.R
import com.kkaloai.app.data.model.GeminiFoodResponse

@Composable
fun BarcodeScannerScreen(
    onResultFound: (GeminiFoodResponse) -> Unit,
    onBack: () -> Unit,
    viewModel: BarcodeScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scanState by viewModel.scanState.collectAsState()
    var launched by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!launched) {
            launched = true
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                    Barcode.FORMAT_EAN_13,
                    Barcode.FORMAT_EAN_8,
                    Barcode.FORMAT_UPC_A,
                    Barcode.FORMAT_UPC_E
                )
                .build()
            GmsBarcodeScanning.getClient(context, options).startScan()
                .addOnSuccessListener { barcode ->
                    val raw = barcode.rawValue
                    if (raw.isNullOrBlank()) onBack() else viewModel.onBarcodeScanned(raw)
                }
                .addOnFailureListener { onBack() }
                .addOnCanceledListener { onBack() }
        }
    }

    LaunchedEffect(scanState) {
        val s = scanState
        if (s is ScannerUiState.Success) {
            onResultFound(s.foodResponse)
            viewModel.resetState()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (val s = scanState) {
            is ScannerUiState.Loading -> CircularProgressIndicator()
            is ScannerUiState.Error -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = stringResource(s.error.messageRes),
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(16.dp))
                    Row {
                        OutlinedButton(onClick = onBack) {
                            Text(stringResource(R.string.common_back))
                        }
                        Spacer(Modifier.width(12.dp))
                        Button(onClick = { viewModel.retryLast() }) {
                            Text(stringResource(R.string.err_retry))
                        }
                    }
                }
            }
            else -> CircularProgressIndicator()
        }
    }
}
