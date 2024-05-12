package me.anno.utils.types

import me.anno.utils.types.Ints.toLongOrDefault

object AnyToLong {

    @JvmStatic
    fun getLong(any: Any?, defaultValue: Long = 0): Long {
        return getLong(any, 0, defaultValue)
    }

    @JvmStatic
    fun getLong(value: Any?, index: Int, defaultValue: Long): Long {
        return when (value) {
            null -> defaultValue
            is Long -> when (index) {
                0 -> value
                else -> defaultValue
            }
            is ULong -> when (index) {
                0 -> value.toLong()
                else -> defaultValue
            }
            is CharSequence -> value.toLongOrDefault(defaultValue)
            else -> {
                val v = AnyToDouble.getDouble(value, index, Double.NaN)
                return if (v.isNaN()) defaultValue else v.toLong()
            }
        }
    }
}