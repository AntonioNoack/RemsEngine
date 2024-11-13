package me.anno.gpu

import me.anno.Engine
import me.anno.Time
import me.anno.gpu.GFX.gpuTaskBudgetNanos
import me.anno.utils.async.Queues.workQueue
import me.anno.utils.structures.Task
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.max

object GPUTasks {

    @JvmField
    val nextGPUTasks = ArrayList<Task>()

    @JvmField
    val gpuTasks: Queue<Task> = ConcurrentLinkedQueue()

    @JvmField
    val lowPriorityGPUTasks: Queue<Task> = ConcurrentLinkedQueue()

    fun combineCost(w: Int, h: Int): Int = max(1, ((w * h.toLong()) ushr 13).toInt())

    @JvmStatic
    fun addGPUTask(name: String, w: Int, h: Int, task: () -> Unit) = addGPUTask(name, w, h, true, task)

    @JvmStatic
    fun addGPUTask(name: String, w: Int, h: Int, lowPriority: Boolean, task: () -> Unit) {
        addGPUTask(name, combineCost(w, h), lowPriority, task)
    }

    @JvmStatic
    fun addGPUTask(name: String, weight: Int, task: () -> Unit) = addGPUTask(name, weight, true, task)

    @JvmStatic
    fun addGPUTask(name: String, weight: Int, lowPriority: Boolean, task: () -> Unit) {
        val queue = if (lowPriority) lowPriorityGPUTasks else gpuTasks
        queue.add(Task(name, weight, task))
    }

    @JvmStatic
    fun addNextGPUTask(name: String, w: Int, h: Int, task: () -> Unit) =
        addNextGPUTask(name, max(1, ((w * h.toLong()) / 10_000).toInt()), task)

    @JvmStatic
    fun addNextGPUTask(name: String, weight: Int, task: () -> Unit) {
        synchronized(nextGPUTasks) {
            nextGPUTasks.add(Task(name, weight, task))
        }
    }

    @JvmStatic
    fun workGPUTasks(all: Boolean) {
        // todo just in case, clear gfx state here:
        //  we might be waiting on the gfx thread, and if so, state would be different than usual
        synchronized(nextGPUTasks) {
            gpuTasks.addAll(nextGPUTasks)
            nextGPUTasks.clear()
        }
        if (workQueue(gpuTasks, getBudget(), all)) {
            workQueue(lowPriorityGPUTasks, getBudget(), all)
        }
    }

    private fun getBudget(): Long {
        val t0 = Time.nanoTime
        return max(gpuTaskBudgetNanos + Time.frameTimeNanos - t0, 1)
    }

    @JvmStatic
    fun workGPUTasksUntilShutdown() {
        while (!Engine.shutdown) {
            workGPUTasks(true)
        }
    }
}