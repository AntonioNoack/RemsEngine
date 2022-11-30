package me.anno.utils

import org.apache.logging.log4j.LogManager

object Warning {

    @JvmStatic
    private val LOGGER = LogManager.getLogger(Warning::class)

    @JvmStatic
    private val warned = HashSet<String>()

    @JvmStatic
    fun warn(key: String) {
        if (key in warned) return
        warned += key
        LOGGER.warn(key)
    }

    @JvmStatic
    fun unused(x: Any?): Any? {
        return x
    }

}