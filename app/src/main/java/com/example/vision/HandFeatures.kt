package com.example.vision

import com.example.model.HandDistance
import com.example.model.HandPreference
import com.example.model.NormalizedPoint
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

data class NormalizedHandFeatures(
    val palmScale: Float,
    val distance: HandDistance,
    // Finger extension ratios (tip to wrist / palmScale) for: Thumb, Index, Middle, Ring, Pinky
    val extensionRatios: FloatArray,
    // Finger curl ratios (tip to MCP / palmScale) for: Thumb, Index, Middle, Ring, Pinky
    val curlRatios: FloatArray,
    // Pinch distance (Thumb tip to Index tip / palmScale)
    val pinchRatio: Float,
    // Thumb orientation relative to palm
    val thumbDirectionY: Float,
    val thumbDirectionX: Float,
    // Handedness estimation: "RIGHT" or "LEFT"
    val detectedHand: HandPreference
) {
    /**
     * Converts the features into a standard numerical array for calibration modeling.
     * Total 13 numerical dimensions.
     */
    fun toFeatureVector(): List<Float> {
        val list = mutableListOf<Float>()
        for (v in extensionRatios) list.add(v)
        for (v in curlRatios) list.add(v)
        list.add(pinchRatio)
        list.add(thumbDirectionY)
        list.add(thumbDirectionX)
        return list
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as NormalizedHandFeatures
        return palmScale == other.palmScale &&
                distance == other.distance &&
                extensionRatios.contentEquals(other.extensionRatios) &&
                curlRatios.contentEquals(other.curlRatios) &&
                pinchRatio == other.pinchRatio &&
                thumbDirectionY == other.thumbDirectionY &&
                thumbDirectionX == other.thumbDirectionX &&
                detectedHand == other.detectedHand
    }

    override fun hashCode(): Int {
        var result = palmScale.hashCode()
        result = 31 * result + distance.hashCode()
        result = 31 * result + extensionRatios.contentHashCode()
        result = 31 * result + curlRatios.contentHashCode()
        result = 31 * result + pinchRatio.hashCode()
        result = 31 * result + thumbDirectionY.hashCode()
        result = 31 * result + thumbDirectionX.hashCode()
        result = 31 * result + detectedHand.hashCode()
        return result
    }
}

object HandFeatureExtractor {

    fun extract(hand: HandData): NormalizedHandFeatures? {
        if (!hand.isPresent) return null
        return extract(hand.landmarks)
    }

    /**
     * Extracts normalized, distance-invariant landmark features from a 21-point hand skeleton.
     */
    fun extract(landmarks: List<NormalizedPoint>): NormalizedHandFeatures? {
        if (landmarks.size < 21) return null

        val wrist = landmarks[0]
        val middleMcp = landmarks[9]

        // Reference palm scale: distance between wrist (0) and middle MCP (9)
        val palmScale = dist(wrist, middleMcp).coerceAtLeast(0.015f)

        // Hand Distance Estimation
        val distance = when {
            palmScale < 0.12f -> HandDistance.TOO_FAR
            palmScale > 0.44f -> HandDistance.TOO_CLOSE
            else -> HandDistance.GOOD
        }

        val tips = intArrayOf(4, 8, 12, 16, 20)
        val mcps = intArrayOf(2, 5, 9, 13, 17)

        val extensionRatios = FloatArray(5)
        val curlRatios = FloatArray(5)

        for (i in 0 until 5) {
            val tip = landmarks[tips[i]]
            val mcp = landmarks[mcps[i]]
            extensionRatios[i] = dist(tip, wrist) / palmScale
            curlRatios[i] = dist(tip, mcp) / palmScale
        }

        val thumbTip = landmarks[4]
        val indexTip = landmarks[8]
        val pinchRatio = dist(thumbTip, indexTip) / palmScale

        val thumbDirectionY = (thumbTip.y - wrist.y) / palmScale
        val thumbDirectionX = (thumbTip.x - wrist.x) / palmScale

        // Estimate handedness using the cross product of (Wrist->IndexMCP) and (Wrist->PinkyMCP)
        val indexMcp = landmarks[5]
        val pinkyMcp = landmarks[17]
        val v1x = indexMcp.x - wrist.x
        val v1y = indexMcp.y - wrist.y
        val v2x = pinkyMcp.x - wrist.x
        val v2y = pinkyMcp.y - wrist.y
        val crossZ = v1x * v2y - v1y * v2x

        val detectedHand = if (crossZ > 0) HandPreference.RIGHT else HandPreference.LEFT

        return NormalizedHandFeatures(
            palmScale = palmScale,
            distance = distance,
            extensionRatios = extensionRatios,
            curlRatios = curlRatios,
            pinchRatio = pinchRatio,
            thumbDirectionY = thumbDirectionY,
            thumbDirectionX = thumbDirectionX,
            detectedHand = detectedHand
        )
    }

    private fun dist(p1: NormalizedPoint, p2: NormalizedPoint): Float {
        return hypot(p1.x - p2.x, p1.y - p2.y)
    }
}
