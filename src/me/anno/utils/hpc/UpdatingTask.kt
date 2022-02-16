package me.anno.utils.hpc

import me.anno.utils.LOGGER
import me.anno.utils.Sleep
import java.io.IOException
import kotlin.concurrent.thread

class UpdatingTask(val threadName: String, val cleaning: () -> Unit) {

    var runningThread: Thread? = null

    fun compute(computation: () -> Unit) {
        runningThread?.interrupt()
        Sleep.waitUntil(true) { runningThread == null }
        runningThread = thread(start = false, name = threadName) {
            try {
                computation()
            } catch (e: InterruptedException) {
                // kill successful
            } catch (e: IOException) {
                // e.g. IOException, because a channel was closed while waiting for the stream
                LOGGER.warn("Catched ${e.message}")
            } catch (e: Exception) {
                // e.g. IOException, because a channel was closed while waiting for the stream
                LOGGER.warn("Catched ${e.message}")
            }
            runningThread = null
        }
        cleaning()
        runningThread!!.start()
    }

    fun destroy() {
        runningThread?.interrupt()
        runningThread = null
    }

}