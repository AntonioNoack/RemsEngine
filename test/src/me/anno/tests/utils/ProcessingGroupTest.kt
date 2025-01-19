package me.anno.tests.utils

import me.anno.Engine
import me.anno.utils.assertions.assertEquals
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.types.Ints.toIntOrDefault
import org.apache.logging.log4j.LogManager
import org.junit.jupiter.api.Test

@Suppress("DEPRECATION")
class ProcessingGroupTest {

    private val logger = LogManager.getLogger(ProcessingGroupTest::class)

    private fun checkOffWork(threads: BooleanArray, data: IntArray, i0: Int, i1: Int, sleepDur: Long) {
        // it is important, that all threads work
        val threadName = Thread.currentThread().name
        val threadId = threadName.split('-').last().toIntOrDefault(0)
        threads[threadId] = true
        for (i in i0 until i1) {
            data[i] += i + 1
        }
        Thread.sleep(sleepDur) // need to sleep a little, or a thread might do two
    }

    private fun checkWork(threads: BooleanArray, data: IntArray) {
        for (i in data.indices) {
            assertEquals(i + 1, data[i], "Entry $i was not computed!")
        }
        for (i in threads.indices) {
            if (!threads[i]) {
                logger.warn("Thread #$i didn't work!")
            }
        }
    }

    @Test
    fun testParallelExecutionBalanced() {
        Engine.cancelShutdown()
        val threads = BooleanArray(16)
        val group = ProcessingGroup("test", threads.size)
        val data = IntArray(1024)
        group.processBalanced(0, data.size, true) { i0, i1 ->
            checkOffWork(threads, data, i0, i1, 10)
        }
        checkWork(threads, data)
        group.stop()
    }

    @Test
    fun testParallelExecutionUnbalanced() {
        Engine.cancelShutdown()
        val threads = BooleanArray(16)
        val group = ProcessingGroup("test", threads.size)
        val data = IntArray(1024)
        group.processUnbalanced(0, data.size, true) { i0, i1 ->
            checkOffWork(threads, data, i0, i1, 1)
        }
        checkWork(threads, data)
        group.stop()
    }

    @Test
    fun testSingleThreaded() {
        Engine.cancelShutdown()
        val threads = BooleanArray(1)
        val group = ProcessingGroup("test", 1)
        val data = IntArray(1024)
        group.processBalanced(0, data.size, true) { i0, i1 ->
            checkOffWork(threads, data, i0, i1, 0)
        }
        checkWork(threads, data)
        group.stop()
    }
}