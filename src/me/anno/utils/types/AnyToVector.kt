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

    private val black4 = Vector4d(0.0)
    private val black3 = Vector3d(0.0)
    private val black2 = Vector2d(0.0)
    private val white4 = Vector4d(1.0)
    private val white3 = Vector3d(1.0)
    private val white2 = Vector2d(1.0)

    @JvmStatic
    fun getVector2f(any: Any?, defaultValue: Vector2f = Color.black2): Vector2f {
        return when (any) {
            is Vector2f -> any
            else -> Vector2f(getVector2d(any, Vector2d(defaultValue)))
        }
    }

    @JvmStatic
    fun getVector2d(any: Any?, defaultValue: Vector2d = black2): Vector2d {
        return when (any) {
            is Vector2d -> any
            true -> white2
            false -> black2
            is Number -> Vector2d(any.toDouble())
            is Vector -> Vector2d(
                any.getCompOr(0, defaultValue.x),
                any.getCompOr(1, defaultValue.y)
            )
            else -> defaultValue
        }
    }

    @JvmStatic
    fun getVector3f(any: Any?, defaultValue: Vector3f = Color.black3): Vector3f {
        return when (any) {
            is Vector3f -> any
            else -> Vector3f(getVector3d(any, Vector3d(defaultValue)))
        }
    }

    @JvmStatic
    fun getVector3d(any: Any?, defaultValue: Vector3d = black3): Vector3d {
        return when (any) {
            is Vector3d -> any
            true -> white3
            false -> black3
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
    fun getVector4f(any: Any?, defaultValue: Vector4f = Color.black4): Vector4f {
        return when (any) {
            is Vector4f -> any
            else -> Vector4f(getVector4d(any, Vector4d(defaultValue)))
        }
    }

    @JvmStatic
    fun getVector4d(any: Any?, defaultValue: Vector4d = black4): Vector4d {
        return when (any) {
            is Vector4d -> any
            true -> white4
            false -> black4
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