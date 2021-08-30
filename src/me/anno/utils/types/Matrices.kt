package me.anno.utils.types

import me.anno.utils.maths.Maths.sq
import org.joml.*
import kotlin.math.sqrt

object Matrices {

    fun Matrix4f.isIdentity(): Boolean {
        return properties().and(4) != 0
    }

    fun Matrix4f.clone() = Matrix4f(this)

    fun Matrix4f.skew(v: Vector2fc) {
        mul3x3(// works
            1f, v.y(), 0f,
            v.x(), 1f, 0f,
            0f, 0f, 1f
        )
    }

    fun Matrix4f.skew(x: Float, y: Float) {
        mul3x3(// works
            1f, y, 0f,
            x, 1f, 0f,
            0f, 0f, 1f
        )
    }

    fun Matrix4d.skew(x: Double, y: Double) {
        mul3x3(// works
            1.0, y, 0.0,
            x, 1.0, 0.0,
            0.0, 0.0, 1.0
        )
    }

    fun Matrix4f.set2(other: Matrix4x3d): Matrix4f {
        return set(
            other.m00().toFloat(), other.m01().toFloat(), other.m02().toFloat(), 0f,
            other.m10().toFloat(), other.m11().toFloat(), other.m12().toFloat(), 0f,
            other.m20().toFloat(), other.m21().toFloat(), other.m22().toFloat(), 0f,
            other.m30().toFloat(), other.m31().toFloat(), other.m32().toFloat(), 1f
        )
    }

    fun Matrix4f.mul2(other: Matrix4x3d): Matrix4f {
        return mul(
            other.m00().toFloat(), other.m01().toFloat(), other.m02().toFloat(), 0f,
            other.m10().toFloat(), other.m11().toFloat(), other.m12().toFloat(), 0f,
            other.m20().toFloat(), other.m21().toFloat(), other.m22().toFloat(), 0f,
            other.m30().toFloat(), other.m31().toFloat(), other.m32().toFloat(), 1f
        )
    }

    fun Matrix4x3d.distanceSquared(other: Matrix4x3d): Double {
        return sq(m30() - other.m30(), m31() - other.m31(), m32() - other.m32())
    }

    fun Matrix4x3d.distance(other: Matrix4x3d): Double {
        return sqrt(distanceSquared(other))
    }

    fun Matrix4x3f.sampleDistanceSquared(other: Matrix4f): Float {
        // compare a few sample points in every direction to also detect rotation issues
        // in my case, the matrices were identical
        return transformPosition(Vector3f()).distanceSquared(other.transformPosition(Vector3f())) +
                transformPosition(Vector3f(1f, 0f, 0f)).distanceSquared(other.transformPosition(Vector3f(1f, 0f, 0f))) +
                transformPosition(Vector3f(0f, 1f, 0f)).distanceSquared(other.transformPosition(Vector3f(0f, 1f, 0f))) +
                transformPosition(Vector3f(0f, 0f, 1f)).distanceSquared(other.transformPosition(Vector3f(0f, 0f, 1f)))
    }

    fun Matrix4f.sampleDistanceSquared(other: Matrix4f): Float {
        // compare a few sample points in every direction to also detect rotation issues
        // in my case, the matrices were identical
        return transformPosition(Vector3f()).distanceSquared(other.transformPosition(Vector3f())) +
                transformPosition(Vector3f(1f, 0f, 0f)).distanceSquared(other.transformPosition(Vector3f(1f, 0f, 0f))) +
                transformPosition(Vector3f(0f, 1f, 0f)).distanceSquared(other.transformPosition(Vector3f(0f, 1f, 0f))) +
                transformPosition(Vector3f(0f, 0f, 1f)).distanceSquared(other.transformPosition(Vector3f(0f, 0f, 1f)))
    }

}