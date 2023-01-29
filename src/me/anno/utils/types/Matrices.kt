package me.anno.utils.types

import me.anno.maths.Maths.sq
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.f3s
import org.joml.*
import kotlin.math.sqrt

@Suppress("unused")
object Matrices {

    // todo move this stuff into KOML

    @JvmStatic
    fun Matrix4f.isIdentity(): Boolean {
        return properties().and(Matrix4f.PROPERTY_IDENTITY) != 0
    }

    @JvmStatic
    fun Matrix4x3f.isIdentity(): Boolean {
        return properties().and(Matrix4x3f.PROPERTY_IDENTITY) != 0
    }

    @JvmStatic
    fun Matrix4f.skew(v: Vector2f) {
        mul3x3(// works
            1f, v.y, 0f,
            v.x, 1f, 0f,
            0f, 0f, 1f
        )
    }

    @JvmStatic
    fun Matrix4f.skew(x: Float, y: Float) {
        mul3x3(// works
            1f, y, 0f,
            x, 1f, 0f,
            0f, 0f, 1f
        )
    }

    @JvmStatic
    fun Matrix4d.skew(x: Double, y: Double) {
        mul3x3(// works
            1.0, y, 0.0,
            x, 1.0, 0.0,
            0.0, 0.0, 1.0
        )
    }

    @JvmStatic
    fun Matrix4f.unprojectInvRay2(
        mouseX: Float,
        mouseY: Float,
        windowX: Float, windowY: Float,
        windowW: Float, windowH: Float,
        // originDest: Vector3f,
        dst: Vector3f = Vector3f()
    ): Vector3f {
        val ndcX = (mouseX - windowX) / windowW * 2.0f - 1.0f
        val ndcY = (mouseY - windowY) / windowH * 2.0f - 1.0f
        val px = this.m00 * ndcX + this.m10 * ndcY + this.m30
        val py = this.m01 * ndcX + this.m11 * ndcY + this.m31
        val pz = this.m02 * ndcX + this.m12 * ndcY + this.m32
        val pw = this.m03 * ndcX + this.m13 * ndcY + this.m33
        val pw1 = pw - this.m23
        val invNearW = 1.0f / pw1
        val nearX = (px - this.m20) * invNearW
        val nearY = (py - this.m21) * invNearW
        val nearZ = (pz - this.m22) * invNearW
        val invW0 = 1.0f / pw
        val x0 = px * invW0
        val y0 = py * invW0
        val z0 = pz * invW0
        /*originDest.x = nearX
        originDest.y = nearY
        originDest.z = nearZ*/
        dst.x = x0 - nearX
        dst.y = y0 - nearY
        dst.z = z0 - nearZ
        return dst
    }

    @JvmStatic
    fun Matrix4x3d.distanceSquared(center: Vector3d): Double {
        val dx = center.x - m30
        val dy = center.y - m31
        val dz = center.z - m32
        return dx * dx + dy * dy + dz * dz
    }

    @JvmStatic
    fun Matrix4f.rotate2(q: Quaterniond): Matrix4f {
        return rotate(JomlPools.quat4f.borrow().set(q))
    }

    @JvmStatic
    fun Matrix4x3d.getScaleLengthSquared(): Double {
        return getScale(JomlPools.vec3d.borrow()).lengthSquared()
    }

    @JvmStatic
    fun Matrix4x3f.getScaleLength(): Float {
        return getScale(JomlPools.vec3f.borrow()).length()
    }

    @JvmStatic
    fun Matrix4x3d.getScaleLength(): Double {
        return getScale(JomlPools.vec3d.borrow()).length()
    }

    /**
     * replace missing setter/constructor
     * */
    @JvmStatic
    fun Matrix4x3f.set2(src: Matrix4x3d): Matrix4x3f {
        set(
            src.m00.toFloat(), src.m01.toFloat(), src.m02.toFloat(),
            src.m10.toFloat(), src.m11.toFloat(), src.m12.toFloat(),
            src.m20.toFloat(), src.m21.toFloat(), src.m22.toFloat(),
            src.m30.toFloat(), src.m31.toFloat(), src.m32.toFloat()
        )
        return this
    }

    @JvmStatic
    fun Matrix4x3f.mul2(src: Matrix4x3d): Matrix4x3f {
        val tmp = JomlPools.mat4x3f.borrow()
        tmp.set2(src)
        mul(tmp)
        return this
    }

    @JvmStatic
    fun Matrix4x3f.getScale2(dst: Vector3d): Vector3d {
        return dst.set(getScale(JomlPools.vec3f.borrow()))
    }

    @JvmStatic
    fun Matrix4f.getScale2(dst: Vector3d = Vector3d()): Vector3d {
        return dst.set(getScale(JomlPools.vec3f.borrow()))
    }

    @JvmStatic
    fun Matrix4f.getTranslation2(dst: Vector3d = Vector3d()): Vector3d {
        return dst.set(m30.toDouble(), m31.toDouble(), m32.toDouble())
    }

    @JvmStatic
    fun Matrix4f.set2(other: Matrix4x3d): Matrix4f {
        return set(
            other.m00.toFloat(), other.m01.toFloat(), other.m02.toFloat(), 0f,
            other.m10.toFloat(), other.m11.toFloat(), other.m12.toFloat(), 0f,
            other.m20.toFloat(), other.m21.toFloat(), other.m22.toFloat(), 0f,
            other.m30.toFloat(), other.m31.toFloat(), other.m32.toFloat(), 1f
        )
    }

    @JvmStatic
    fun Matrix4f.mul2(other: Matrix4x3d): Matrix4f {
        return mul(
            other.m00.toFloat(), other.m01.toFloat(), other.m02.toFloat(), 0f,
            other.m10.toFloat(), other.m11.toFloat(), other.m12.toFloat(), 0f,
            other.m20.toFloat(), other.m21.toFloat(), other.m22.toFloat(), 0f,
            other.m30.toFloat(), other.m31.toFloat(), other.m32.toFloat(), 1f
        )
    }

    @JvmStatic
    fun Matrix4f.mul2(other: Matrix4d): Matrix4f {
        return mul(
            other.m00.toFloat(), other.m01.toFloat(), other.m02.toFloat(), other.m03.toFloat(),
            other.m10.toFloat(), other.m11.toFloat(), other.m12.toFloat(), other.m13.toFloat(),
            other.m20.toFloat(), other.m21.toFloat(), other.m22.toFloat(), other.m23.toFloat(),
            other.m30.toFloat(), other.m31.toFloat(), other.m32.toFloat(), other.m33.toFloat()
        )
    }

    @JvmStatic
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

    @JvmStatic
    fun Matrix3d.mul2(other: Matrix4d): Matrix3d {
        mul(
            Matrix3d(
                other.m00, other.m01, other.m02,
                other.m10, other.m11, other.m12,
                other.m20, other.m21, other.m22
            )
        )
        return this
    }

    @JvmStatic
    fun Matrix4x3d.distanceSquared(other: Matrix4x3d): Double {
        return sq(m30 - other.m30, m31 - other.m31, m32 - other.m32)
    }

    @JvmStatic
    fun Matrix4x3d.distanceSquared(other: Matrix4x3f): Double {
        return sq(m30 - other.m30, m31 - other.m31, m32 - other.m32)
    }

    @JvmStatic
    fun Matrix4x3d.distance(other: Matrix4x3d): Double {
        return sqrt(distanceSquared(other))
    }

    @JvmStatic
    fun Matrix4x3d.distance(other: Matrix4x3f): Double {
        return sqrt(distanceSquared(other))
    }

    @JvmStatic
    fun Matrix4x3f.sampleDistanceSquared(other: Matrix4x3f): Float {
        // compare a few sample points in every direction to also detect rotation issues
        // in my case, the matrices were identical
        return transformPosition(Vector3f()).distanceSquared(other.transformPosition(Vector3f())) +
                transformPosition(Vector3f(1f, 0f, 0f)).distanceSquared(other.transformPosition(Vector3f(1f, 0f, 0f))) +
                transformPosition(Vector3f(0f, 1f, 0f)).distanceSquared(other.transformPosition(Vector3f(0f, 1f, 0f))) +
                transformPosition(Vector3f(0f, 0f, 1f)).distanceSquared(other.transformPosition(Vector3f(0f, 0f, 1f)))
    }

    @JvmStatic
    fun Matrix4f.sampleDistanceSquared(other: Matrix4f): Float {
        // compare a few sample points in every direction to also detect rotation issues
        // in my case, the matrices were identical
        return transformPosition(Vector3f()).distanceSquared(other.transformPosition(Vector3f())) +
                transformPosition(Vector3f(1f, 0f, 0f)).distanceSquared(other.transformPosition(Vector3f(1f, 0f, 0f))) +
                transformPosition(Vector3f(0f, 1f, 0f)).distanceSquared(other.transformPosition(Vector3f(0f, 1f, 0f))) +
                transformPosition(Vector3f(0f, 0f, 1f)).distanceSquared(other.transformPosition(Vector3f(0f, 0f, 1f)))
    }

    @JvmStatic
    fun Matrix4x3d.transformPosition2(v: Vector3f, dst: Vector3f = v): Vector3f {
        return dst.set(
            m00 * v.x + m10 * v.y + m20 * v.z + m30,
            m01 * v.x + m11 * v.y + m21 * v.z + m31,
            m02 * v.x + m12 * v.y + m22 * v.z + m32
        )
    }

    @JvmStatic
    fun Matrix4x3d.f3() = "${m00.f3s()} ${m10.f3s()} ${m20.f3s()} ${m30.f3s()}\n" +
            "${m01.f3s()} ${m11.f3s()} ${m21.f3s()} ${m31.f3s()}\n" +
            "${m02.f3s()} ${m12.f3s()} ${m22.f3s()} ${m32.f3s()}\n"

}