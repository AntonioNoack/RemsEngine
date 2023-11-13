package me.anno.utils.hpc

import me.anno.utils.OS
import me.anno.utils.Sleep
import org.apache.logging.log4j.LogManager
import java.io.IOException
import kotlin.concurrent.thread

/**
 * Task, that can be cancelled and re-run;
 * Insert Thread.sleep(0) every once in a while into your calculations, so they can be cancelled
 * */
class UpdatingTask(val threadName: String, val cleaning: () -> Unit) {

    var runningThread: Thread? = null

    fun compute(computation: () -> Unit) {
        if (OS.isWeb) {// multithreading isn't available yet
            computation()
        } else {
            runningThread?.interrupt()
            Sleep.waitUntil(true) { runningThread == null }
            runningThread = thread(start = false, name = threadName) {
                try {
                    computation()
                } catch (e: InterruptedException) {
                    // kill successful
                } catch (e: Exception) {
                    // e.g. IOException, because a channel was closed while waiting for the stream
                    LOGGER.warn("Catched error", e)
                }
                runningThread = null
            }
            cleaning()
            runningThread!!.start()
        }
    }

    fun destroy() {
        runningThread?.interrupt()
        runningThread = null
    }

    companion object {
        private val LOGGER = LogManager.getLogger(UpdatingTask::class)
    }

}