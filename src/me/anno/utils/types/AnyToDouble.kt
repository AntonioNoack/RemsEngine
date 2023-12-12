package me.anno.utils.types

import org.apache.logging.log4j.LogManager
import org.joml.*

object AnyToDouble {

    private val LOGGER = LogManager.getLogger(AnyToDouble::class)

    fun getDouble(any: Any?, index: Int, defaultValue: Double): Double {
        return any[index, defaultValue]
    }

    fun getDouble(any: Any?, defaultValue: Double): Double {
        return any[0, defaultValue]
    }

    operator fun Any?.get(index: Int, defaultValue: Double): Double {
        return when (this) {
            null -> defaultValue
            is Byte -> when (index) {
                0 -> this.toDouble()
                else -> defaultValue
            }
            is Short -> when (index) {
                0 -> this.toDouble()
                else -> defaultValue
            }
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
            is Planef -> when (index) {
                0 -> dirX.toDouble()
                1 -> dirY.toDouble()
                2 -> dirZ.toDouble()
                3 -> distance.toDouble()
                else -> defaultValue
            }
            is Quaternionf -> when (index) {
                0 -> x.toDouble()
                1 -> y.toDouble()
                2 -> z.toDouble()
                3 -> w.toDouble()
                else -> defaultValue
            }
            is Vector2d -> when (index) {
                0 -> x
                1 -> y
                else -> defaultValue
            }
            is Vector3d -> when (index) {
                0 -> x
                1 -> y
                2 -> z
                else -> defaultValue
            }
            is Vector4d -> when (index) {
                0 -> x
                1 -> y
                2 -> z
                3 -> w
                else -> defaultValue
            }
            is Planed -> when (index) {
                0 -> dirX
                1 -> dirY
                2 -> dirZ
                3 -> distance
                else -> defaultValue
            }
            is Quaterniond -> when (index) {
                0 -> x
                1 -> y
                2 -> z
                3 -> w
                else -> defaultValue
            }
            is Vector2i -> when (index) {
                0 -> x.toDouble()
                1 -> y.toDouble()
                else -> defaultValue
            }
            is Vector3i -> when (index) {
                0 -> x.toDouble()
                1 -> y.toDouble()
                2 -> z.toDouble()
                else -> defaultValue
            }
            is Vector4i -> when (index) {
                0 -> x.toDouble()
                1 -> y.toDouble()
                2 -> z.toDouble()
                3 -> w.toDouble()
                else -> defaultValue
            }
            is CharSequence -> toString().toDoubleOrNull() ?: defaultValue
            else -> {
                LOGGER.info("Unknown ${this::class.simpleName}[$index] to Double")
                defaultValue
            }
        }
    }
}