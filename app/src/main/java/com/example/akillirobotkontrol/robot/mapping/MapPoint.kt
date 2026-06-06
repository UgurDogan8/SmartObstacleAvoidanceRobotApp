package com.example.akillirobotkontrol.robot.mapping

data class MapPoint(
    val angle: Int,
    val distance: Long,
    val encoderTicks: Long
)
