package com.example.akillirobotkontrol.bluetooth

data class BluetoothDeviceModel(
    val name: String,
    val macAddress: String,
    val isPaired: Boolean = true
)
