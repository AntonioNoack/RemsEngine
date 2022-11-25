package me.anno.utils.types

object Booleans {
    @JvmStatic
    fun Boolean.toInt() = if(this) 1 else 0
    @JvmStatic
    fun Boolean.toInt(n: Int) = if(this) n else 0
}