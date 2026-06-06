package com.example.akillirobotkontrol.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.akillirobotkontrol.RobotApplication
import com.example.akillirobotkontrol.bluetooth.ConnectionState
import com.example.akillirobotkontrol.robot.RobotCommand
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ControlViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as RobotApplication

    val connectionService = app.bluetoothService
    private val settingsRepository = app.settingsRepository
    val blackboxLogger = com.example.akillirobotkontrol.data.BlackboxLogger(application)

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private val _lastConnectedDeviceName = MutableStateFlow<String?>(null)
    val lastConnectedDeviceName: StateFlow<String?> = _lastConnectedDeviceName.asStateFlow()

    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private val _appSettings = MutableStateFlow(settingsRepository.getSettings())
    val appSettings: StateFlow<com.example.akillirobotkontrol.data.AppSettings> = _appSettings.asStateFlow()

    private val _currentSpeed = MutableStateFlow(_appSettings.value.speedLevel)
    val currentSpeed: StateFlow<Int> = _currentSpeed.asStateFlow()

    val occupancyGrid = com.example.akillirobotkontrol.robot.mapping.OccupancyGrid(cellSizeCm = 5)
    val odometryTracker = com.example.akillirobotkontrol.robot.mapping.OdometryTracker()
    
    private val _gridUpdateVersion = MutableStateFlow(0L)
    val gridUpdateVersion: StateFlow<Long> = _gridUpdateVersion.asStateFlow()

    init {
        _lastConnectedDeviceName.value = settingsRepository.getLastConnectedName()
        connectionService.updateSettings(_appSettings.value)
        connectionService.logCallback = { msg, type -> addLog(msg, type) }

        viewModelScope.launch {
            var heartbeatJob: kotlinx.coroutines.Job? = null
            var blackboxJob: kotlinx.coroutines.Job? = null
            
            connectionService.connectionState.collect { state ->
                if (state == ConnectionState.CONNECTED) {
                    connectionService.sendCommand(RobotCommand.Speed(_currentSpeed.value))
                    
                    heartbeatJob?.cancel()
                    heartbeatJob = launch {
                        while (true) {
                            delay(500)
                            connectionService.sendCommand(RobotCommand.Ping)
                        }
                    }
                    
                    blackboxJob?.cancel()
                    blackboxJob = launch {
                        while (true) {
                            delay(1000)
                            connectionService.robotStatus.value?.let { status ->
                                blackboxLogger.logStatus(status)
                            }
                        }
                    }
                } else {
                    heartbeatJob?.cancel()
                    blackboxJob?.cancel()
                }
            }
        }

        viewModelScope.launch {
            connectionService.robotStatus.collect { status ->
                odometryTracker.update(status.movementState, status.encoderTicks)
                
                status.currentMapPoint?.let { mapPoint ->
                    occupancyGrid.updateWithSensor(
                        robotXCm = odometryTracker.xCm,
                        robotYCm = odometryTracker.yCm,
                        robotHeading = odometryTracker.headingDegrees,
                        sensorAngle = mapPoint.angle,
                        distanceCm = mapPoint.distance
                    )
                    _gridUpdateVersion.value++
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Memory leak koruması: View model yok olduğunda callback'i temizle
        if (connectionService.logCallback != null) {
            connectionService.logCallback = null
        }
    }

    fun addLog(message: String, type: LogType) {
        val timeStr = dateFormat.format(Date())
        val newEntry = LogEntry(timeStr, message, type)
        _logs.value = (_logs.value + newEntry).takeLast(200)
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }

    fun updateSettings(newSettings: com.example.akillirobotkontrol.data.AppSettings) {
        var validatedSettings = newSettings
        if (validatedSettings.criticalDistanceCm >= validatedSettings.cautionDistanceCm) {
            validatedSettings = validatedSettings.copy(cautionDistanceCm = validatedSettings.criticalDistanceCm + 5)
        }
        _appSettings.value = validatedSettings
        settingsRepository.saveSettings(validatedSettings)
        connectionService.updateSettings(validatedSettings)
        
        if (_currentSpeed.value != validatedSettings.speedLevel) {
            setSpeed(validatedSettings.speedLevel)
        }
    }

    // --- ENCAPSULATED COMMANDS ---

    fun sendForward() = connectionService.sendCommand(RobotCommand.Forward)
    fun sendBackward() = connectionService.sendCommand(RobotCommand.Backward)
    fun sendLeft() = connectionService.sendCommand(RobotCommand.Left)
    fun sendRight() = connectionService.sendCommand(RobotCommand.Right)
    fun sendForwardLeft() = connectionService.sendCommand(RobotCommand.ForwardLeft)
    fun sendForwardRight() = connectionService.sendCommand(RobotCommand.ForwardRight)
    fun sendBackwardLeft() = connectionService.sendCommand(RobotCommand.BackwardLeft)
    fun sendBackwardRight() = connectionService.sendCommand(RobotCommand.BackwardRight)
    fun sendStop() = connectionService.sendCommand(RobotCommand.Stop)
    fun setAutoMode() = connectionService.sendCommand(RobotCommand.AutoMode)
    fun setProtectedManualMode() = connectionService.sendCommand(RobotCommand.ProtectedManualMode)
    fun setFreeManualMode() = connectionService.sendCommand(RobotCommand.FreeManualMode)

    fun sendEmergency() {
        connectionService.sendCommand(RobotCommand.Emergency)
        connectionService.updateEmergencyState(true)
    }

    fun setSpeed(speed: Int) {
        if (_currentSpeed.value != speed) {
            _currentSpeed.value = speed
            settingsRepository.saveSpeedLevel(speed)
            connectionService.sendCommand(RobotCommand.Speed(speed))
        }
    }

    fun disconnect() {
        connectionService.disconnect()
    }

    fun resetEmergency() {
        viewModelScope.launch {
            connectionService.sendCommand(RobotCommand.ClearEmergency)
            delay(200)
            setProtectedManualMode()
            connectionService.updateEmergencyState(false)
        }
    }

    fun reconnectToLastDevice(adapter: android.bluetooth.BluetoothAdapter?) {
        val mac = settingsRepository.getLastConnectedMac()
        val name = settingsRepository.getLastConnectedName()
        if (mac != null && name != null) {
            val device = adapter?.getRemoteDevice(mac)
            if (device != null) {
                connectionService.connect(device)
            }
        }
    }
}
