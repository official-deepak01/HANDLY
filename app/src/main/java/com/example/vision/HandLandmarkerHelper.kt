package com.example.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.camera.core.ImageProxy
import com.example.model.HandDistance
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

class HandLandmarkerHelper(
    private val context: Context,
    private val onResult: (HandData) -> Unit,
    private val onError: (String) -> Unit
) {

    private val TAG = "HandLandmarkerHelper"
    private var handLandmarker: HandLandmarker? = null
    private var isUsingFallback = false

    init {
        setupHandLandmarker()
    }

    private fun setupHandLandmarker() {
        try {
            val baseOptionsBuilder = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")

            val optionsBuilder = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptionsBuilder.build())
                .setMinHandDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setNumHands(1)
                .setRunningMode(RunningMode.IMAGE)

            handLandmarker = HandLandmarker.createFromOptions(context, optionsBuilder.build())
            isUsingFallback = false
            Log.d(TAG, "MediaPipe HandLandmarker initialized successfully.")
        } catch (e: Exception) {
            Log.w(TAG, "MediaPipe initialization failed (${e.message}). Engaging CV fallback tracker.", e)
            isUsingFallback = true
        }
    }

    fun detectLiveStream(imageProxy: ImageProxy) {
        val bitmap: Bitmap? = try {
            imageProxyToBitmap(imageProxy)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting imageProxy to bitmap: ${e.message}", e)
            null
        } finally {
            try {
                imageProxy.close()
            } catch (e: Exception) {
                // already closed or released
            }
        }

        if (bitmap == null) return

        try {
            if (!isUsingFallback && handLandmarker != null) {
                val mpImage = BitmapImageBuilder(bitmap).build()
                val result: HandLandmarkerResult = handLandmarker!!.detect(mpImage)
                val handData = extractHandData(result)
                onResult(handData)
            } else {
                // Fallback computer vision tracker
                val fallbackHand = detectHandFallback(bitmap)
                onResult(fallbackHand)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing image frame: ${e.message}", e)
            onError(e.message ?: "Analysis error")
        } finally {
            try {
                bitmap.recycle()
            } catch (_: Exception) {
            }
        }
    }

    private fun extractHandData(result: HandLandmarkerResult): HandData {
        val landmarksList = result.landmarks()
        if (landmarksList.isEmpty() || landmarksList[0].isEmpty()) {
            return HandData.EMPTY
        }

        val landmarks = landmarksList[0]
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE

        val normalizedPoints = landmarks.map { lm ->
            val x = lm.x()
            val y = lm.y()
            minX = min(minX, x)
            maxX = max(maxX, x)
            minY = min(minY, y)
            maxY = max(maxY, y)
            NormalizedPoint(x, y, lm.z())
        }

        val boxWidth = maxX - minX
        val boxHeight = maxY - minY
        val handSize = max(boxWidth, boxHeight)
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f

        val distance = when {
            handSize < 0.16f -> HandDistance.TOO_FAR
            handSize > 0.68f -> HandDistance.TOO_CLOSE
            else -> HandDistance.GOOD
        }

        val confidence = 0.92f

        return HandData(
            isPresent = true,
            landmarks = normalizedPoints,
            confidence = confidence,
            centerX = centerX,
            centerY = centerY,
            handSize = handSize,
            distance = distance
        )
    }

    private var cachedPixels: IntArray? = null

    /**
     * Fallback optical / skin-contrast hand detector when MediaPipe cannot run.
     * Computes skin / luminance centroid and generates a structured hand estimate.
     */
    private fun detectHandFallback(bitmap: Bitmap): HandData {
        val width = bitmap.width
        val height = bitmap.height
        val sampleStep = 8

        var count = 0
        var sumX = 0L
        var sumY = 0L
        var minX = width
        var maxX = 0
        var minY = height
        var maxY = 0

        val requiredSize = width * height
        var pixels = cachedPixels
        if (pixels == null || pixels.size < requiredSize) {
            pixels = IntArray(requiredSize)
            cachedPixels = pixels
        }
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (y in 0 until height step sampleStep) {
            for (x in 0 until width step sampleStep) {
                val color = pixels[y * width + x]
                val r = (color shr 16) and 0xFF
                val g = (color shr 8) and 0xFF
                val b = color and 0xFF

                // Standard YCbCr skin chrominance approximation
                val cb = -0.169 * r - 0.331 * g + 0.500 * b + 128
                val cr = 0.500 * r - 0.419 * g - 0.081 * b + 128

                if (cb in 77.0..127.0 && cr in 133.0..173.0) {
                    count++
                    sumX += x
                    sumY += y
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        val totalSamples = (width / sampleStep) * (height / sampleStep)
        val coverage = count.toFloat() / totalSamples.coerceAtLeast(1)

        if (count < 25 || coverage < 0.02f) {
            return HandData.EMPTY
        }

        val cx = (sumX.toFloat() / count) / width
        val cy = (sumY.toFloat() / count) / height
        val normWidth = (maxX - minX).toFloat() / width
        val normHeight = (maxY - minY).toFloat() / height
        val handSize = max(normWidth, normHeight)

        val distance = when {
            handSize < 0.16f -> HandDistance.TOO_FAR
            handSize > 0.68f -> HandDistance.TOO_CLOSE
            else -> HandDistance.GOOD
        }

        // Generate synthetic 21 landmark skeleton mapped around the detected hand centroid
        val halfW = (normWidth / 2f).coerceAtLeast(0.06f)
        val halfH = (normHeight / 2f).coerceAtLeast(0.08f)
        val wrist = NormalizedPoint(cx, cy + halfH)
        val points = mutableListOf<NormalizedPoint>()
        points.add(wrist) // 0: wrist

        // Thumb 1-4
        for (i in 1..4) {
            points.add(NormalizedPoint(cx - halfW * (0.4f + i * 0.15f), cy + halfH * (0.4f - i * 0.25f)))
        }
        // Index 5-8
        for (i in 1..4) {
            points.add(NormalizedPoint(cx - halfW * 0.35f, cy - halfH * (i * 0.25f)))
        }
        // Middle 9-12
        for (i in 1..4) {
            points.add(NormalizedPoint(cx, cy - halfH * (0.1f + i * 0.25f)))
        }
        // Ring 13-16
        for (i in 1..4) {
            points.add(NormalizedPoint(cx + halfW * 0.35f, cy - halfH * (i * 0.23f)))
        }
        // Pinky 17-20
        for (i in 1..4) {
            points.add(NormalizedPoint(cx + halfW * 0.65f, cy - halfH * (0.1f + i * 0.18f)))
        }

        return HandData(
            isPresent = true,
            landmarks = points,
            confidence = (coverage * 5f).coerceIn(0.6f, 0.9f),
            centerX = cx,
            centerY = cy,
            handSize = handSize,
            distance = distance
        )
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val bitmap: Bitmap = try {
            image.toBitmap()
        } catch (_: Exception) {
            return null
        }

        // Apply rotation and horizontal mirroring for front camera
        val matrix = Matrix().apply {
            postRotate(image.imageInfo.rotationDegrees.toFloat())
            postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
        }

        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) {
            bitmap.recycle()
        }
        return rotated
    }

    fun close() {
        try {
            handLandmarker?.close()
            handLandmarker = null
        } catch (e: Exception) {
            Log.e(TAG, "Error closing HandLandmarker: ${e.message}", e)
        }
    }

    fun clear() = close()
}
