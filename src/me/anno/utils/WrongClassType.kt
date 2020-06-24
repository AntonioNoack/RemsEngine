package me.anno.utils

import me.anno.io.ISaveable
import java.lang.RuntimeException

object WrongClassType {
    fun warn(type: String, value: ISaveable?){
        if(value != null) println("Got $type, that isn't one: ${value?.getClassName()}")
    }
}