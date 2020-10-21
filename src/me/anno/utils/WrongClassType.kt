package me.anno.utils

import me.anno.io.ISaveable

object WrongClassType {
    fun warn(type: String, value: ISaveable?){
        if(value != null) LOGGER.warn("Got $type, that isn't one: ${value.getClassName()}")
    }
}