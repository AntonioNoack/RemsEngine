package me.anno.utils

import me.anno.utils.types.Floats.f3
import org.apache.logging.log4j.LogManager

class Clock(private val printWholeAccuracy: Boolean = false){

    val firstTime = System.nanoTime()
    var lastTime = firstTime

    val timeSinceStart get() = (System.nanoTime() - firstTime)*1e-9

    fun start() {
        lastTime = System.nanoTime()
    }

    fun stop(wasUsedFor: String) {
        val time = System.nanoTime()
        val dt = (time-lastTime) * 1e-9
        lastTime = time
        LOGGER.info("Used ${if(printWholeAccuracy) dt.toString() else dt.f3()}s for $wasUsedFor")
    }

    fun total(wasUsedFor: String) {
        val time = System.nanoTime()
        val dt = (time-firstTime) * 1e-9
        lastTime = time
        LOGGER.info("Used ${if(printWholeAccuracy) dt.toString() else dt.f3()}s in total for $wasUsedFor")
    }

    companion object {
        private val LOGGER = LogManager.getLogger(Clock::class)
    }
}