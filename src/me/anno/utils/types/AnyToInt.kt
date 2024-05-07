package me.anno.utils.types

import me.anno.utils.types.AnyToLong.getLong

object AnyToInt {

    @JvmStatic
    fun getInt(value: Any?, defaultValue: Int): Int {
        return getInt(value, 0, defaultValue)
    }

    @JvmStatic
    fun getInt(value: Any?, index: Int, defaultValue: Int): Int {
        return getLong(value, index, defaultValue.toLong()).toInt()
    }
}