package me.anno.utils.hpc

import me.anno.utils.Sleep.sleepShortly
import org.apache.logging.log4j.LogManager
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

open class ProcessingQueue(val name: String){

    private val tasks = LinkedBlockingQueue<() -> Unit>()

    private var hasBeenStarted = false
    open fun start(name: String = this.name, force: Boolean = false) {
        if(hasBeenStarted && !force) return
        hasBeenStarted = true
        thread(name = name) {
            while (!shallShutDown) {
                try {
                    // will block, until we have new work
                    val task = tasks.poll() ?: null
                    if(task == null){
                        sleepShortly()
                    } else {
                        task()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    operator fun plusAssign(task: () -> Unit) {
        tasks += task
    }

    companion object {
        private val LOGGER = LogManager.getLogger(ProcessingQueue::class)
        var shallShutDown = false
        fun destroy(){
            LOGGER.info("Shutting down")
            shallShutDown = true
        }
    }

}