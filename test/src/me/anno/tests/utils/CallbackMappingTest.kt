package me.anno.tests.utils

import me.anno.engine.Events
import me.anno.engine.Events.addEvent
import me.anno.maths.Maths
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.Callback.Companion.mapCallback
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.test.assertIs

class CallbackMappingTest {

    fun checkResult(hasResult: () -> Int) {
        Thread.sleep(10)
        Events.workEventTasks()
        assertEquals(1, hasResult())
    }

    @Test
    fun testMapMapCallback() {
        var hasResult = 0
        mapOf("a" to 1, "b" to 2, "c" to 3).mapCallback<String, Int, String>(
            { key, value, cb ->
                addEvent(Maths.randomLong(0, 10)) {
                    cb.ok("$key: $value")
                }
            }, { result, err ->
                assertNull(err)
                assertEquals(mapOf("a" to "a: 1", "b" to "b: 2", "c" to "c: 3"), result)
                hasResult++
            })
        checkResult { hasResult }
    }

    @Test
    fun testListMapCallback() {
        var hasResult = 0
        listOf(1, 2, 3).mapCallback<Int, Int>(
            { index, value, cb ->
                assertEquals(index + 1, value)
                addEvent(Maths.randomLong(0, 10)) {
                    cb.ok(value + 1)
                }
            }, { result, err ->
                assertNull(err)
                assertEquals(listOf(2, 3, 4), result)
                hasResult++
            })
        checkResult { hasResult }
    }

    @Test
    fun testSetMapCallback() {
        var hasResult = 0
        setOf(1, 2, 3).mapCallback<Int, Int>(
            { index, value, cb ->
                assertTrue(index in 0 until 3)
                addEvent(Maths.randomLong(0, 10)) {
                    cb.ok(value + 1)
                }
            }, { result, err ->
                assertNull(err)
                assertEquals(setOf(2, 3, 4), result)
                hasResult++
            })
        checkResult { hasResult }
    }

    @Test
    fun testListError() {
        var hasResult = 0
        listOf(2, -1, 1, 3).mapCallback<Int, Int>(
            { index, value, cb ->
                assertTrue(index in 0 until 4)
                addEvent(Maths.randomLong(0, 10)) {
                    if (value < 0) cb.err(NullPointerException())
                    else cb.ok(value + 1)
                }
            }, { result, err ->
                assertNull(result)
                assertIs<NullPointerException>(err)
                hasResult++
            })
        checkResult { hasResult }
    }

    @Test
    fun testSetError() {
        var hasResult = 0
        setOf(2, -1, 1, 3).mapCallback<Int, Int>(
            { index, value, cb ->
                assertTrue(index in 0 until 4)
                addEvent(Maths.randomLong(0, 10)) {
                    if (value < 0) cb.err(NullPointerException())
                    else cb.ok(value + 1)
                }
            }, { result, err ->
                assertNull(result)
                assertIs<NullPointerException>(err)
                hasResult++
            })
        checkResult { hasResult }
    }
}