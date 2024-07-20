package me.anno.utils.types

import me.anno.utils.types.AnyToDouble.getDouble
import me.anno.utils.types.AnyToFloat.getFloat
import me.anno.utils.types.AnyToInt.getInt
import me.anno.utils.types.AnyToLong.getLong
import me.anno.utils.types.Floats.toLongOr
import org.joml.Planed
import org.joml.Planef
import org.joml.Vector
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f

/**
 * utilities for NumberType
 * */
object Casting {

    fun castToInt2(it: Any?) = getInt(it, 0)
    fun castToInt(it: Any?): Int? = castToLong(it)?.toInt()

    fun castToLong2(it: Any?) = getLong(it, 0L)
    fun castToLong(it: Any?): Long? = when (it) {
        is Int -> it.toLong()
        is Long -> it
        is Float -> it.toLongOr()
        is Double -> it.toLongOr()
        is Vector -> it.getComp(0).toLongOr()
        is String -> it.toLongOrNull()
        else -> null
    }

    fun castToFloat2(it: Any?) = getFloat(it, 0f)
    fun castToFloat(it: Any?): Float? = castToDouble(it)?.toFloat()

    fun castToDouble2(it: Any?) = getDouble(it, 0.0)
    fun castToDouble(it: Any?): Double? = when (it) {
        is Int -> it.toDouble()
        is Long -> it.toDouble()
        is Float -> it.toDouble()
        is Double -> it
        is Vector -> it.getComp(0)
        is String -> it.toDoubleOrNull()
        else -> null
    }

    fun castToVector2f(it: Any?): Vector2f? = when (it) {
        is Int -> Vector2f(it.toFloat())
        is Long -> Vector2f(it.toFloat())
        is Float -> Vector2f(it)
        is Double -> Vector2f(it.toFloat())
        is Vector2f -> it
        is Vector -> Vector2f(
            it.getCompOr(0).toFloat(),
            it.getCompOr(1).toFloat()
        )
        else -> null
    }

    fun castToVector2d(it: Any?): Vector2d? = when (it) {
        is Int -> Vector2d(it.toDouble())
        is Long -> Vector2d(it.toDouble())
        is Float -> Vector2d(it.toDouble())
        is Double -> Vector2d(it)
        is Vector2d -> it
        is Vector -> Vector2d(
            it.getCompOr(0),
            it.getCompOr(1)
        )
        else -> null
    }

    fun castToVector3f(it: Any?): Vector3f? = when (it) {
        is Int -> Vector3f(it.toFloat())
        is Long -> Vector3f(it.toFloat())
        is Float -> Vector3f(it)
        is Double -> Vector3f(it.toFloat())
        is Vector3f -> it
        is Vector -> Vector3f(
            it.getCompOr(0).toFloat(),
            it.getCompOr(1).toFloat(),
            it.getCompOr(2).toFloat()
        )
        else -> null
    }

    fun castToVector3d(it: Any?): Vector3d? = when (it) {
        is Int -> Vector3d(it.toDouble())
        is Long -> Vector3d(it.toDouble())
        is Float -> Vector3d(it.toDouble())
        is Double -> Vector3d(it)
        is Vector3d -> it
        is Vector -> Vector3d(
            it.getCompOr(0),
            it.getCompOr(1),
            it.getCompOr(2)
        )
        else -> null
    }

    fun castToVector4f(it: Any?): Vector4f? = when (it) {
        is Int -> Vector4f(it.toFloat())
        is Long -> Vector4f(it.toFloat())
        is Float -> Vector4f(it)
        is Double -> Vector4f(it.toFloat())
        is Vector4f -> it
        is Vector -> Vector4f(
            it.getCompOr(0).toFloat(),
            it.getCompOr(1).toFloat(),
            it.getCompOr(2).toFloat(),
            it.getCompOr(3, 1.0).toFloat()
        )
        else -> null
    }

    fun castToVector4d(it: Any?): Vector4d? = when (it) {
        is Int -> Vector4d(it.toDouble())
        is Long -> Vector4d(it.toDouble())
        is Float -> Vector4d(it.toDouble())
        is Double -> Vector4d(it)
        is Vector4d -> it
        is Vector -> Vector4d(
            it.getCompOr(0),
            it.getCompOr(1),
            it.getCompOr(2),
            it.getCompOr(3, 1.0)
        )
        else -> null
    }

    fun castToPlanef(it: Any?): Planef? = when (it) {
        is Planef -> it
        is Vector -> Planef(
            it.getCompOr(0).toFloat(),
            it.getCompOr(1).toFloat(),
            it.getCompOr(2).toFloat(),
            it.getCompOr(3).toFloat()
        )
        else -> null
    }

    fun castToPlaned(it: Any?): Planed? = when (it) {
        is Planed -> it
        is Vector -> Planed(
            it.getCompOr(0),
            it.getCompOr(1),
            it.getCompOr(2),
            it.getCompOr(3)
        )
        else -> null
    }

    fun castToString(it: Any?) = it?.toString() ?: ""
}