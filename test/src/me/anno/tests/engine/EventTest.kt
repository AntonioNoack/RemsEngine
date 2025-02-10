package me.anno.tests.engine

import me.anno.engine.Events.addEvent
import me.anno.engine.Events.workEventTasks
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

class EventTest {
    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testImmediateEventsOrder() {
        val expected = (0 until 20).toList()
        val tested = ArrayList<Int>()
        for (i in expected) {
            addEvent { tested.add(i) }
        }
        workEventTasks()
        assertEquals(expected, tested)
    }

    @Test
    @Execution(ExecutionMode.SAME_THREAD)
    fun testScheduledEventsOrder() {
        val expected = (0 until 20).toList()
        val events = expected.shuffled()
        val tested = ArrayList<Int>()
        for (i in events) {
            addEvent(i.toLong()) { tested.add(i) }
        }
        Thread.sleep(expected.max() + 1L)
        workEventTasks()
        assertEquals(expected, tested)
    }
}