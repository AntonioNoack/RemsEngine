package me.anno.utils.types

import me.anno.utils.types.Ints.toLongOrDefault
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4f
import org.joml.Vector4i

object AnyToLong {

    @JvmStatic
    fun getLong(any: Any?, defaultValue: Long): Long {
        return any[0, defaultValue]
    }

    @JvmStatic
    fun getLong(any: Any?, index: Int, defaultValue: Long): Long {
        return any[index, defaultValue]
    }

    @JvmStatic
    operator fun Any?.get(index: Int, defaultValue: Long): Long {
        return when (this) {
            null -> defaultValue
            is Byte -> when (index) {
                0 -> this.toLong()
                else -> defaultValue
            }
            is UByte -> when (index) {
                0 -> this.toLong()
                else -> defaultValue
            }
            is Short -> when (index) {
                0 -> this.toLong()
                else -> defaultValue
            }
            is UShort -> when (index) {
                0 -> this.toLong()
                else -> defaultValue
            }
            is Int -> when (index) {
                0 -> this.toLong()
                else -> defaultValue
            }
            is UInt -> when (index) {
                0 -> this.toLong()
                else -> defaultValue
            }
            is Long -> when (index) {
                0 -> this
                else -> defaultValue
            }
            is ULong -> when (index) {
                0 -> this.toLong()
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
            is CharSequence -> toLongOrDefault(defaultValue)
            else -> defaultValue
        }
    }
}