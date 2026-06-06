package com.example.akillirobotkontrol.robot.mapping

class OdometryTracker {
    var xCm: Float = 0f
    var yCm: Float = 0f
    var headingDegrees: Float = 0f // 0 is facing "Up" (Y axis positive)
    
    private var lastEncoderTicks: Long = 0
    private var lastUpdateTime: Long = System.currentTimeMillis()
    
    fun update(movementState: String, encoderTicks: Long) {
        val currentTime = System.currentTimeMillis()
        val dt = (currentTime - lastUpdateTime) / 1000f // seconds
        lastUpdateTime = currentTime
        
        // Update Translation
        if (encoderTicks > lastEncoderTicks) {
            val deltaTicks = encoderTicks - lastEncoderTicks
            lastEncoderTicks = encoderTicks
            
            // 1 tick = 1 cm
            val distanceCm = deltaTicks * 1.0f 
            
            // Move forward or backward based on movement state
            val direction = if (movementState == "B" || movementState == "H" || movementState == "J") -1 else 1
            
            if (movementState == "F" || movementState == "B") {
                val rad = Math.toRadians(headingDegrees.toDouble())
                xCm += (distanceCm * Math.sin(rad) * direction).toFloat()
                yCm += (distanceCm * Math.cos(rad) * direction).toFloat()
            }
        }
        
        // Update Rotation (Drift simulation)
        // Eğer hareket L veya R ise zamanla kendi etrafında döner (Örn: 90 derece/saniye)
        val turnRate = 90f // degrees per second
        when (movementState) {
            "L" -> headingDegrees -= turnRate * dt
            "R" -> headingDegrees += turnRate * dt
            "G" -> headingDegrees -= (turnRate / 2) * dt // Forward-Left
            "I" -> headingDegrees += (turnRate / 2) * dt // Forward-Right
            "H" -> headingDegrees -= (turnRate / 2) * dt // Backward-Left
            "J" -> headingDegrees += (turnRate / 2) * dt // Backward-Right
        }
        
        // Normalize heading 0-360
        headingDegrees = (headingDegrees % 360f)
        if (headingDegrees < 0) headingDegrees += 360f
    }
    
    fun reset() {
        xCm = 0f
        yCm = 0f
        headingDegrees = 0f
        lastEncoderTicks = 0
    }
}
