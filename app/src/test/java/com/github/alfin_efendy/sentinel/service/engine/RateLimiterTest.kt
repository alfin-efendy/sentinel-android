package com.github.alfin_efendy.sentinel.service.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RateLimiterTest {

    private lateinit var limiter: RateLimiter

    @Before
    fun setUp() {
        limiter = RateLimiter(maxCount = 3, windowMs = 60_000L)
    }

    @Test
    fun `record adds entry to window`() {
        limiter.record(nowMs = 0L)
        assertEquals(1, limiter.countInWindow(nowMs = 0L))
    }

    @Test
    fun `record does not exceed before maxCount`() {
        repeat(2) { limiter.record(nowMs = 0L) }
        assertFalse(limiter.isExceeded(nowMs = 0L))
    }

    @Test
    fun `isExceeded returns true at maxCount`() {
        repeat(3) { limiter.record(nowMs = 0L) }
        assertTrue(limiter.isExceeded(nowMs = 0L))
    }

    @Test
    fun `isExceeded returns false after window expires`() {
        repeat(3) { limiter.record(nowMs = 0L) }
        // Advance past the window
        assertFalse(limiter.isExceeded(nowMs = 60_001L))
    }

    @Test
    fun `countInWindow excludes expired entries`() {
        repeat(3) { limiter.record(nowMs = 0L) }
        assertEquals(0, limiter.countInWindow(nowMs = 60_001L))
    }

    @Test
    fun `msUntilReset returns zero when empty`() {
        assertEquals(0L, limiter.msUntilReset(nowMs = 0L))
    }

    @Test
    fun `msUntilReset returns correct duration`() {
        limiter.record(nowMs = 0L)
        // At t=30000, oldest entry is at t=0, expires at t=60000 → remaining = 30000
        assertEquals(30_000L, limiter.msUntilReset(nowMs = 30_000L))
    }

    @Test
    fun `msUntilReset returns zero when entry has expired`() {
        limiter.record(nowMs = 0L)
        assertEquals(0L, limiter.msUntilReset(nowMs = 60_001L))
    }

    @Test
    fun `reset clears all entries`() {
        repeat(3) { limiter.record(nowMs = 0L) }
        limiter.reset()
        assertEquals(0, limiter.countInWindow(nowMs = 0L))
        assertFalse(limiter.isExceeded(nowMs = 0L))
    }

    @Test
    fun `sliding window allows new entry after oldest expires`() {
        // Fill window at t=0
        repeat(3) { limiter.record(nowMs = 0L) }
        assertTrue(limiter.isExceeded(nowMs = 0L))
        // At t=60001 oldest expires, add one more → count = 1, not exceeded
        limiter.record(nowMs = 60_001L)
        assertFalse(limiter.isExceeded(nowMs = 60_001L))
        assertEquals(1, limiter.countInWindow(nowMs = 60_001L))
    }
}
