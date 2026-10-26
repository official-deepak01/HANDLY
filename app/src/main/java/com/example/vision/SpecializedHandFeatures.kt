package com.example.vision

import com.example.model.NormalizedPoint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Fixed-size circular buffer to avoid repeated memory allocations during frame processing.
 */
class FixedCircularBuffer<T>(val capacity: Int) {
    private val buffer = java.util.ArrayList<T?>(capacity).apply {
        for (i in 0 until capacity) add(null)
    }
    private var head = 0
    private var count = 0

    val size: Int get() = count

    fun isNotEmpty(): Boolean = count > 0

    fun isEmpty(): Boolean = count == 0

    fun add(item: T) {
        buffer[head] = item
        head = (head + 1) % capacity
        if (count < capacity) {
            count++
        }
    }

    fun clear() {
        for (i in 0 until capacity) {
            buffer[i] = null
        }
        head = 0
        count = 0
    }

    /**
     * Retrieves elements from oldest (index 0) to newest (index size - 1).
     */
    operator fun get(index: Int): T {
        if (index < 0 || index >= count) {
            throw IndexOutOfBoundsException("Index $index out of bounds for size $count")
        }
        val actualIndex = if (count < capacity) {
            index
        } else {
            (head + index) % capacity
        }
        @Suppress("UNCHECKED_CAST")
        return buffer[actualIndex] as T
    }

    fun first(): T = get(0)

    fun last(): T = get(count - 1)

    fun toList(): List<T> {
        val result = ArrayList<T>(count)
        for (i in 0 until count) {
            result.add(get(i))
        }
        return result
    }
}

data class MotionSample(
    val point: NormalizedPoint,
    val timestampMs: Long
)

data class DynamicMotionFeatures(
    val deltaX: Float,
    val deltaY: Float,
    val displacement: Float,
    val normDisplacement: Float,
    val velocity: Float,
    val durationMs: Long,
    val straightness: Float,
    val dominantAxisRatio: Float,
    val isMoving: Boolean
)

data class PinchSpecificFeatures(
    val tipDistance: Float,
    val normalizedPinchDistance: Float,
    val isContact: Boolean,
    val otherFingersCurled: Boolean,
    val middleRingPinkyExtensionAvg: Float
)

data class PointSpecificFeatures(
    val indexExtension: Float,
    val middleCurl: Float,
    val ringCurl: Float,
    val pinkyCurl: Float,
    val thumbCurl: Float,
    val isIndexUpright: Boolean,
    val otherFingersCurled: Boolean
)

data class PeaceSpecificFeatures(
    val indexExtension: Float,
    val middleExtension: Float,
    val ringCurl: Float,
    val pinkyCurl: Float,
    val fingerSeparation: Float,
    val otherFingersCurled: Boolean
)

data class ThumbOrientationFeatures(
    val thumbExtension: Float,
    val verticalVectorY: Float,
    val horizontalVectorX: Float,
    val isThumbPointingUp: Boolean,
    val isThumbPointingDown: Boolean,
    val otherFingersCurled: Boolean,
    val pinchSeparation: Float
)

data class FistSpecificFeatures(
    val indexCurl: Float,
    val middleCurl: Float,
    val ringCurl: Float,
    val pinkyCurl: Float,
    val thumbFolded: Boolean,
    val palmCompactness: Float,
    val areAllFingersCurled: Boolean
)

data class OpenPalmSpecificFeatures(
    val indexExtension: Float,
    val middleExtension: Float,
    val ringExtension: Float,
    val pinkyExtension: Float,
    val thumbExtended: Boolean,
    val fingerSpreadAvg: Float,
    val areAllFourFingersExtended: Boolean
)

data class OkSignSpecificFeatures(
    val pinchRatio: Float,
    val isPinching: Boolean,
    val middleExtension: Float,
    val ringExtension: Float,
    val pinkyExtension: Float,
    val areOtherThreeExtended: Boolean
)

/**
 * Specialized feature extractor providing granular geometric metrics for each gesture class.
 */
object SpecializedFeatureExtractor {

    private fun dist(p1: NormalizedPoint, p2: NormalizedPoint): Float {
        return hypot(p1.x - p2.x, p1.y - p2.y)
    }

    fun extractFingerExtension(
        tip: NormalizedPoint,
        pip: NormalizedPoint,
        mcp: NormalizedPoint,
        palmCenter: NormalizedPoint,
        wrist: NormalizedPoint,
        palmScale: Float
    ): Float {
        val tipToMcp = dist(tip, mcp) / palmScale
        val scoreMcp = ((tipToMcp - 0.48f) / 0.38f).coerceIn(0f, 1f)

        val tipToWrist = dist(tip, wrist)
        val pipToWrist = dist(pip, wrist).coerceAtLeast(0.001f)
        val wristRatio = tipToWrist / pipToWrist
        val scoreWrist = ((wristRatio - 0.98f) / 0.18f).coerceIn(0f, 1f)

        val tipToCenter = dist(tip, palmCenter) / palmScale
        val scoreCenter = ((tipToCenter - 0.60f) / 0.38f).coerceIn(0f, 1f)

        return (scoreMcp * 0.45f + scoreWrist * 0.35f + scoreCenter * 0.20f).coerceIn(0f, 1f)
    }

    fun extractPinchFeatures(
        landmarks: List<NormalizedPoint>,
        palmScale: Float,
        extMiddle: Float,
        extRing: Float,
        extPinky: Float
    ): PinchSpecificFeatures {
        val thumbTip = landmarks[4]
        val indexTip = landmarks[8]
        val tipDist = dist(thumbTip, indexTip)
        val normDist = tipDist / palmScale

        val isContact = normDist <= 0.34f
        val otherExtAvg = (extMiddle + extRing + extPinky) / 3f
        val otherCurled = otherExtAvg <= 0.48f

        return PinchSpecificFeatures(
            tipDistance = tipDist,
            normalizedPinchDistance = normDist,
            isContact = isContact,
            otherFingersCurled = otherCurled,
            middleRingPinkyExtensionAvg = otherExtAvg
        )
    }

    fun extractPointFeatures(
        extIndex: Float,
        extMiddle: Float,
        extRing: Float,
        extPinky: Float,
        landmarks: List<NormalizedPoint>,
        palmScale: Float
    ): PointSpecificFeatures {
        val indexTip = landmarks[8]
        val indexMcp = landmarks[5]
        val thumbTip = landmarks[4]

        val isUpright = (indexTip.y - indexMcp.y) < -0.15f * palmScale
        val otherCurled = extMiddle <= 0.44f && extRing <= 0.44f && extPinky <= 0.44f
        val thumbCurl = dist(thumbTip, indexMcp) / palmScale

        return PointSpecificFeatures(
            indexExtension = extIndex,
            middleCurl = extMiddle,
            ringCurl = extRing,
            pinkyCurl = extPinky,
            thumbCurl = thumbCurl,
            isIndexUpright = isUpright,
            otherFingersCurled = otherCurled
        )
    }

    fun extractPeaceFeatures(
        extIndex: Float,
        extMiddle: Float,
        extRing: Float,
        extPinky: Float,
        landmarks: List<NormalizedPoint>,
        palmScale: Float
    ): PeaceSpecificFeatures {
        val indexTip = landmarks[8]
        val middleTip = landmarks[12]
        val spread = dist(indexTip, middleTip) / palmScale
        val otherCurled = extRing <= 0.44f && extPinky <= 0.44f

        return PeaceSpecificFeatures(
            indexExtension = extIndex,
            middleExtension = extMiddle,
            ringCurl = extRing,
            pinkyCurl = extPinky,
            fingerSeparation = spread,
            otherFingersCurled = otherCurled
        )
    }

    fun extractThumbFeatures(
        landmarks: List<NormalizedPoint>,
        palmScale: Float,
        extIndex: Float,
        extMiddle: Float,
        extRing: Float,
        extPinky: Float
    ): ThumbOrientationFeatures {
        val thumbTip = landmarks[4]
        val thumbMcp = landmarks[2]
        val wrist = landmarks[0]
        val indexTip = landmarks[8]

        val thumbTipToMcp = dist(thumbTip, thumbMcp) / palmScale
        val dy = (thumbTip.y - thumbMcp.y) / palmScale
        val dx = (thumbTip.x - thumbMcp.x) / palmScale
        val pinchSep = dist(thumbTip, indexTip) / palmScale

        val otherCurled = extIndex <= 0.45f && extMiddle <= 0.45f && extRing <= 0.45f && extPinky <= 0.45f
        val isUp = dy < -0.32f && thumbTipToMcp >= 0.40f
        val isDown = dy > 0.32f && thumbTipToMcp >= 0.40f

        return ThumbOrientationFeatures(
            thumbExtension = thumbTipToMcp,
            verticalVectorY = dy,
            horizontalVectorX = dx,
            isThumbPointingUp = isUp,
            isThumbPointingDown = isDown,
            otherFingersCurled = otherCurled,
            pinchSeparation = pinchSep
        )
    }

    fun extractFistFeatures(
        extIndex: Float,
        extMiddle: Float,
        extRing: Float,
        extPinky: Float,
        landmarks: List<NormalizedPoint>,
        palmCenter: NormalizedPoint,
        palmScale: Float
    ): FistSpecificFeatures {
        val thumbTip = landmarks[4]
        val thumbMcp = landmarks[2]
        val thumbFolded = dist(thumbTip, thumbMcp) / palmScale < 0.38f

        // Compactness: average distance of all 5 fingertips to palm center
        val tipIndices = intArrayOf(4, 8, 12, 16, 20)
        var sumDist = 0f
        for (idx in tipIndices) {
            sumDist += dist(landmarks[idx], palmCenter)
        }
        val compactness = (sumDist / 5f) / palmScale

        val allCurled = extIndex <= 0.44f && extMiddle <= 0.44f && extRing <= 0.44f && extPinky <= 0.44f

        return FistSpecificFeatures(
            indexCurl = extIndex,
            middleCurl = extMiddle,
            ringCurl = extRing,
            pinkyCurl = extPinky,
            thumbFolded = thumbFolded,
            palmCompactness = compactness,
            areAllFingersCurled = allCurled
        )
    }

    fun extractOpenPalmFeatures(
        extIndex: Float,
        extMiddle: Float,
        extRing: Float,
        extPinky: Float,
        landmarks: List<NormalizedPoint>,
        palmScale: Float
    ): OpenPalmSpecificFeatures {
        val thumbTip = landmarks[4]
        val thumbMcp = landmarks[2]
        val thumbExt = dist(thumbTip, thumbMcp) / palmScale >= 0.40f

        val indexTip = landmarks[8]
        val middleTip = landmarks[12]
        val ringTip = landmarks[16]
        val pinkyTip = landmarks[20]

        val s1 = dist(thumbTip, indexTip)
        val s2 = dist(indexTip, middleTip)
        val s3 = dist(middleTip, ringTip)
        val s4 = dist(ringTip, pinkyTip)
        val avgSpread = ((s1 + s2 + s3 + s4) / 4f) / palmScale

        val allFour = extIndex >= 0.54f && extMiddle >= 0.54f && extRing >= 0.54f && extPinky >= 0.54f

        return OpenPalmSpecificFeatures(
            indexExtension = extIndex,
            middleExtension = extMiddle,
            ringExtension = extRing,
            pinkyExtension = extPinky,
            thumbExtended = thumbExt,
            fingerSpreadAvg = avgSpread,
            areAllFourFingersExtended = allFour
        )
    }

    fun extractOkSignFeatures(
        pinchDist: Float,
        palmScale: Float,
        extMiddle: Float,
        extRing: Float,
        extPinky: Float
    ): OkSignSpecificFeatures {
        val pinchRatio = pinchDist / palmScale
        val isPinch = pinchRatio <= 0.38f
        val otherThree = extMiddle >= 0.52f && extRing >= 0.52f && extPinky >= 0.52f

        return OkSignSpecificFeatures(
            pinchRatio = pinchRatio,
            isPinching = isPinch,
            middleExtension = extMiddle,
            ringExtension = extRing,
            pinkyExtension = extPinky,
            areOtherThreeExtended = otherThree
        )
    }

    fun extractDynamicMotion(
        history: FixedCircularBuffer<MotionSample>,
        palmScale: Float
    ): DynamicMotionFeatures? {
        if (history.size < 3) return null

        val oldest = history.first()
        val newest = history.last()
        val durationMs = newest.timestampMs - oldest.timestampMs

        if (durationMs < 90L || durationMs > 550L) return null

        val deltaX = newest.point.x - oldest.point.x
        val deltaY = newest.point.y - oldest.point.y
        val displacement = hypot(deltaX, deltaY)
        val normDisplacement = displacement / palmScale.coerceAtLeast(0.04f)

        // Calculate path length across all points in history to evaluate straightness
        var totalPathLength = 0f
        for (i in 0 until history.size - 1) {
            val p1 = history[i].point
            val p2 = history[i + 1].point
            totalPathLength += hypot(p2.x - p1.x, p2.y - p1.y)
        }

        val straightness = if (totalPathLength > 0.001f) {
            (displacement / totalPathLength).coerceIn(0f, 1f)
        } else {
            1f
        }

        val velocity = normDisplacement / (durationMs / 1000f)

        val absX = abs(deltaX)
        val absY = abs(deltaY)
        val dominantAxisRatio = if (min(absX, absY) > 0.0001f) {
            max(absX, absY) / min(absX, absY)
        } else {
            10f
        }

        val isMoving = velocity >= 1.15f && normDisplacement >= 0.30f && straightness >= 0.70f

        return DynamicMotionFeatures(
            deltaX = deltaX,
            deltaY = deltaY,
            displacement = displacement,
            normDisplacement = normDisplacement,
            velocity = velocity,
            durationMs = durationMs,
            straightness = straightness,
            dominantAxisRatio = dominantAxisRatio,
            isMoving = isMoving
        )
    }
}
