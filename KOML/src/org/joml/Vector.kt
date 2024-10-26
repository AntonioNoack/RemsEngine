package org.joml

interface Vector {
    fun getComp(i: Int): Double
    fun setComp(i: Int, v: Double)
    val numComponents: Int

    fun getCompOr(i: Int, defaultValue: Double = 0.0): Double {
        return if (i in 0 until numComponents) getComp(i) else defaultValue
    }
}