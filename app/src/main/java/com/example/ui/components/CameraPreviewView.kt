package com.example.ui.components

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.GestureType
import com.example.vision.HandData
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable
fun CameraPreviewView(
    handData: HandData,
    activeGesture: GestureType?,
    onFrame: (androidx.camera.core.ImageProxy) -> Unit,
    onCameraReady: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var cameraError by remember { mutableStateOf<String?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val isAnalyzing = remember { AtomicBoolean(false) }
    var cameraProviderRef by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            try {
                cameraProviderRef?.unbindAll()
            } catch (e: Exception) {
                Log.e("CameraPreviewView", "Error unbinding camera on dispose: ${e.message}")
            }
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        cameraProviderRef = cameraProvider

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also { analysis ->
                                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    if (!isAnalyzing.compareAndSet(false, true)) {
                                        try {
                                            imageProxy.close()
                                        } catch (_: Throwable) {
                                        }
                                        return@setAnalyzer
                                    }
                                    try {
                                        onFrame(imageProxy)
                                    } catch (e: Throwable) {
                                        Log.e("CameraPreviewView", "Error processing frame", e)
                                        try {
                                            imageProxy.close()
                                        } catch (_: Throwable) {
                                        }
                                    } finally {
                                        isAnalyzing.set(false)
                                    }
                                }
                            }

                        // Prefer front camera, fallback to back camera
                        val cameraSelector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                            CameraSelector.DEFAULT_FRONT_CAMERA
                        } else {
                            CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                        onCameraReady(true)
                        cameraError = null
                        Log.d("CameraPreviewView", "Camera bound successfully.")
                    } catch (e: Exception) {
                        Log.e("CameraPreviewView", "Failed to bind camera: ${e.message}", e)
                        cameraError = e.message ?: "Failed to start camera"
                        onCameraReady(false)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Hand Skeleton Landmark Overlay
        HandSkeletonOverlay(
            handData = handData,
            activeGesture = activeGesture,
            modifier = Modifier.fillMaxSize()
        )

        // Error display if camera fails
        if (cameraError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Camera Unavailable:\n$cameraError",
                    color = Color.White
                )
            }
        }
    }
}
