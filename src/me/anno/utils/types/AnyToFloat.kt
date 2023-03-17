package me.anno.utils.types

import me.anno.utils.types.AnyToDouble.getDouble

object AnyToFloat {

    fun getFloat(any: Any?, defaultValue: Float): Float {
        return any[0, defaultValue]
    }

    fun getFloat(any: Any?, index: Int, defaultValue: Float): Float {
        return any[index, defaultValue]
    }

    operator fun Any?.get(index: Int, defaultValue: Float): Float {
        return getDouble(this, index, defaultValue.toDouble()).toFloat()
    }
}