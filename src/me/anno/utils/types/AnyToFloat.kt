package me.anno.utils.types

import org.joml.*

object AnyToFloat {

    fun getFloat(any: Any, index: Int, defaultValue: Float = 0f): Float {
        return any[index, defaultValue]
    }

    operator fun Any.get(index: Int, defaultValue: Float = 0f): Float {
        return when (this) {
            is Int -> when (index) {
                0 -> this.toFloat()
                else -> defaultValue
            }
            is Long -> when (index) {
                0 -> this.toFloat()
                else -> defaultValue
            }
            is Float -> when (index) {
                0 -> this
                else -> defaultValue
            }
            is Double -> when (index) {
                0 -> this.toFloat()
                else -> defaultValue
            }
            is Vector2fc -> when (index) {
                0 -> x()
                1 -> y()
                else -> defaultValue
            }
            is Vector3fc -> when (index) {
                0 -> x()
                1 -> y()
                2 -> z()
                else -> defaultValue
            }
            is Vector4fc -> when (index) {
                0 -> x()
                1 -> y()
                2 -> z()
                3 -> w()
                else -> defaultValue
            }
            is Quaternionfc -> when (index) {
                0 -> x()
                1 -> y()
                2 -> z()
                3 -> w()
                else -> defaultValue
            }
            is Vector2dc -> when (index) {
                0 -> x().toFloat()
                1 -> y().toFloat()
                else -> defaultValue
            }
            is Vector3dc -> when (index) {
                0 -> x().toFloat()
                1 -> y().toFloat()
                2 -> z().toFloat()
                else -> defaultValue
            }
            is Vector4dc -> when (index) {
                0 -> x().toFloat()
                1 -> y().toFloat()
                2 -> z().toFloat()
                3 -> w().toFloat()
                else -> defaultValue
            }
            is Quaterniondc -> when (index) {
                0 -> x().toFloat()
                1 -> y().toFloat()
                2 -> z().toFloat()
                3 -> w().toFloat()
                else -> defaultValue
            }
            is Vector2ic -> when (index) {
                0 -> x().toFloat()
                1 -> y().toFloat()
                else -> defaultValue
            }
            is Vector3ic -> when (index) {
                0 -> x().toFloat()
                1 -> y().toFloat()
                2 -> z().toFloat()
                else -> defaultValue
            }
            is Vector4ic -> when (index) {
                0 -> x().toFloat()
                1 -> y().toFloat()
                2 -> z().toFloat()
                3 -> w().toFloat()
                else -> defaultValue
            }
            else -> defaultValue
        }
    }
}