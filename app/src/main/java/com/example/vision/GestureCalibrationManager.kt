package com.example.vision

import android.content.Context
import android.util.Log
import com.example.model.CalibrationOverallStatus
import com.example.model.GestureCalibrationData
import com.example.model.GestureType
import com.example.model.HandDistance
import com.example.model.HandPreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sqrt

data class TrainingProgress(
    val gesture: GestureType,
    val collectedCount: Int,
    val targetCount: Int,
    val currentConfidence: Float,
    val distance: HandDistance,
    val isComplete: Boolean,
    val qualityPercentage: Int = 0,
    val feedbackMessage: String = ""
)

class GestureCalibrationManager(private val context: Context) {

    companion object {
        private const val TAG = "GestureCalibration"
        private const val PREFS_NAME = "gesture_calibration_prefs"
        private const val KEY_CALIBRATION_JSON = "calibration_data_json"
        private const val KEY_HAND_PREF = "hand_preference"
        const val TARGET_SAMPLES_STATIC = 35
        const val TARGET_SAMPLES_DYNAMIC = 25
    }

    private val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _calibrations = MutableStateFlow<Map<GestureType, GestureCalibrationData>>(emptyMap())
    val calibrations: StateFlow<Map<GestureType, GestureCalibrationData>> = _calibrations.asStateFlow()

    private val _handPreference = MutableStateFlow(HandPreference.AUTO)
    val handPreference: StateFlow<HandPreference> = _handPreference.asStateFlow()

    // Active Training State
    private var trainingGesture: GestureType? = null
    private val collectedSampleVectors = mutableListOf<List<Float>>()
    private val collectedConfidences = mutableListOf<Float>()
    private var dynamicSwipeTrack = mutableListOf<Pair<Float, Float>>() // X, Y points
    private var lastSampleTime = 0L

    init {
        loadPersistedCalibrations()
    }

    private fun loadPersistedCalibrations() {
        val map = mutableMapOf<GestureType, GestureCalibrationData>()
        val jsonStr = sharedPrefs.getString(KEY_CALIBRATION_JSON, null)
        if (!jsonStr.isNullOrBlank()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val gName = obj.getString("gesture")
                    val gesture = GestureType.valueOf(gName)
                    val count = obj.getInt("sampleCount")
                    val avgConf = obj.getDouble("averageConfidence").toFloat()
                    val quality = obj.getInt("qualityPercentage")
                    val isTrained = obj.getBoolean("isTrained")
                    val ts = obj.optLong("timestamp", System.currentTimeMillis())

                    val meanArr = obj.getJSONArray("meanFeatures")
                    val meanList = mutableListOf<Float>()
                    for (j in 0 until meanArr.length()) meanList.add(meanArr.getDouble(j).toFloat())

                    val varArr = obj.getJSONArray("varianceFeatures")
                    val varList = mutableListOf<Float>()
                    for (j in 0 until varArr.length()) varList.add(varArr.getDouble(j).toFloat())

                    map[gesture] = GestureCalibrationData(
                        gesture = gesture,
                        sampleCount = count,
                        meanFeatures = meanList,
                        varianceFeatures = varList,
                        averageConfidence = avgConf,
                        qualityPercentage = quality,
                        isTrained = isTrained,
                        timestamp = ts
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing calibration json", e)
            }
        }
        _calibrations.value = map

        val handPrefStr = sharedPrefs.getString(KEY_HAND_PREF, HandPreference.AUTO.name)
        _handPreference.value = try {
            HandPreference.valueOf(handPrefStr ?: HandPreference.AUTO.name)
        } catch (_: Exception) {
            HandPreference.AUTO
        }
    }

    private fun persistCalibrations() {
        try {
            val array = JSONArray()
            for ((_, data) in _calibrations.value) {
                val obj = JSONObject().apply {
                    put("gesture", data.gesture.name)
                    put("sampleCount", data.sampleCount)
                    put("averageConfidence", data.averageConfidence.toDouble())
                    put("qualityPercentage", data.qualityPercentage)
                    put("isTrained", data.isTrained)
                    put("timestamp", data.timestamp)

                    val meanArr = JSONArray()
                    data.meanFeatures.forEach { meanArr.put(it.toDouble()) }
                    put("meanFeatures", meanArr)

                    val varArr = JSONArray()
                    data.varianceFeatures.forEach { varArr.put(it.toDouble()) }
                    put("varianceFeatures", varArr)
                }
                array.put(obj)
            }
            sharedPrefs.edit()
                .putString(KEY_CALIBRATION_JSON, array.toString())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist calibrations: ${e.message}", e)
        }
    }

    fun setHandPreference(pref: HandPreference) {
        _handPreference.value = pref
        sharedPrefs.edit().putString(KEY_HAND_PREF, pref.name).apply()
    }

    fun startTraining(gesture: GestureType) {
        trainingGesture = gesture
        collectedSampleVectors.clear()
        collectedConfidences.clear()
        dynamicSwipeTrack.clear()
        lastSampleTime = System.currentTimeMillis()
        Log.d(TAG, "Started training for gesture: ${gesture.displayName}")
    }

    fun cancelTraining() {
        trainingGesture = null
        collectedSampleVectors.clear()
        collectedConfidences.clear()
        dynamicSwipeTrack.clear()
    }

    /**
     * Feeds a single frame during active gesture training.
     * Rejects frames that have no hand, low confidence, or bad distance.
     */
    fun feedTrainingSample(hand: HandData): TrainingProgress {
        val gesture = trainingGesture ?: return TrainingProgress(
            gesture = GestureType.OPEN_PALM,
            collectedCount = 0,
            targetCount = TARGET_SAMPLES_STATIC,
            currentConfidence = 0f,
            distance = HandDistance.NO_HAND,
            isComplete = false,
            feedbackMessage = "Training not active"
        )

        val targetCount = if (gesture.isDynamic) TARGET_SAMPLES_DYNAMIC else TARGET_SAMPLES_STATIC

        if (!hand.isPresent || hand.landmarks.size < 21) {
            return TrainingProgress(
                gesture = gesture,
                collectedCount = collectedSampleVectors.size,
                targetCount = targetCount,
                currentConfidence = 0f,
                distance = HandDistance.NO_HAND,
                isComplete = false,
                feedbackMessage = "Show your hand in the camera view"
            )
        }

        val features = HandFeatureExtractor.extract(hand.landmarks)
        if (features == null) {
            return TrainingProgress(
                gesture = gesture,
                collectedCount = collectedSampleVectors.size,
                targetCount = targetCount,
                currentConfidence = hand.confidence,
                distance = hand.distance,
                isComplete = false,
                feedbackMessage = "Position hand clearly"
            )
        }

        // Distance validation
        if (features.distance == HandDistance.TOO_FAR) {
            return TrainingProgress(
                gesture = gesture,
                collectedCount = collectedSampleVectors.size,
                targetCount = targetCount,
                currentConfidence = hand.confidence,
                distance = HandDistance.TOO_FAR,
                isComplete = false,
                feedbackMessage = "Move hand closer"
            )
        }
        if (features.distance == HandDistance.TOO_CLOSE) {
            return TrainingProgress(
                gesture = gesture,
                collectedCount = collectedSampleVectors.size,
                targetCount = targetCount,
                currentConfidence = hand.confidence,
                distance = HandDistance.TOO_CLOSE,
                isComplete = false,
                feedbackMessage = "Move hand slightly further back"
            )
        }

        // Confidence validation
        if (hand.confidence < 0.60f) {
            return TrainingProgress(
                gesture = gesture,
                collectedCount = collectedSampleVectors.size,
                targetCount = targetCount,
                currentConfidence = hand.confidence,
                distance = features.distance,
                isComplete = false,
                feedbackMessage = "Low lighting or ambiguous hand position"
            )
        }

        // Valid sample accepted
        val vector = features.toFeatureVector()
        collectedSampleVectors.add(vector)
        collectedConfidences.add(hand.confidence)

        val isComplete = collectedSampleVectors.size >= targetCount

        return TrainingProgress(
            gesture = gesture,
            collectedCount = collectedSampleVectors.size,
            targetCount = targetCount,
            currentConfidence = hand.confidence,
            distance = features.distance,
            isComplete = isComplete,
            feedbackMessage = if (isComplete) "Calibration samples collected!" else "Hold gesture steady..."
        )
    }

    /**
     * Finalizes calibration, computes mean and variance vectors, and saves to persistence.
     */
    fun finishTraining(): GestureCalibrationData? {
        val gesture = trainingGesture ?: return null
        if (collectedSampleVectors.size < 10) {
            cancelTraining()
            return null
        }

        val numDimensions = collectedSampleVectors[0].size
        val sampleSize = collectedSampleVectors.size

        val mean = MutableList(numDimensions) { 0f }
        val variance = MutableList(numDimensions) { 0f }

        // Compute means
        for (sample in collectedSampleVectors) {
            for (d in 0 until numDimensions) {
                mean[d] += sample[d]
            }
        }
        for (d in 0 until numDimensions) {
            mean[d] /= sampleSize
        }

        // Compute variances
        for (sample in collectedSampleVectors) {
            for (d in 0 until numDimensions) {
                val diff = sample[d] - mean[d]
                variance[d] += diff * diff
            }
        }
        for (d in 0 until numDimensions) {
            variance[d] = (variance[d] / sampleSize).coerceAtLeast(0.001f)
        }

        val avgConfidence = (collectedConfidences.sum() / collectedConfidences.size).coerceIn(0.7f, 0.99f)

        // Quality Score based on stability (inverse of average standard deviation across extension ratios)
        var sumStdDev = 0f
        for (d in 0 until 5) {
            sumStdDev += sqrt(variance[d])
        }
        val avgStdDev = sumStdDev / 5f
        val stabilityFactor = (1.0f - avgStdDev * 2.5f).coerceIn(0.6f, 0.98f)
        val qualityPercentage = (stabilityFactor * 100f * (avgConfidence / 0.95f)).toInt().coerceIn(75, 98)

        val data = GestureCalibrationData(
            gesture = gesture,
            sampleCount = sampleSize,
            meanFeatures = mean,
            varianceFeatures = variance,
            averageConfidence = avgConfidence,
            qualityPercentage = qualityPercentage,
            isTrained = true,
            timestamp = System.currentTimeMillis()
        )

        val updatedMap = _calibrations.value.toMutableMap()
        updatedMap[gesture] = data
        _calibrations.value = updatedMap
        persistCalibrations()

        cancelTraining()
        Log.i(TAG, "Completed calibration for ${gesture.displayName}: Quality $qualityPercentage%")
        return data
    }

    /**
     * Evaluates an incoming feature vector against this gesture's trained calibration.
     * Returns matching confidence score between 0.0f and 1.0f.
     */
    fun matchCalibratedPose(gesture: GestureType, features: NormalizedHandFeatures): Float? {
        val cal = _calibrations.value[gesture] ?: return null
        if (!cal.isTrained || cal.meanFeatures.isEmpty()) return null

        val vector = features.toFeatureVector()
        var weightedDistance = 0f

        // Extension ratios (0..4) and pinch (10) are primary indicators
        for (d in 0 until vector.size) {
            val diff = vector[d] - cal.meanFeatures[d]
            val variance = cal.varianceFeatures[d]
            // Standard normalized distance
            val weight = when (d) {
                in 0..4 -> 1.5f // Finger extensions
                10 -> 2.0f // Pinch ratio
                in 5..9 -> 1.0f // Finger curls
                else -> 0.8f // Thumb angle
            }
            weightedDistance += weight * (diff * diff) / (variance + 0.008f)
        }

        val normalizedDistance = weightedDistance / vector.size
        // Convert distance to confidence (exponential decay)
        val confidence = kotlin.math.exp(-normalizedDistance * 0.4f).coerceIn(0f, 1f)
        return confidence
    }

    fun resetGesture(gesture: GestureType) {
        val updatedMap = _calibrations.value.toMutableMap()
        updatedMap.remove(gesture)
        _calibrations.value = updatedMap
        persistCalibrations()
    }

    fun resetAll() {
        _calibrations.value = emptyMap()
        persistCalibrations()
    }

    fun getOverallStatus(): CalibrationOverallStatus {
        val trainedCount = _calibrations.value.count { it.value.isTrained }
        return when {
            trainedCount == 0 -> CalibrationOverallStatus.UNCALIBRATED
            trainedCount in 1..4 -> CalibrationOverallStatus.FAIR
            trainedCount in 5..9 -> CalibrationOverallStatus.GOOD
            else -> CalibrationOverallStatus.EXCELLENT
        }
    }
}
