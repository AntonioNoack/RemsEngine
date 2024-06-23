package me.anno.utils.types

import me.anno.io.files.InvalidRef
import me.anno.io.saveable.Saveable
import org.apache.logging.log4j.LogManager
import org.joml.Matrix2d
import org.joml.Matrix2f
import org.joml.Matrix3d
import org.joml.Matrix3f
import org.joml.Matrix3x2d
import org.joml.Matrix3x2f
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Matrix4x3d
import org.joml.Matrix4x3f
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

object Defaults {
    private val LOGGER = LogManager.getLogger(Defaults::class)
    fun getDefaultValue(type: String): Any? {
        if (type.endsWith("?")) return null
        return when (type) {
            "Byte" -> 0.toByte()
            "Short" -> 0.toShort()
            "Char" -> ' '
            "Int", "Integer" -> 0
            "Long" -> 0L
            "Float" -> 0f
            "Double" -> 0.0
            "String" -> ""
            "Vector2f" -> Vector2f()
            "Vector3f", "Color3", "Color3HDR" -> Vector3f()
            "Vector4f", "Color4", "Color4HDR" -> Vector4f()
            "Vector2d" -> Vector2d()
            "Vector3d" -> Vector3d()
            "Vector4d" -> Vector4d()
            "Vector2i" -> Vector2i()
            "Vector3i" -> Vector3i()
            "Vector4i" -> Vector4i()
            "Quaternionf" -> Quaternionf()
            "Quaterniond" -> Quaterniond()
            "Matrix2f" -> Matrix2f()
            "Matrix2d" -> Matrix2d()
            "Matrix3x2f" -> Matrix3x2f()
            "Matrix3x2d" -> Matrix3x2d()
            "Matrix3f" -> Matrix3f()
            "Matrix3d" -> Matrix3d()
            "Matrix4x3f" -> Matrix4x3f()
            "Matrix4x3d" -> Matrix4x3d()
            "Matrix4f" -> Matrix4f()
            "Matrix4d" -> Matrix4d()
            "Planef" -> Planef()
            "Planed" -> Planed()
            "File", "FileReference", "Reference" -> InvalidRef
            else -> {
                if (type.endsWith("/FileReference") || type.endsWith("/Reference")) {
                    return InvalidRef
                }
                val newInstance = Saveable.createOrNull(type)
                if (newInstance == null) {
                    LOGGER.warn("Unknown type $type")
                }
                newInstance
            }
        }
    }
}