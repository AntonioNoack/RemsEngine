package me.anno.utils.types

import org.joml.*

object AnyToLong {

    fun getLong(any: Any?, index: Int, defaultValue: Long = 0): Long {
        return any[index, defaultValue]
    }

    operator fun Any?.get(index: Int, defaultValue: Long = 0): Long {
        return when (this) {
            null -> defaultValue
            is Int -> when (index) {
                0 -> this.toLong()
                else -> defaultValue
            }
            is Long -> when (index) {
                0 -> this
                else -> defaultValue
            }
            is Float -> when (index) {
                0 -> this.toLong()
                else -> defaultValue
            }
            is Double -> when (index) {
                0 -> this.toLong()
                else -> defaultValue
            }
            is Vector2i -> when (index) {
                0 -> x.toLong()
                1 -> y.toLong()
                else -> defaultValue
            }
            is Vector3i -> when (index) {
                0 -> x.toLong()
                1 -> y.toLong()
                2 -> z.toLong()
                else -> defaultValue
            }
            is Vector4i -> when (index) {
                0 -> x.toLong()
                1 -> y.toLong()
                2 -> z.toLong()
                3 -> w.toLong()
                else -> defaultValue
            }
            is Vector2f -> when (index) {
                0 -> x.toLong()
                1 -> y.toLong()
                else -> defaultValue
            }
            is Vector3f -> when (index) {
                0 -> x.toLong()
                1 -> y.toLong()
                2 -> z.toLong()
                else -> defaultValue
            }
            is Vector4f -> when (index) {
                0 -> x.toLong()
                1 -> y.toLong()
                2 -> z.toLong()
                3 -> w.toLong()
                else -> defaultValue
            }
            else -> defaultValue
        }
    }
}