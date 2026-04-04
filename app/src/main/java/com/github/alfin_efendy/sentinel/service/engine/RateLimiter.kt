package com.github.alfin_efendy.sentinel.service.engine

/**
 * Sliding-window rate limiter.
 * Tracks timestamps of relaunch attempts and rejects if [maxCount] is exceeded
 * within [windowMs] milliseconds.
 *
 * Not thread-safe — must be called from a single coroutine (Dispatchers.Default).
 */
class RateLimiter(
    private val maxCount: Int,
    private val windowMs: Long
) {
    private val timestamps = ArrayDeque<Long>()

    fun record(nowMs: Long = System.currentTimeMillis()) {
        timestamps.addLast(nowMs)
        pruneExpired(nowMs)
    }

    fun isExceeded(nowMs: Long = System.currentTimeMillis()): Boolean {
        pruneExpired(nowMs)
        return timestamps.size >= maxCount
    }

    fun countInWindow(nowMs: Long = System.currentTimeMillis()): Int {
        pruneExpired(nowMs)
        return timestamps.size
    }

    /** Time until the oldest entry in the window expires, giving us room for one more relaunch. */
    fun msUntilReset(nowMs: Long = System.currentTimeMillis()): Long {
        pruneExpired(nowMs)
        return if (timestamps.isEmpty()) 0L
        else (timestamps.first() + windowMs - nowMs).coerceAtLeast(0L)
    }

    fun reset() {
        timestamps.clear()
    }

    private fun pruneExpired(nowMs: Long) {
        while (timestamps.isNotEmpty() && (nowMs - timestamps.first()) >= windowMs) {
            timestamps.removeFirst()
        }
    }
}
