package com.example.service

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThermalStatusLevel(val displayName: String, val isThrottled: Boolean) {
    NORMAL("Normal", false),
    LIGHT("Light Warmth", false),
    MODERATE("Moderate Heat", true),
    SEVERE("Severe Throttling", true),
    CRITICAL("Critical Heat", true),
    EMERGENCY("Emergency Shutoff", true)
}

/**
 * Monitors device thermal status via Android PowerManager (API 29+).
 * Dynamically informs the adaptive inference pipeline to lower processing frequency
 * when the hardware is under thermal stress, preventing device heating.
 */
class DeviceThermalMonitor(private val context: Context) {

    private val TAG = "DeviceThermalMonitor"
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager

    private val _thermalLevel = MutableStateFlow(ThermalStatusLevel.NORMAL)
    val thermalLevel: StateFlow<ThermalStatusLevel> = _thermalLevel.asStateFlow()

    private val _thermalWarningMessage = MutableStateFlow<String?>(null)
    val thermalWarningMessage: StateFlow<String?> = _thermalWarningMessage.asStateFlow()

    private var thermalListener: PowerManager.OnThermalStatusChangedListener? = null

    init {
        checkCurrentStatus()
        registerListener()
    }

    private fun checkCurrentStatus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            try {
                val status = powerManager.currentThermalStatus
                updateThermalStatus(status)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to read initial thermal status: ${e.message}")
            }
        }
    }

    private fun registerListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
            try {
                thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
                    updateThermalStatus(status)
                }
                powerManager.addThermalStatusListener(context.mainExecutor, thermalListener!!)
                Log.d(TAG, "Registered PowerManager thermal status listener.")
            } catch (e: Exception) {
                Log.w(TAG, "ThermalStatusListener not supported on this device: ${e.message}")
            }
        }
    }

    private fun updateThermalStatus(statusCode: Int) {
        val level = when (statusCode) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalStatusLevel.NORMAL
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalStatusLevel.LIGHT
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalStatusLevel.MODERATE
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalStatusLevel.SEVERE
            PowerManager.THERMAL_STATUS_CRITICAL -> ThermalStatusLevel.CRITICAL
            PowerManager.THERMAL_STATUS_EMERGENCY -> ThermalStatusLevel.EMERGENCY
            PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalStatusLevel.EMERGENCY
            else -> ThermalStatusLevel.NORMAL
        }

        _thermalLevel.value = level

        if (level.isThrottled) {
            _thermalWarningMessage.value = "Performance reduced temporarily to keep your device cool."
            Log.i(TAG, "Device thermal throttle triggered: ${level.displayName}. Adaptive frame rate will reduce load.")
        } else {
            _thermalWarningMessage.value = null
        }
    }

    fun unregister() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null && thermalListener != null) {
            try {
                powerManager.removeThermalStatusListener(thermalListener!!)
                thermalListener = null
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering thermal listener: ${e.message}")
            }
        }
    }
}
