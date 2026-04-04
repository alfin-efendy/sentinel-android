package com.github.alfin_efendy.sentinel.core

import com.github.alfin_efendy.sentinel.domain.model.ForegroundEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Process-wide IPC backbone between SentinelAccessibilityService and SentinelForegroundService.
 *
 * Uses a SharedFlow with extraBufferCapacity=64 so tryEmit() never suspends or blocks —
 * it is safe to call from the accessibility thread without coroutines.
 */
object AppEventBus {

    private val _events = MutableSharedFlow<ForegroundEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<ForegroundEvent> = _events.asSharedFlow()

    /** Non-blocking, non-suspending. Safe to call on any thread including the accessibility thread. */
    fun tryEmit(event: ForegroundEvent) {
        _events.tryEmit(event)
    }
}
