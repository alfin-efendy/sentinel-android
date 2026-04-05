package com.github.alfin_efendy.sentinel.core

import app.cash.turbine.test
import com.github.alfin_efendy.sentinel.domain.model.ForegroundEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)

class AppEventBusTest {

    @Test
    fun `tryEmit delivers event to subscriber`() = runTest {
        val event = ForegroundEvent.AppEnteredForeground("com.example", 1000L)
        AppEventBus.events.test {
            AppEventBus.tryEmit(event)
            assertEquals(event, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `tryEmit delivers multiple events in FIFO order`() = runTest {
        val e1 = ForegroundEvent.AppEnteredForeground("com.a", 1L)
        val e2 = ForegroundEvent.AppLeftForeground("com.a", 2L)
        val e3 = ForegroundEvent.AccessibilityConnected
        AppEventBus.events.test {
            AppEventBus.tryEmit(e1)
            AppEventBus.tryEmit(e2)
            AppEventBus.tryEmit(e3)
            assertEquals(e1, awaitItem())
            assertEquals(e2, awaitItem())
            assertEquals(e3, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple subscribers each receive the same event`() = runTest {
        val event = ForegroundEvent.AccessibilityDisconnected
        val received1 = mutableListOf<ForegroundEvent>()
        val received2 = mutableListOf<ForegroundEvent>()

        val job1 = launch { AppEventBus.events.collect { received1.add(it) } }
        val job2 = launch { AppEventBus.events.collect { received2.add(it) } }

        // Let collectors actually start and suspend waiting for events
        runCurrent()
        AppEventBus.tryEmit(event)
        // Let collectors process the event
        runCurrent()

        job1.cancel()
        job2.cancel()

        assertEquals(listOf(event), received1)
        assertEquals(listOf(event), received2)
    }

    @Test
    fun `AccessibilityConnected event is delivered correctly`() = runTest {
        AppEventBus.events.test {
            AppEventBus.tryEmit(ForegroundEvent.AccessibilityConnected)
            assertEquals(ForegroundEvent.AccessibilityConnected, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
