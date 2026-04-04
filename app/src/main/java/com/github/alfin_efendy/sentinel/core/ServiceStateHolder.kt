package com.github.alfin_efendy.sentinel.core

import com.github.alfin_efendy.sentinel.domain.model.MonitoringState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Process-wide runtime state. Observed by both services and the UI layer.
 * Thread-safe: MutableStateFlow.value is atomic.
 */
object ServiceStateHolder {

    private val _state = MutableStateFlow<MonitoringState>(MonitoringState.Idle)
    val state: StateFlow<MonitoringState> = _state.asStateFlow()

    fun setState(newState: MonitoringState) {
        _state.value = newState
    }

    val currentState: MonitoringState get() = _state.value
}
