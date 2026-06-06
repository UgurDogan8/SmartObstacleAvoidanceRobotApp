package com.example.akillirobotkontrol.data

import android.content.Context

data class AppSettings(
    val speedLevel: Int = 5,
    val rememberLastDevice: Boolean = true,
    val commandRepeatMs: Int = 150,
    val telemetryTimeoutMs: Int = 1000,
    val criticalDistanceCm: Int = 20,
    val cautionDistanceCm: Int = 35
)

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("SettingsPrefs", Context.MODE_PRIVATE)

    fun getSettings(): AppSettings {
        return AppSettings(
            speedLevel = prefs.getInt("speed_level", 5),
            rememberLastDevice = prefs.getBoolean("remember_device", true),
            commandRepeatMs = prefs.getInt("cmd_repeat_ms", 150),
            telemetryTimeoutMs = prefs.getInt("telemetry_timeout_ms", 1000),
            criticalDistanceCm = prefs.getInt("critical_dist_cm", 20),
            cautionDistanceCm = prefs.getInt("caution_dist_cm", 35)
        )
    }

    fun saveSettings(settings: AppSettings) {
        prefs.edit()
            .putInt("speed_level", settings.speedLevel)
            .putBoolean("remember_device", settings.rememberLastDevice)
            .putInt("cmd_repeat_ms", settings.commandRepeatMs)
            .putInt("telemetry_timeout_ms", settings.telemetryTimeoutMs)
            .putInt("critical_dist_cm", settings.criticalDistanceCm)
            .putInt("caution_dist_cm", settings.cautionDistanceCm)
            .apply()
    }
    
    // Backward compatibility for MainViewModel's direct speed calls if needed
    fun saveSpeedLevel(speed: Int) {
        prefs.edit().putInt("speed_level", speed).apply()
    }

    // --- Device Connection Data ---
    private val robotPrefs = context.getSharedPreferences("RobotPrefs", Context.MODE_PRIVATE)

    fun getLastConnectedMac(): String? {
        return robotPrefs.getString("last_mac", null)
    }

    fun getLastConnectedName(): String? {
        return robotPrefs.getString("last_name", null)
    }

    fun saveLastConnectedDevice(mac: String, name: String) {
        robotPrefs.edit()
            .putString("last_mac", mac)
            .putString("last_name", name)
            .apply()
    }
}
