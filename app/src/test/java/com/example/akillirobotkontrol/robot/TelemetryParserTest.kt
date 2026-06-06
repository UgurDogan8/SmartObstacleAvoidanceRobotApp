package com.example.akillirobotkontrol.robot

import org.junit.Assert.assertEquals
import org.junit.Test

class TelemetryParserTest {

    @Test
    fun testValidTelemetry() {
        val telemetry = "D=28;M=A0;MS=S;S=F;V=5"
        val status = TelemetryParser.parse(telemetry)
        
        assertEquals(28, status.distanceCm)
        assertEquals("A0", status.mode)
        assertEquals("S", status.manualSubMode)
        assertEquals("F", status.movementState)
        assertEquals(5, status.speedLevel)
        assertEquals(WarningLevel.CAUTION, status.warningLevel)
    }
    
    @Test
    fun testPartialTelemetry() {
        val initialStatus = RobotStatus(distanceCm = 40, mode = "A1", manualSubMode = "K", movementState = "S", speedLevel = 2)
        // Sadece mesafe ve hız güncelleniyor
        val telemetry = "D=15;V=8"
        val status = TelemetryParser.parse(telemetry, initialStatus)
        
        // D ve V güncellenmeli, M, MS ve S initialStatus'tan korunmalı
        assertEquals(15, status.distanceCm)
        assertEquals("A1", status.mode)
        assertEquals("K", status.manualSubMode)
        assertEquals("S", status.movementState)
        assertEquals(8, status.speedLevel)
        assertEquals(WarningLevel.CRITICAL, status.warningLevel) // 15 < 20 olduğundan CRITICAL
    }
    
    @Test
    fun testMalformedTelemetry() {
        val initialStatus = RobotStatus(distanceCm = 50, mode = "A0", movementState = "F", speedLevel = 5)
        // D ve V için geçersiz karakterler, S boş
        val telemetry = "D=abc;M=A1;S=;V=xyz"
        val status = TelemetryParser.parse(telemetry, initialStatus)
        
        // D geçersiz olduğu için parse edilemez, önceki değer (50) korunur
        assertEquals(50, status.distanceCm)
        assertEquals(WarningLevel.SAFE, status.warningLevel) // 50 > 35 olduğundan SAFE
        
        // M 'A1' olarak parse edilir
        assertEquals("A1", status.mode)
        
        // S boş string olarak parse edilir
        assertEquals("", status.movementState)
        
        // V geçersiz olduğu için önceki değer (5) korunur
        assertEquals(5, status.speedLevel)
    }
    
    @Test
    fun testWarningLevels() {
        assertEquals(WarningLevel.CRITICAL, RobotStatus.calculateWarningLevel(10))
        assertEquals(WarningLevel.CRITICAL, RobotStatus.calculateWarningLevel(19))
        assertEquals(WarningLevel.CAUTION, RobotStatus.calculateWarningLevel(20))
        assertEquals(WarningLevel.CAUTION, RobotStatus.calculateWarningLevel(35))
        assertEquals(WarningLevel.SAFE, RobotStatus.calculateWarningLevel(36))
        assertEquals(WarningLevel.UNKNOWN, RobotStatus.calculateWarningLevel(null))
    }
}
