package com.example.akillirobotkontrol.data

import android.content.Context
import com.example.akillirobotkontrol.robot.RobotStatus
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BlackboxLogger(private val context: Context) {
    private val fileName = "robot_blackbox.csv"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val file: File
        get() = File(context.getExternalFilesDir(null), fileName)

    init {
        if (!file.exists()) {
            try {
                FileWriter(file, false).use { writer ->
                    writer.append("Timestamp,SpeedLevel,DistanceCm,WarningLevel,Mode,MovementState,IsEmergency,BatteryLevel,EncoderTicks\n")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun logStatus(status: RobotStatus) {
        try {
            FileWriter(file, true).use { writer ->
                val time = dateFormat.format(Date())
                val distance = status.distanceCm ?: -1
                val line = "$time,${status.speedLevel},$distance,${status.warningLevel},${status.mode},${status.movementState},${status.isEmergency},${status.batteryLevel ?: ""},${status.encoderTicks}\n"
                writer.append(line)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLogFileSizeKb(): Long {
        return if (file.exists()) file.length() / 1024 else 0
    }

    fun clearLogs() {
        try {
            FileWriter(file, false).use { writer ->
                writer.append("Timestamp,SpeedLevel,DistanceCm,WarningLevel,Mode,MovementState,IsEmergency,BatteryLevel,EncoderTicks\n")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
