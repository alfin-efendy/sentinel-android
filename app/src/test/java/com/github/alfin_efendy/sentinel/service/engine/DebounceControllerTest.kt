package com.github.alfin_efendy.sentinel.service.engine

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DebounceControllerTest {

    @Test
    fun `schedule executes action after delay`() = runTest {
        val controller = DebounceController(this)
        var ran = false
        controller.schedule(100L) { ran = true }
        advanceTimeBy(101L) // exclusive: tasks at exactly +100ms need +1ms extra
        assertTrue(ran)
    }

    @Test
    fun `schedule does not execute before delay`() = runTest {
        val controller = DebounceController(this)
        var ran = false
        controller.schedule(100L) { ran = true }
        advanceTimeBy(50L)
        assertFalse(ran)
    }

    @Test
    fun `cancel prevents scheduled action from running`() = runTest {
        val controller = DebounceController(this)
        var ran = false
        controller.schedule(100L) { ran = true }
        controller.cancel()
        advanceTimeBy(200L)
        assertFalse(ran)
    }

    @Test
    fun `schedule cancels existing job and only runs last one`() = runTest {
        val controller = DebounceController(this)
        var actionA = false
        var actionB = false
        controller.schedule(100L) { actionA = true }
        advanceTimeBy(50L)
        controller.schedule(100L) { actionB = true }
        advanceTimeBy(101L) // exclusive: tasks at exactly +100ms need +1ms extra
        assertFalse(actionA)
        assertTrue(actionB)
    }

    @Test
    fun `isPending is true while job is active`() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(testDispatcher)
        val controller = DebounceController(scope)
        controller.schedule(100L) { }
        assertTrue(controller.isPending)
    }

    @Test
    fun `isPending is false after delay elapses`() = runTest {
        val controller = DebounceController(this)
        controller.schedule(100L) { }
        advanceTimeBy(101L)
        assertFalse(controller.isPending)
    }

    @Test
    fun `isPending is false after cancel`() = runTest {
        val controller = DebounceController(this)
        controller.schedule(100L) { }
        controller.cancel()
        assertFalse(controller.isPending)
    }

    @Test
    fun `isPending is false on fresh instance`() = runTest {
        val controller = DebounceController(this)
        assertFalse(controller.isPending)
    }
}
