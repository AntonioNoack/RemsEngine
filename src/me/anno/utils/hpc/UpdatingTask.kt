package me.anno.utils.hpc

import me.anno.utils.Sleep
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * Task, that can be cancelled and re-run
 * */
class UpdatingTask(val threadName: String) {

    val id = AtomicInteger(0)
    var runningThread: Thread? = null

    fun compute(computation: (id: Int, done: () -> Unit) -> Unit) {
        val newId = id.incrementAndGet()
        Sleep.waitUntil(true, {
            runningThread == null || id.get() != newId
        }, {
            synchronized(this) {
                if (id.get() == newId && runningThread == null) {
                    runningThread = thread(name = threadName) {
                        if (id.get() == newId) {
                            computation(newId) {
                                runningThread = null
                            }
                        } else runningThread = null
                    }
                }// else another one started
            }
        })
    }

    fun destroy() {
        id.incrementAndGet()
    }
}