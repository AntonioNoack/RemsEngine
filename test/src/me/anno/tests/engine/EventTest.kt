package me.anno.tests.engine

import me.anno.engine.Events.addEvent
import me.anno.engine.Events.workEventTasks
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EventTest {
    @Test
    fun testImmediateEvents() {
        var value = 0
        // must be executed in order
        addEvent { value++ }
        addEvent { value *= 5 }
        workEventTasks()
        assertEquals(5, value)
    }

    @Test
    fun testScheduledEvents() {
        var value = 0
        // must be executed in order by time
        addEvent(4) { value *= 5 }
        addEvent(0) { value++ }
        workEventTasks()
        assertEquals(1, value)

        Thread.sleep(5)
        workEventTasks()
        assertEquals(5, value)
    }
}