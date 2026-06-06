package com.example.akillirobotkontrol.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.example.akillirobotkontrol.robot.RobotCommand
import com.example.akillirobotkontrol.robot.RobotStatus
import com.example.akillirobotkontrol.robot.TelemetryParser
import com.example.akillirobotkontrol.ui.LogType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BluetoothConnectionService(private val context: Context) {

    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private var socket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _robotStatus = MutableStateFlow(RobotStatus())
    val robotStatus: StateFlow<RobotStatus> = _robotStatus.asStateFlow()

    private var connectionJob: Job? = null
    private var readJob: Job? = null

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        context.getSystemService(BluetoothManager::class.java)?.adapter
    }

    var logCallback: ((String, LogType) -> Unit)? = null

    private var appSettings = com.example.akillirobotkontrol.data.AppSettings()

    fun updateSettings(settings: com.example.akillirobotkontrol.data.AppSettings) {
        appSettings = settings
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        if (_connectionState.value == ConnectionState.CONNECTING || _connectionState.value == ConnectionState.CONNECTED) {
            return
        }

        _connectionState.value = ConnectionState.CONNECTING

        connectionJob = CoroutineScope(Dispatchers.IO).launch {
            // 1. Discovery işlemini durdur (Bağlantı hızını ve stabilitesini artırır)
            bluetoothAdapter?.cancelDiscovery()

            try {
                // 1. Standart bağlantı denemesi (5 Saniye Timeout ile)
                var success = false
                try {
                    socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                    kotlinx.coroutines.withTimeout(5000L) {
                        socket?.connect()
                    }
                    success = true
                } catch (e: Exception) {
                    // Timeout veya IOException
                    socket?.close()
                }

                // 2. Fallback: Standart bağlantı başarısız olursa güvensiz soketi dene (5 Saniye Timeout)
                if (!success) {
                    socket = device.createInsecureRfcommSocketToServiceRecord(SPP_UUID)
                    kotlinx.coroutines.withTimeout(5000L) {
                        socket?.connect()
                    }
                }

                inputStream = socket?.inputStream
                outputStream = socket?.outputStream

                _connectionState.value = ConnectionState.CONNECTED
                logCallback?.invoke("Bağlandı: ${device.name}", LogType.EVENT)
                
                startReading()
            } catch (e: Exception) {
                e.printStackTrace()
                _connectionState.value = ConnectionState.ERROR
                logCallback?.invoke("Bağlantı hatası: Zaman Aşımı veya Uyumsuz Cihaz.", LogType.ERROR)
                disconnect()
            }
        }
    }

    private fun startReading() {
        readJob = CoroutineScope(Dispatchers.IO).launch {
            val reader = inputStream?.bufferedReader()
            var lastReadTime = System.currentTimeMillis()
            
            val watchdogJob = launch(Dispatchers.Default) {
                while (isActive) {
                    kotlinx.coroutines.delay(200)
                    if (System.currentTimeMillis() - lastReadTime > appSettings.telemetryTimeoutMs) {
                        _connectionState.value = ConnectionState.ERROR
                        logCallback?.invoke("Watchdog Zaman Aşımı! Bağlantı kesiliyor.", LogType.ERROR)
                        disconnect()
                        break
                    }
                }
            }
            
            while (isActive && _connectionState.value == ConnectionState.CONNECTED) {
                try {
                    val line = reader?.readLine()
                    lastReadTime = System.currentTimeMillis()
                    
                    if (line != null && line.isNotBlank()) {
                        logCallback?.invoke(line, LogType.RX)
                        val currentStatus = _robotStatus.value
                        val newStatus = TelemetryParser.parse(
                            line, 
                            currentStatus,
                            cautionDistance = appSettings.cautionDistanceCm,
                            criticalDistance = appSettings.criticalDistanceCm
                        )
                        _robotStatus.value = newStatus.copy(isConnected = true)
                    } else if (line == null) {
                        // EOF, socket kapandı
                        _connectionState.value = ConnectionState.ERROR
                        logCallback?.invoke("Bağlantı soketi kapandı (EOF).", LogType.ERROR)
                        disconnect()
                        break
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                    _connectionState.value = ConnectionState.ERROR
                    logCallback?.invoke("Okuma hatası: ${e.message}", LogType.ERROR)
                    disconnect()
                    break
                }
            }
            watchdogJob.cancel()
        }
    }

    fun sendCommand(command: RobotCommand) {
        if (_connectionState.value == ConnectionState.CONNECTED) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val payload = command.toPayload()
                    outputStream?.write(payload.toByteArray())
                    outputStream?.flush()
                    logCallback?.invoke(payload.replace("\n", ""), LogType.TX)
                } catch (e: IOException) {
                    _connectionState.value = ConnectionState.ERROR
                    logCallback?.invoke("Yazma hatası: ${e.message}", LogType.ERROR)
                    disconnect()
                }
            }
        }
    }

    fun updateEmergencyState(isEmergency: Boolean) {
        _robotStatus.value = _robotStatus.value.copy(isEmergency = isEmergency)
    }

    fun disconnect() {
        try {
            if (_connectionState.value == ConnectionState.CONNECTED) {
                val stopPayload = RobotCommand.Stop.toPayload()
                outputStream?.write(stopPayload.toByteArray())
                outputStream?.flush()
            }
        } catch (e: Exception) {
            // Ignore if we can't send Stop
        }

        try {
            readJob?.cancel()
            connectionJob?.cancel()
            inputStream?.close()
            outputStream?.close()
            socket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            socket = null
            inputStream = null
            outputStream = null
            
            if (_connectionState.value != ConnectionState.ERROR) {
                _connectionState.value = ConnectionState.DISCONNECTED
            }
            _robotStatus.value = _robotStatus.value.copy(isConnected = false)
            logCallback?.invoke("Bağlantı kesildi.", LogType.EVENT)
        }
    }
}
