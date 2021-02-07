package me.anno.utils

import org.apache.logging.log4j.LogManager

class Clock {

    var lastTime = System.nanoTime()

    fun start() {
        lastTime = System.nanoTime()
    }

    fun stop(name: String) {
        val time = System.nanoTime()
        val dt = (time-lastTime) / 1e9
        lastTime = time
        LOGGER.info("$name: $dt")
    }

    companion object {
        private val LOGGER = LogManager.getLogger(Clock::class)
    }
}