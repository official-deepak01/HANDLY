package com.example.vision

import com.example.model.Cooldown
import com.example.model.GestureConfirmation
import com.example.model.GestureLifecycleState
import com.example.model.GestureType
import com.example.model.HandDistance
import com.example.model.HandPreference
import com.example.model.NormalizedPoint
import com.example.model.Sensitivity
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

data class GestureRecognitionResult(
    val gesture: GestureType?,
    val confidence: Float,
    val state: GestureLifecycleState,
    val isSafetyActivationTrigger: Boolean = false,
    val isSafetyDeactivationTrigger: Boolean = false,
    val distance: HandDistance = HandDistance.NO_HAND,
    val debugInfo: String = ""
)

class GestureClassifier(private val calibrationManager: GestureCalibrationManager? = null) {

    // Dynamic Swipe tracking with Palm Center
    private val motionHistory = ArrayDeque<Pair<NormalizedPoint, Long>>()
    private val MOTION_WINDOW_MS = 600L

    // Temporal smoothing window for static poses
    private val recentClassifications = ArrayDeque<Pair<GestureType?, Float>>()
    private val RECENT_BUFFER_SIZE = 5

    // State machine tracking
    private var currentCandidateGesture: GestureType? = null
    private var candidateFirstSeenTime: Long = 0L
    private var candidateConsecutiveFrames: Int = 0
    private var lastExecutedTime: Long = 0L
    private var lastExecutedGesture: GestureType? = null

    // Pose release lock: prevents repeat firing while holding a pose (e.g. Point or Peace)
    private var lockedExecutedGesture: GestureType? = null
    private var lastPoseReleaseTime: Long = 0L

    // Pinch state machine
    private var isPinchActive = false

    // Swipe lock: requires hand movement to settle before recognizing another swipe
    private var isSwipeConsumed = false
    private var swipeCalmStartTime: Long = 0L

    // Safety hold timers (Activation = Open Palm 1s, Deactivation = Fist 1s)
    private var openPalmHoldStartTime: Long = 0L
    private var fistHoldStartTime: Long = 0L
    private val SAFETY_HOLD_DURATION_MS = 1000L

    // Hand tracking loss grace period (hysteresis) to prevent flicker from 1-frame dropouts
    private var lastHandSeenTime: Long = 0L
    private val HAND_LOSS_GRACE_MS = 220L

    fun reset() {
        motionHistory.clear()
        recentClassifications.clear()
        currentCandidateGesture = null
        candidateFirstSeenTime = 0L
        candidateConsecutiveFrames = 0
        lastExecutedTime = 0L
        lastExecutedGesture = null
        lockedExecutedGesture = null
        lastPoseReleaseTime = 0L
        isPinchActive = false
        isSwipeConsumed = false
        swipeCalmStartTime = 0L
        openPalmHoldStartTime = 0L
        fistHoldStartTime = 0L
        lastHandSeenTime = 0L
    }

    fun processHand(
        hand: HandData,
        sensitivity: Sensitivity,
        cooldown: Cooldown,
        confirmation: GestureConfirmation = GestureConfirmation.NORMAL,
        handPreference: HandPreference = HandPreference.AUTO,
        isControlActive: Boolean
    ): GestureRecognitionResult {
        val now = System.currentTimeMillis()

        if (!hand.isPresent || hand.landmarks.size < 21) {
            val elapsedSinceHand = now - lastHandSeenTime
            if (lastHandSeenTime != 0L && elapsedSinceHand < HAND_LOSS_GRACE_MS) {
                // Within hysteresis grace period: preserve state without flicker
                return GestureRecognitionResult(
                    gesture = currentCandidateGesture ?: lockedExecutedGesture,
                    confidence = 0.5f,
                    state = GestureLifecycleState.DETECTING,
                    distance = HandDistance.NO_HAND,
                    debugInfo = "Hand tracking grace period (${elapsedSinceHand}ms)"
                )
            }
            motionHistory.clear()
            recentClassifications.clear()
            currentCandidateGesture = null
            candidateConsecutiveFrames = 0
            openPalmHoldStartTime = 0L
            fistHoldStartTime = 0L
            isPinchActive = false
            isSwipeConsumed = false
            lockedExecutedGesture = null
            return GestureRecognitionResult(
                gesture = null,
                confidence = 0f,
                state = GestureLifecycleState.READY,
                distance = HandDistance.NO_HAND,
                debugInfo = "No hand detected"
            )
        }

        lastHandSeenTime = now

        val features = HandFeatureExtractor.extract(hand.landmarks)
        val distance = features?.distance ?: hand.distance

        // Filter by Hand Preference (if user strictly configured Right or Left Hand)
        if (handPreference != HandPreference.AUTO && features != null) {
            if (features.detectedHand != handPreference) {
                return GestureRecognitionResult(
                    gesture = null,
                    confidence = hand.confidence,
                    state = GestureLifecycleState.READY,
                    distance = distance,
                    debugInfo = "Ignored hand (${features.detectedHand.displayName}) - Expected ${handPreference.displayName}"
                )
            }
        }

        // Calculate stable palm center from landmarks 0 (Wrist), 5 (Index MCP), 9 (Middle MCP), 13 (Ring MCP), 17 (Pinky MCP)
        val wrist = hand.landmarks[0]
        val indexMcp = hand.landmarks[5]
        val middleMcp = hand.landmarks[9]
        val ringMcp = hand.landmarks[13]
        val pinkyMcp = hand.landmarks[17]

        val palmCenter = NormalizedPoint(
            x = (wrist.x + indexMcp.x + middleMcp.x + ringMcp.x + pinkyMcp.x) / 5f,
            y = (wrist.y + indexMcp.y + middleMcp.y + ringMcp.y + pinkyMcp.y) / 5f,
            z = (wrist.z + indexMcp.z + middleMcp.z + ringMcp.z + pinkyMcp.z) / 5f
        )

        val palmScale = features?.palmScale ?: dist(wrist, middleMcp).coerceAtLeast(0.02f)

        // Maintain motion history
        pruneMotionHistory(now)
        motionHistory.addLast(palmCenter to now)

        // 1. Static Gesture Classification (Blended Heuristics + Multi-joint features + Calibration)
        val (rawStaticGesture, staticConfidence) = classifyStaticPose(hand.landmarks, palmCenter, palmScale, features, sensitivity)

        // 2. Dynamic Gesture Check (Swipes) - strictly disabled if hand is in a pointing/holding static pose
        val dynamicSwipe = detectSwipe(sensitivity, palmScale, rawStaticGesture)
        if (dynamicSwipe != null) {
            if (!isSwipeConsumed) {
                isSwipeConsumed = true
                swipeCalmStartTime = 0L
                lastExecutedTime = now
                lastExecutedGesture = dynamicSwipe
                lockedExecutedGesture = null
                currentCandidateGesture = null
                candidateConsecutiveFrames = 0

                return GestureRecognitionResult(
                    gesture = dynamicSwipe,
                    confidence = hand.confidence.coerceIn(0.85f, 0.98f),
                    state = GestureLifecycleState.EXECUTED,
                    distance = distance,
                    debugInfo = "Dynamic Swipe: ${dynamicSwipe.displayName}"
                )
            }
        } else {
            // Unlock swipe once hand motion calms down
            if (isMovementCalm()) {
                if (swipeCalmStartTime == 0L) swipeCalmStartTime = now
                if (now - swipeCalmStartTime >= 120L) {
                    isSwipeConsumed = false
                }
            } else {
                swipeCalmStartTime = 0L
            }
        }

        // Record in temporal smoothing buffer
        if (recentClassifications.size >= RECENT_BUFFER_SIZE) {
            recentClassifications.removeFirst()
        }
        recentClassifications.addLast(rawStaticGesture to staticConfidence)

        // Determine smoothed gesture via majority vote
        val smoothedGesture = getSmoothedGesture()

        // 3. Safety Activation / Deactivation Holds (Always active out of the box)
        if (rawStaticGesture == GestureType.OPEN_PALM || smoothedGesture == GestureType.OPEN_PALM) {
            if (openPalmHoldStartTime == 0L) openPalmHoldStartTime = now
            val holdDuration = now - openPalmHoldStartTime
            if (!isControlActive && holdDuration >= SAFETY_HOLD_DURATION_MS) {
                openPalmHoldStartTime = 0L
                return GestureRecognitionResult(
                    gesture = GestureType.OPEN_PALM,
                    confidence = staticConfidence,
                    state = GestureLifecycleState.EXECUTED,
                    isSafetyActivationTrigger = true,
                    distance = distance,
                    debugInfo = "Safety Activation: Open Palm held 1s"
                )
            } else if (!isControlActive) {
                return GestureRecognitionResult(
                    gesture = GestureType.OPEN_PALM,
                    confidence = staticConfidence,
                    state = GestureLifecycleState.STABLE,
                    distance = distance,
                    debugInfo = "Hold Open Palm to activate (${holdDuration}/$SAFETY_HOLD_DURATION_MS ms)"
                )
            }
        } else {
            openPalmHoldStartTime = 0L
        }

        if (rawStaticGesture == GestureType.FIST || smoothedGesture == GestureType.FIST) {
            if (fistHoldStartTime == 0L) fistHoldStartTime = now
            val holdDuration = now - fistHoldStartTime
            if (isControlActive && holdDuration >= SAFETY_HOLD_DURATION_MS) {
                fistHoldStartTime = 0L
                return GestureRecognitionResult(
                    gesture = GestureType.FIST,
                    confidence = staticConfidence,
                    state = GestureLifecycleState.EXECUTED,
                    isSafetyDeactivationTrigger = true,
                    distance = distance,
                    debugInfo = "Safety Deactivation: Fist held 1s"
                )
            } else if (isControlActive && holdDuration >= 350L) {
                return GestureRecognitionResult(
                    gesture = GestureType.FIST,
                    confidence = staticConfidence,
                    state = GestureLifecycleState.STABLE,
                    distance = distance,
                    debugInfo = "Hold Fist to pause (${holdDuration}/$SAFETY_HOLD_DURATION_MS ms)"
                )
            }
        } else {
            fistHoldStartTime = 0L
        }

        // Release locked static gesture if hand moved to another pose or neutral
        if (lockedExecutedGesture != null) {
            if (rawStaticGesture != lockedExecutedGesture) {
                if (lastPoseReleaseTime == 0L) lastPoseReleaseTime = now
                if (now - lastPoseReleaseTime >= 150L) {
                    lockedExecutedGesture = null
                    lastPoseReleaseTime = 0L
                }
            } else {
                lastPoseReleaseTime = 0L
                // Pose is held: suppress repeat execution
                return GestureRecognitionResult(
                    gesture = lockedExecutedGesture,
                    confidence = staticConfidence,
                    state = GestureLifecycleState.STABLE,
                    distance = distance,
                    debugInfo = "Pose ${lockedExecutedGesture?.displayName} held (release to re-trigger)"
                )
            }
        }

        // 4. Pinch Handling (Instant Tap Trigger)
        if (rawStaticGesture == GestureType.PINCH) {
            if (!isPinchActive) {
                isPinchActive = true
                val timeSinceLast = now - lastExecutedTime
                if (timeSinceLast >= cooldown.durationMs || lastExecutedGesture != GestureType.PINCH) {
                    lastExecutedTime = now
                    lastExecutedGesture = GestureType.PINCH
                    return GestureRecognitionResult(
                        gesture = GestureType.PINCH,
                        confidence = staticConfidence,
                        state = GestureLifecycleState.EXECUTED,
                        distance = distance,
                        debugInfo = "Pinch Triggered Tap"
                    )
                }
            } else {
                return GestureRecognitionResult(
                    gesture = GestureType.PINCH,
                    confidence = staticConfidence,
                    state = GestureLifecycleState.STABLE,
                    distance = distance,
                    debugInfo = "Pinch Holding (active)"
                )
            }
        } else {
            isPinchActive = false
        }

        // 5. Confidence and Ambiguity Filter
        val activeGesture = smoothedGesture ?: rawStaticGesture
        val threshold = sensitivity.confidenceThreshold
        if (activeGesture == null || staticConfidence < threshold) {
            currentCandidateGesture = null
            candidateFirstSeenTime = 0L
            candidateConsecutiveFrames = 0
            val isInCooldown = (now - lastExecutedTime) < cooldown.durationMs
            return GestureRecognitionResult(
                gesture = null,
                confidence = staticConfidence,
                state = if (isInCooldown) GestureLifecycleState.COOLDOWN else GestureLifecycleState.READY,
                distance = distance,
                debugInfo = if (activeGesture != null) "Confidence (${(staticConfidence * 100).toInt()}%) below threshold (${(threshold * 100).toInt()}%)" else "Neutral hand"
            )
        }

        // 6. Check Cooldown Window
        val timeSinceLastExecution = now - lastExecutedTime
        if (timeSinceLastExecution < cooldown.durationMs) {
            return GestureRecognitionResult(
                gesture = activeGesture,
                confidence = staticConfidence,
                state = GestureLifecycleState.COOLDOWN,
                distance = distance,
                debugInfo = "In cooldown (${timeSinceLastExecution}ms / ${cooldown.durationMs}ms)"
            )
        }

        // 7. State Machine: DETECTING -> CANDIDATE -> STABLE -> CONFIRMED -> EXECUTED
        if (currentCandidateGesture != activeGesture) {
            currentCandidateGesture = activeGesture
            candidateFirstSeenTime = now
            candidateConsecutiveFrames = 1
            return GestureRecognitionResult(
                gesture = activeGesture,
                confidence = staticConfidence,
                state = GestureLifecycleState.DETECTING,
                distance = distance,
                debugInfo = "Detecting: ${activeGesture.displayName}"
            )
        }

        candidateConsecutiveFrames++
        val durationHeld = now - candidateFirstSeenTime

        if (candidateConsecutiveFrames == 2) {
            return GestureRecognitionResult(
                gesture = activeGesture,
                confidence = staticConfidence,
                state = GestureLifecycleState.CANDIDATE,
                distance = distance,
                debugInfo = "Candidate: ${activeGesture.displayName}"
            )
        }

        // Required Hold & Stability Frames based on Sensitivity & Confirmation
        val requiredHoldMs = max(sensitivity.minHoldMs, confirmation.minHoldMs)
        val requiredFrames = confirmation.stabilityFrames

        if (durationHeld < requiredHoldMs || candidateConsecutiveFrames < requiredFrames) {
            return GestureRecognitionResult(
                gesture = activeGesture,
                confidence = staticConfidence,
                state = GestureLifecycleState.STABLE,
                distance = distance,
                debugInfo = "Stabilizing ${activeGesture.displayName} ($durationHeld/$requiredHoldMs ms)"
            )
        }

        // Gesture Confirmed and Executed!
        lastExecutedTime = now
        lastExecutedGesture = activeGesture
        lockedExecutedGesture = activeGesture
        currentCandidateGesture = null
        candidateFirstSeenTime = 0L
        candidateConsecutiveFrames = 0

        return GestureRecognitionResult(
            gesture = activeGesture,
            confidence = staticConfidence,
            state = GestureLifecycleState.EXECUTED,
            distance = distance,
            debugInfo = "Confirmed & Executed ${activeGesture.displayName}"
        )
    }

    private fun getSmoothedGesture(): GestureType? {
        if (recentClassifications.size < 3) return null
        val counts = mutableMapOf<GestureType, Int>()
        var nullCount = 0
        for ((g, _) in recentClassifications) {
            if (g == null) nullCount++
            else counts[g] = (counts[g] ?: 0) + 1
        }
        val best = counts.maxByOrNull { it.value } ?: return null
        // Require at least 2 out of recent frames
        return if (best.value >= 2) best.key else null
    }

    private fun pruneMotionHistory(now: Long) {
        while (motionHistory.isNotEmpty() && (now - motionHistory.first().second) > MOTION_WINDOW_MS) {
            motionHistory.removeFirst()
        }
    }

    private fun isMovementCalm(): Boolean {
        if (motionHistory.size < 3) return true
        val first = motionHistory.first().first
        val last = motionHistory.last().first
        return hypot(last.x - first.x, last.y - first.y) < 0.035f
    }

    private fun detectSwipe(sensitivity: Sensitivity, palmScale: Float, staticGesture: GestureType? = null): GestureType? {
        if (motionHistory.size < 3) return null
        val oldest = motionHistory.first()
        val newest = motionHistory.last()
        val durationMs = newest.second - oldest.second

        // A deliberate swipe happens between 70ms and 550ms
        if (durationMs < 70L || durationMs > 550L) return null

        val deltaX = newest.first.x - oldest.first.x
        val deltaY = newest.first.y - oldest.first.y

        val displacement = hypot(deltaX, deltaY)
        val normDisplacement = displacement / palmScale.coerceAtLeast(0.05f)

        // Disallow dynamic swipe when holding a fine interaction gesture pose (Point or Pinch)
        // UNLESS the hand is moving with deliberate swift displacement (intentional swipe)
        if (normDisplacement < 0.45f && (staticGesture == GestureType.INDEX_POINT || staticGesture == GestureType.PINCH)) {
            return null
        }

        // Threshold adjusted by sensitivity
        val requiredNormDisplacement = when (sensitivity) {
            Sensitivity.LOW -> 0.55f
            Sensitivity.MEDIUM -> 0.38f
            Sensitivity.HIGH -> 0.28f
        }

        if (normDisplacement < requiredNormDisplacement) return null

        // Trajectory straightness check: rejects circular movement, erratic shaking, or random movements
        var pathLength = 0f
        for (i in 0 until motionHistory.size - 1) {
            val p1 = motionHistory[i].first
            val p2 = motionHistory[i + 1].first
            pathLength += hypot(p2.x - p1.x, p2.y - p1.y)
        }
        if (pathLength > 0f) {
            val straightness = displacement / pathLength
            if (straightness < 0.60f) return null
        }

        // Velocity check: normalized displacement per second
        val velocity = normDisplacement / (durationMs / 1000f)
        if (velocity < 0.90f) return null

        val absX = abs(deltaX)
        val absY = abs(deltaY)

        // Direction dominance ratio: reject ambiguous diagonal movement
        val DOMINANCE_RATIO = 1.35f

        // Vertical Dominance
        if (absY > absX * DOMINANCE_RATIO) {
            return if (deltaY < 0) GestureType.SWIPE_UP else GestureType.SWIPE_DOWN
        }

        // Horizontal Dominance
        // In front camera image analysis:
        // Moving user's hand to their right moves coordinates towards 0 (deltaX < 0)
        // Moving user's hand to their left moves coordinates towards 1 (deltaX > 0)
        if (absX > absY * DOMINANCE_RATIO) {
            return if (deltaX < 0) GestureType.SWIPE_LEFT else GestureType.SWIPE_RIGHT
        }

        return null
    }

    /**
     * Highly robust multi-feature static pose classifier.
     * Uses 21 MediaPipe hand landmarks, relative bone vectors, and palm-normalized metrics.
     */
    fun classifyStaticPose(
        landmarks: List<NormalizedPoint>,
        palmCenter: NormalizedPoint,
        palmScale: Float,
        features: NormalizedHandFeatures?,
        sensitivity: Sensitivity
    ): Pair<GestureType?, Float> {
        // First check user's personalized calibration if available
        if (features != null && calibrationManager != null) {
            val candidates = listOf(
                GestureType.OPEN_PALM,
                GestureType.FIST,
                GestureType.THUMBS_UP,
                GestureType.THUMBS_DOWN,
                GestureType.INDEX_POINT,
                GestureType.TWO_FINGERS,
                GestureType.PINCH,
                GestureType.OK_SIGN
            )
            var bestCalibratedGesture: GestureType? = null
            var bestCalibratedScore = 0f

            for (g in candidates) {
                val score = calibrationManager.matchCalibratedPose(g, features)
                if (score != null && score > 0.72f && score > bestCalibratedScore) {
                    bestCalibratedScore = score
                    bestCalibratedGesture = g
                }
            }

            if (bestCalibratedGesture != null && bestCalibratedScore >= (sensitivity.confidenceThreshold * 0.88f)) {
                return bestCalibratedGesture to bestCalibratedScore
            }
        }

        // Baseline Anatomical Landmark Analysis across all 21 points
        val wrist = landmarks[0]

        // Thumb: 1=CMC, 2=MCP, 3=IP, 4=Tip
        val thumbMcp = landmarks[2]
        val thumbTip = landmarks[4]

        // Index: 5=MCP, 6=PIP, 7=DIP, 8=Tip
        val indexMcp = landmarks[5]
        val indexPip = landmarks[6]
        val indexTip = landmarks[8]

        // Middle: 9=MCP, 10=PIP, 11=DIP, 12=Tip
        val middleMcp = landmarks[9]
        val middlePip = landmarks[10]
        val middleTip = landmarks[12]

        // Ring: 13=MCP, 14=PIP, 15=DIP, 16=Tip
        val ringMcp = landmarks[13]
        val ringPip = landmarks[14]
        val ringTip = landmarks[16]

        // Pinky: 17=MCP, 18=PIP, 19=DIP, 20=Tip
        val pinkyMcp = landmarks[17]
        val pinkyPip = landmarks[18]
        val pinkyTip = landmarks[20]

        // Finger Extension Evaluation
        // Computes independent extension score in [0.0, 1.0] for Index, Middle, Ring, Pinky
        val extIndex = evaluateFingerExtension(indexTip, indexPip, indexMcp, palmCenter, wrist, palmScale)
        val extMiddle = evaluateFingerExtension(middleTip, middlePip, middleMcp, palmCenter, wrist, palmScale)
        val extRing = evaluateFingerExtension(ringTip, ringPip, ringMcp, palmCenter, wrist, palmScale)
        val extPinky = evaluateFingerExtension(pinkyTip, pinkyPip, pinkyMcp, palmCenter, wrist, palmScale)

        val isIndexExt = extIndex >= 0.52f
        val isMiddleExt = extMiddle >= 0.52f
        val isRingExt = extRing >= 0.50f
        val isPinkyExt = extPinky >= 0.48f

        val isIndexCurled = extIndex <= 0.45f
        val isMiddleCurled = extMiddle <= 0.45f
        val isRingCurled = extRing <= 0.45f
        val isPinkyCurled = extPinky <= 0.45f

        // Thumb Evaluation
        val thumbTipToMcp = dist(thumbTip, thumbMcp) / palmScale
        val thumbTipToIndexMcp = dist(thumbTip, indexMcp) / palmScale
        val thumbToWrist = dist(thumbTip, wrist) / palmScale
        val thumbDy = (thumbTip.y - thumbMcp.y) / palmScale

        // Fingertip Spacings
        val pinchRatio = dist(thumbTip, indexTip) / palmScale
        val peaceSpread = dist(indexTip, middleTip) / palmScale

        val pinchThreshold = when (sensitivity) {
            Sensitivity.LOW -> 0.28f
            Sensitivity.MEDIUM -> 0.36f
            Sensitivity.HIGH -> 0.44f
        }

        // --- Gesture Matching ---

        // 1. OK SIGN: Thumb and Index tips touch, while Middle, Ring, Pinky are EXTENDED
        if (pinchRatio < 0.38f && isMiddleExt && isRingExt && isPinkyExt) {
            val confidence = ((extMiddle + extRing + extPinky) / 3f).coerceIn(0.85f, 0.98f)
            return GestureType.OK_SIGN to confidence
        }

        // 2. OPEN PALM: All 4 fingers extended (Index, Middle, Ring, Pinky)
        if (isIndexExt && isMiddleExt && isRingExt && isPinkyExt) {
            val thumbBonus = if (thumbTipToMcp >= 0.42f) 0.05f else 0.0f
            val confidence = (((extIndex + extMiddle + extRing + extPinky) / 4f) + thumbBonus).coerceIn(0.88f, 0.99f)
            return GestureType.OPEN_PALM to confidence
        }

        // 3. THUMBS UP: All 4 fingers curled + thumb extended pointing UP + NOT pinched
        if (isIndexCurled && isMiddleCurled && isRingCurled && isPinkyCurled &&
            thumbDy < -0.15f && thumbTip.y < indexMcp.y && pinchRatio >= 0.36f && thumbTipToIndexMcp >= 0.36f) {
            val confidence = 0.95f
            return GestureType.THUMBS_UP to confidence
        }

        // 4. THUMBS DOWN: All 4 fingers curled + thumb extended pointing DOWN + NOT pinched
        if (isIndexCurled && isMiddleCurled && isRingCurled && isPinkyCurled &&
            thumbDy > 0.15f && thumbTip.y > thumbMcp.y && thumbTipToMcp >= 0.38f && pinchRatio >= 0.36f) {
            val confidence = 0.95f
            return GestureType.THUMBS_DOWN to confidence
        }

        // 5. PINCH: Thumb tip and Index tip touching/close, thumb active (not tucked in fist)
        if (pinchRatio < pinchThreshold && (thumbTipToMcp >= 0.35f || indexTip.y < indexMcp.y) && (!isMiddleExt || !isRingExt)) {
            val confidence = ((pinchThreshold - pinchRatio) / pinchThreshold).coerceIn(0.78f, 0.98f)
            return GestureType.PINCH to confidence
        }

        // 6. FIST: All 4 fingers curled + thumb folded/neutral (not pointing up or down)
        if (isIndexCurled && isMiddleCurled && isRingCurled && isPinkyCurled && (pinchRatio >= pinchThreshold || thumbTipToMcp < 0.35f)) {
            val confidence = 0.93f
            return GestureType.FIST to confidence
        }

        // 7. TWO FINGERS (V / Peace sign): Index & Middle extended, Ring & Pinky curled
        if (isIndexExt && isMiddleExt && isRingCurled && isPinkyCurled && peaceSpread >= 0.14f) {
            val confidence = ((extIndex + extMiddle) / 2f).coerceIn(0.88f, 0.96f)
            return GestureType.TWO_FINGERS to confidence
        }

        // 8. INDEX POINT: Index extended, Middle, Ring, Pinky curled, pinch not touching
        if (isIndexExt && isMiddleCurled && isRingCurled && isPinkyCurled && pinchRatio > 0.35f) {
            val confidence = extIndex.coerceIn(0.86f, 0.96f)
            return GestureType.INDEX_POINT to confidence
        }

        return null to 0.40f
    }

    /**
     * Evaluates a finger's extension score between 0.0 (tightly curled) and 1.0 (fully extended).
     * Combines tip-to-MCP, tip-to-wrist, and tip-to-palmCenter normalized geometric ratios.
     */
    private fun evaluateFingerExtension(
        tip: NormalizedPoint,
        pip: NormalizedPoint,
        mcp: NormalizedPoint,
        palmCenter: NormalizedPoint,
        wrist: NormalizedPoint,
        palmScale: Float
    ): Float {
        // Ratio 1: Tip to MCP normalized by palm scale
        val tipToMcp = dist(tip, mcp) / palmScale
        val scoreMcp = ((tipToMcp - 0.50f) / 0.38f).coerceIn(0f, 1f)

        // Ratio 2: Tip to Wrist vs PIP to Wrist
        val tipToWrist = dist(tip, wrist)
        val pipToWrist = dist(pip, wrist).coerceAtLeast(0.001f)
        val wristRatio = tipToWrist / pipToWrist
        val scoreWrist = ((wristRatio - 0.98f) / 0.16f).coerceIn(0f, 1f)

        // Ratio 3: Tip to Palm Center
        val tipToCenter = dist(tip, palmCenter) / palmScale
        val scoreCenter = ((tipToCenter - 0.65f) / 0.35f).coerceIn(0f, 1f)

        return (scoreMcp * 0.45f + scoreWrist * 0.35f + scoreCenter * 0.20f).coerceIn(0f, 1f)
    }

    private fun dist(p1: NormalizedPoint, p2: NormalizedPoint): Float {
        return hypot(p1.x - p2.x, p1.y - p2.y)
    }
}
