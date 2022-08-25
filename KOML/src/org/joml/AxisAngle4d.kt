package org.joml

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class AxisAngle4d : Cloneable {
    var angle = 0.0
    var x = 0.0
    var y = 0.0
    var z = 0.0

    constructor() {
        z = 1.0
    }

    constructor(a: AxisAngle4d) {
        x = a.x
        y = a.y
        z = a.z
        angle = (if (a.angle < 0.0) 6.283185307179586 + a.angle % 6.283185307179586 else a.angle) % 6.283185307179586
    }

    constructor(a: AxisAngle4f) {
        x = a.x.toDouble()
        y = a.y.toDouble()
        z = a.z.toDouble()
        angle =
            (if (a.angle.toDouble() < 0.0) 6.283185307179586 + a.angle.toDouble() % 6.283185307179586 else a.angle.toDouble()) % 6.283185307179586
    }

    constructor(q: Quaternionf) {
        val acos = JomlMath.safeAcos(q.w)
        val invSqrt = JomlMath.invsqrt(1f - q.w * q.w)
        if (java.lang.Float.isInfinite(invSqrt)) {
            x = 0.0
            y = 0.0
            z = 1.0
        } else {
            x = (q.x * invSqrt).toDouble()
            y = (q.y * invSqrt).toDouble()
            z = (q.z * invSqrt).toDouble()
        }
        angle = (acos + acos).toDouble()
    }

    constructor(q: Quaterniond) {
        val acos = JomlMath.safeAcos(q.w)
        val invSqrt = JomlMath.invsqrt(1.0 - q.w * q.w)
        if (java.lang.Double.isInfinite(invSqrt)) {
            x = 0.0
            y = 0.0
            z = 1.0
        } else {
            x = q.x * invSqrt
            y = q.y * invSqrt
            z = q.z * invSqrt
        }
        angle = acos + acos
    }

    constructor(angle: Double, x: Double, y: Double, z: Double) {
        this.x = x
        this.y = y
        this.z = z
        this.angle = (if (angle < 0.0) 6.283185307179586 + angle % 6.283185307179586 else angle) % 6.283185307179586
    }

    constructor(angle: Double, v: Vector3d) : this(angle, v.x, v.y, v.z) {}
    constructor(angle: Double, v: Vector3f) : this(angle, v.x.toDouble(), v.y.toDouble(), v.z.toDouble()) {}

    fun set(a: AxisAngle4d): AxisAngle4d {
        x = a.x
        y = a.y
        z = a.z
        angle = (if (a.angle < 0.0) 6.283185307179586 + a.angle % 6.283185307179586 else a.angle) % 6.283185307179586
        return this
    }

    fun set(a: AxisAngle4f): AxisAngle4d {
        x = a.x.toDouble()
        y = a.y.toDouble()
        z = a.z.toDouble()
        angle =
            (if (a.angle.toDouble() < 0.0) 6.283185307179586 + a.angle.toDouble() % 6.283185307179586 else a.angle.toDouble()) % 6.283185307179586
        return this
    }

    operator fun set(angle: Double, x: Double, y: Double, z: Double): AxisAngle4d {
        this.x = x
        this.y = y
        this.z = z
        this.angle = (if (angle < 0.0) 6.283185307179586 + angle % 6.283185307179586 else angle) % 6.283185307179586
        return this
    }

    operator fun set(angle: Double, v: Vector3d): AxisAngle4d {
        return this.set(angle, v.x, v.y, v.z)
    }

    operator fun set(angle: Double, v: Vector3f): AxisAngle4d {
        return this.set(angle, v.x.toDouble(), v.y.toDouble(), v.z.toDouble())
    }

    fun set(q: Quaternionf): AxisAngle4d {
        val acos = JomlMath.safeAcos(q.w)
        val invSqrt = JomlMath.invsqrt(1f - q.w * q.w)
        if (java.lang.Float.isInfinite(invSqrt)) {
            x = 0.0
            y = 0.0
            z = 1.0
        } else {
            x = (q.x * invSqrt).toDouble()
            y = (q.y * invSqrt).toDouble()
            z = (q.z * invSqrt).toDouble()
        }
        angle = (acos + acos).toDouble()
        return this
    }

    fun set(q: Quaterniond): AxisAngle4d {
        val acos = JomlMath.safeAcos(q.w)
        val invSqrt = JomlMath.invsqrt(1.0 - q.w * q.w)
        if (java.lang.Double.isInfinite(invSqrt)) {
            x = 0.0
            y = 0.0
            z = 1.0
        } else {
            x = q.x * invSqrt
            y = q.y * invSqrt
            z = q.z * invSqrt
        }
        angle = acos + acos
        return this
    }

    fun set(m: Matrix3f): AxisAngle4d {
        var nm00 = m.m00.toDouble()
        var nm01 = m.m01.toDouble()
        var nm02 = m.m02.toDouble()
        var nm10 = m.m10.toDouble()
        var nm11 = m.m11.toDouble()
        var nm12 = m.m12.toDouble()
        var nm20 = m.m20.toDouble()
        var nm21 = m.m21.toDouble()
        var nm22 = m.m22.toDouble()
        val lenX = JomlMath.invsqrt(m.m00 * m.m00 + m.m01 * m.m01 + m.m02 * m.m02).toDouble()
        val lenY = JomlMath.invsqrt(m.m10 * m.m10 + m.m11 * m.m11 + m.m12 * m.m12).toDouble()
        val lenZ = JomlMath.invsqrt(m.m20 * m.m20 + m.m21 * m.m21 + m.m22 * m.m22).toDouble()
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
                x = 0.0
                y = 0.0
                z = 1.0
                angle = 0.0
            } else {
                angle = java.lang.Math.PI
                xx = (nm00 + 1.0) / 2.0
                val yy = (nm11 + 1.0) / 2.0
                val zz = (nm22 + 1.0) / 2.0
                val xy = (nm10 + nm01) / 4.0
                val xz = (nm20 + nm02) / 4.0
                val yz = (nm21 + nm12) / 4.0
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
            angle = JomlMath.safeAcos((nm00 + nm11 + nm22 - 1.0) / 2.0)
            x = (nm12 - nm21) / xx
            y = (nm20 - nm02) / xx
            z = (nm01 - nm10) / xx
        }
        return this
    }

    fun set(m: Matrix3d): AxisAngle4d {
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
                x = 0.0
                y = 0.0
                z = 1.0
                angle = 0.0
            } else {
                angle = java.lang.Math.PI
                xx = (nm00 + 1.0) / 2.0
                val yy = (nm11 + 1.0) / 2.0
                val zz = (nm22 + 1.0) / 2.0
                val xy = (nm10 + nm01) / 4.0
                val xz = (nm20 + nm02) / 4.0
                val yz = (nm21 + nm12) / 4.0
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
            angle = JomlMath.safeAcos((nm00 + nm11 + nm22 - 1.0) / 2.0)
            x = (nm12 - nm21) / xx
            y = (nm20 - nm02) / xx
            z = (nm01 - nm10) / xx
        }
        return this
    }

    fun set(m: Matrix4f): AxisAngle4d {
        var nm00 = m.m00.toDouble()
        var nm01 = m.m01.toDouble()
        var nm02 = m.m02.toDouble()
        var nm10 = m.m10.toDouble()
        var nm11 = m.m11.toDouble()
        var nm12 = m.m12.toDouble()
        var nm20 = m.m20.toDouble()
        var nm21 = m.m21.toDouble()
        var nm22 = m.m22.toDouble()
        val lenX = JomlMath.invsqrt(m.m00 * m.m00 + m.m01 * m.m01 + m.m02 * m.m02).toDouble()
        val lenY = JomlMath.invsqrt(m.m10 * m.m10 + m.m11 * m.m11 + m.m12 * m.m12).toDouble()
        val lenZ = JomlMath.invsqrt(m.m20 * m.m20 + m.m21 * m.m21 + m.m22 * m.m22).toDouble()
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
            if (abs(nm10 + nm01) < epsilon2 && abs(nm20 + nm02) < epsilon2 && abs(
                    nm21 + nm12
                ) < epsilon2 && abs(nm00 + nm11 + nm22 - 3.0) < epsilon2
            ) {
                x = 0.0
                y = 0.0
                z = 1.0
                angle = 0.0
            } else {
                angle = java.lang.Math.PI
                xx = (nm00 + 1.0) / 2.0
                val yy = (nm11 + 1.0) / 2.0
                val zz = (nm22 + 1.0) / 2.0
                val xy = (nm10 + nm01) / 4.0
                val xz = (nm20 + nm02) / 4.0
                val yz = (nm21 + nm12) / 4.0
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
            angle = JomlMath.safeAcos((nm00 + nm11 + nm22 - 1.0) / 2.0)
            x = (nm12 - nm21) / xx
            y = (nm20 - nm02) / xx
            z = (nm01 - nm10) / xx
        }
        return this
    }

    fun set(m: Matrix4x3f): AxisAngle4d {
        var nm00 = m.m00.toDouble()
        var nm01 = m.m01.toDouble()
        var nm02 = m.m02.toDouble()
        var nm10 = m.m10.toDouble()
        var nm11 = m.m11.toDouble()
        var nm12 = m.m12.toDouble()
        var nm20 = m.m20.toDouble()
        var nm21 = m.m21.toDouble()
        var nm22 = m.m22.toDouble()
        val lenX = JomlMath.invsqrt(m.m00 * m.m00 + m.m01 * m.m01 + m.m02 * m.m02).toDouble()
        val lenY = JomlMath.invsqrt(m.m10 * m.m10 + m.m11 * m.m11 + m.m12 * m.m12).toDouble()
        val lenZ = JomlMath.invsqrt(m.m20 * m.m20 + m.m21 * m.m21 + m.m22 * m.m22).toDouble()
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
                x = 0.0
                y = 0.0
                z = 1.0
                angle = 0.0
            } else {
                angle = java.lang.Math.PI
                xx = (nm00 + 1.0) / 2.0
                val yy = (nm11 + 1.0) / 2.0
                val zz = (nm22 + 1.0) / 2.0
                val xy = (nm10 + nm01) / 4.0
                val xz = (nm20 + nm02) / 4.0
                val yz = (nm21 + nm12) / 4.0
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
            angle = JomlMath.safeAcos((nm00 + nm11 + nm22 - 1.0) / 2.0)
            x = (nm12 - nm21) / xx
            y = (nm20 - nm02) / xx
            z = (nm01 - nm10) / xx
        }
        return this
    }

    fun set(m: Matrix4d): AxisAngle4d {
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
                x = 0.0
                y = 0.0
                z = 1.0
                angle = 0.0
            } else {
                angle = java.lang.Math.PI
                xx = (nm00 + 1.0) / 2.0
                val yy = (nm11 + 1.0) / 2.0
                val zz = (nm22 + 1.0) / 2.0
                val xy = (nm10 + nm01) / 4.0
                val xz = (nm20 + nm02) / 4.0
                val yz = (nm21 + nm12) / 4.0
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
            angle = JomlMath.safeAcos((nm00 + nm11 + nm22 - 1.0) / 2.0)
            x = (nm12 - nm21) / xx
            y = (nm20 - nm02) / xx
            z = (nm01 - nm10) / xx
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

    fun normalize(): AxisAngle4d {
        val invLength = JomlMath.invsqrt(x * x + y * y + z * z)
        x *= invLength
        y *= invLength
        z *= invLength
        return this
    }

    fun rotate(ang: Double): AxisAngle4d {
        angle += ang
        angle = (if (angle < 0.0) 6.283185307179586 + angle % 6.283185307179586 else angle) % 6.283185307179586
        return this
    }

    @JvmOverloads
    fun transform(v: Vector3d, dst: Vector3d = v): Vector3d {
        val sin = sin(angle)
        val cos = cos(angle)
        val dot = x * v.x + y * v.y + z * v.z
        dst.set(
            v.x * cos + sin * (y * v.z - z * v.y) + (1.0 - cos) * dot * x,
            v.y * cos + sin * (z * v.x - x * v.z) + (1.0 - cos) * dot * y,
            v.z * cos + sin * (x * v.y - y * v.x) + (1.0 - cos) * dot * z
        )
        return dst
    }

    @JvmOverloads
    fun transform(v: Vector3f, dst: Vector3f = v): Vector3f {
        val sin = sin(angle)
        val cos = cos(angle)
        val dot = x * v.x.toDouble() + y * v.y.toDouble() + z * v.z.toDouble()
        dst.set(
            (v.x.toDouble() * cos + sin * (y * v.z.toDouble() - z * v.y.toDouble()) + (1.0 - cos) * dot * x).toFloat(),
            (v.y.toDouble() * cos + sin * (z * v.x.toDouble() - x * v.z.toDouble()) + (1.0 - cos) * dot * y).toFloat(),
            (v.z.toDouble() * cos + sin * (x * v.y.toDouble() - y * v.x.toDouble()) + (1.0 - cos) * dot * z).toFloat()
        )
        return dst
    }

    @JvmOverloads
    fun transform(v: Vector4d, dst: Vector4d = v): Vector4d {
        val sin = sin(angle)
        val cos = cos(angle)
        val dot = x * v.x + y * v.y + z * v.z
        dst[v.x * cos + sin * (y * v.z - z * v.y) + (1.0 - cos) * dot * x, v.y * cos + sin * (z * v.x - x * v.z) + (1.0 - cos) * dot * y, v.z * cos + sin * (x * v.y - y * v.x) + (1.0 - cos) * dot * z] =
            dst.w
        return dst
    }

    override fun toString(): String {
        return "($x,$y,$z <| $angle)"
    }

    override fun hashCode(): Int {
        var result = 1
        var temp =
            java.lang.Double.doubleToLongBits((if (angle < 0.0) 6.283185307179586 + angle % 6.283185307179586 else angle) % 6.283185307179586)
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = java.lang.Double.doubleToLongBits(x)
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = java.lang.Double.doubleToLongBits(y)
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        temp = java.lang.Double.doubleToLongBits(z)
        result = 31 * result + (temp xor (temp ushr 32)).toInt()
        return result
    }

    override fun equals(obj: Any?): Boolean {
        return if (this === obj) {
            true
        } else if (obj == null) {
            false
        } else if (this.javaClass != obj.javaClass) {
            false
        } else {
            val other = obj as AxisAngle4d
            if (java.lang.Double.doubleToLongBits((if (angle < 0.0) 6.283185307179586 + angle % 6.283185307179586 else angle) % 6.283185307179586) != java.lang.Double.doubleToLongBits(
                    (if (other.angle < 0.0) 6.283185307179586 + other.angle % 6.283185307179586 else other.angle) % 6.283185307179586
                )
            ) {
                false
            } else if (java.lang.Double.doubleToLongBits(x) != java.lang.Double.doubleToLongBits(other.x)) {
                false
            } else if (java.lang.Double.doubleToLongBits(y) != java.lang.Double.doubleToLongBits(other.y)) {
                false
            } else {
                java.lang.Double.doubleToLongBits(z) == java.lang.Double.doubleToLongBits(other.z)
            }
        }
    }

    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any {
        return super.clone()
    }
}