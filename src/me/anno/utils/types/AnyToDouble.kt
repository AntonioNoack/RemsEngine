package me.anno.utils.types

import org.apache.logging.log4j.LogManager
import org.joml.AABBd
import org.joml.AABBf
import org.joml.Planed
import org.joml.Planef
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector

object AnyToDouble {

    @JvmStatic
    private val LOGGER = LogManager.getLogger(AnyToDouble::class)

    @JvmStatic
    fun getDouble(any: Any?, defaultValue: Double = 0.0): Double {
        return any.get(0, defaultValue)
    }

    @JvmStatic
    fun getDouble(any: Any?, index: Int, defaultValue: Double): Double {
        return any.get(index, defaultValue)
    }

    @JvmStatic
    private fun Any?.get(index: Int, defaultValue: Double): Double {
        return when (this) {
            null -> defaultValue
            is Number -> when (index) {
                0 -> toDouble()
                else -> defaultValue
            }
            is Boolean -> when (index) {
                0 -> if (this) 1.0 else 0.0
                else -> defaultValue
            }
            is Vector -> getCompOr(index, defaultValue)
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