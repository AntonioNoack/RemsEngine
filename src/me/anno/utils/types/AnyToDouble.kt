package me.anno.utils.types

import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Planed
import org.joml.Planef
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4d
import org.joml.Vector4f
import org.joml.Vector4i

object AnyToDouble {

    @JvmStatic
    private val LOGGER = LogManager.getLogger(AnyToDouble::class)

    @JvmStatic
    fun getDouble(any: Any?, index: Int, defaultValue: Double): Double {
        return any.get(index, defaultValue)
    }

    @JvmStatic
    fun getDouble(any: Any?, defaultValue: Double): Double {
        return any.get(0, defaultValue)
    }

    @JvmStatic
    private fun Any?.get(index: Int, defaultValue: Double): Double {
        return when (this) {
            null -> defaultValue
            is Boolean -> when (index) {
                0 -> if (this) 1.0 else 0.0
                else -> defaultValue
            }
            is Byte -> when (index) {
                0 -> this.toDouble()
                else -> defaultValue
            }
            is UByte -> when (index) {
                0 -> this.toDouble()
                else -> defaultValue
            }
            is Short -> when (index) {
                0 -> this.toDouble()
                else -> defaultValue
            }
            is UShort -> when (index) {
                0 -> this.toDouble()
                else -> defaultValue
            }
            is Int -> when (index) {
                0 -> this.toDouble()
                else -> defaultValue
            }
            is UInt -> when (index) {
                0 -> this.toDouble()
                else -> defaultValue
            }
            is Long -> when (index) {
                0 -> this.toDouble()
                else -> defaultValue
            }
            is ULong -> when (index) {
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
            is Vector2f -> if (index in 0 until 2) get(index).toDouble() else defaultValue
            is Vector3f -> if (index in 0 until 3) get(index).toDouble() else defaultValue
            is Vector4f -> if (index in 0 until 4) get(index).toDouble() else defaultValue
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
            is Vector2d -> if (index in 0 until 2) get(index) else defaultValue
            is Vector3d -> if (index in 0 until 3) get(index) else defaultValue
            is Vector4d -> if (index in 0 until 4) get(index) else defaultValue
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
            is Vector2i -> if (index in 0 until 2) get(index).toDouble() else defaultValue
            is Vector3i -> if (index in 0 until 3) get(index).toDouble() else defaultValue
            is Vector4i -> if (index in 0 until 4) get(index).toDouble() else defaultValue
            is AABBf -> when (index) {
                0 -> minX.toDouble()
                1 -> minY.toDouble()
                2 -> minZ.toDouble()
                3 -> maxX.toDouble()
                4 -> maxY.toDouble()
                5 -> maxZ.toDouble()
                else -> defaultValue
            }
            is AABBd -> when (index) {
                0 -> minX
                1 -> minY
                2 -> minZ
                3 -> maxX
                4 -> maxY
                5 -> maxZ
                else -> defaultValue
            }
            is Enum<*> -> ordinal.toDouble()
            is CharSequence -> toString()
                .replace(',', '.')
                .toDoubleOrNull() ?: defaultValue
            else -> {
                LOGGER.info("Unknown ${this::class.simpleName}[$index] to Double")
                defaultValue
            }
        }
    }
}