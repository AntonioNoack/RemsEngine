package me.anno.utils.async

import me.anno.Time
import me.anno.gpu.GFX
import me.anno.gpu.GFX.glThread
import me.anno.gpu.GPUTasks.gpuTasks
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
    fun workQueue(queue: Queue<Task>, timeLimitNanos: Long, all: Boolean): Boolean {
        return workQueue(queue, if (all) Long.MAX_VALUE else timeLimitNanos)
    }

    /**
     * time limit in seconds
     * returns whether time is left
     * */
    @JvmStatic
    fun workQueue(queue: Queue<Task>, timeLimitNanos: Long): Boolean {
        if (queue.isEmpty()) return true // fast-path

        // async work section
        val startTime = Time.nanoTime

        // work 1/5th of the tasks by weight...

        // changing to 10 doesn't make the frame rate smoother :/
        val framesForWork = 5
        val isGLThread = Thread.currentThread() == glThread
        if (isGLThread) {
            GFX.checkWithoutCrashing("workQueue")
        }

        val workTodo = max(1000, queue.sumOf { it.cost } / framesForWork)
        var workDone = 0
        while (true) {
            val task = queue.poll() ?: return true
            try {
                task.work()
            } catch (e: Throwable) {
                RuntimeException(task.name, e)
                    .printStackTrace()
            }
            if (isGLThread || queue === gpuTasks) {
                GFX.checkWithoutCrashing(task.name)
            }
            workDone += task.cost
            val currentTime = Time.nanoTime
            val workTime = abs(currentTime - startTime)
            if (workTime.shr(1) > timeLimitNanos) {
                LOGGER.warn("Spent ${(workTime / 1e9).f3()}s on '${task.name}' with cost ${task.cost}")
            }
            if (workDone >= workTodo) return false
            if (workTime > timeLimitNanos) return false // too much work
            FBStack.reset() // so we can reuse resources in different tasks
        }
    }
}