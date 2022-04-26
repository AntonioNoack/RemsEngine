package me.anno.utils.hpc

import me.anno.Engine
import org.apache.logging.log4j.LogManager
import kotlin.concurrent.thread
import kotlin.math.min

/**
 * conditional locks
 * run each thread in a loop
 *
 * probably no longer needed...
 * */
class SyncMaster {

    private var sessionId = 0

    fun nextSession() {
        sessionId++
        LOGGER.debug("Changed session to $sessionId")
    }

    /**
     * add a thread to run until the engine stops
     * */
    fun addThread(name: String, run: () -> Long, getLockObject: () -> Any?) {
        val session = sessionId
        thread(name = name) {
            while (!Engine.shutdown && sessionId == session) {
                // ("thread $name is running")
                val sleepNanos = executeInternally(run, getLockObject)
                if (sleepNanos < 0) break // done
                if (sleepNanos > 1000) {
                    val millisInNano = 1_000_000
                    Thread.sleep(min(sleepNanos / millisInNano, 100), (sleepNanos % millisInNano).toInt())
                }
            }
            LOGGER.info("Thread $name finished")
        }
    }

    /**
     * execute without starting a new thread
     * */
    fun execute(run: () -> Unit, getLockObject: () -> Any?) {
        val lock = getLockObject()
        return if (lock != null) {
            synchronized(lock) {
                run()
            }
        } else run()
    }

    /**
     * execute without starting a new thread
     * */
    fun executeInternally(run: () -> Long, getLockObject: () -> Any?): Long {
        val lock = getLockObject()
        return if (lock != null) {
            synchronized(lock) {
                run()
            }
        } else run()
    }

    companion object {
        private val LOGGER = LogManager.getLogger(SyncMaster::class)
    }

}