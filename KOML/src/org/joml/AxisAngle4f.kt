package org.joml

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class AxisAngle4f {
    var angle = 0f
    var x = 0f
    var y = 0f
    var z = 0f

    constructor() {
        z = 1.0f
    }

    constructor(a: AxisAngle4f) {
        x = a.x
        y = a.y
        z = a.z
        angle = a.angle
    }

    constructor(q: Quaternionf) {
        val acos = JomlMath.safeAcos(q.w)
        val invSqrt = JomlMath.invsqrt(1.0f - q.w * q.w)
        if (java.lang.Float.isInfinite(invSqrt)) {
            x = 0.0f
            y = 0.0f
            z = 1.0f
        } else {
            x = q.x * invSqrt
            y = q.y * invSqrt
            z = q.z * invSqrt
        }
        angle = acos + acos
    }

    constructor(angle: Float, x: Float, y: Float, z: Float) {
        this.x = x
        this.y = y
        this.z = z
        this.angle =
            ((if (angle.toDouble() < 0.0) 6.283185307179586 + angle.toDouble() % 6.283185307179586 else angle.toDouble()) % 6.283185307179586).toFloat()
    }

    constructor(angle: Float, v: Vector3f) : this(angle, v.x, v.y, v.z) {}

    fun set(a: AxisAngle4f): AxisAngle4f {
        x = a.x
        y = a.y
        z = a.z
        angle = a.angle
        angle =
            ((if (angle.toDouble() < 0.0) 6.283185307179586 + angle.toDouble() % 6.283185307179586 else angle.toDouble()) % 6.283185307179586).toFloat()
        return this
    }

    fun set(a: AxisAngle4d): AxisAngle4f {
        x = a.x.toFloat()
        y = a.y.toFloat()
        z = a.z.toFloat()
        angle = a.angle.toFloat()
        angle =
            ((if (angle.toDouble() < 0.0) 6.283185307179586 + angle.toDouble() % 6.283185307179586 else angle.toDouble()) % 6.283185307179586).toFloat()
        return this
    }

    operator fun set(angle: Float, x: Float, y: Float, z: Float): AxisAngle4f {
        this.x = x
        this.y = y
        this.z = z
        this.angle =
            ((if (angle.toDouble() < 0.0) 6.283185307179586 + angle.toDouble() % 6.283185307179586 else angle.toDouble()) % 6.283185307179586).toFloat()
        return this
    }

    operator fun set(angle: Float, v: Vector3f): AxisAngle4f {
        return this.set(angle, v.x, v.y, v.z)
    }

    fun set(q: Quaternionf): AxisAngle4f {
        val acos = JomlMath.safeAcos(q.w)
        val invSqrt = JomlMath.invsqrt(1.0f - q.w * q.w)
        if (java.lang.Float.isInfinite(invSqrt)) {
            x = 0.0f
            y = 0.0f
            z = 1.0f
        } else {
            x = q.x * invSqrt
            y = q.y * invSqrt
            z = q.z * invSqrt
        }
        angle = acos + acos
        return this
    }

    fun set(q: Quaterniond): AxisAngle4f {
        val acos = JomlMath.safeAcos(q.w)
        val invSqrt = JomlMath.invsqrt(1.0 - q.w * q.w)
        if (java.lang.Double.isInfinite(invSqrt)) {
            x = 0.0f
            y = 0.0f
            z = 1.0f
        } else {
            x = (q.x * invSqrt).toFloat()
            y = (q.y * invSqrt).toFloat()
            z = (q.z * invSqrt).toFloat()
        }
        angle = (acos + acos).toFloat()
        return this
    }

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

    operator fun get(dest: AxisAngle4d): AxisAngle4d {
        return dest.set(this)
    }

    operator fun get(dest: AxisAngle4f): AxisAngle4f {
        return dest.set(this)
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
    fun transform(v: Vector3f, dest: Vector3f = v): Vector3f {
        val sin = sin(angle)
        val cos = cos(angle)
        val dot = x * v.x + y * v.y + z * v.z
        dest[(v.x.toDouble() * cos + sin * (y * v.z - z * v.y).toDouble() + (1.0 - cos) * dot.toDouble() * x.toDouble()).toFloat(), (v.y.toDouble() * cos + sin * (z * v.x - x * v.z).toDouble() + (1.0 - cos) * dot.toDouble() * y.toDouble()).toFloat()] =
            (v.z.toDouble() * cos + sin * (x * v.y - y * v.x).toDouble() + (1.0 - cos) * dot.toDouble() * z.toDouble()).toFloat()
        return dest
    }

    @JvmOverloads
    fun transform(v: Vector4f, dest: Vector4f = v): Vector4f {
        val sin = sin(angle).toDouble()
        val cos = cos(angle.toDouble())
        val dot = x * v.x + y * v.y + z * v.z
        dest[(v.x.toDouble() * cos + sin * (y * v.z - z * v.y).toDouble() + (1.0 - cos) * dot.toDouble() * x.toDouble()).toFloat(), (v.y.toDouble() * cos + sin * (z * v.x - x * v.z).toDouble() + (1.0 - cos) * dot.toDouble() * y.toDouble()).toFloat(), (v.z.toDouble() * cos + sin * (x * v.y - y * v.x).toDouble() + (1.0 - cos) * dot.toDouble() * z.toDouble()).toFloat()] =
            dest.w
        return dest
    }

    override fun toString(): String {
        return "($x,$y,$z <| $angle)"
    }

    override fun hashCode(): Int {
        var result = 1
        val nangle =
            ((if (angle.toDouble() < 0.0) 6.283185307179586 + angle.toDouble() % 6.283185307179586 else angle.toDouble()) % 6.283185307179586).toFloat()
        result = 31 * result + java.lang.Float.floatToIntBits(nangle)
        result = 31 * result + java.lang.Float.floatToIntBits(x)
        result = 31 * result + java.lang.Float.floatToIntBits(y)
        result = 31 * result + java.lang.Float.floatToIntBits(z)
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
            val other = obj as AxisAngle4f
            val nangle =
                ((if (angle.toDouble() < 0.0) 6.283185307179586 + angle.toDouble() % 6.283185307179586 else angle.toDouble()) % 6.283185307179586).toFloat()
            val nangleOther =
                ((if (other.angle.toDouble() < 0.0) 6.283185307179586 + other.angle.toDouble() % 6.283185307179586 else other.angle.toDouble()) % 6.283185307179586).toFloat()
            if (java.lang.Float.floatToIntBits(nangle) != java.lang.Float.floatToIntBits(nangleOther)) {
                false
            } else if (java.lang.Float.floatToIntBits(x) != java.lang.Float.floatToIntBits(other.x)) {
                false
            } else if (java.lang.Float.floatToIntBits(y) != java.lang.Float.floatToIntBits(other.y)) {
                false
            } else {
                java.lang.Float.floatToIntBits(z) == java.lang.Float.floatToIntBits(other.z)
            }
        }
    }
}