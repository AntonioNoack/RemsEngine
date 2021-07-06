package me.anno.utils

import me.anno.io.ISaveable
import org.apache.logging.log4j.LogManager

object WrongClassType {
    private val LOGGER = LogManager.getLogger(WrongClassType::class)
    fun warn(type: String, value: ISaveable?){
        if(value != null) LOGGER.warn("Got $type, that isn't one: ${value.className}")
    }
}