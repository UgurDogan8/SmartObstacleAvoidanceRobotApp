package com.example.akillirobotkontrol.robot

/**
 * Robot ile Bluetooth üzerinden haberleşmek için kullanılan komut protokolü.
 * Her komut toPayload() çağrıldığında sonuna "\n" eklenmiş şekilde raw string döndürür.
 */
sealed class RobotCommand(protected val commandString: String) {
    fun toPayload(): String = "$commandString\n"

    object Forward : RobotCommand("F")
    object Backward : RobotCommand("B")
    object Left : RobotCommand("L")
    object Right : RobotCommand("R")
    object ForwardLeft : RobotCommand("G")
    object ForwardRight : RobotCommand("I")
    object BackwardLeft : RobotCommand("H")
    object BackwardRight : RobotCommand("J")
    object Stop : RobotCommand("S")
    object AutoMode : RobotCommand("A")
    object ProtectedManualMode : RobotCommand("M")
    object FreeManualMode : RobotCommand("X")
    object Emergency : RobotCommand("E")
    object ClearEmergency : RobotCommand("C")
    object Ping : RobotCommand("P")

    /**
     * Hız komutu. Girdi değeri otomatik olarak 0 ile 9 arasında sınırlandırılır (clamp).
     */
    class Speed(inputLevel: Int) : RobotCommand("V${inputLevel.coerceIn(0, 9)}") {
        val level: Int = inputLevel.coerceIn(0, 9)

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Speed) return false
            return level == other.level
        }

        override fun hashCode(): Int = level.hashCode()
    }
}
