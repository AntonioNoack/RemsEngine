package me.anno.utils.types

import me.anno.utils.types.AnyToLong.getLong

object AnyToInt {

    @JvmStatic
    fun getInt(any: Any?, defaultValue: Int): Int {
        return getInt(any, 0, defaultValue)
    }

    @JvmStatic
    fun getInt(any: Any?, index: Int, defaultValue: Int): Int {
        return getLong(any, index, defaultValue.toLong()).toInt()
    }
}