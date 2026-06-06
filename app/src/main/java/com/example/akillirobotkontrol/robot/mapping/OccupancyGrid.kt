package com.example.akillirobotkontrol.robot.mapping

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.sin

class OccupancyGrid(private val cellSizeCm: Int = 5) {
    // Key: Pair(gridX, gridY) -> Value: CellState
    val grid = ConcurrentHashMap<Pair<Int, Int>, CellState>()
    
    fun updateWithSensor(
        robotXCm: Float, 
        robotYCm: Float, 
        robotHeading: Float, 
        sensorAngle: Int, // 95 is center
        distanceCm: Long
    ) {
        if (distanceCm >= 999) return // No obstacle detected / out of range
        if (distanceCm <= 2) return // Noise
        
        // Calculate absolute angle of the sensor beam
        // sensorAngle = 95 means straight ahead (relative 0)
        // angle < 95 means right, angle > 95 means left (assuming servo looks left on high angles)
        // Offset = sensorAngle - 95. Left is positive, Right is negative.
        val relativeAngle = sensorAngle - 95
        val absoluteAngleDegrees = robotHeading + relativeAngle
        val absoluteAngleRads = Math.toRadians(absoluteAngleDegrees.toDouble())
        
        val robotGridX = (robotXCm / cellSizeCm).toInt()
        val robotGridY = (robotYCm / cellSizeCm).toInt()
        
        val targetXCm = robotXCm + (distanceCm * sin(absoluteAngleRads)).toFloat()
        val targetYCm = robotYCm + (distanceCm * cos(absoluteAngleRads)).toFloat()
        
        val targetGridX = (targetXCm / cellSizeCm).toInt()
        val targetGridY = (targetYCm / cellSizeCm).toInt()
        
        // Bresenham's Line Algorithm to mark free cells
        rayCastFree(robotGridX, robotGridY, targetGridX, targetGridY)
        
        // Mark target cell as obstacle
        grid[Pair(targetGridX, targetGridY)] = CellState.OBSTACLE
    }
    
    private fun rayCastFree(x0: Int, y0: Int, x1: Int, y1: Int) {
        var dx = Math.abs(x1 - x0)
        var dy = Math.abs(y1 - y0)
        var sx = if (x0 < x1) 1 else -1
        var sy = if (y0 < y1) 1 else -1
        var err = dx - dy
        
        var currentX = x0
        var currentY = y0
        
        while (true) {
            if (currentX == x1 && currentY == y1) break // Don't mark the final target as FREE
            
            val key = Pair(currentX, currentY)
            if (grid[key] != CellState.OBSTACLE) {
                grid[key] = CellState.FREE
            }
            
            val e2 = 2 * err
            if (e2 > -dy) {
                err -= dy
                currentX += sx
            }
            if (e2 < dx) {
                err += dx
                currentY += sy
            }
        }
    }
    
    fun clear() {
        grid.clear()
    }
}
