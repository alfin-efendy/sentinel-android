package com.github.alfin_efendy.sentinel.service.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Cancellable coroutine-based debounce.
 * Schedules a block to run after [delayMs] ms; cancels any previously scheduled block.
 */
class DebounceController(private val scope: CoroutineScope) {

    private var pendingJob: Job? = null

    fun schedule(delayMs: Long, action: suspend () -> Unit) {
        cancel()
        pendingJob = scope.launch {
            delay(delayMs)
            action()
        }
    }

    fun cancel() {
        pendingJob?.cancel()
        pendingJob = null
    }

    val isPending: Boolean get() = pendingJob?.isActive == true
}
