package me.anno.utils

import org.apache.logging.log4j.LogManager

object Warning {
    private val LOGGER = LogManager.getLogger(Warning::class)
    private val warned = HashSet<String>()
    fun warn(key: String) {
        if (key in warned) return
        warned += key
        LOGGER.warn(key)
    }

    fun unused(x: Any?): Any? {
        return x
    }

}