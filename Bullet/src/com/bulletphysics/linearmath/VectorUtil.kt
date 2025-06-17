package com.bulletphysics.linearmath

import org.joml.Vector3d
import org.joml.Vector4d
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Utility functions for vectors.
 *
 * @author jezek2
 */
object VectorUtil {

    @JvmStatic
    fun maxAxis(v: Vector3d): Int {
        var maxIndex = -1
        var maxVal = -1e308
        if (v.x > maxVal) {
            maxIndex = 0
            maxVal = v.x
        }
        if (v.y > maxVal) {
            maxIndex = 1
            maxVal = v.y
        }
        if (v.z > maxVal) {
            maxIndex = 2
        }
        return maxIndex
    }

    @JvmStatic
    fun closestAxis4(x: Double, y: Double, z: Double, w: Double): Int {
        var x = x
        var y = y
        var z = z
        var w = w
        x = abs(x)
        y = abs(y)
        z = abs(z)
        w = abs(w)

        var maxIndex = -1
        var maxVal = Double.NEGATIVE_INFINITY
        if (x > maxVal) {
            maxIndex = 0
            maxVal = x
        }
        if (y > maxVal) {
            maxIndex = 1
            maxVal = y
        }
        if (z > maxVal) {
            maxIndex = 2
            maxVal = z
        }
        if (w > maxVal) {
            maxIndex = 3
        }
        return maxIndex
    }

    @JvmStatic
    fun getCoord(vec: Vector3d, num: Int): Double {
        when (num) {
            0 -> return vec.x
            1 -> return vec.y
            else -> return vec.z
        }
    }

    @JvmStatic
    fun setCoord(vec: Vector3d, num: Int, value: Double) {
        when (num) {
            0 -> vec.x = value
            1 -> vec.y = value
            else -> vec.z = value
        }
    }

    @JvmStatic
    fun mulCoord(vec: Vector3d, num: Int, value: Double) {
        when (num) {
            0 -> vec.x *= value
            1 -> vec.y *= value
            else -> vec.z *= value
        }
    }

    @JvmStatic
    fun setInterpolate3(dst: Vector3d, v0: Vector3d, v1: Vector3d, rt: Double) {
        val s = 1.0 - rt
        dst.x = s * v0.x + rt * v1.x
        dst.y = s * v0.y + rt * v1.y
        dst.z = s * v0.z + rt * v1.z
        // don't do the unused w component
        //		m_co[3] = s * v0[3] + rt * v1[3];
    }

    fun add(dst: Vector3d, v1: Vector3d, v2: Vector3d) {
        dst.x = v1.x + v2.x
        dst.y = v1.y + v2.y
        dst.z = v1.z + v2.z
    }

    @JvmStatic
    fun add(dst: Vector3d, v1: Vector3d, v2: Vector3d, v3: Vector3d) {
        dst.x = v1.x + v2.x + v3.x
        dst.y = v1.y + v2.y + v3.y
        dst.z = v1.z + v2.z + v3.z
    }

    @JvmStatic
    fun add(dst: Vector3d, v1: Vector3d, v2: Vector3d, v3: Vector3d, v4: Vector3d) {
        dst.x = v1.x + v2.x + v3.x + v4.x
        dst.y = v1.y + v2.y + v3.y + v4.y
        dst.z = v1.z + v2.z + v3.z + v4.z
    }

    @JvmStatic
    fun mul(dst: Vector3d, v1: Vector3d, v2: Vector3d) {
        dst.x = v1.x * v2.x
        dst.y = v1.y * v2.y
        dst.z = v1.z * v2.z
    }

    @JvmStatic
    fun div(dst: Vector3d, v1: Vector3d, v2: Vector3d) {
        dst.x = v1.x / v2.x
        dst.y = v1.y / v2.y
        dst.z = v1.z / v2.z
    }

    @JvmStatic
    fun setMin(a: Vector3d, b: Vector3d) {
        setMin(a, a, b)
    }

    @JvmStatic
    fun setMax(a: Vector3d, b: Vector3d) {
        setMax(a, a, b)
    }

    @JvmStatic
    fun setMin(dst: Vector3d, a: Vector3d, b: Vector3d) {
        dst.x = min(a.x, b.x)
        dst.y = min(a.y, b.y)
        dst.z = min(a.z, b.z)
    }

    @JvmStatic
    fun setMax(dst: Vector3d, a: Vector3d, b: Vector3d) {
        dst.x = max(a.x, b.x)
        dst.y = max(a.y, b.y)
        dst.z = max(a.z, b.z)
    }

    @JvmStatic
    fun dot3(v0: Vector3d, v1: Vector4d): Double {
        return (v0.x * v1.x + v0.y * v1.y + v0.z * v1.z)
    }
}
