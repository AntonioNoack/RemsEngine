package me.anno.tests

import me.anno.Engine
import me.anno.utils.hpc.ProcessingGroup

fun main() {
    val threads = BooleanArray(16)
    val group = ProcessingGroup("test", threads.size - 1)
    val data = IntArray(1024)
    group.processBalanced(0, data.size, true) { i0, i1 ->
        // it is important, that all threads work
        val threadName = Thread.currentThread().name
        val threadId = threadName.split('-').last().toIntOrNull() ?: -1
        threads[threadId + 1] = true
        for (i in i0 until i1) {
            data[i] += i + 1
        }
        Thread.sleep(100)
    }
    for (i in data.indices) {
        if (data[i] != i + 1) throw RuntimeException("Entry $i was not computed!")
    }
    for (i in threads.indices) {
        if (!threads[i]) throw RuntimeException("Thread #${i - 1} didn't work!")
    }
    Engine.requestShutdown()
}