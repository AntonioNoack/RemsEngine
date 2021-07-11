package me.anno.utils.types

import org.joml.*

object AnyToInt {

    fun getInt(any: Any, index: Int, defaultValue: Int = 0): Int {
        return any[index, defaultValue]
    }

    operator fun Any.get(index: Int, defaultValue: Int = 0): Int {
        return when (this) {
            is Int -> when (index) {
                0 -> this.toInt()
                else -> defaultValue
            }
            is Long -> when (index) {
                0 -> this.toInt()
                else -> defaultValue
            }
            is Float -> when (index) {
                0 -> this.toInt()
                else -> defaultValue
            }
            is Double -> when (index) {
                0 -> this.toInt()
                else -> defaultValue
            }
            is Vector2i -> when (index) {
                0 -> x
                1 -> y
                else -> defaultValue
            }
            is Vector3i -> when (index) {
                0 -> x
                1 -> y
                2 -> z
                else -> defaultValue
            }
            is Vector4i -> when (index) {
                0 -> x
                1 -> y
                2 -> z
                3 -> w
                else -> defaultValue
            }
            is Vector2f -> when (index) {
                0 -> x.toInt()
                1 -> y.toInt()
                else -> defaultValue
            }
            is Vector3f -> when (index) {
                0 -> x.toInt()
                1 -> y.toInt()
                2 -> z.toInt()
                else -> defaultValue
            }
            is Vector4f -> when (index) {
                0 -> x.toInt()
                1 -> y.toInt()
                2 -> z.toInt()
                3 -> w.toInt()
                else -> defaultValue
            }
            else -> defaultValue
        }
    }
}