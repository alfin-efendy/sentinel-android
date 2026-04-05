package com.github.alfin_efendy.sentinel.core

import app.cash.turbine.test
import com.github.alfin_efendy.sentinel.domain.model.MonitoringState
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class ServiceStateHolderTest {

    @After
    fun tearDown() {
        // Reset singleton state to avoid polluting other tests
        ServiceStateHolder.setState(MonitoringState.Idle)
    }

    @Test
    fun `initial state is Idle`() {
        ServiceStateHolder.setState(MonitoringState.Idle)
        assertEquals(MonitoringState.Idle, ServiceStateHolder.currentState)
    }

    @Test
    fun `setState updates currentState`() {
        ServiceStateHolder.setState(MonitoringState.Monitoring("com.example"))
        assertEquals(MonitoringState.Monitoring("com.example"), ServiceStateHolder.currentState)
    }

    @Test
    fun `setState emits to state flow`() = runTest {
        ServiceStateHolder.state.test {
            // Skip current value
            awaitItem()
            ServiceStateHolder.setState(MonitoringState.Monitoring("com.roblox.client"))
            assertEquals(MonitoringState.Monitoring("com.roblox.client"), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple state transitions reflected in flow`() = runTest {
        ServiceStateHolder.setState(MonitoringState.Idle)
        ServiceStateHolder.state.test {
            awaitItem() // consume current Idle

            ServiceStateHolder.setState(MonitoringState.Monitoring("com.example"))
            assertEquals(MonitoringState.Monitoring("com.example"), awaitItem())

            ServiceStateHolder.setState(MonitoringState.Paused("com.example"))
            assertEquals(MonitoringState.Paused("com.example"), awaitItem())

            ServiceStateHolder.setState(MonitoringState.Idle)
            assertEquals(MonitoringState.Idle, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `currentState reflects last setState call`() {
        ServiceStateHolder.setState(MonitoringState.Relaunching("com.example", 2))
        assertEquals(MonitoringState.Relaunching("com.example", 2), ServiceStateHolder.currentState)
    }
}
