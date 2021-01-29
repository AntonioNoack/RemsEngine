package me.anno.utils.types

import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

object AnyToDouble {

    fun getDouble(any: Any, index: Int, defaultValue: Double = 0.0): Double {
        return any[index, defaultValue]
    }

    operator fun Any.get(index: Int, defaultValue: Double = 0.0): Double {
        return when (this) {
            is Int -> when (index) {
                0 -> this.toDouble()
                else -> defaultValue
            }
            is Long -> when (index) {
                0 -> this.toDouble()
                else -> defaultValue
            }
            is Float -> when (index) {
                0 -> this.toDouble()
                else -> defaultValue
            }
            is Double -> when (index) {
                0 -> this
                else -> defaultValue
            }
            is Vector2f -> when (index) {
                0 -> x.toDouble()
                1 -> y.toDouble()
                else -> defaultValue
            }
            is Vector3f -> when (index) {
                0 -> x.toDouble()
                1 -> y.toDouble()
                2 -> z.toDouble()
                else -> defaultValue
            }
            is Vector4f -> when (index) {
                0 -> x.toDouble()
                1 -> y.toDouble()
                2 -> z.toDouble()
                3 -> w.toDouble()
                else -> defaultValue
            }
            is Quaternionf -> when (index) {
                0 -> x.toDouble()
                1 -> y.toDouble()
                2 -> z.toDouble()
                3 -> w.toDouble()
                else -> defaultValue
            }
            else -> defaultValue
        }
    }
}