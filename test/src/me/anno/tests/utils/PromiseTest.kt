package me.anno.tests.utils

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertNull
import me.anno.utils.async.promise
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread

class PromiseTest {
    @Test
    fun testSuccess1() {
        var reached = 0
        promise { cb ->
            thread {
                Thread.sleep(2)
                cb.ok("Test")
            }
        }.then {
            assertEquals("Test", it)
            assertEquals(0, reached)
            reached++
        }.catch {
            assertFalse(true)
        }
        Thread.sleep(6)
        assertEquals(1, reached)
    }

    @Test
    fun testSuccess2() {
        var reached = 0
        promise { cb ->
            thread {
                Thread.sleep(2)
                cb.ok("Test")
            }
        }.then {
            assertEquals("Test", it)
            assertEquals(0, reached)
            ++reached
        }.then {
            assertEquals(1, it)
            ++reached
        }.then {
            assertEquals(2, it)
            ++reached
        }.catch {
            assertFalse(true)
        }
        Thread.sleep(6)
        assertEquals(3, reached)
    }

    @Test
    fun testFail0() {
        var reached = 0
        promise<String> { cb ->
            thread {
                Thread.sleep(2)
                cb.err(null)
            }
        }.catch {
            assertNull(it)
            reached++
        }
        Thread.sleep(6)
        assertEquals(1, reached)
    }

    @Test
    fun testFailX() {
        var reached = 0
        promise<String> { cb ->
            thread {
                Thread.sleep(2)
                cb.err(null)
            }
        }.then {
            // shall not be executed
            assertFalse(true)
        }.then {
            // shall not be executed
            assertFalse(true)
        }.catch {
            assertNull(it)
            reached++
        }.then {
            // shall not be executed
            assertFalse(true)
        }.then {
            // shall not be executed
            assertFalse(true)
        }.catch {
            assertNull(it)
            reached++
        }.catch {
            assertNull(it)
            reached++
        }
        Thread.sleep(6)
        assertEquals(3, reached)
    }

    @Test
    fun testFailX2() {
        var reached = 0
        promise { cb ->
            assertEquals(1, ++reached)
            thread {
                Thread.sleep(1)
                cb.ok("x")
            }
        }.thenAsync<String> { v, cb ->
            assertEquals("x", v)
            assertEquals(1, reached)
            thread {
                Thread.sleep(1)
                cb.err(null)
            }
        }.then {
            // shall not be executed
            assertFalse(true)
        }.catch {
            assertNull(it)
            reached++
        }.then {
            // shall not be executed
            assertFalse(true)
        }.then {
            // shall not be executed
            assertFalse(true)
        }.catch {
            assertNull(it)
            reached++
        }.catch {
            assertNull(it)
            reached++
        }
        Thread.sleep(6)
        assertEquals(4, reached)
    }
}