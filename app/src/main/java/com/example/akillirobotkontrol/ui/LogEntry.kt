package com.example.akillirobotkontrol.ui

enum class LogType { TX, RX, EVENT, ERROR }

data class LogEntry(
    val time: String,
    val message: String,
    val type: LogType
)
