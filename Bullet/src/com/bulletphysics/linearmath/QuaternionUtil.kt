package com.bulletphysics.linearmath

import com.bulletphysics.BulletGlobals
import cz.advel.stack.Stack
import org.joml.Quaterniond
import org.joml.Vector3d
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Utility functions for quaternions.
 *
 * @author jezek2
 */
object QuaternionUtil {

    @JvmStatic
    fun getAngle(q: Quaterniond): Double {
        return 2.0 * acos(q.w)
    }

    @JvmStatic
    fun setRotation(q: Quaterniond, axis: Vector3d, angle: Double) {
        val d = axis.length()
        assert(d != 0.0)
        val s = sin(angle * 0.5) / d
        q.set(axis.x * s, axis.y * s, axis.z * s, cos(angle * 0.5))
    }

    // Game Programming Gems 2.10. make sure v0,v1 are normalized
    @JvmStatic
    fun shortestArcQuat(v0: Vector3d, v1: Vector3d, out: Quaterniond): Quaterniond {
        val c = Stack.newVec()
        v0.cross(v1, c)
        val d = v0.dot(v1)

        if (d < -1.0 + BulletGlobals.FLT_EPSILON) {
            // just pick any vector
            out.set(0.0, 1.0, 0.0, 0.0)
            return out
        }

        val s = sqrt((1.0 + d) * 2.0)
        val rs = 1.0 / s

        out.set(c.x * rs, c.y * rs, c.z * rs, s * 0.5)
        return out
    }

    private fun mul(q: Quaterniond, w: Vector3d) {
        val rx = q.w * w.x + q.y * w.z - q.z * w.y
        val ry = q.w * w.y + q.z * w.x - q.x * w.z
        val rz = q.w * w.z + q.x * w.y - q.y * w.x
        val rw = -q.x * w.x - q.y * w.y - q.z * w.z
        q.set(rx, ry, rz, rw)
    }

    @JvmStatic
    fun quatRotate(rotation: Quaterniond, v: Vector3d, out: Vector3d): Vector3d {
        val q = Stack.newQuat(rotation)
        mul(q, v)

        val tmp = Stack.newQuat()
        inverse(tmp, rotation)
        q.mul(tmp)

        out.set(q.x, q.y, q.z)
        Stack.subQuat(2)
        return out
    }

    private fun inverse(q: Quaterniond, src: Quaterniond) {
        q.x = -src.x
        q.y = -src.y
        q.z = -src.z
        q.w = src.w
    }
}
