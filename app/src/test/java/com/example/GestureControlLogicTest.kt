package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.Cooldown
import com.example.model.GestureConfirmation
import com.example.model.GestureLifecycleState
import com.example.model.GestureType
import com.example.model.HandDistance
import com.example.model.HandPreference
import com.example.model.NormalizedPoint
import com.example.model.Sensitivity
import com.example.vision.GestureCalibrationManager
import com.example.vision.GestureClassifier
import com.example.vision.HandData
import com.example.vision.HandFeatureExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GestureControlLogicTest {

    @Test
    fun testHandFeatureExtraction_emptyHand() {
        val features = HandFeatureExtractor.extract(HandData.EMPTY)
        assertNull(features)
    }

    @Test
    fun testHandFeatureExtraction_validSyntheticHand() {
        // Create 21 landmarks simulating an open hand
        val landmarks = MutableList(21) { i ->
            NormalizedPoint(0.5f + (i % 5) * 0.05f, 0.5f - (i / 5) * 0.08f, 0f)
        }
        val hand = HandData(
            isPresent = true,
            landmarks = landmarks,
            confidence = 0.95f,
            centerX = 0.5f,
            centerY = 0.5f,
            handSize = 0.35f,
            distance = HandDistance.GOOD
        )
        val features = HandFeatureExtractor.extract(hand)
        assertNotNull(features)
        assertTrue(features!!.palmScale > 0f)
        assertEquals(5, features.extensionRatios.size)
    }

    @Test
    fun testGestureCalibrationManager_flow() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = GestureCalibrationManager(context)

        manager.startTraining(GestureType.OPEN_PALM)

        val landmarks = MutableList(21) { i ->
            NormalizedPoint(0.5f + (i % 5) * 0.05f, 0.5f - (i / 5) * 0.08f, 0f)
        }
        val hand = HandData(
            isPresent = true,
            landmarks = landmarks,
            confidence = 0.95f,
            centerX = 0.5f,
            centerY = 0.5f,
            handSize = 0.35f,
            distance = HandDistance.GOOD
        )

        var lastProgress = manager.feedTrainingSample(hand)
        for (i in 1..40) {
            lastProgress = manager.feedTrainingSample(hand)
        }

        assertTrue(lastProgress.isComplete)
        val saved = manager.finishTraining()
        assertNotNull(saved)
        assertTrue(saved!!.isTrained)
        assertEquals(GestureType.OPEN_PALM, saved.gesture)
    }

    @Test
    fun testGestureClassifier_processesSafely() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = GestureCalibrationManager(context)
        val classifier = GestureClassifier(manager)

        val landmarks = MutableList(21) { i ->
            NormalizedPoint(0.5f + (i % 5) * 0.05f, 0.5f - (i / 5) * 0.08f, 0f)
        }
        val hand = HandData(
            isPresent = true,
            landmarks = landmarks,
            confidence = 0.95f,
            centerX = 0.5f,
            centerY = 0.5f,
            handSize = 0.35f,
            distance = HandDistance.GOOD
        )

        val result = classifier.processHand(
            hand = hand,
            sensitivity = Sensitivity.MEDIUM,
            cooldown = Cooldown.MEDIUM,
            confirmation = GestureConfirmation.OFF,
            handPreference = HandPreference.AUTO,
            isControlActive = true
        )

        assertNotNull(result)
        // Check that classifier safely handles frames without crashing
        assertTrue(
            result.state == GestureLifecycleState.READY ||
                    result.state == GestureLifecycleState.DETECTING ||
                    result.state == GestureLifecycleState.DETECTED ||
                    result.state == GestureLifecycleState.CANDIDATE ||
                    result.state == GestureLifecycleState.STABLE ||
                    result.state == GestureLifecycleState.CONFIRMED ||
                    result.state == GestureLifecycleState.EXECUTED ||
                    result.state == GestureLifecycleState.COOLDOWN
        )
    }

    private fun buildHandLandmarks(
        indexExtended: Boolean = true,
        middleExtended: Boolean = true,
        ringExtended: Boolean = true,
        pinkyExtended: Boolean = true,
        thumbExtended: Boolean = true,
        thumbDirY: Float = -0.1f, // negative is up
        pinchThumbIndex: Boolean = false
    ): List<NormalizedPoint> {
        val list = MutableList(21) { NormalizedPoint(0.5f, 0.5f, 0f) }
        // 0: Wrist
        list[0] = NormalizedPoint(0.5f, 0.80f, 0f)

        // 1-4: Thumb
        list[1] = NormalizedPoint(0.42f, 0.72f, 0f)
        list[2] = NormalizedPoint(0.38f, 0.65f, 0f)
        list[3] = NormalizedPoint(0.35f, 0.58f, 0f)
        if (thumbExtended) {
            val ty = if (pinchThumbIndex) 0.44f else (0.65f + thumbDirY)
            val tx = if (pinchThumbIndex) 0.46f else 0.32f
            list[4] = NormalizedPoint(tx, ty, 0f)
        } else {
            list[4] = NormalizedPoint(0.42f, 0.60f, 0f) // folded
        }

        // 5-8: Index
        list[5] = NormalizedPoint(0.45f, 0.52f, 0f) // MCP
        list[6] = NormalizedPoint(0.45f, 0.42f, 0f) // PIP
        list[7] = NormalizedPoint(0.45f, 0.34f, 0f) // DIP
        if (pinchThumbIndex) {
            list[8] = NormalizedPoint(0.46f, 0.44f, 0f) // touch thumb tip in pinched position
        } else if (indexExtended) {
            list[8] = NormalizedPoint(0.45f, 0.22f, 0f) // extended straight
        } else {
            list[8] = NormalizedPoint(0.45f, 0.54f, 0f) // curled to palm
        }

        // 9-12: Middle
        list[9] = NormalizedPoint(0.50f, 0.50f, 0f) // MCP
        list[10] = NormalizedPoint(0.50f, 0.40f, 0f)
        list[11] = NormalizedPoint(0.50f, 0.32f, 0f)
        list[12] = if (middleExtended) NormalizedPoint(0.50f, 0.20f, 0f) else NormalizedPoint(0.50f, 0.54f, 0f)

        // 13-16: Ring
        list[13] = NormalizedPoint(0.55f, 0.52f, 0f) // MCP
        list[14] = NormalizedPoint(0.55f, 0.42f, 0f)
        list[15] = NormalizedPoint(0.55f, 0.35f, 0f)
        list[16] = if (ringExtended) NormalizedPoint(0.55f, 0.24f, 0f) else NormalizedPoint(0.55f, 0.55f, 0f)

        // 17-20: Pinky
        list[17] = NormalizedPoint(0.60f, 0.55f, 0f) // MCP
        list[18] = NormalizedPoint(0.60f, 0.46f, 0f)
        list[19] = NormalizedPoint(0.60f, 0.39f, 0f)
        list[20] = if (pinkyExtended) NormalizedPoint(0.60f, 0.28f, 0f) else NormalizedPoint(0.60f, 0.57f, 0f)

        return list
    }

    @Test
    fun testClassifyStaticPose_allDefaultGestures() {
        val classifier = GestureClassifier(null)
        val palmCenter = NormalizedPoint(0.5f, 0.58f, 0f)
        val palmScale = 0.30f

        // 1. OPEN PALM
        val openPalmLm = buildHandLandmarks(indexExtended = true, middleExtended = true, ringExtended = true, pinkyExtended = true, thumbExtended = true)
        val (gOpen, cOpen) = classifier.classifyStaticPose(openPalmLm, palmCenter, palmScale, null, Sensitivity.MEDIUM)
        assertEquals(GestureType.OPEN_PALM, gOpen)
        assertTrue(cOpen > 0.80f)

        // 2. FIST
        val fistLm = buildHandLandmarks(indexExtended = false, middleExtended = false, ringExtended = false, pinkyExtended = false, thumbExtended = false)
        val (gFist, cFist) = classifier.classifyStaticPose(fistLm, palmCenter, palmScale, null, Sensitivity.MEDIUM)
        assertEquals(GestureType.FIST, gFist)
        assertTrue(cFist > 0.80f)

        // 3. INDEX POINT
        val pointLm = buildHandLandmarks(indexExtended = true, middleExtended = false, ringExtended = false, pinkyExtended = false, thumbExtended = false)
        val (gPoint, cPoint) = classifier.classifyStaticPose(pointLm, palmCenter, palmScale, null, Sensitivity.MEDIUM)
        assertEquals(GestureType.INDEX_POINT, gPoint)
        assertTrue(cPoint > 0.80f)

        // 4. TWO FINGERS (PEACE)
        val peaceLm = buildHandLandmarks(indexExtended = true, middleExtended = true, ringExtended = false, pinkyExtended = false, thumbExtended = false)
        val (gPeace, cPeace) = classifier.classifyStaticPose(peaceLm, palmCenter, palmScale, null, Sensitivity.MEDIUM)
        assertEquals(GestureType.TWO_FINGERS, gPeace)
        assertTrue(cPeace > 0.80f)

        // 5. THUMBS UP
        val thumbsUpLm = buildHandLandmarks(indexExtended = false, middleExtended = false, ringExtended = false, pinkyExtended = false, thumbExtended = true, thumbDirY = -0.18f)
        val (gThumbUp, cThumbUp) = classifier.classifyStaticPose(thumbsUpLm, palmCenter, palmScale, null, Sensitivity.MEDIUM)
        assertEquals(GestureType.THUMBS_UP, gThumbUp)
        assertTrue(cThumbUp > 0.80f)

        // 6. THUMBS DOWN
        val thumbsDownLm = buildHandLandmarks(indexExtended = false, middleExtended = false, ringExtended = false, pinkyExtended = false, thumbExtended = true, thumbDirY = 0.18f)
        val (gThumbDown, cThumbDown) = classifier.classifyStaticPose(thumbsDownLm, palmCenter, palmScale, null, Sensitivity.MEDIUM)
        assertEquals(GestureType.THUMBS_DOWN, gThumbDown)
        assertTrue(cThumbDown > 0.80f)

        // 7. PINCH
        val pinchLm = buildHandLandmarks(indexExtended = false, middleExtended = false, ringExtended = false, pinkyExtended = false, thumbExtended = true, pinchThumbIndex = true)
        val (gPinch, cPinch) = classifier.classifyStaticPose(pinchLm, palmCenter, palmScale, null, Sensitivity.MEDIUM)
        assertEquals(GestureType.PINCH, gPinch)
        assertTrue(cPinch > 0.75f)

        // 8. OK SIGN (Pinch thumb + index, but middle, ring, pinky extended)
        val okLm = buildHandLandmarks(indexExtended = false, middleExtended = true, ringExtended = true, pinkyExtended = true, thumbExtended = true, pinchThumbIndex = true)
        val (gOk, cOk) = classifier.classifyStaticPose(okLm, palmCenter, palmScale, null, Sensitivity.MEDIUM)
        assertEquals(GestureType.OK_SIGN, gOk)
        assertTrue(cOk > 0.80f)
    }

    @Test
    fun testDynamicSwipe_upwardMotion() {
        val classifier = GestureClassifier(null)

        // Simulate 5 frames of rapid upward hand movement
        var result: com.example.vision.GestureRecognitionResult? = null
        val startTime = System.currentTimeMillis()

        for (i in 0..5) {
            val yPos = 0.70f - (i * 0.08f) // moving from 0.70 to 0.30
            val landmarks = MutableList(21) { idx ->
                NormalizedPoint(0.5f, yPos + (idx * 0.005f), 0f)
            }
            // Ensure wrist and middle MCP define a healthy palmScale
            landmarks[0] = NormalizedPoint(0.5f, yPos + 0.15f, 0f)
            landmarks[9] = NormalizedPoint(0.5f, yPos, 0f)

            val hand = HandData(
                isPresent = true,
                landmarks = landmarks,
                confidence = 0.95f,
                centerX = 0.5f,
                centerY = yPos,
                handSize = 0.35f,
                distance = HandDistance.GOOD
            )

            Thread.sleep(30L)
            result = classifier.processHand(
                hand = hand,
                sensitivity = Sensitivity.MEDIUM,
                cooldown = Cooldown.MEDIUM,
                confirmation = GestureConfirmation.OFF,
                handPreference = HandPreference.AUTO,
                isControlActive = true
            )
            if (result.gesture == GestureType.SWIPE_UP) break
        }

        assertNotNull(result)
        assertEquals(GestureType.SWIPE_UP, result!!.gesture)
        assertEquals(GestureLifecycleState.EXECUTED, result.state)
    }
}
