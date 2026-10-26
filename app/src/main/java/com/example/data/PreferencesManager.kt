package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.ActionHistoryItem
import com.example.model.ActionResult
import com.example.model.ActionType
import com.example.model.BatteryMode
import com.example.model.ControlMode
import com.example.model.Cooldown
import com.example.model.CursorCalibrationData
import com.example.model.CursorPoint
import com.example.model.GestureConfirmation
import com.example.model.GestureProfile
import com.example.model.GestureType
import com.example.model.HandPreference
import com.example.model.PinchBehavior
import com.example.model.ProfileMapping
import com.example.model.Sensitivity
import com.example.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "gesture_control_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
        val KEY_ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
        val KEY_SENSITIVITY = stringPreferencesKey("sensitivity")
        val KEY_COOLDOWN = stringPreferencesKey("cooldown")
        val KEY_HAPTIC = booleanPreferencesKey("haptic_feedback")
        val KEY_VISUAL_FEEDBACK = booleanPreferencesKey("visual_feedback")
        val KEY_AUTO_PROFILE_SWITCH = booleanPreferencesKey("auto_profile_switch")
        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_HAND_PREFERENCE = stringPreferencesKey("hand_preference")
        val KEY_GESTURE_CONFIRMATION = stringPreferencesKey("gesture_confirmation")
        val KEY_CUSTOM_PROFILES_JSON = stringPreferencesKey("custom_profiles_json")
        val KEY_PROFILES_OVERRIDE_JSON = stringPreferencesKey("profiles_override_json")
        val KEY_IS_PRO_USER = booleanPreferencesKey("is_pro_user")

        // Air Cursor & Control Mode Keys
        val KEY_CONTROL_MODE = stringPreferencesKey("control_mode")
        val KEY_PINCH_BEHAVIOR = stringPreferencesKey("pinch_behavior")
        val KEY_BATTERY_MODE = stringPreferencesKey("battery_mode")
        val KEY_CURSOR_SENSITIVITY = floatPreferencesKey("cursor_sensitivity")
        val KEY_CURSOR_SMOOTHING = floatPreferencesKey("cursor_smoothing")
        val KEY_CURSOR_ACCELERATION = booleanPreferencesKey("cursor_acceleration")
        val KEY_CURSOR_DEADZONE = floatPreferencesKey("cursor_deadzone")
        val KEY_CURSOR_CLICK_THRESHOLD = floatPreferencesKey("cursor_click_threshold")
        val KEY_CURSOR_CLICK_COOLDOWN = longPreferencesKey("cursor_click_cooldown")
        val KEY_DWELL_CLICK_ENABLED = booleanPreferencesKey("dwell_click_enabled")
        val KEY_DWELL_DURATION_MS = longPreferencesKey("dwell_duration_ms")
        val KEY_FLOATING_OVERLAY_ENABLED = booleanPreferencesKey("floating_overlay_enabled")
        val KEY_DEBUG_MODE_ENABLED = booleanPreferencesKey("debug_mode_enabled")
        val KEY_HISTORY_ENABLED = booleanPreferencesKey("history_enabled")
        val KEY_CURSOR_CALIBRATION_JSON = stringPreferencesKey("cursor_calibration_json")
        val KEY_ACTION_HISTORY_JSON = stringPreferencesKey("action_history_json")
    }

    val isOnboardingCompleted: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_DONE] ?: false
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_DONE] = completed
        }
    }

    val activeProfileId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ACTIVE_PROFILE_ID] ?: DefaultProfiles.GENERAL_PHONE.id
    }

    suspend fun setActiveProfileId(id: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_PROFILE_ID] = id
        }
    }

    val sensitivity: Flow<Sensitivity> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_SENSITIVITY] ?: Sensitivity.MEDIUM.name
        try {
            Sensitivity.valueOf(raw)
        } catch (_: Exception) {
            Sensitivity.MEDIUM
        }
    }

    suspend fun setSensitivity(sensitivity: Sensitivity) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SENSITIVITY] = sensitivity.name
        }
    }

    val cooldown: Flow<Cooldown> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_COOLDOWN] ?: Cooldown.MEDIUM.name
        try {
            Cooldown.valueOf(raw)
        } catch (_: Exception) {
            Cooldown.MEDIUM
        }
    }

    suspend fun setCooldown(cooldown: Cooldown) {
        context.dataStore.edit { prefs ->
            prefs[KEY_COOLDOWN] = cooldown.name
        }
    }

    val isHapticFeedbackEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HAPTIC] ?: true
    }

    suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAPTIC] = enabled
        }
    }

    val isVisualFeedbackEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_VISUAL_FEEDBACK] ?: true
    }

    suspend fun setVisualFeedbackEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_VISUAL_FEEDBACK] = enabled
        }
    }

    val isAutoProfileSwitchEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTO_PROFILE_SWITCH] ?: true
    }

    suspend fun setAutoProfileSwitchEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_PROFILE_SWITCH] = enabled
        }
    }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(raw)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(theme: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme.name
        }
    }

    val handPreference: Flow<HandPreference> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_HAND_PREFERENCE] ?: HandPreference.AUTO.name
        try {
            HandPreference.valueOf(raw)
        } catch (_: Exception) {
            HandPreference.AUTO
        }
    }

    suspend fun setHandPreference(pref: HandPreference) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAND_PREFERENCE] = pref.name
        }
    }

    val gestureConfirmation: Flow<GestureConfirmation> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_GESTURE_CONFIRMATION] ?: GestureConfirmation.NORMAL.name
        try {
            GestureConfirmation.valueOf(raw)
        } catch (_: Exception) {
            GestureConfirmation.NORMAL
        }
    }

    suspend fun setGestureConfirmation(conf: GestureConfirmation) {
        context.dataStore.edit { prefs ->
            prefs[KEY_GESTURE_CONFIRMATION] = conf.name
        }
    }

    val isProUser: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_PRO_USER] ?: false
    }

    suspend fun setProUser(isPro: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_PRO_USER] = isPro
        }
    }

    // Control Mode & Air Cursor settings
    val controlMode: Flow<ControlMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_CONTROL_MODE] ?: ControlMode.GESTURE.name
        try { ControlMode.valueOf(raw) } catch (_: Exception) { ControlMode.GESTURE }
    }

    suspend fun setControlMode(mode: ControlMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CONTROL_MODE] = mode.name
        }
    }

    val pinchBehavior: Flow<PinchBehavior> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_PINCH_BEHAVIOR] ?: PinchBehavior.CURSOR_CLICK.name
        try { PinchBehavior.valueOf(raw) } catch (_: Exception) { PinchBehavior.CURSOR_CLICK }
    }

    suspend fun setPinchBehavior(behavior: PinchBehavior) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PINCH_BEHAVIOR] = behavior.name
        }
    }

    val batteryMode: Flow<BatteryMode> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_BATTERY_MODE] ?: BatteryMode.BALANCED.name
        try { BatteryMode.valueOf(raw) } catch (_: Exception) { BatteryMode.BALANCED }
    }

    suspend fun setBatteryMode(mode: BatteryMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BATTERY_MODE] = mode.name
        }
    }

    val cursorSensitivity: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURSOR_SENSITIVITY] ?: 1.2f
    }

    suspend fun setCursorSensitivity(value: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CURSOR_SENSITIVITY] = value.coerceIn(0.5f, 3.0f)
        }
    }

    val cursorSmoothing: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURSOR_SMOOTHING] ?: 0.65f
    }

    suspend fun setCursorSmoothing(value: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CURSOR_SMOOTHING] = value.coerceIn(0.1f, 0.95f)
        }
    }

    val cursorAcceleration: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURSOR_ACCELERATION] ?: true
    }

    suspend fun setCursorAcceleration(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CURSOR_ACCELERATION] = enabled
        }
    }

    val cursorDeadZone: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURSOR_DEADZONE] ?: 0.012f
    }

    suspend fun setCursorDeadZone(value: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CURSOR_DEADZONE] = value.coerceIn(0.002f, 0.05f)
        }
    }

    val cursorClickThreshold: Flow<Float> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURSOR_CLICK_THRESHOLD] ?: 0.07f
    }

    suspend fun setCursorClickThreshold(value: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CURSOR_CLICK_THRESHOLD] = value.coerceIn(0.03f, 0.15f)
        }
    }

    val cursorClickCooldown: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURSOR_CLICK_COOLDOWN] ?: 380L
    }

    suspend fun setCursorClickCooldown(value: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CURSOR_CLICK_COOLDOWN] = value.coerceIn(200L, 1000L)
        }
    }

    val isDwellClickEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DWELL_CLICK_ENABLED] ?: false
    }

    suspend fun setDwellClickEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DWELL_CLICK_ENABLED] = enabled
        }
    }

    val dwellDurationMs: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_DWELL_DURATION_MS] ?: 1200L
    }

    suspend fun setDwellDurationMs(value: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DWELL_DURATION_MS] = value.coerceIn(600L, 2500L)
        }
    }

    val isFloatingOverlayEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_FLOATING_OVERLAY_ENABLED] ?: true
    }

    suspend fun setFloatingOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FLOATING_OVERLAY_ENABLED] = enabled
        }
    }

    val isDebugModeEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEBUG_MODE_ENABLED] ?: false
    }

    suspend fun setDebugModeEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEBUG_MODE_ENABLED] = enabled
        }
    }

    val isHistoryEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_HISTORY_ENABLED] ?: true
    }

    suspend fun setHistoryEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HISTORY_ENABLED] = enabled
        }
    }

    val cursorCalibration: Flow<CursorCalibrationData> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_CURSOR_CALIBRATION_JSON]
        if (!json.isNullOrBlank()) {
            try {
                parseCursorCalibrationJson(json)
            } catch (_: Exception) {
                CursorCalibrationData.DEFAULT
            }
        } else {
            CursorCalibrationData.DEFAULT
        }
    }

    suspend fun saveCursorCalibration(data: CursorCalibrationData) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CURSOR_CALIBRATION_JSON] = serializeCursorCalibration(data)
        }
    }

    suspend fun resetCursorCalibration() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_CURSOR_CALIBRATION_JSON)
        }
    }

    val actionHistory: Flow<List<ActionHistoryItem>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_ACTION_HISTORY_JSON]
        if (!json.isNullOrBlank()) {
            try {
                parseActionHistoryJson(json)
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    suspend fun addActionHistoryItem(item: ActionHistoryItem) {
        context.dataStore.edit { prefs ->
            val json = prefs[KEY_ACTION_HISTORY_JSON]
            val currentList = if (!json.isNullOrBlank()) {
                try { parseActionHistoryJson(json).toMutableList() } catch (_: Exception) { mutableListOf() }
            } else mutableListOf()
            currentList.add(0, item)
            // Cap at 50 recent items
            val trimmed = currentList.take(50)
            prefs[KEY_ACTION_HISTORY_JSON] = serializeActionHistory(trimmed)
        }
    }

    suspend fun clearActionHistory() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACTION_HISTORY_JSON)
        }
    }

    private fun serializeCursorCalibration(data: CursorCalibrationData): String {
        return JSONObject().apply {
            put("centerX", data.center.x.toDouble())
            put("centerY", data.center.y.toDouble())
            put("topLeftX", data.topLeft.x.toDouble())
            put("topLeftY", data.topLeft.y.toDouble())
            put("topRightX", data.topRight.x.toDouble())
            put("topRightY", data.topRight.y.toDouble())
            put("bottomLeftX", data.bottomLeft.x.toDouble())
            put("bottomLeftY", data.bottomLeft.y.toDouble())
            put("bottomRightX", data.bottomRight.x.toDouble())
            put("bottomRightY", data.bottomRight.y.toDouble())
            put("isCalibrated", data.isCalibrated)
            put("timestamp", data.timestamp)
        }.toString()
    }

    private fun parseCursorCalibrationJson(jsonStr: String): CursorCalibrationData {
        val obj = JSONObject(jsonStr)
        return CursorCalibrationData(
            center = CursorPoint(obj.getDouble("centerX").toFloat(), obj.getDouble("centerY").toFloat()),
            topLeft = CursorPoint(obj.getDouble("topLeftX").toFloat(), obj.getDouble("topLeftY").toFloat()),
            topRight = CursorPoint(obj.getDouble("topRightX").toFloat(), obj.getDouble("topRightY").toFloat()),
            bottomLeft = CursorPoint(obj.getDouble("bottomLeftX").toFloat(), obj.getDouble("bottomLeftY").toFloat()),
            bottomRight = CursorPoint(obj.getDouble("bottomRightX").toFloat(), obj.getDouble("bottomRightY").toFloat()),
            isCalibrated = obj.optBoolean("isCalibrated", true),
            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
        )
    }

    private fun serializeActionHistory(list: List<ActionHistoryItem>): String {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("timestamp", item.timestamp)
                put("gesture", item.gesture?.name ?: JSONObject.NULL)
                put("action", item.action.name)
                put("result", item.result.name)
                put("packageName", item.packageName ?: JSONObject.NULL)
                put("profileName", item.profileName)
                put("errorMessage", item.errorMessage ?: JSONObject.NULL)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseActionHistoryJson(jsonStr: String): List<ActionHistoryItem> {
        val list = mutableListOf<ActionHistoryItem>()
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val id = obj.getString("id")
            val timestamp = obj.getLong("timestamp")
            val gName = if (obj.isNull("gesture")) null else obj.getString("gesture")
            val gesture = if (gName != null) {
                try { GestureType.valueOf(gName) } catch (_: Exception) { null }
            } else null
            val action = try { ActionType.valueOf(obj.getString("action")) } catch (_: Exception) { ActionType.NONE }
            val result = try { ActionResult.valueOf(obj.getString("result")) } catch (_: Exception) { ActionResult.SUCCESS }
            val pkg = if (obj.isNull("packageName")) null else obj.getString("packageName")
            val profile = obj.optString("profileName", "General")
            val err = if (obj.isNull("errorMessage")) null else obj.getString("errorMessage")

            list.add(ActionHistoryItem(id, timestamp, gesture, action, result, pkg, profile, err))
        }
        return list
    }

    val storedProfiles: Flow<List<GestureProfile>> = context.dataStore.data.map { prefs ->
        val builtIns = DefaultProfiles.getAllBuiltInProfiles().toMutableList()
        val overrideJson = prefs[KEY_PROFILES_OVERRIDE_JSON]
        val customJson = prefs[KEY_CUSTOM_PROFILES_JSON]

        // Apply overrides to built-in profiles if user edited them
        if (!overrideJson.isNullOrBlank()) {
            try {
                val overridesMap = parseProfilesJson(overrideJson)
                for (i in builtIns.indices) {
                    val id = builtIns[i].id
                    overridesMap.find { it.id == id }?.let { override ->
                        builtIns[i] = override.copy(isBuiltIn = true)
                    }
                }
            } catch (_: Exception) {
            }
        }

        val customProfiles = if (!customJson.isNullOrBlank()) {
            try {
                parseProfilesJson(customJson).map { it.copy(isBuiltIn = false) }
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        builtIns + customProfiles
    }

    suspend fun saveProfiles(profiles: List<GestureProfile>) {
        val builtInOverrides = profiles.filter { it.isBuiltIn }
        val customOnly = profiles.filter { !it.isBuiltIn }

        context.dataStore.edit { prefs ->
            prefs[KEY_PROFILES_OVERRIDE_JSON] = serializeProfiles(builtInOverrides)
            prefs[KEY_CUSTOM_PROFILES_JSON] = serializeProfiles(customOnly)
        }
    }

    private fun serializeProfiles(profiles: List<GestureProfile>): String {
        val array = JSONArray()
        for (p in profiles) {
            val obj = JSONObject().apply {
                put("id", p.id)
                put("name", p.name)
                put("description", p.description)
                put("isBuiltIn", p.isBuiltIn)
                put("packageName", p.packageName ?: JSONObject.NULL)
                val mappingsArray = JSONArray()
                for ((g, m) in p.mappings) {
                    val mObj = JSONObject().apply {
                        put("gesture", g.name)
                        put("action", m.action.name)
                        put("isEnabled", m.isEnabled)
                    }
                    mappingsArray.put(mObj)
                }
                put("mappings", mappingsArray)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseProfilesJson(jsonStr: String): List<GestureProfile> {
        val list = mutableListOf<GestureProfile>()
        val array = JSONArray(jsonStr)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val id = obj.getString("id")
            val name = obj.getString("name")
            val desc = obj.optString("description", "")
            val isBuiltIn = obj.optBoolean("isBuiltIn", false)
            val pkg = if (obj.isNull("packageName")) null else obj.optString("packageName").takeIf { it.isNotEmpty() }

            val mappingsMap = mutableMapOf<GestureType, ProfileMapping>()
            val mArray = obj.optJSONArray("mappings")
            if (mArray != null) {
                for (j in 0 until mArray.length()) {
                    val mObj = mArray.getJSONObject(j)
                    val gName = mObj.getString("gesture")
                    val aName = mObj.getString("action")
                    val enabled = mObj.optBoolean("isEnabled", true)
                    try {
                        val g = GestureType.valueOf(gName)
                        val a = ActionType.valueOf(aName)
                        mappingsMap[g] = ProfileMapping(g, a, enabled)
                    } catch (_: Exception) {
                    }
                }
            }

            // Merge with defaults for any missing gestures
            val completeMappings = DefaultProfiles.getDefaultMappings().toMutableMap().apply {
                putAll(mappingsMap)
            }

            list.add(
                GestureProfile(
                    id = id,
                    name = name,
                    description = desc,
                    isBuiltIn = isBuiltIn,
                    packageName = pkg,
                    mappings = completeMappings
                )
            )
        }
        return list
    }
}
