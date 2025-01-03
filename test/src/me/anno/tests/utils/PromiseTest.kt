package me.anno.tests.utils

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFail
import me.anno.utils.assertions.assertNull
import me.anno.utils.assertions.assertTrue
import me.anno.utils.async.promise
import me.anno.utils.async.firstPromise
import org.junit.jupiter.api.Test
import java.io.FileNotFoundException
import java.io.IOException

class PromiseTest {

    @Test
    fun testSuccessSimple() {
        var reached = 0
        val thread = Thread1()
        promise { cb ->
            thread += {
                cb.ok("Test")
            }
        }.then {
            assertEquals("Test", it)
            assertEquals(0, reached++)
        }.catch {
            assertFail()
        }
        thread.work()
        assertEquals(1, reached)
    }

    @Test
    fun testSuccessChain() {
        var reached = 0
        val thread = Thread1()
        promise { cb ->
            thread += {
                cb.ok(17)
            }
        }.then {
            assertEquals(17, it)
            assertEquals(0, reached++)
            39
        }.then {
            assertEquals(39, it)
            assertEquals(1, reached++)
            21
        }.then {
            assertEquals(21, it)
            assertEquals(2, reached++)
        }.catch {
            assertFail()
        }
        thread.work()
        assertEquals(3, reached)
    }

    @Test
    fun testSuccessAsyncChain() {
        var reached = 0
        val thread = Thread1()
        promise { cb ->
            thread += {
                cb.ok(17)
            }
        }.thenAsync { it, cb ->
            thread += {
                assertEquals(17, it)
                assertEquals(0, reached++)
                cb.ok(39)
            }
        }.then {
            assertEquals(39, it)
            assertEquals(1, reached++)
            -16
        }.thenAsync { it, cb ->
            thread += {
                assertEquals(-16, it)
                assertEquals(2, reached++)
                cb.ok(100)
            }
        }.catch {
            assertFail()
        }
        thread.work()
        assertEquals(3, reached)
    }

    @Test
    fun testSimpleFailWithSleep() {
        var reached = 0
        val thread = Thread1()
        promise<String> { cb ->
            thread += {
                cb.err(null)
            }
        }.catch {
            assertNull(it)
            reached++
        }
        thread.work()
        assertEquals(1, reached)
    }

    @Test
    fun testSimpleFailWithoutSleep() {
        var reached = 0
        promise<String> { cb ->
            cb.err(null)
        }.catch {
            assertNull(it)
            reached++
        }
        assertEquals(1, reached)
    }

    @Test
    fun testFailTwiceWithSleep() {
        var reached = 0
        val thread = Thread1()
        promise<String> { cb ->
            thread += {
                cb.err(IOException())
            }
        }.then {
            // shall not be executed
            assertFail()
        }.catch {
            assertTrue(it is IOException)
            assertEquals(0, reached++)
        }.then {
            // shall not be executed
            assertFail()
        }.catch {
            assertTrue(it is IOException)
            assertEquals(1, reached++)
        }
        thread.work()
        assertEquals(2, reached)
    }

    @Test
    fun testFailTwiceWithoutSleep() {
        var reached = 0
        promise<String> { cb ->
            assertEquals(0, reached++)
            cb.err(FileNotFoundException("test"))
        }.then {
            // shall not be executed
            assertFail()
        }.catch {
            assertTrue(it is IOException)
            assertEquals(1, reached++)
        }.then {
            // shall not be executed
            assertFail()
        }.catch {
            assertTrue(it is IOException)
            assertEquals(2, reached++)
        }
        assertEquals(3, reached)
    }

    @Test
    fun testFailThenCatch() {
        var reached = 0
        promise<Int> { cb ->
            cb.err(IOException())
        }.then {
            assertFail()
        }.catch {
            assertTrue(it is IOException)
            assertEquals(0, reached++)
        }
        assertEquals(1, reached)
    }

    @Test
    fun testFailCatchThen() {
        var reached = 0
        promise<Int> { cb ->
            cb.err(IOException())
        }.catch {
            assertTrue(it is IOException)
            assertEquals(0, reached++)
        }.then {
            assertFail()
        }
        assertEquals(1, reached)
    }

    @Test
    fun testFailComplex() {
        var reached = 0
        val thread = Thread1()
        promise { cb ->
            assertEquals(0, reached++)
            thread += {
                cb.ok("x")
            }
        }.thenAsync<String> { v, cb ->
            assertEquals("x", v)
            assertEquals(1, reached++)
            thread += {
                cb.err(IOException())
            }
        }.then {
            assertFail("shall not be executed")
        }.catch {
            assertTrue(it is IOException)
            assertEquals(2, reached++)
        }.then {
            assertFail("shall not be executed")
        }.then {
            assertFail("shall not be executed")
        }.catch {
            assertTrue(it is IOException)
            assertEquals(3, reached++)
        }.catch {
            assertTrue(it is IOException)
            assertEquals(4, reached++)
        }
        thread.work()
        assertEquals(5, reached)
    }

    @Test
    fun testFirstPromiseWithSuccess() {
        var isFinished = false
        firstPromise(listOf(1, 2, 3)) { idx, cb ->
            when (idx) {
                1 -> cb.err(null) // wait for next one
                2 -> cb.ok("good")
                else -> throw IllegalStateException()
            }
        }.then { good ->
            assertEquals("good", good)
            isFinished = true
        }.catch { _ ->
            assertFail("shall not be executed")
        }
        assertTrue(isFinished)
    }

    @Test
    fun testFirstPromiseWithFailure() {
        var reached = 0
        firstPromise<Int, Unit>(listOf(1, 2)) { idx, cb ->
            when (idx) {
                1 -> {
                    assertEquals(0, reached++)
                    cb.err(null)
                }
                2 -> {
                    assertEquals(1, reached++)
                    cb.err(IOException())
                }
                else -> throw IllegalStateException()
            }
        }.then {
            assertFail("shall not be executed")
        }.catch { _ ->
            assertEquals(2, reached++)
        }
        assertEquals(3, reached)
    }
}