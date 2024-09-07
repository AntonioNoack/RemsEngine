package me.anno.gpu

import me.anno.Engine
import me.anno.Time
import me.anno.gpu.GFX.gpuTaskBudget
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

    @JvmStatic
    fun addGPUTask(name: String, w: Int, h: Int, task: () -> Unit) = addGPUTask(name, w, h, false, task)

    @JvmStatic
    fun addGPUTask(name: String, w: Int, h: Int, lowPriority: Boolean, task: () -> Unit) {
        addGPUTask(name, max(1, ((w * h.toLong()) / 10_000).toInt()), lowPriority, task)
    }

    @JvmStatic
    fun addGPUTask(name: String, weight: Int, task: () -> Unit) = addGPUTask(name, weight, false, task)

    @JvmStatic
    fun addGPUTask(name: String, weight: Int, lowPriority: Boolean, task: () -> Unit) {
        (if (lowPriority) lowPriorityGPUTasks else gpuTasks) += Task(name, weight, task)
    }

    @JvmStatic
    fun addNextGPUTask(name: String, w: Int, h: Int, task: () -> Unit) =
        addNextGPUTask(name, max(1, ((w * h.toLong()) / 10_000).toInt()), task)

    @JvmStatic
    fun addNextGPUTask(name: String, weight: Int, task: () -> Unit) {
        nextGPUTasks += Task(name, weight, task)
    }

    @JvmStatic
    fun workGPUTasks(all: Boolean) {
        val t0 = Time.nanoTime
        // todo just in case, clear gfx state here:
        //  we might be waiting on the gfx thread, and if so, state would be different than usual
        synchronized(nextGPUTasks) {
            gpuTasks.addAll(nextGPUTasks)
            nextGPUTasks.clear()
        }
        if (workQueue(gpuTasks, gpuTaskBudget, all)) {
            val remainingTime = Time.nanoTime - t0
            workQueue(lowPriorityGPUTasks, remainingTime * 1e-9f, all)
        }
    }

    @JvmStatic
    fun workGPUTasksUntilShutdown() {
        while (!Engine.shutdown) {
            workGPUTasks(true)
        }
    }

}