package me.anno.utils.async

import me.anno.Time
import me.anno.gpu.GFX
import me.anno.gpu.GFX.glThread
import me.anno.gpu.GFX.gpuTasks
import me.anno.gpu.framebuffer.FBStack
import me.anno.utils.structures.Task
import me.anno.utils.types.Floats.f3
import org.apache.logging.log4j.LogManager
import java.util.Queue
import kotlin.math.abs
import kotlin.math.max

object Queues {

    private val LOGGER = LogManager.getLogger(Queues::class)

    /**
     * time limit in seconds
     * returns whether time is left
     * */
    @JvmStatic
    fun workQueue(queue: Queue<Task>, timeLimit: Float, all: Boolean): Boolean {
        return workQueue(queue, if (all) Float.POSITIVE_INFINITY else timeLimit)
    }

    /**
     * time limit in seconds
     * returns whether time is left
     * */
    @JvmStatic
    fun workQueue(queue: Queue<Task>, timeLimit: Float): Boolean {
        if (queue.isEmpty()) return true // fast-path

        // async work section
        val startTime = Time.nanoTime

        // work 1/5th of the tasks by weight...

        // changing to 10 doesn't make the frame rate smoother :/
        val framesForWork = 5
        if (Thread.currentThread() == glThread) GFX.check()

        val workTodo = max(1000, queue.sumOf { it.cost } / framesForWork)
        var workDone = 0
        while (true) {
            val task = queue.poll() ?: return true
            try {
                task.work()
                if (queue === gpuTasks) {
                    GFX.check()
                }
            } catch (e: Throwable) {
                RuntimeException(task.name, e)
                    .printStackTrace()
            }
            if (Thread.currentThread() == glThread) GFX.check()
            workDone += task.cost
            val currentTime = Time.nanoTime
            val workTime = abs(currentTime - startTime) * 1e-9f
            if (workTime > 2f * timeLimit) {
                LOGGER.warn("Spent ${workTime.f3()}s on '${task.name}' with cost ${task.cost}")
            }
            if (workDone >= workTodo) return false
            if (workTime > timeLimit) return false // too much work
            FBStack.reset() // so we can reuse resources in different tasks
        }
    }
}