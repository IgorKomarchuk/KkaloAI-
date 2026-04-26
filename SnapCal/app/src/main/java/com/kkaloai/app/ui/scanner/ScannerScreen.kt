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

        if (uiState is ScannerUiState.Error) {
            ErrorDialog(
                error = (uiState as ScannerUiState.Error).error,
                onRetry = { viewModel.retryLast() },
                onDismiss = { viewModel.resetState() }
            )
        }
    }
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
