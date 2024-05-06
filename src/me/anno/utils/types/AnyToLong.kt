package me.anno.utils.types

import me.anno.utils.types.Ints.toLongOrDefault

object AnyToLong {

    @JvmStatic
    fun getLong(any: Any?, defaultValue: Long): Long {
        return getLong(any, 0, defaultValue)
    }

    @JvmStatic
    fun getLong(any: Any?, index: Int, defaultValue: Long): Long {
        return when (any) {
            null -> defaultValue
            is Long -> when (index) {
                0 -> any
                else -> defaultValue
            }
            is ULong -> when (index) {
                0 -> any.toLong()
                else -> defaultValue
            }
            is CharSequence -> any.toLongOrDefault(defaultValue)
            else -> {
                val v = AnyToDouble.getDouble(index, Double.NaN)
                return if (v.isNaN()) defaultValue else v.toLong()
            }
        }
    }
}