package me.anno.utils.types

import me.anno.utils.LOGGER
import org.joml.*

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
            is Vector2fc -> when (index) {
                0 -> x().toDouble()
                1 -> y().toDouble()
                else -> defaultValue
            }
            is Vector3fc -> when (index) {
                0 -> x().toDouble()
                1 -> y().toDouble()
                2 -> z().toDouble()
                else -> defaultValue
            }
            is Vector4fc -> when (index) {
                0 -> x().toDouble()
                1 -> y().toDouble()
                2 -> z().toDouble()
                3 -> w().toDouble()
                else -> defaultValue
            }
            is Quaternionfc -> when (index) {
                0 -> x().toDouble()
                1 -> y().toDouble()
                2 -> z().toDouble()
                3 -> w().toDouble()
                else -> defaultValue
            }
            is Vector2dc -> when (index) {
                0 -> x()
                1 -> y()
                else -> defaultValue
            }
            is Vector3dc -> when (index) {
                0 -> x()
                1 -> y()
                2 -> z()
                else -> defaultValue
            }
            is Vector4dc -> when (index) {
                0 -> x()
                1 -> y()
                2 -> z()
                3 -> w()
                else -> defaultValue
            }
            is Quaterniondc -> when (index) {
                0 -> x()
                1 -> y()
                2 -> z()
                3 -> w()
                else -> defaultValue
            }
            is Vector2ic -> when (index) {
                0 -> x().toDouble()
                1 -> y().toDouble()
                else -> defaultValue
            }
            is Vector3ic -> when (index) {
                0 -> x().toDouble()
                1 -> y().toDouble()
                2 -> z().toDouble()
                else -> defaultValue
            }
            is Vector4ic -> when (index) {
                0 -> x().toDouble()
                1 -> y().toDouble()
                2 -> z().toDouble()
                3 -> w().toDouble()
                else -> defaultValue
            }
            else -> {
                LOGGER.info("Unknown ${javaClass.simpleName}[$index] to Double")
                defaultValue
            }
        }
    }
}