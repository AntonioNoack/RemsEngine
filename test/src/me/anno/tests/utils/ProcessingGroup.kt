package me.anno.tests.utils

import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.types.Ints.toIntOrDefault
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProcessingGroupTest {

    @Test
    fun testParallelExecutionBalanced() {
        val threads = BooleanArray(16)
        val group = ProcessingGroup("test", threads.size)
        val data = IntArray(1024)
        group.processBalanced(0, data.size, true) { i0, i1 ->
            // it is important, that all threads work
            val threadName = Thread.currentThread().name
            val threadId = threadName.split('-').last().toIntOrDefault(0)
            threads[threadId] = true
            for (i in i0 until i1) {
                data[i] += i + 1
            }
            Thread.sleep(10) // need to sleep a little, or a thread might do two
        }
        for (i in data.indices) {
            assertEquals(i + 1, data[i], "Entry $i was not computed!")
        }
        for (i in threads.indices) {
            assertTrue(threads[i], "Thread #$i didn't work!")
        }
        group.stop()
    }

    @Test
    fun testParallelExecutionUnbalanced() {
        val threads = BooleanArray(16)
        val group = ProcessingGroup("test", threads.size)
        val data = IntArray(1024)
        group.processUnbalanced(0, data.size, true) { i0, i1 ->
            // it is important, that all threads work
            val threadName = Thread.currentThread().name
            val threadId = threadName.split('-').last().toIntOrDefault(0)
            threads[threadId] = true
            for (i in i0 until i1) {
                data[i] += i + 1
            }
            Thread.sleep(1)
        }
        for (i in data.indices) {
            assertEquals(i + 1, data[i], "Entry $i was not computed!")
        }
        for (i in threads.indices) {
            assertTrue(threads[i], "Thread #$i didn't work!")
        }
        group.stop()
    }
}