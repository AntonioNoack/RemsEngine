package me.anno.utils.types

import me.anno.utils.maths.Maths.sq
import me.anno.utils.pooling.JomlPools
import org.joml.*
import kotlin.math.sqrt

object Matrices {


    fun Matrix4f.isIdentity(): Boolean {
        return properties().and(Matrix4f.PROPERTY_IDENTITY.toInt()) != 0
    }

    fun Matrix4x3f.isIdentity(): Boolean {
        return properties().and(Matrix4x3f.PROPERTY_IDENTITY.toInt()) != 0
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

    fun Matrix4x3d.distanceSquared(center: Vector3d): Double {
        val dx = center.x - m30()
        val dy = center.y - m31()
        val dz = center.z - m32()
        return dx * dx + dy * dy + dz * dz
    }

    fun Matrix4f.rotate2(q: Quaterniond): Matrix4f {
        return rotate(q.x.toFloat(), q.y.toFloat(), q.z.toFloat(), q.w.toFloat())
    }

    fun Matrix4x3d.getScaleLengthSquared(): Double {
        return getScale(JomlPools.vec3d.borrow()).lengthSquared()
    }

    fun Matrix4x3d.getScaleLength(): Double {
        return getScale(JomlPools.vec3d.borrow()).length()
    }

    fun Matrix4x3f.getScale2(dst: Vector3d): Vector3d {
        return dst.set(getScale(JomlPools.vec3f.borrow()))
    }

    fun Matrix4f.getScale2(dst: Vector3d): Vector3d {
        return dst.set(getScale(JomlPools.vec3f.borrow()))
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

    fun Matrix4f.mul2(other: Matrix4d): Matrix4f {
        return mul(
            other.m00().toFloat(), other.m01().toFloat(), other.m02().toFloat(), other.m03().toFloat(),
            other.m10().toFloat(), other.m11().toFloat(), other.m12().toFloat(), other.m13().toFloat(),
            other.m20().toFloat(), other.m21().toFloat(), other.m22().toFloat(), other.m23().toFloat(),
            other.m30().toFloat(), other.m31().toFloat(), other.m32().toFloat(), other.m33().toFloat()
        )
    }

    fun Matrix4d.mirror(p: Vector3d, normal: Vector3d): Matrix4d {
        val nx = normal.x
        val ny = normal.y
        val nz = normal.z
        val xx = -2f * nx * nx
        val xy = -2f * nx * ny
        val xz = -2f * nx * nz
        val yy = -2f * ny * ny
        val yz = -2f * ny * nz
        val zz = -2f * nz * nz
        translate(p)
        mul(
            1.0 + xx, xy, xz, 0.0,
            xy, 1.0 + yy, yz, 0.0,
            xz, yz, 1.0 + zz, 0.0,
            0.0, 0.0, 0.0, 1.0
        )
        translate(-p.x, -p.y, -p.z)
        return this
    }

    fun Matrix3d.mul2(other: Matrix4d): Matrix3d {
        mul(
            Matrix3d(
                other.m00(), other.m01(), other.m02(),
                other.m10(), other.m11(), other.m12(),
                other.m20(), other.m21(), other.m22()
            )
        )
        return this
    }

    fun Matrix4x3d.distanceSquared(other: Matrix4x3d): Double {
        return sq(m30() - other.m30(), m31() - other.m31(), m32() - other.m32())
    }

    fun Matrix4x3d.distance(other: Matrix4x3d): Double {
        return sqrt(distanceSquared(other))
    }

    fun Matrix4x3f.sampleDistanceSquared(other: Matrix4x3f): Float {
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