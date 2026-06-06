package com.example.akillirobotkontrol.robot

enum class WarningLevel {
    SAFE,
    CAUTION,
    CRITICAL,
    UNKNOWN
}

data class RobotStatus(
    val distanceCm: Int? = null,
    val mode: String = "A0",
    val manualSubMode: String = "K",
    val movementState: String = "S",
    val speedLevel: Int = 0,
    val isEmergency: Boolean = false,
    val isConnected: Boolean = false,
    val warningLevel: WarningLevel = WarningLevel.UNKNOWN,
    val batteryLevel: Int? = null,
    val encoderTicks: Long = 0L,
    val currentAngle: Int = 95,
    val currentMapPoint: com.example.akillirobotkontrol.robot.mapping.MapPoint? = null
) {
    companion object {
        fun calculateWarningLevel(distanceCm: Int?, cautionDistance: Int = 35, criticalDistance: Int = 20): WarningLevel {
            if (distanceCm == null) return WarningLevel.UNKNOWN
            return when {
                distanceCm > cautionDistance -> WarningLevel.SAFE
                distanceCm in criticalDistance..cautionDistance -> WarningLevel.CAUTION
                else -> WarningLevel.CRITICAL
            }
        }
    }
}
