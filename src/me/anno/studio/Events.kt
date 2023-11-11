package me.anno.studio

import me.anno.Engine
import me.anno.Time
import me.anno.maths.Maths
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.PriorityBlockingQueue

/**
 * This is the Android-equivalent of runOnUIThread():
 *  these events will be executed together before rendering each frame
 * */
object Events {

    private val eventTasks: Queue<() -> Unit> = ConcurrentLinkedQueue()
    private val scheduledTasks: Queue<Pair<Long, () -> Unit>> =
        PriorityBlockingQueue(16) { a, b -> a.first.compareTo(b.first) }

    /**
     * schedules a task that will be executed on the main loop
     * */
    fun addEvent(event: () -> Unit) {
        eventTasks += event
    }

    /**
     * schedules a task that will be executed on the main loop;
     * will wait at least deltaMillis before it is executed
     * */
    fun addEvent(deltaMillis: Long, event: () -> Unit) {
        scheduledTasks.add(Pair(Time.nanoTime + deltaMillis * Maths.MILLIS_TO_NANOS, event))
    }

    fun workEventTasks() {
        while (scheduledTasks.isNotEmpty()) {
            try {
                val time = Time.nanoTime
                val peeked = scheduledTasks.peek()!!
                if (time >= peeked.first) {
                    scheduledTasks.poll()!!.second.invoke()
                } else break
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
        while (eventTasks.isNotEmpty()) {
            try {
                eventTasks.poll()!!.invoke()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
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
                scheduledTasks.poll()!!.second.invoke()
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}