package com.bulletphysics.linearmath

import org.joml.Vector3d
import org.joml.Vector4d
import kotlin.math.abs

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
    fun setInterpolate3(dst: Vector3d, v0: Vector3d, v1: Vector3d, rt: Double) {
        v0.lerp(v1, rt, dst)
    }

    fun add(dst: Vector3d, v1: Vector3d, v2: Vector3d) {
        v1.add(v2, dst)
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
        v1.mul(v2, dst)
    }

    @JvmStatic
    fun div(dst: Vector3d, v1: Vector3d, v2: Vector3d) {
        v1.div(v2, dst)
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
        a.min(b, dst)
    }

    @JvmStatic
    fun setMax(dst: Vector3d, a: Vector3d, b: Vector3d) {
        a.max(b, dst)
    }

    @JvmStatic
    fun dot3(v0: Vector3d, v1: Vector4d): Double {
        return (v0.x * v1.x + v0.y * v1.y + v0.z * v1.z)
    }
}
