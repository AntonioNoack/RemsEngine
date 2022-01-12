package me.anno.utils.hpc

import me.anno.Engine.shutdown
import me.anno.utils.ShutdownException
import me.anno.utils.Sleep.sleepShortly
import org.apache.logging.log4j.LogManager
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

open class ProcessingQueue(val name: String){

    private val tasks = LinkedBlockingQueue<() -> Unit>()

    private var hasBeenStarted = false
    private var shouldStop = false

    fun stop(){
        shouldStop = true
        hasBeenStarted = false
    }

    open fun start(name: String = this.name, force: Boolean = false) {
        if(hasBeenStarted && !force) return
        hasBeenStarted = true
        shouldStop = false
        LOGGER.info("Starting queue $name")
        thread(name = name) {
            while (!shutdown && !shouldStop) {
                try {
                    // will block, until we have new work
                    val task = tasks.poll() ?: null
                    if(task == null){
                        sleepShortly(true)
                    } else {
                        task()
                    }
                } catch (e: ShutdownException){
                    // nothing to worry about (probably)
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            LOGGER.info("Finished $name")
        }
    }

    operator fun plusAssign(task: () -> Unit) {
        if(!hasBeenStarted) start()
        tasks += task
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ProcessingQueue::class)
    }

}