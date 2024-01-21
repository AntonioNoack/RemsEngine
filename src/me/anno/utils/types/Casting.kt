package me.anno.utils.types

import me.anno.utils.types.AnyToDouble.getDouble
import me.anno.utils.types.AnyToFloat.getFloat
import me.anno.utils.types.AnyToInt.getInt
import me.anno.utils.types.AnyToLong.getLong
import org.joml.Planed
import org.joml.Planef
import org.joml.Quaternionf
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f

object Casting {

    fun castToInt2(it: Any?) = getInt(it, 0)
    fun castToInt(it: Any): Int? = when (it) {
        is Int -> it
        is Long -> it.toInt()
        is Float -> it.toInt()
        is Double -> it.toInt()
        is Vector2f -> it.x.toInt()
        is Vector3f -> it.x.toInt()
        is Vector4f -> it.x.toInt()
        is Vector4d -> it.x.toInt()
        is Quaternionf -> it.x.toInt()
        is String -> it.toIntOrNull()
        else -> null
    }

    fun castToLong2(it: Any?) = getLong(it, 0L)
    fun castToLong(it: Any): Long? = when (it) {
        is Int -> it.toLong()
        is Long -> it
        is Float -> it.toLong()
        is Double -> it.toLong()
        is Vector2f -> it.x.toLong()
        is Vector3f -> it.x.toLong()
        is Vector4f -> it.x.toLong()
        is Vector4d -> it.x.toLong()
        is Quaternionf -> it.x.toLong()
        is String -> it.toLongOrNull()
        else -> null
    }

    fun castToFloat2(it: Any?) = getFloat(it, 0f)
    fun castToFloat(it: Any): Float? = when (it) {
        is Int -> it.toFloat()
        is Long -> it.toFloat()
        is Float -> it
        is Double -> it.toFloat()
        is Vector2f -> it.x
        is Vector3f -> it.x
        is Vector4f -> it.x
        is Vector4d -> it.x.toFloat()
        is Quaternionf -> it.x
        is String -> it.toFloatOrNull()
        else -> null
    }

    fun castToDouble2(it: Any?) = getDouble(it, 0.0)
    fun castToDouble(it: Any): Double? = when (it) {
        is Int -> it.toDouble()
        is Long -> it.toDouble()
        is Float -> it.toDouble()
        is Double -> it
        is Vector2f -> it.x.toDouble()
        is Vector3f -> it.x.toDouble()
        is Vector4f -> it.x.toDouble()
        is Vector4d -> it.x
        is Quaternionf -> it.x.toDouble()
        is String -> it.toDoubleOrNull()
        else -> null
    }

    fun castToVector2f(it: Any): Vector2f? = when (it) {
        is Int -> Vector2f(it.toFloat())
        is Long -> Vector2f(it.toFloat())
        is Float -> Vector2f(it)
        is Double -> Vector2f(it.toFloat())
        is Vector2f -> it
        is Vector3f -> Vector2f(it.x, it.y)
        is Vector4f -> Vector2f(it.x, it.y)
        is Vector4d -> Vector2f(it.x.toFloat(), it.y.toFloat())
        is Quaternionf -> Vector2f(it.x, it.y)
        else -> null
    }

    fun castToVector2d(it: Any): Vector2d? = when (it) {
        is Int -> Vector2d(it.toDouble())
        is Long -> Vector2d(it.toDouble())
        is Float -> Vector2d(it.toDouble())
        is Double -> Vector2d(it)
        is Vector2d -> it
        else -> null
    }

    fun castToVector3f(it: Any): Vector3f? = when (it) {
        is Int -> Vector3f(it.toFloat())
        is Long -> Vector3f(it.toFloat())
        is Float -> Vector3f(it)
        is Double -> Vector3f(it.toFloat())
        is Vector2f -> Vector3f(it, 0f)
        is Vector3f -> it
        is Vector4d -> Vector3f(it.x.toFloat(), it.y.toFloat(), it.z.toFloat())
        is Vector4f -> Vector3f(it.x, it.y, it.z)
        is Quaternionf -> Vector3f(it.x, it.y, it.z)
        else -> null
    }

    fun castToVector3d(it: Any): Vector3d? = when (it) {
        is Int -> Vector3d(it.toDouble())
        is Long -> Vector3d(it.toDouble())
        is Float -> Vector3d(it.toDouble())
        is Double -> Vector3d(it)
        is Vector3d -> it
        else -> null
    }

    fun castToVector4f(it: Any): Vector4f? = when (it) {
        is Int -> Vector4f(it.toFloat())
        is Long -> Vector4f(it.toFloat())
        is Float -> Vector4f(it)
        is Double -> Vector4f(it.toFloat())
        is Vector2f -> Vector4f(it.x, it.x, it.x, it.y)
        is Vector3f -> Vector4f(it, 1f)
        is Vector4f -> it
        is Vector4d -> Vector4f(it.x.toFloat(), it.y.toFloat(), it.z.toFloat(), it.w.toFloat())
        is Quaternionf -> Vector4f(it.x, it.y, it.z, it.w)
        else -> null
    }

    fun castToPlanef(it: Any): Planef? = when (it) {
        is Vector4f -> Planef(it.x, it.y, it.z, it.w)
        is Planef -> it
        is Vector4d -> Planef(it.x.toFloat(), it.y.toFloat(), it.z.toFloat(), it.w.toFloat())
        is Planed -> Planef(it.dirX.toFloat(), it.dirY.toFloat(), it.dirZ.toFloat(), it.distance.toFloat())
        else -> null
    }

    fun castToPlaned(it: Any): Planed? = when (it) {
        is Vector4f -> Planed(it.x.toDouble(), it.y.toDouble(), it.z.toDouble(), it.w.toDouble())
        is Planef -> Planed(it.dirX.toDouble(), it.dirY.toDouble(), it.dirZ.toDouble(), it.distance.toDouble())
        is Vector4d -> Planed(it.x, it.y, it.z, it.w)
        is Planed -> it
        else -> null
    }

    fun castToVector4d(it: Any): Vector4d? = when (it) {
        is Int -> Vector4d(it.toDouble())
        is Long -> Vector4d(it.toDouble())
        is Float -> Vector4d(it.toDouble())
        is Double -> Vector4d(it)
        is Vector2f -> Vector4d(it.x.toDouble(), it.y.toDouble(), it.x.toDouble(), it.y.toDouble())
        is Vector3f -> Vector4d(it.x.toDouble(), it.y.toDouble(), it.z.toDouble(), 1.0)
        is Vector4f -> Vector4d(it.x.toDouble(), it.y.toDouble(), it.z.toDouble(), it.w.toDouble())
        is Vector4d -> it
        is Quaternionf -> Vector4d(it.x.toDouble(), it.y.toDouble(), it.z.toDouble(), it.w.toDouble())
        else -> null
    }

    fun castToString(it: Any?) = it?.toString() ?: ""
}