package org.joml

abstract class Vector {
    abstract fun getComp(i: Int): Double
    abstract fun setComp(i: Int, v: Double)
    abstract val numComponents: Int

    fun getCompOr(i: Int, defaultValue: Double = 0.0): Double {
        return if (i in 0 until numComponents) getComp(i) else defaultValue
    }
}