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
) : Vector {

    constructor() : this(0f, 0f, 0f, 1f)
    constructor(a: AxisAngle4f) : this(a.angle, a.x, a.y, a.z)
    constructor(a: AxisAngle4d) : this(a.angle.toFloat(), a.x.toFloat(), a.y.toFloat(), a.z.toFloat())
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

    fun set(
        m00: Float, m01: Float, m02: Float,
        m10: Float, m11: Float, m12: Float,
        m20: Float, m21: Float, m22: Float
    ): AxisAngle4f {
        val lenX = JomlMath.invLength(m00, m01, m02)
        val lenY = JomlMath.invLength(m10, m11, m12)
        val lenZ = JomlMath.invLength(m20, m21, m22)
        val nm00 = m00 * lenX
        val nm01 = m01 * lenX
        val nm02 = m02 * lenX
        val nm10 = m10 * lenY
        val nm11 = m11 * lenY
        val nm12 = m12 * lenY
        val nm20 = m20 * lenZ
        val nm21 = m21 * lenZ
        val nm22 = m22 * lenZ
        val epsilon = 1.0E-4f
        val epsilon2 = 0.001f
        val xx: Float
        if (abs(nm10 - nm01) < epsilon && abs(nm20 - nm02) < epsilon && abs(nm21 - nm12) < epsilon) {
            if (abs(nm10 + nm01) < epsilon2 && abs(nm20 + nm02) < epsilon2 && abs(nm21 + nm12) < epsilon2 && abs(
                    nm00 + nm11 + nm22 - 3.0
                ) < epsilon2
            ) {
                x = 0f
                y = 0f
                z = 1f
                angle = 0f
            } else {
                angle = PI.toFloat()
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


    fun set(
        m00: Double, m01: Double, m02: Double,
        m10: Double, m11: Double, m12: Double,
        m20: Double, m21: Double, m22: Double
    ): AxisAngle4f {
        return set(
            m00.toFloat(), m01.toFloat(), m02.toFloat(),
            m10.toFloat(), m11.toFloat(), m12.toFloat(),
            m20.toFloat(), m21.toFloat(), m22.toFloat()
        )
    }

    fun set(m: Matrix3f): AxisAngle4f {
        return set(
            m.m00, m.m01, m.m02,
            m.m10, m.m11, m.m12,
            m.m20, m.m21, m.m22
        )
    }

    fun set(m: Matrix3d): AxisAngle4f {
        return set(
            m.m00, m.m01, m.m02,
            m.m10, m.m11, m.m12,
            m.m20, m.m21, m.m22
        )
    }

    fun set(m: Matrix4f): AxisAngle4f {
        return set(
            m.m00, m.m01, m.m02,
            m.m10, m.m11, m.m12,
            m.m20, m.m21, m.m22
        )
    }

    fun set(m: Matrix4x3f): AxisAngle4f {
        return set(
            m.m00, m.m01, m.m02,
            m.m10, m.m11, m.m12,
            m.m20, m.m21, m.m22
        )
    }

    fun set(m: Matrix4x3): AxisAngle4f {
        return set(
            m.m00, m.m01, m.m02,
            m.m10, m.m11, m.m12,
            m.m20, m.m21, m.m22
        )
    }

    fun set(m: Matrix4d): AxisAngle4f {
        return set(
            m.m00, m.m01, m.m02,
            m.m10, m.m11, m.m12,
            m.m20, m.m21, m.m22
        )
    }

    fun get(q: Quaternionf): Quaternionf {
        return q.set(this)
    }

    fun get(q: Quaterniond): Quaterniond {
        return q.set(this)
    }

    fun get(m: Matrix4f): Matrix4f {
        return m.set(this)
    }

    fun get(m: Matrix3f): Matrix3f {
        return m.set(this)
    }

    fun get(m: Matrix3d): Matrix3d {
        return m.set(this)
    }

    fun get(dst: AxisAngle4d): AxisAngle4d {
        return dst.set(this)
    }

    fun get(dst: AxisAngle4f): AxisAngle4f {
        return dst.set(this)
    }

    override val numComponents: Int get() = 4

    override fun getComp(i: Int): Double {
        return when (i) {
            0 -> angle
            1 -> x
            2 -> y
            else -> z
        }.toDouble()
    }

    override fun setComp(i: Int, v: Double) {
        when (i) {
            0 -> angle = v.toFloat()
            1 -> x = v.toFloat()
            2 -> y = v.toFloat()
            else -> z = v.toFloat()
        }
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
        val base = a % tau
        return if (base < 0.0) {
            (tau + base)
        } else {
            base
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