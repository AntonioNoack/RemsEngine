package com.bulletphysics.util

object Packing {
    @JvmStatic
    fun pack(high: Int, low: Int): Long {
        return (high.toLong() shl 32) or ((low.toLong()) and 0xffffffffL)
    }

    @JvmStatic
    fun unpackHigh(value: Long): Int {
        return (value ushr 32).toInt()
    }

    @JvmStatic
    fun unpackLow(value: Long): Int {
        return value.toInt()
    }
}
