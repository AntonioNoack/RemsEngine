package me.anno.engine

import me.anno.Build
import me.anno.Engine
import me.anno.Time
import me.anno.gpu.GFX.checkIfGFX
import me.anno.maths.Maths
import java.util.Queue
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.PriorityBlockingQueue

/**
 * This is the Android-equivalent of runOnUIThread():
 *  these events will be executed together before rendering each frame
 * */
object Events {

    private val eventTasks: Queue<NamedTask> = ConcurrentLinkedQueue()
    private val scheduledTasks: Queue<ScheduledTask> = PriorityBlockingQueue(16)

    fun getCalleeName(): String {
        if (!Build.isDebug) return ""
        val trace = Exception().stackTrace
        val entry = trace
            .firstOrNull { it.className != "me.anno.utils.Sleep" &&
                    it.className != "me.anno.cache.AsyncCacheData" &&
                    it.methodName != "getCalleeName" }
            ?: return "?"
        return entry.toString()
    }

    /**
     * schedules a task that will be executed on the main loop
     * */
    fun addEvent(name: String, event: () -> Unit) {
        eventTasks.add(NamedTask(name, event))
    }

    fun addEvent(event: () -> Unit) {
        addEvent(getCalleeName(), event)
    }

    /**
     * schedules a task that will be executed on the main loop;
     * will wait at least deltaMillis before it is executed;
     *
     * if deltaMillis = 0, this is like addGPUTask { addEvent { run() } }
     * */
    fun addEvent(name: String, deltaMillis: Long, event: () -> Unit) {
        scheduledTasks.add(ScheduledTask(name, Time.nanoTime + deltaMillis * Maths.MILLIS_TO_NANOS, event))
    }

    fun addEvent(deltaMillis: Long, event: () -> Unit) {
        addEvent(getCalleeName(), deltaMillis, event)
    }

    fun workTasks(tasks: Queue<NamedTask>) {
        // limit exists to prevent an infinite loop, when inside addEvent another event is added
        for (i in 0 until tasks.size) {
            val task = tasks.poll() ?: break
            try {
                task.runnable()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            checkIfGFX(task.name)
        }
    }

    private fun workImmediateTasks() {
        workTasks(eventTasks)
    }

    private fun workScheduledTasks() {
        val time = Time.nanoTime
        while (scheduledTasks.isNotEmpty()) {
            val peeked = scheduledTasks.peek()!!
            if (time >= peeked.time) {
                val task = scheduledTasks.poll()!!
                try {
                    task.runnable()
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
                checkIfGFX(task.name)
            } else break
        }
    }

    fun workEventTasks() {
        workImmediateTasks()
        workScheduledTasks()
    }

    init {
        Engine.registerForShutdown {
            finishEventTasks()
        }
    }

    /**
     * if you want this to execute, just properly request shutdown from the Engine
     * */
    private fun finishEventTasks() {
        workEventTasks()
        while (scheduledTasks.isNotEmpty()) {
            try {
                scheduledTasks.poll()!!.runnable()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}