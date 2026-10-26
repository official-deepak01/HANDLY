# HANDLY — Hands-Free Android Gesture Controller

> **Control your phone hands-free.**  
> *Your hands. Your control.*

HANDLY is an Android application that uses on-device computer vision and hand landmark tracking to control phone actions through intuitive hand gestures.

---

## 🌟 Key Features

- **Live Hand Tracking**: Real-time 21 3D hand landmark detection using MediaPipe.
- **Hierarchical Gesture Engine**:
  - Distinguishes static poses (Open Palm, Fist, Point, Peace / V, Thumbs Up, Thumbs Down, Pinch, OK Sign).
  - Recognizes dynamic gestures (Swipe Left, Swipe Right, Swipe Up, Swipe Down) with trajectory straightness, velocity verification, and axis dominance.
  - Hysteresis, smoothing, debounce, and cooldown states to prevent false triggers.
- **System-Wide Accessibility Actions**:
  - Global Navigation (Back, Home, Recents, Notifications, Quick Settings).
  - Screen Interaction (Scroll Up, Scroll Down, Tap / Click).
- **Personalized Gesture Calibration**:
  - Interactive calibration wizard to train custom poses for unique hand shapes.
- **Adaptive Jetpack Compose UI**:
  - Modern Material Design 3 interface with Deep Graphite and Electric Violet branding.
  - Adaptive layout supporting smartphones (Bottom Navigation) and Foldables / Tablets (Navigation Rail).
- **Safety Lock & Cooldown**:
  - Intentional hold gestures (e.g. hold Open Palm to activate, hold Fist to pause) to avoid accidental system actions.

---

## 🏗 Architecture & Flow

```text
Camera Preview (Front Camera via CameraX)
   ↓
Frame Processing (MediaPipe Hand Landmark Tracking)
   ↓
21 3D Landmark Extraction (HandFeatureExtractor)
   ↓
Gesture Classifier (Geometric & Temporal Engine)
   ↓
Temporal Validation & Cooldown State Machine
   ↓
Action Dispatcher / Accessibility Service
   ↓
Android System Actions (Home, Back, Recents, Scroll, Tap)
```

---

## 🛠 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Computer Vision**: Google MediaPipe Hands & CameraX
- **Architecture**: MVVM with Coroutines & StateFlow
- **System Control**: Android AccessibilityService
- **Testing**: JUnit & Robolectric for JVM testing

---

## 🚀 Building & Running

### Requirements
- Android Studio Ladybug (or newer)
- Android SDK 34+
- Java 17

### Build via Gradle
```bash
# Run unit tests
gradle :app:testDebugUnitTest

# Assemble Debug APK
gradle :app:assembleDebug
```

---

## 🔒 Permissions

HANDLY requests only the permissions strictly required to perform hands-free controls:
- `CAMERA`: Required to process front camera frames on-device for hand tracking. Camera frames are processed locally and never recorded or transmitted.
- `ACCESSIBILITY_SERVICE`: Used to perform system actions (Back, Home, Recents, Scroll, Tap) on behalf of the user when gestures are triggered.
- `POST_NOTIFICATIONS`: Displays an ongoing foreground service notification while Live Control is active.
