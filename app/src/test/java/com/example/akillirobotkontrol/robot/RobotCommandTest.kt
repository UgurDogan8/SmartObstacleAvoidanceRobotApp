package com.example.akillirobotkontrol.robot

import org.junit.Assert.assertEquals
import org.junit.Test

class RobotCommandTest {

    @Test
    fun testSimpleCommandsPayload() {
        assertEquals("F\n", RobotCommand.Forward.toPayload())
        assertEquals("B\n", RobotCommand.Backward.toPayload())
        assertEquals("L\n", RobotCommand.Left.toPayload())
        assertEquals("R\n", RobotCommand.Right.toPayload())
        assertEquals("S\n", RobotCommand.Stop.toPayload())
        assertEquals("A1\n", RobotCommand.AutoMode.toPayload())
        assertEquals("A0\n", RobotCommand.ManualMode.toPayload())
        assertEquals("E\n", RobotCommand.Emergency.toPayload())
    }

    @Test
    fun testSpeedCommandValidRange() {
        val speed5 = RobotCommand.Speed(5)
        assertEquals(5, speed5.level)
        assertEquals("V5\n", speed5.toPayload())

        val speed0 = RobotCommand.Speed(0)
        assertEquals(0, speed0.level)
        assertEquals("V0\n", speed0.toPayload())

        val speed9 = RobotCommand.Speed(9)
        assertEquals(9, speed9.level)
        assertEquals("V9\n", speed9.toPayload())
    }

    @Test
    fun testSpeedCommandOutOfRange() {
        // -5 değeri verildiğinde 0'a sınırlandırılmalıdır
        val speedTooLow = RobotCommand.Speed(-5)
        assertEquals(0, speedTooLow.level)
        assertEquals("V0\n", speedTooLow.toPayload())

        // 15 değeri verildiğinde 9'a sınırlandırılmalıdır
        val speedTooHigh = RobotCommand.Speed(15)
        assertEquals(9, speedTooHigh.level)
        assertEquals("V9\n", speedTooHigh.toPayload())
    }
}
