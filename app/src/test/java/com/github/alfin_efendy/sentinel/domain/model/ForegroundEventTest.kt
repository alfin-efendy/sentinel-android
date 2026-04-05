package com.github.alfin_efendy.sentinel.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ForegroundEventTest {

    @Test
    fun `AppEnteredForeground stores packageName and timestamp`() {
        val event = ForegroundEvent.AppEnteredForeground("com.example", 1000L)
        assertEquals("com.example", event.packageName)
        assertEquals(1000L, event.timestamp)
    }

    @Test
    fun `AppEnteredForeground default timestamp is non-zero`() {
        val before = System.currentTimeMillis()
        val event = ForegroundEvent.AppEnteredForeground("com.example")
        val after = System.currentTimeMillis()
        assertTrue(event.timestamp in before..after)
    }

    @Test
    fun `AppLeftForeground stores packageName and timestamp`() {
        val event = ForegroundEvent.AppLeftForeground("com.example", 2000L)
        assertEquals("com.example", event.packageName)
        assertEquals(2000L, event.timestamp)
    }

    @Test
    fun `AccessibilityConnected is equal to itself`() {
        assertEquals(ForegroundEvent.AccessibilityConnected, ForegroundEvent.AccessibilityConnected)
    }

    @Test
    fun `AccessibilityDisconnected is equal to itself`() {
        assertEquals(ForegroundEvent.AccessibilityDisconnected, ForegroundEvent.AccessibilityDisconnected)
    }

    @Test
    fun `AccessibilityConnected and Disconnected are not equal`() {
        assertNotEquals(
            ForegroundEvent.AccessibilityConnected,
            ForegroundEvent.AccessibilityDisconnected
        )
    }

    @Test
    fun `sealed class exhaustive when compiles`() {
        val event: ForegroundEvent = ForegroundEvent.AccessibilityConnected
        val handled = when (event) {
            is ForegroundEvent.AppEnteredForeground -> "entered"
            is ForegroundEvent.AppLeftForeground -> "left"
            ForegroundEvent.AccessibilityConnected -> "connected"
            ForegroundEvent.AccessibilityDisconnected -> "disconnected"
        }
        assertEquals("connected", handled)
    }
}
