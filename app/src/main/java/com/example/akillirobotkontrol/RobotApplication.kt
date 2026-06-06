package com.example.akillirobotkontrol

import android.app.Application
import com.example.akillirobotkontrol.bluetooth.BluetoothConnectionService
import com.example.akillirobotkontrol.data.SettingsRepository

class RobotApplication : Application() {
    lateinit var bluetoothService: BluetoothConnectionService
        private set
        
    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        bluetoothService = BluetoothConnectionService(this)
        settingsRepository = SettingsRepository(this)
    }
}
