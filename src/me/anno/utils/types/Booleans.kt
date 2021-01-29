package me.anno.utils.types

object Booleans {
    fun Boolean.toInt() = if(this) 1 else 0
    fun Boolean.toInt(n: Int) = if(this) n else 0
}