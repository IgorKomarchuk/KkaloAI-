package com.kkaloai.app.ui.scanner

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Matrix
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import android.widget.Toast
import com.kkaloai.app.R

@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = hiltViewModel(),
    onResultFound: (com.kkaloai.app.data.model.GeminiFoodResponse) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val userHint by viewModel.userHint.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasCameraPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    val controller = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(LifecycleCameraController.IMAGE_CAPTURE)
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    LaunchedEffect(hasCameraPermission, controller, lifecycleOwner) {
        if (hasCameraPermission) {
            controller.bindToLifecycle(lifecycleOwner)
        } else {
            controller.unbind()
        }
    }

    val voiceEmptyMsg = stringResource(R.string.scan_voice_empty)
    val voiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.takeIf { it.isNotBlank() }
            if (spoken == null) {
                Toast.makeText(context, voiceEmptyMsg, Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }
            viewModel.onVoiceInput(spoken)
        }
    }

    val mode by viewModel.mode.collectAsState()

    LaunchedEffect(uiState) {
        if (uiState is ScannerUiState.Success) {
            onResultFound((uiState as ScannerUiState.Success).foodResponse)
            viewModel.resetState()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            AndroidView(
                factory = {
                    PreviewView(it).apply {
                        this.controller = controller
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text("Camera permission required", color = Color.White)
            }
        }

        ScannerModeBar(
            current = mode,
            onSelect = { viewModel.setMode(it) },
            modifier = Modifier.align(Alignment.TopCenter)
        )

        ScannerHud(
            isLoading = uiState is ScannerUiState.Loading,
            userHint = userHint ?: "",
            onHintChange = { viewModel.updateUserHint(it) },
            onCaptureClick = {
                capturePhoto(controller, context) { bitmap ->
                    viewModel.onImageCaptured(bitmap)
                }
            },
            onVoiceClick = {
                val voicePrompt = context.getString(R.string.scan_voice_prompt)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_PROMPT, voicePrompt)
                }
                val launched = runCatching { voiceLauncher.launch(intent) }
                if (launched.isFailure) {
                    Toast.makeText(context, R.string.scan_voice_unavailable, Toast.LENGTH_SHORT).show()
                }
            }
        )

        when (val s = uiState) {
            is ScannerUiState.Error -> ErrorDialog(
                error = s.error,
                onRetry = { viewModel.retryLast() },
                onDismiss = { viewModel.resetState() }
            )
            is ScannerUiState.RecipeSuccess -> RecipeResultSheet(
                response = s.response,
                onDismiss = { viewModel.resetState() }
            )
            is ScannerUiState.MenuSuccess -> MenuResultSheet(
                response = s.response,
                onDismiss = { viewModel.resetState() }
            )
            is ScannerUiState.FridgeSuccess -> FridgeResultSheet(
                response = s.response,
                onDismiss = { viewModel.resetState() }
            )
            else -> Unit
        }
    }
}

@Composable
private fun ScannerModeBar(
    current: ScannerMode,
    onSelect: (ScannerMode) -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.foundation.layout.Row(
        modifier = modifier
            .padding(top = 36.dp)
            .background(
                Color.Black.copy(alpha = 0.45f),
                androidx.compose.foundation.shape.RoundedCornerShape(50)
            )
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ScannerMode.values().forEach { m ->
            val selected = m == current
            androidx.compose.material3.Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                color = if (selected) Color(0xFFD4AF37) else Color.Transparent,
                modifier = Modifier
                    .padding(2.dp)
                    .clickable { onSelect(m) }
            ) {
                Text(
                    text = m.name,
                    color = if (selected) Color.Black else Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
private fun RecipeResultSheet(
    response: com.kkaloai.app.data.model.RecipeScanResponse,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(response.recipeName) },
        text = {
            androidx.compose.foundation.layout.Column {
                Text("Servings: ${response.servings} • ${response.totalCalories} kcal")
                Spacer(Modifier.height(8.dp))
                Text("Per serving: ${response.perServing.calories} kcal • P ${response.perServing.proteins.toInt()}g • C ${response.perServing.carbs.toInt()}g • F ${response.perServing.fats.toInt()}g")
                Spacer(Modifier.height(8.dp))
                Text(response.summary, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("OK") }
        }
    )
}

@Composable
private fun MenuResultSheet(
    response: com.kkaloai.app.data.model.MenuScanResponse,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Menu (${response.items.size} dishes)") },
        text = {
            androidx.compose.foundation.lazy.LazyColumn {
                androidx.compose.foundation.lazy.items(response.items) { item ->
                    val color = when {
                        item.healthScore >= 8 -> Color(0xFF2ECC71)
                        item.healthScore >= 5 -> Color(0xFFF1C40F)
                        else -> Color(0xFFE74C3C)
                    }
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.padding(vertical = 4.dp),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.size(10.dp).background(color, androidx.compose.foundation.shape.RoundedCornerShape(50))
                        )
                        Spacer(Modifier.width(8.dp))
                        androidx.compose.foundation.layout.Column(Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.bodyMedium)
                            Text("${item.estimatedCalories} kcal • score ${item.healthScore}/10", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun FridgeResultSheet(
    response: com.kkaloai.app.data.model.FridgeScanResponse,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Fridge (${response.suggestions.size} ideas)") },
        text = {
            androidx.compose.foundation.lazy.LazyColumn {
                androidx.compose.foundation.lazy.items(response.suggestions) { s ->
                    androidx.compose.foundation.layout.Column(Modifier.padding(vertical = 6.dp)) {
                        Text(s.name, style = MaterialTheme.typography.titleSmall)
                        Text("${s.estimatedKcalPerServing} kcal · ${s.whyGoodForUser}", style = MaterialTheme.typography.labelSmall)
                        if (s.ingredientsMissing.isNotEmpty()) {
                            Text("Missing: ${s.ingredientsMissing.joinToString()}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun ScannerHud(
    isLoading: Boolean,
    userHint: String,
    onHintChange: (String) -> Unit,
    onCaptureClick: () -> Unit,
    onVoiceClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (!isLoading) {
            TextField(
                value = userHint,
                onValueChange = onHintChange,
                placeholder = { Text(stringResource(R.string.scan_hint_placeholder), color = Color.White.copy(alpha = 0.6f)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                shape = MaterialTheme.shapes.medium
            )
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(bottom = 20.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    IconButton(
                        onClick = onVoiceClick,
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = stringResource(R.string.scan_voice_cd),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(
                        onClick = onCaptureClick,
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = stringResource(R.string.scan_capture_cd),
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorDialog(
    error: com.kkaloai.app.util.ScannerError,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.err_title)) },
        text = { Text(stringResource(error.messageRes)) },
        confirmButton = {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.err_retry))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.err_dismiss))
            }
        }
    )
}

private fun capturePhoto(
    controller: LifecycleCameraController,
    context: android.content.Context,
    onBitmapCaptured: (Bitmap) -> Unit
) {
    val mainExecutor = ContextCompat.getMainExecutor(context)
    controller.takePicture(
        mainExecutor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = image.toBitmap()
                val rotatedBitmap = rotateBitmap(bitmap, image.imageInfo.rotationDegrees)
                onBitmapCaptured(rotatedBitmap)
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                Log.e("ScannerScreen", "Capture failed", exception)
            }
        }
    )
}

private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
    val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}
