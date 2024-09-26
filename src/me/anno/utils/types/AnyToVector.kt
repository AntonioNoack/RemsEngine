package me.anno.utils.types

import me.anno.utils.Color
import org.joml.Vector
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f

object AnyToVector {

    private val black4d = Vector4d()
    private val black3d = Vector3d()
    private val black2d = Vector2d()
    private val white4d = Vector4d(1.0)
    private val white3d = Vector3d(1.0)
    private val white2d = Vector2d(1.0)

    private val black4f get() = Color.black4
    private val black3f get() = Color.black3
    private val black2f = Vector2f()
    private val white4f get() = Color.white4
    private val white3f get() = Color.white3
    private val white2f = Vector2f(1f)

    @JvmStatic
    fun getVector2f(any: Any?, defaultValue: Vector2f = black2f): Vector2f {
        return when (any) {
            is Vector2f -> any
            true -> white2f
            false -> black2f
            is Number -> Vector2f(any.toFloat())
            is Vector -> Vector2f(
                any.getCompOr(0, defaultValue.x.toDouble()).toFloat(),
                any.getCompOr(1, defaultValue.y.toDouble()).toFloat()
            )
            else -> defaultValue
        }
    }

    @JvmStatic
    fun getVector2d(any: Any?, defaultValue: Vector2d = black2d): Vector2d {
        return when (any) {
            is Vector2d -> any
            true -> white2d
            false -> black2d
            is Number -> Vector2d(any.toDouble())
            is Vector -> Vector2d(
                any.getCompOr(0, defaultValue.x),
                any.getCompOr(1, defaultValue.y)
            )
            else -> defaultValue
        }
    }

    @JvmStatic
    fun getVector3f(any: Any?, defaultValue: Vector3f = black3f): Vector3f {
        return when (any) {
            is Vector3f -> any
            true -> white3f
            false -> black3f
            is Number -> Vector3f(any.toFloat())
            is Vector -> Vector3f(
                any.getCompOr(0, defaultValue.x.toDouble()).toFloat(),
                any.getCompOr(1, defaultValue.y.toDouble()).toFloat(),
                any.getCompOr(2, defaultValue.z.toDouble()).toFloat()
            )
            else -> defaultValue
        }
    }

    @JvmStatic
    fun getVector3d(any: Any?, defaultValue: Vector3d = black3d): Vector3d {
        return when (any) {
            is Vector3d -> any
            true -> white3d
            false -> black3d
            is Number -> Vector3d(any.toDouble())
            is Vector -> Vector3d(
                any.getCompOr(0, defaultValue.x),
                any.getCompOr(1, defaultValue.y),
                any.getCompOr(2, defaultValue.z)
            )
            else -> defaultValue
        }
    }

    @JvmStatic
    fun getVector4f(any: Any?, defaultValue: Vector4f = black4f): Vector4f {
        return when (any) {
            is Vector4f -> any
            true -> white4f
            false -> black4f
            is Number -> Vector4f(any.toFloat())
            is Vector -> Vector4f(
                any.getCompOr(0, defaultValue.x.toDouble()).toFloat(),
                any.getCompOr(1, defaultValue.y.toDouble()).toFloat(),
                any.getCompOr(2, defaultValue.z.toDouble()).toFloat(),
                any.getCompOr(3, defaultValue.w.toDouble()).toFloat()
            )
            else -> defaultValue
        }
    }

    @JvmStatic
    fun getVector4d(any: Any?, defaultValue: Vector4d = black4d): Vector4d {
        return when (any) {
            is Vector4d -> any
            true -> white4d
            false -> black4d
            is Number -> Vector4d(any.toDouble())
            is Vector -> Vector4d(
                any.getCompOr(0, defaultValue.x),
                any.getCompOr(1, defaultValue.y),
                any.getCompOr(2, defaultValue.z),
                any.getCompOr(3, defaultValue.w)
            )
            else -> defaultValue
        }
    }
}