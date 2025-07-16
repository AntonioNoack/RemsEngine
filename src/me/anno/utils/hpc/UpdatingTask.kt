package me.anno.utils.hpc

import me.anno.Time
import me.anno.maths.Maths.MILLIS_TO_NANOS
import me.anno.utils.Sleep
import me.anno.utils.Threads
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.math.abs

/**
 * Task, that can be cancelled and re-run
 * */
class UpdatingTask(val threadName: String) {

    companion object {
        private const val TIMEOUT_NANOS = 5000L * MILLIS_TO_NANOS
    }

    val id = AtomicInteger(0)
    var runningThread: Thread? = null
    var lastSuccessfulStart = 0L

    fun compute(computation: (id: Int, done: () -> Unit) -> Unit) {
        val newId = id.incrementAndGet()
        Sleep.waitUntil("UpdatingTask", true, {
            runningThread == null || id.get() != newId
        }, {
            synchronized(this) {
                val time = Time.nanoTime
                if (id.get() == newId && (runningThread == null || abs(time - lastSuccessfulStart) >= TIMEOUT_NANOS)) {
                    lastSuccessfulStart = time
                    runningThread = Threads.runWorkerThread(threadName) {
                        if (id.get() == newId) {
                            computation(newId) {
                                freeThread()
                            }
                        } else freeThread()
                    }
                }// else another one started
            }
        })
    }

    private fun freeThread() {
        synchronized(this) {
            runningThread = null
        }
    }

    fun destroy() {
        id.incrementAndGet()
    }
}