package com.example.akillirobotkontrol.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.akillirobotkontrol.RobotApplication
import com.example.akillirobotkontrol.bluetooth.BluetoothController
import com.example.akillirobotkontrol.bluetooth.BluetoothDeviceModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ConnectionViewModel(application: Application) : AndroidViewModel(application) {
    
    private val app = application as RobotApplication
    private val settingsRepository = app.settingsRepository

    val connectionService = app.bluetoothService

    private val _permissionGranted = MutableStateFlow(false)
    val permissionGranted: StateFlow<Boolean> = _permissionGranted.asStateFlow()

    private val _permissionErrorMessage = MutableStateFlow<String?>(null)
    val permissionErrorMessage: StateFlow<String?> = _permissionErrorMessage.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceModel>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDeviceModel>> = _pairedDevices.asStateFlow()

    private val _selectedDevice = MutableStateFlow<BluetoothDeviceModel?>(null)
    val selectedDevice: StateFlow<BluetoothDeviceModel?> = _selectedDevice.asStateFlow()

    private val _lastConnectedDeviceMac = MutableStateFlow<String?>(null)
    val lastConnectedDeviceMac: StateFlow<String?> = _lastConnectedDeviceMac.asStateFlow()

    private val _lastConnectedDeviceName = MutableStateFlow<String?>(null)
    val lastConnectedDeviceName: StateFlow<String?> = _lastConnectedDeviceName.asStateFlow()

    init {
        _lastConnectedDeviceMac.value = settingsRepository.getLastConnectedMac()
        _lastConnectedDeviceName.value = settingsRepository.getLastConnectedName()
    }

    fun updatePermissionState(isGranted: Boolean) {
        _permissionGranted.value = isGranted
        if (isGranted) {
            _permissionErrorMessage.value = null
        }
    }

    fun setPermissionErrorMessage(message: String) {
        _permissionErrorMessage.value = message
    }

    fun loadPairedDevices(context: Context) {
        if (_permissionGranted.value) {
            val devices = BluetoothController.getPairedDevices(context)
            _pairedDevices.value = devices
            if (_selectedDevice.value != null && !devices.any { it.macAddress == _selectedDevice.value?.macAddress }) {
                _selectedDevice.value = null
            }
        }
    }

    fun selectDevice(device: BluetoothDeviceModel) {
        _selectedDevice.value = device
    }

    fun connectToSelectedDevice(adapter: android.bluetooth.BluetoothAdapter?) {
        val deviceModel = _selectedDevice.value ?: return
        connectToMacAddress(deviceModel.macAddress, deviceModel.name, adapter)
    }

    fun connectToMacAddress(macAddress: String, name: String, adapter: android.bluetooth.BluetoothAdapter?) {
        val device = adapter?.getRemoteDevice(macAddress)
        if (device != null) {
            connectionService.connect(device)
            saveLastConnectedDevice(macAddress, name)
        }
    }

    private fun saveLastConnectedDevice(macAddress: String, name: String) {
        settingsRepository.saveLastConnectedDevice(macAddress, name)
        _lastConnectedDeviceMac.value = macAddress
        _lastConnectedDeviceName.value = name
    }
}
