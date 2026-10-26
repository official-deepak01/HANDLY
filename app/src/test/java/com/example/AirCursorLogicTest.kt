package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.action.ActionEngine
import com.example.data.DefaultProfiles
import com.example.data.PreferencesManager
import com.example.model.ActionResult
import com.example.model.ActionType
import com.example.model.CursorCalibrationData
import com.example.model.CursorPoint
import com.example.model.HandData
import com.example.model.HandDistance
import com.example.model.NormalizedPoint
import com.example.model.PinchBehavior
import com.example.vision.AirCursorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AirCursorLogicTest {

    @Test
    fun testAirCursorEngine_processesHandTracking() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = AirCursorEngine(context)

        // Hand pointing with index finger (tip at index 8)
        val landmarks = MutableList(21) { i ->
            NormalizedPoint(0.5f, 0.5f, 0f)
        }
        // Index tip
        landmarks[8] = NormalizedPoint(0.4f, 0.4f, 0f)
        // Thumb tip
        landmarks[4] = NormalizedPoint(0.42f, 0.42f, 0f)

        val hand = HandData(
            isPresent = true,
            landmarks = landmarks,
            confidence = 0.95f,
            centerX = 0.5f,
            centerY = 0.5f,
            handSize = 0.3f,
            distance = HandDistance.GOOD
        )

        val cursorState = engine.processFrame(
            hand = hand,
            screenWidth = 1080f,
            screenHeight = 2400f,
            sensitivity = 1.0f,
            smoothing = 0.5f,
            pinchBehavior = PinchBehavior.CURSOR_CLICK
        )

        assertNotNull(cursorState)
        assertTrue(cursorState.isPointing)
        assertTrue(cursorState.pinchDistance >= 0f)
        assertTrue(cursorState.screenX >= 0f)
        assertTrue(cursorState.screenY >= 0f)
    }

    @Test
    fun testAirCursorEngine_calibrationMapping() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = AirCursorEngine(context)

        val customCalibration = CursorCalibrationData(
            center = CursorPoint(0.5f, 0.5f),
            topLeft = CursorPoint(0.2f, 0.2f),
            topRight = CursorPoint(0.8f, 0.2f),
            bottomLeft = CursorPoint(0.2f, 0.8f),
            bottomRight = CursorPoint(0.8f, 0.8f),
            isCalibrated = true
        )

        val landmarks = MutableList(21) { i ->
            NormalizedPoint(0.5f, 0.5f, 0f)
        }
        val hand = HandData(
            isPresent = true,
            landmarks = landmarks,
            confidence = 0.9f,
            centerX = 0.5f,
            centerY = 0.5f,
            handSize = 0.3f,
            distance = HandDistance.GOOD
        )

        val cursorState = engine.processFrame(
            hand = hand,
            screenWidth = 1080f,
            screenHeight = 2400f,
            calibration = customCalibration
        )

        assertTrue(cursorState.normalizedX in 0f..1f)
        assertTrue(cursorState.normalizedY in 0f..1f)
    }

    @Test
    fun testDefaultProfiles_allBuiltIns() {
        val profiles = DefaultProfiles.getAllBuiltInProfiles()
        assertTrue(profiles.isNotEmpty())

        val youtubeProfile = profiles.find { it.packageName == "com.google.android.youtube" }
        assertNotNull(youtubeProfile)
        assertEquals(DefaultProfiles.YOUTUBE.name, youtubeProfile?.name)
    }

    @Test
    fun testActionEngine_executionHandling() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val engine = ActionEngine(context)

        val outcome = engine.executeAction(ActionType.BACK)
        assertNotNull(outcome)
        assertTrue(
            outcome.result == ActionResult.SERVICE_DISABLED ||
            outcome.result == ActionResult.SUCCESS ||
            outcome.result == ActionResult.FAILED
        )
    }
}
