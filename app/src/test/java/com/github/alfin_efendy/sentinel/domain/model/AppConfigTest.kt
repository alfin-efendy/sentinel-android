package com.github.alfin_efendy.sentinel.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppConfigTest {

    @Test
    fun `isValid returns true when packageName is not blank`() {
        assertTrue(AppConfig(packageName = "com.example.app").isValid)
    }

    @Test
    fun `isValid returns false when packageName is blank`() {
        assertFalse(AppConfig(packageName = "").isValid)
    }

    @Test
    fun `isValid returns false when packageName is whitespace`() {
        assertFalse(AppConfig(packageName = "   ").isValid)
    }

    @Test
    fun `default values are correct`() {
        val config = AppConfig()
        assertEquals("", config.packageName)
        assertEquals(null, config.deepLinkUrl)
        assertFalse(config.isEnabled)
        assertEquals(2500L, config.debounceMs)
        assertEquals(8000L, config.gracePeriodMs)
        assertEquals(3, config.maxRelaunchesPerWindow)
        assertEquals(60_000L, config.windowDurationMs)
    }

    @Test
    fun `copy preserves validity`() {
        val original = AppConfig(packageName = "com.roblox.client")
        val copy = original.copy(deepLinkUrl = "roblox://placeId=123")
        assertTrue(copy.isValid)
        assertEquals("com.roblox.client", copy.packageName)
    }

    @Test
    fun `data class equality by value`() {
        val a = AppConfig(packageName = "com.example", isEnabled = true)
        val b = AppConfig(packageName = "com.example", isEnabled = true)
        assertEquals(a, b)
    }
}
