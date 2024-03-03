package me.anno.utils.types

import me.anno.utils.types.AnyToLong.getLong

object AnyToInt {

    @JvmStatic
    fun getInt(any: Any?, defaultValue: Int): Int {
        return any[0, defaultValue]
    }

    @JvmStatic
    fun getInt(any: Any?, index: Int, defaultValue: Int): Int {
        return any[index, defaultValue]
    }

    @JvmStatic
    operator fun Any?.get(index: Int, defaultValue: Int = 0): Int {
        return getLong(this, index, defaultValue.toLong()).toInt()
    }
}