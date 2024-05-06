package me.anno.utils.types

import me.anno.utils.types.AnyToDouble.getDouble

object AnyToFloat {

    @JvmStatic
    fun getFloat(any: Any?, defaultValue: Float): Float {
        return getFloat(any, 0, defaultValue)
    }

    @JvmStatic
    fun getFloat(any: Any?, index: Int, defaultValue: Float): Float {
        return getDouble(any, index, defaultValue.toDouble()).toFloat()
    }
}