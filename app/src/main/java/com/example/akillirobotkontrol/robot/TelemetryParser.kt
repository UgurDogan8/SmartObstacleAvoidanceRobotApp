package com.example.akillirobotkontrol.robot

object TelemetryParser {
    
    private val distanceHistory = mutableListOf<Int>()
    private const val HISTORY_SIZE = 3

    /**
     * Gelen telemetri verisini (ör: "D=28;M=A0;S=F;V=5") ayrıştırır.
     * Eksik veya hatalı veri geldiğinde çökmemesi için `previousStatus` kullanılır.
     * Hatalı kısımlarda eski değerler korunurken, başarılı okunan kısımlar güncellenir.
     */
    fun parse(telemetry: String, previousStatus: RobotStatus = RobotStatus(), cautionDistance: Int = 35, criticalDistance: Int = 20): RobotStatus {
        if (telemetry.isBlank() || !telemetry.startsWith("D=")) return previousStatus

        var rawDistance: Int? = null
        var distanceCm = previousStatus.distanceCm
        var mode = previousStatus.mode
        var manualSubMode = previousStatus.manualSubMode
        var movementState = previousStatus.movementState
        var speedLevel = previousStatus.speedLevel
        var isEmergency = previousStatus.isEmergency
        var batteryLevel = previousStatus.batteryLevel
        var encoderTicks = previousStatus.encoderTicks
        var currentAngle = previousStatus.currentAngle
        var currentMapPoint: com.example.akillirobotkontrol.robot.mapping.MapPoint? = null

        try {
            val parts = telemetry.split(";")
            for (part in parts) {
                val keyValue = part.trim().split("=")
                if (keyValue.size == 2) {
                    val key = keyValue[0].trim().uppercase()
                    val value = keyValue[1].trim()

                    when (key) {
                        "D" -> rawDistance = value.toIntOrNull()
                        "M" -> mode = value
                        "MS" -> manualSubMode = value.uppercase()
                        "S" -> movementState = value
                        "V" -> speedLevel = value.toIntOrNull() ?: speedLevel
                        "EM" -> isEmergency = (value == "1" || value.uppercase() == "TRUE")
                        "B" -> batteryLevel = value.toIntOrNull()
                        "E" -> encoderTicks = value.toLongOrNull() ?: encoderTicks
                        "ANG" -> currentAngle = value.toIntOrNull() ?: currentAngle
                        "MAP" -> {
                            val mapParts = value.split(",")
                            if (mapParts.size == 3) {
                                val ang = mapParts[0].toIntOrNull()
                                val dist = mapParts[1].toLongOrNull()
                                val enc = mapParts[2].toLongOrNull()
                                if (ang != null && dist != null && enc != null) {
                                    currentMapPoint = com.example.akillirobotkontrol.robot.mapping.MapPoint(ang, dist, enc)
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Parse hatası olsa bile mevcut değişken değerleri korunur.
        }

        if (rawDistance != null) {
            distanceHistory.add(rawDistance)
            if (distanceHistory.size > HISTORY_SIZE) {
                distanceHistory.removeAt(0)
            }
            distanceCm = distanceHistory.average().toInt()
        }

        val warningLevel = RobotStatus.calculateWarningLevel(distanceCm, cautionDistance, criticalDistance)

        return previousStatus.copy(
            distanceCm = distanceCm,
            mode = mode,
            manualSubMode = manualSubMode,
            movementState = movementState,
            speedLevel = speedLevel,
            isEmergency = isEmergency,
            warningLevel = warningLevel,
            batteryLevel = batteryLevel,
            encoderTicks = encoderTicks,
            currentAngle = currentAngle,
            currentMapPoint = currentMapPoint
        )
    }
}
