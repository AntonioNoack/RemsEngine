package org.joml

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class AxisAngle4f(
    @JvmField var angle: Float,
    @JvmField var x: Float,
    @JvmField var y: Float,
    @JvmField var z: Float
) {

    constructor() : this(0f, 0f, 0f, 1f)
    constructor(a: AxisAngle4f) : this(a.angle, a.x, a.y, a.z)
    constructor(angle: Float, v: Vector3f) : this(angle, v.x, v.y, v.z)

    constructor(q: Quaternionf) : this() {
        set(q)
    }

    init {
        angle = posMod(angle)
    }

    fun set(a: AxisAngle4f): AxisAngle4f = set(a.angle, a.x, a.y, a.z)
    fun set(a: AxisAngle4d): AxisAngle4f = set(a.angle.toFloat(), a.x.toFloat(), a.y.toFloat(), a.z.toFloat())
    fun set(angle: Float, x: Float, y: Float, z: Float): AxisAngle4f {
        this.x = x
        this.y = y
        this.z = z
        this.angle = posMod(angle)
        return this
    }

    fun set(angle: Float, v: Vector3f): AxisAngle4f = this.set(angle, v.x, v.y, v.z)

    fun setByQuaternion(qx: Float, qy: Float, qz: Float, qw: Float): AxisAngle4f {
        val acos = JomlMath.safeAcos(qw)
        val invSqrt = JomlMath.invsqrt(1.0f - qw * qw)
        if (invSqrt.isInfinite()) {
            x = 0.0f
            y = 0.0f
            z = 1.0f
        } else {
            x = qx * invSqrt
            y = qy * invSqrt
            z = qz * invSqrt
        }
        angle = acos + acos
        return this
    }

    fun set(q: Quaternionf): AxisAngle4f = setByQuaternion(q.x, q.y, q.z, q.w)
    fun set(q: Quaterniond): AxisAngle4f = setByQuaternion(q.x.toFloat(), q.y.toFloat(), q.z.toFloat(), q.w.toFloat())

    fun set(m: Matrix3f): AxisAngle4f {
        var nm00 = m.m00
        var nm01 = m.m01
        var nm02 = m.m02
        var nm10 = m.m10
        var nm11 = m.m11
        var nm12 = m.m12
        var nm20 = m.m20
        var nm21 = m.m21
        var nm22 = m.m22
        val lenX = JomlMath.invsqrt(m.m00 * m.m00 + m.m01 * m.m01 + m.m02 * m.m02)
        val lenY = JomlMath.invsqrt(m.m10 * m.m10 + m.m11 * m.m11 + m.m12 * m.m12)
        val lenZ = JomlMath.invsqrt(m.m20 * m.m20 + m.m21 * m.m21 + m.m22 * m.m22)
        nm00 *= lenX
        nm01 *= lenX
        nm02 *= lenX
        nm10 *= lenY
        nm11 *= lenY
        nm12 *= lenY
        nm20 *= lenZ
        nm21 *= lenZ
        nm22 *= lenZ
        val epsilon = 1.0E-4f
        val epsilon2 = 0.001f
        val xx: Float
        if (abs(nm10 - nm01) < epsilon && abs(nm20 - nm02) < epsilon && abs(nm21 - nm12) < epsilon) {
            if (abs(nm10 + nm01) < epsilon2 && abs(nm20 + nm02) < epsilon2 && abs(nm21 + nm12) < epsilon2 && abs(
                    nm00 + nm11 + nm22 - 3.0f
                ) < epsilon2
            ) {
                x = 0.0f
                y = 0.0f
                z = 1.0f
                angle = 0.0f
            } else {
                angle = 3.1415927f
                xx = (nm00 + 1.0f) / 2.0f
                val yy = (nm11 + 1.0f) / 2.0f
                val zz = (nm22 + 1.0f) / 2.0f
                val xy = (nm10 + nm01) / 4.0f
                val xz = (nm20 + nm02) / 4.0f
                val yz = (nm21 + nm12) / 4.0f
                if (xx > yy && xx > zz) {
                    x = sqrt(xx)
                    y = xy / x
                    z = xz / x
                } else if (yy > zz) {
                    y = sqrt(yy)
                    x = xy / y
                    z = yz / y
                } else {
                    z = sqrt(zz)
                    x = xz / z
                    y = yz / z
                }
            }
        } else {
            xx =
                sqrt((nm12 - nm21) * (nm12 - nm21) + (nm20 - nm02) * (nm20 - nm02) + (nm01 - nm10) * (nm01 - nm10))
            angle = JomlMath.safeAcos((nm00 + nm11 + nm22 - 1.0f) / 2.0f)
            x = (nm12 - nm21) / xx
            y = (nm20 - nm02) / xx
            z = (nm01 - nm10) / xx
        }
        return this
    }

    fun set(m: Matrix3d): AxisAngle4f {
        var nm00 = m.m00
        var nm01 = m.m01
        var nm02 = m.m02
        var nm10 = m.m10
        var nm11 = m.m11
        var nm12 = m.m12
        var nm20 = m.m20
        var nm21 = m.m21
        var nm22 = m.m22
        val lenX = JomlMath.invsqrt(m.m00 * m.m00 + m.m01 * m.m01 + m.m02 * m.m02)
        val lenY = JomlMath.invsqrt(m.m10 * m.m10 + m.m11 * m.m11 + m.m12 * m.m12)
        val lenZ = JomlMath.invsqrt(m.m20 * m.m20 + m.m21 * m.m21 + m.m22 * m.m22)
        nm00 *= lenX
        nm01 *= lenX
        nm02 *= lenX
        nm10 *= lenY
        nm11 *= lenY
        nm12 *= lenY
        nm20 *= lenZ
        nm21 *= lenZ
        nm22 *= lenZ
        val epsilon = 1.0E-4
        val epsilon2 = 0.001
        val xx: Double
        if (abs(nm10 - nm01) < epsilon && abs(nm20 - nm02) < epsilon && abs(nm21 - nm12) < epsilon) {
            if (abs(nm10 + nm01) < epsilon2 && abs(nm20 + nm02) < epsilon2 && abs(nm21 + nm12) < epsilon2 && abs(
                    nm00 + nm11 + nm22 - 3.0
                ) < epsilon2
            ) {
                x = 0.0f
                y = 0.0f
                z = 1.0f
                angle = 0.0f
            } else {
                angle = 3.1415927f
                xx = (nm00 + 1.0) / 2.0
                val yy = (nm11 + 1.0) / 2.0
                val zz = (nm22 + 1.0) / 2.0
                val xy = (nm10 + nm01) / 4.0
                val xz = (nm20 + nm02) / 4.0
                val yz = (nm21 + nm12) / 4.0
                if (xx > yy && xx > zz) {
                    x = sqrt(xx).toFloat()
                    y = (xy / x.toDouble()).toFloat()
                    z = (xz / x.toDouble()).toFloat()
                } else if (yy > zz) {
                    y = sqrt(yy).toFloat()
                    x = (xy / y.toDouble()).toFloat()
                    z = (yz / y.toDouble()).toFloat()
                } else {
                    z = sqrt(zz).toFloat()
                    x = (xz / z.toDouble()).toFloat()
                    y = (yz / z.toDouble()).toFloat()
                }
            }
        } else {
            xx =
                sqrt((nm12 - nm21) * (nm12 - nm21) + (nm20 - nm02) * (nm20 - nm02) + (nm01 - nm10) * (nm01 - nm10))
            angle = JomlMath.safeAcos((nm00 + nm11 + nm22 - 1.0) / 2.0).toFloat()
            x = ((nm12 - nm21) / xx).toFloat()
            y = ((nm20 - nm02) / xx).toFloat()
            z = ((nm01 - nm10) / xx).toFloat()
        }
        return this
    }

    fun set(m: Matrix4f): AxisAngle4f {
        var nm00 = m.m00
        var nm01 = m.m01
        var nm02 = m.m02
        var nm10 = m.m10
        var nm11 = m.m11
        var nm12 = m.m12
        var nm20 = m.m20
        var nm21 = m.m21
        var nm22 = m.m22
        val lenX = JomlMath.invsqrt(m.m00 * m.m00 + m.m01 * m.m01 + m.m02 * m.m02)
        val lenY = JomlMath.invsqrt(m.m10 * m.m10 + m.m11 * m.m11 + m.m12 * m.m12)
        val lenZ = JomlMath.invsqrt(m.m20 * m.m20 + m.m21 * m.m21 + m.m22 * m.m22)
        nm00 *= lenX
        nm01 *= lenX
        nm02 *= lenX
        nm10 *= lenY
        nm11 *= lenY
        nm12 *= lenY
        nm20 *= lenZ
        nm21 *= lenZ
        nm22 *= lenZ
        val epsilon = 1.0E-4f
        val epsilon2 = 0.001f
        val xx: Float
        if (abs(nm10 - nm01) < epsilon && abs(nm20 - nm02) < epsilon && abs(nm21 - nm12) < epsilon) {
            if (abs(nm10 + nm01) < epsilon2 && abs(nm20 + nm02) < epsilon2 && abs(nm21 + nm12) < epsilon2 && abs(
                    nm00 + nm11 + nm22 - 3.0f
                ) < epsilon2
            ) {
                x = 0.0f
                y = 0.0f
                z = 1.0f
                angle = 0.0f
            } else {
                angle = 3.1415927f
                xx = (nm00 + 1.0f) / 2.0f
                val yy = (nm11 + 1.0f) / 2.0f
                val zz = (nm22 + 1.0f) / 2.0f
                val xy = (nm10 + nm01) / 4.0f
                val xz = (nm20 + nm02) / 4.0f
                val yz = (nm21 + nm12) / 4.0f
                if (xx > yy && xx > zz) {
                    x = sqrt(xx)
                    y = xy / x
                    z = xz / x
                } else if (yy > zz) {
                    y = sqrt(yy)
                    x = xy / y
                    z = yz / y
                } else {
                    z = sqrt(zz)
                    x = xz / z
                    y = yz / z
                }
            }
        } else {
            xx =
                sqrt((nm12 - nm21) * (nm12 - nm21) + (nm20 - nm02) * (nm20 - nm02) + (nm01 - nm10) * (nm01 - nm10))
            angle = JomlMath.safeAcos((nm00 + nm11 + nm22 - 1.0f) / 2.0f)
            x = (nm12 - nm21) / xx
            y = (nm20 - nm02) / xx
            z = (nm01 - nm10) / xx
        }
        return this
    }

    fun set(m: Matrix4x3f): AxisAngle4f {
        var nm00 = m.m00
        var nm01 = m.m01
        var nm02 = m.m02
        var nm10 = m.m10
        var nm11 = m.m11
        var nm12 = m.m12
        var nm20 = m.m20
        var nm21 = m.m21
        var nm22 = m.m22
        val lenX = JomlMath.invsqrt(m.m00 * m.m00 + m.m01 * m.m01 + m.m02 * m.m02)
        val lenY = JomlMath.invsqrt(m.m10 * m.m10 + m.m11 * m.m11 + m.m12 * m.m12)
        val lenZ = JomlMath.invsqrt(m.m20 * m.m20 + m.m21 * m.m21 + m.m22 * m.m22)
        nm00 *= lenX
        nm01 *= lenX
        nm02 *= lenX
        nm10 *= lenY
        nm11 *= lenY
        nm12 *= lenY
        nm20 *= lenZ
        nm21 *= lenZ
        nm22 *= lenZ
        val epsilon = 1.0E-4f
        val epsilon2 = 0.001f
        val xx: Float
        if (abs(nm10 - nm01) < epsilon && abs(nm20 - nm02) < epsilon && abs(nm21 - nm12) < epsilon) {
            if (abs(nm10 + nm01) < epsilon2 && abs(nm20 + nm02) < epsilon2 && abs(nm21 + nm12) < epsilon2 && abs(
                    nm00 + nm11 + nm22 - 3.0f
                ) < epsilon2
            ) {
                x = 0.0f
                y = 0.0f
                z = 1.0f
                angle = 0.0f
            } else {
                angle = 3.1415927f
                xx = (nm00 + 1.0f) / 2.0f
                val yy = (nm11 + 1.0f) / 2.0f
                val zz = (nm22 + 1.0f) / 2.0f
                val xy = (nm10 + nm01) / 4.0f
                val xz = (nm20 + nm02) / 4.0f
                val yz = (nm21 + nm12) / 4.0f
                if (xx > yy && xx > zz) {
                    x = sqrt(xx)
                    y = xy / x
                    z = xz / x
                } else if (yy > zz) {
                    y = sqrt(yy)
                    x = xy / y
                    z = yz / y
                } else {
                    z = sqrt(zz)
                    x = xz / z
                    y = yz / z
                }
            }
        } else {
            xx =
                sqrt((nm12 - nm21) * (nm12 - nm21) + (nm20 - nm02) * (nm20 - nm02) + (nm01 - nm10) * (nm01 - nm10))
            angle = JomlMath.safeAcos((nm00 + nm11 + nm22 - 1.0f) / 2.0f)
            x = (nm12 - nm21) / xx
            y = (nm20 - nm02) / xx
            z = (nm01 - nm10) / xx
        }
        return this
    }

    fun set(m: Matrix4d): AxisAngle4f {
        var nm00 = m.m00
        var nm01 = m.m01
        var nm02 = m.m02
        var nm10 = m.m10
        var nm11 = m.m11
        var nm12 = m.m12
        var nm20 = m.m20
        var nm21 = m.m21
        var nm22 = m.m22
        val lenX = JomlMath.invsqrt(m.m00 * m.m00 + m.m01 * m.m01 + m.m02 * m.m02)
        val lenY = JomlMath.invsqrt(m.m10 * m.m10 + m.m11 * m.m11 + m.m12 * m.m12)
        val lenZ = JomlMath.invsqrt(m.m20 * m.m20 + m.m21 * m.m21 + m.m22 * m.m22)
        nm00 *= lenX
        nm01 *= lenX
        nm02 *= lenX
        nm10 *= lenY
        nm11 *= lenY
        nm12 *= lenY
        nm20 *= lenZ
        nm21 *= lenZ
        nm22 *= lenZ
        val epsilon = 1.0E-4
        val epsilon2 = 0.001
        val xx: Double
        if (abs(nm10 - nm01) < epsilon && abs(nm20 - nm02) < epsilon && abs(nm21 - nm12) < epsilon) {
            if (abs(nm10 + nm01) < epsilon2 && abs(nm20 + nm02) < epsilon2 && abs(nm21 + nm12) < epsilon2 && abs(
                    nm00 + nm11 + nm22 - 3.0
                ) < epsilon2
            ) {
                x = 0.0f
                y = 0.0f
                z = 1.0f
                angle = 0.0f
            } else {
                angle = 3.1415927f
                xx = (nm00 + 1.0) / 2.0
                val yy = (nm11 + 1.0) / 2.0
                val zz = (nm22 + 1.0) / 2.0
                val xy = (nm10 + nm01) / 4.0
                val xz = (nm20 + nm02) / 4.0
                val yz = (nm21 + nm12) / 4.0
                if (xx > yy && xx > zz) {
                    x = sqrt(xx).toFloat()
                    y = (xy / x.toDouble()).toFloat()
                    z = (xz / x.toDouble()).toFloat()
                } else if (yy > zz) {
                    y = sqrt(yy).toFloat()
                    x = (xy / y.toDouble()).toFloat()
                    z = (yz / y.toDouble()).toFloat()
                } else {
                    z = sqrt(zz).toFloat()
                    x = (xz / z.toDouble()).toFloat()
                    y = (yz / z.toDouble()).toFloat()
                }
            }
        } else {
            xx =
                sqrt((nm12 - nm21) * (nm12 - nm21) + (nm20 - nm02) * (nm20 - nm02) + (nm01 - nm10) * (nm01 - nm10))
            angle = JomlMath.safeAcos((nm00 + nm11 + nm22 - 1.0) / 2.0).toFloat()
            x = ((nm12 - nm21) / xx).toFloat()
            y = ((nm20 - nm02) / xx).toFloat()
            z = ((nm01 - nm10) / xx).toFloat()
        }
        return this
    }

    operator fun get(q: Quaternionf): Quaternionf {
        return q.set(this)
    }

    operator fun get(q: Quaterniond): Quaterniond {
        return q.set(this)
    }

    operator fun get(m: Matrix4f): Matrix4f {
        return m.set(this)
    }

    operator fun get(m: Matrix3f): Matrix3f {
        return m.set(this)
    }

    operator fun get(m: Matrix4d): Matrix4d {
        return m.set(this)
    }

    operator fun get(m: Matrix3d): Matrix3d {
        return m.set(this)
    }

    operator fun get(dst: AxisAngle4d): AxisAngle4d {
        return dst.set(this)
    }

    operator fun get(dst: AxisAngle4f): AxisAngle4f {
        return dst.set(this)
    }

    fun normalize(): AxisAngle4f {
        val invLength = JomlMath.invsqrt(x * x + y * y + z * z)
        x *= invLength
        y *= invLength
        z *= invLength
        return this
    }

    fun rotate(ang: Float): AxisAngle4f {
        angle += ang
        angle =
            ((if (angle.toDouble() < 0.0) 6.283185307179586 + angle.toDouble() % 6.283185307179586 else angle.toDouble()) % 6.283185307179586).toFloat()
        return this
    }

    @JvmOverloads
    fun transform(v: Vector3f, dst: Vector3f = v): Vector3f {
        val sin = sin(angle)
        val cos = cos(angle)
        val dot = x * v.x + y * v.y + z * v.z
        return dst.set(
            (v.x * cos + sin * (y * v.z - z * v.y) + (1f - cos) * dot * x),
            (v.y * cos + sin * (z * v.x - x * v.z) + (1f - cos) * dot * y),
            (v.z * cos + sin * (x * v.y - y * v.x) + (1f - cos) * dot * z)
        )
    }

    @JvmOverloads
    fun transform(v: Vector4f, dst: Vector4f = v): Vector4f {
        val sin = sin(angle)
        val cos = cos(angle)
        val dot = x * v.x + y * v.y + z * v.z
        return dst.set(
            (v.x * cos + sin * (y * v.z - z * v.y) + (1f - cos) * dot * x),
            (v.y * cos + sin * (z * v.x - x * v.z) + (1f - cos) * dot * y),
            (v.z * cos + sin * (x * v.y - y * v.x) + (1f - cos) * dot * z),
            dst.w
        )
    }

    override fun toString(): String {
        return "($x,$y,$z <| $angle)"
    }

    private fun posMod(value: Float): Float {
        val a = value.toDouble()
        val tau = PI * 2.0
        return if (a < 0.0) {
            (tau + a % tau)
        } else {
            a % tau
        }.toFloat()
    }

    override fun hashCode(): Int {
        var result = posMod(angle).toBits()
        result = 31 * result + x.toBits()
        result = 31 * result + y.toBits()
        result = 31 * result + z.toBits()
        return result
    }

    override fun equals(other: Any?): Boolean {
        return if (this === other) true
        else if (other !is AxisAngle4f) false
        else x == other.x && y == other.y && z == other.z && posMod(angle) == posMod(other.angle)
    }
}