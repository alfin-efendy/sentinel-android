package com.github.alfin_efendy.sentinel.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MonitoringStateTest {

    // isActive tests

    @Test
    fun `Idle isActive is false`() {
        assertFalse(MonitoringState.Idle.isActive)
    }

    @Test
    fun `Monitoring isActive is true`() {
        assertTrue(MonitoringState.Monitoring("com.example").isActive)
    }

    @Test
    fun `Relaunching isActive is true`() {
        assertTrue(MonitoringState.Relaunching("com.example", 1).isActive)
    }

    @Test
    fun `GracePeriod isActive is true`() {
        assertTrue(MonitoringState.GracePeriod("com.example", 9999L).isActive)
    }

    @Test
    fun `Paused isActive is false`() {
        assertFalse(MonitoringState.Paused("com.example").isActive)
    }

    // targetPackageOrNull tests

    @Test
    fun `Idle targetPackageOrNull is null`() {
        assertNull(MonitoringState.Idle.targetPackageOrNull)
    }

    @Test
    fun `Monitoring targetPackageOrNull returns package`() {
        assertEquals("com.roblox.client", MonitoringState.Monitoring("com.roblox.client").targetPackageOrNull)
    }

    @Test
    fun `Relaunching targetPackageOrNull returns package`() {
        assertEquals("com.roblox.client", MonitoringState.Relaunching("com.roblox.client", 2).targetPackageOrNull)
    }

    @Test
    fun `GracePeriod targetPackageOrNull returns package`() {
        assertEquals("com.roblox.client", MonitoringState.GracePeriod("com.roblox.client", 1000L).targetPackageOrNull)
    }

    @Test
    fun `Paused targetPackageOrNull returns package`() {
        assertEquals("com.roblox.client", MonitoringState.Paused("com.roblox.client").targetPackageOrNull)
    }

    // data class tests

    @Test
    fun `Relaunching equality and hashCode`() {
        val a = MonitoringState.Relaunching("com.example", 3)
        val b = MonitoringState.Relaunching("com.example", 3)
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `GracePeriod preserves expiresAt field`() {
        val state = MonitoringState.GracePeriod("com.example", 123456789L)
        assertEquals(123456789L, state.expiresAt)
    }
}
