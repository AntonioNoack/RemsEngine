package me.anno.tests.utils.hpc

import me.anno.gpu.drawing.DrawTexts.drawText
import me.anno.ui.base.components.AxisAlignment
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.Threads
import speiger.primitivecollections.LongHashSet

fun main() {
    // todo the ThreadPool is spawning tons of new threads,
    //  and each one seems to run for a whole second
    //  this is very bad
    val totalThreads = LongHashSet()
    testDrawing("ThreadPool") {
        for (i in 0 until 100) Threads.runTaskThread("tmp") {
            synchronized(totalThreads) { totalThreads.add(Thread.currentThread().id) }
            Thread.sleep(1)
        }
        drawText(
            it.x + 10, it.y + it.height - 10, 0, "" +
                    "wait: ${Threads.numWaitingTasks}, " +
                    "sleep: ${Threads.numSleepingThreads}, " +
                    "unfin: ${Threads.numUnfinishedTasks}, " +
                    "workers: ${Threads.numWorkerThreads}, " +
                    "total: ${totalThreads.size}",
            AxisAlignment.MIN, AxisAlignment.MAX
        )
    }
}