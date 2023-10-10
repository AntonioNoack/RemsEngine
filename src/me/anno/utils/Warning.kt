package me.anno.utils

import org.apache.logging.log4j.LogManager

object Warning {

    @JvmStatic
    private val LOGGER = LogManager.getLogger(Warning::class)

    @JvmStatic
    private val warned = HashSet<String>()

    /**
     * warns once
     * */
    @JvmStatic
    fun warn(key: String) {
        if (key in warned) return
        warned += key
        LOGGER.warn(key)
    }

    /**
     * gets rid of the "unused variable" warning
     * */
    @JvmStatic
    fun unused(x: Any?): Any? {
        return x
    }

}