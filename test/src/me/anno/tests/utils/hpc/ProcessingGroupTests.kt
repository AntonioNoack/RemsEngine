package me.anno.tests.utils.hpc

import me.anno.Engine
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.hpc.ProcessingGroup
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

class ProcessingGroupTests {

    private fun createGroup(size: Int): ProcessingGroup {
        return ProcessingGroup("ProcessingGroupTests", size)
    }

    @BeforeEach
    fun init() {
        Engine.cancelShutdown()
    }

    @Test
    fun testProcessedBalanced() {
        val group = createGroup(4)
        val grid = IntArray(100)
        val seenThreads = HashSet<Thread>()
        val numCalls = AtomicInteger()
        group.processBalanced(0, 100, 1) { x0, x1 ->

            // mark worked on cells
            for (i in x0 until x1) grid[i]++

            // ensure work is distributed evenly
            assertEquals(0, x0 % 25)
            assertEquals(0, x1 % 25)

            seenThreads.add(Thread.currentThread())
            numCalls.incrementAndGet()

            Thread.sleep(100) // ensure all threads have a chance to work
        }
        assertTrue(grid.all { it == 1 }) // ensure all cells were processed exactly once
        assertEquals(4, seenThreads.size) // ensure all threads are working
        assertEquals(4, numCalls.get())
        group.stop()
    }

    @Test
    fun testProcessedBalanced2d() {
        val group = createGroup(4)
        val grid = Array(12) { IntArray(28) }
        val seenThreads = HashSet<Thread>()
        val numCalls = AtomicInteger()
        val tileSize = 10
        group.processBalanced2d(0, 0, 28, 12, tileSize, 1) { x0, y0, x1, y1 ->

            // mark worked on cells
            for (y in y0 until y1) {
                for (x in x0 until x1) {
                    grid[y][x]++
                }
            }

            // ensure tile size is respected
            assertTrue(x1 - x0 <= tileSize)
            assertTrue(y1 - y0 <= tileSize)

            seenThreads.add(Thread.currentThread())
            numCalls.incrementAndGet()

            Thread.sleep(100) // ensure all threads have a chance to work
        }
        assertTrue(grid.all { column -> column.all { it == 1 } }) // ensure all cells were processed exactly once
        assertEquals(4, seenThreads.size) // ensure all threads are working
        assertEquals(6, numCalls.get()) // 6 tiles are to be processed
        group.stop()
    }
}