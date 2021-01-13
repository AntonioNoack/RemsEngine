package me.anno.utils

import org.joml.*

object Casting {

    fun castToInt2(it: Any?) = castToInt(it ?: 0) ?: 0
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
        else -> null
    }

    fun castToLong2(it: Any?) = castToLong(it ?: 0L) ?: 0L
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
        else -> null
    }

    fun castToFloat2(it: Any?) = castToFloat(it ?: 0f) ?: 0f
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
        else -> null
    }

    fun castToDouble2(it: Any?) = castToDouble(it ?: 0.0) ?: 0.0
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
        else -> null
    }

    fun castToVector2fb(it: Any?) = castToVector2f(it ?: Unit) ?: Vector2f()
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

    fun castToVector3fb(it: Any?) = castToVector3f(it ?: Unit) ?: Vector3f()
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

    fun castToVector4fb(it: Any?) = castToVector4f(it ?: Unit) ?: Vector4f()
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

    fun castToVector4db(it: Any?) = castToVector4d(it ?: Unit) ?: Vector4d()
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

}