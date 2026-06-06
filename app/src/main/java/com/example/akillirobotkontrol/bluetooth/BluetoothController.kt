package com.example.akillirobotkontrol.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context

object BluetoothController {
    
    @SuppressLint("MissingPermission")
    fun getPairedDevices(context: Context): List<BluetoothDeviceModel> {
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager?.adapter ?: return emptyList()

        if (!bluetoothAdapter.isEnabled) {
            return emptyList()
        }

        val pairedDevices = bluetoothAdapter.bondedDevices ?: return emptyList()

        return pairedDevices
            .filter { 
                it.type == BluetoothDevice.DEVICE_TYPE_CLASSIC || 
                it.type == BluetoothDevice.DEVICE_TYPE_DUAL ||
                it.type == BluetoothDevice.DEVICE_TYPE_UNKNOWN
            }
            .map { device ->
                BluetoothDeviceModel(
                    name = device.name ?: "Bilinmeyen Cihaz",
                    macAddress = device.address,
                    isPaired = true
                )
            }
    }
}
