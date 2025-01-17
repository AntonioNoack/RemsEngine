package org.joml

import org.joml.JomlMath.hash
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sqrt

class AxisAngle4d(
    @JvmField var angle: Double,
    @JvmField var x: Double,
    @JvmField var y: Double,
    @JvmField var z: Double
) : Vector {

    constructor() : this(0.0, 0.0, 0.0, 1.0)
    constructor(a: AxisAngle4d) : this(a.angle, a.x, a.y, a.z)
    constructor(a: AxisAngle4f) : this(a.angle.toDouble(), a.x.toDouble(), a.y.toDouble(), a.z.toDouble())

    constructor(angle: Double, v: Vector3d) : this(angle, v.x, v.y, v.z)
    constructor(angle: Double, v: Vector3f) : this(angle, v.x.toDouble(), v.y.toDouble(), v.z.toDouble())

    constructor(q: Quaternionf) : this() {
        set(q)
    }

    constructor(q: Quaterniond) : this() {
        set(q)
    }

    init {
        angle = posMod(angle)
    }

    fun set(a: AxisAngle4d): AxisAngle4d = set(a.angle, a.x, a.y, a.z)
    fun set(a: AxisAngle4f): AxisAngle4d = set(a.angle.toDouble(), a.x.toDouble(), a.y.toDouble(), a.z.toDouble())
    fun set(angle: Double, x: Double, y: Double, z: Double): AxisAngle4d {
        this.x = x
        this.y = y
        this.z = z
        this.angle = posMod(angle)
        return this
    }

    fun set(angle: Double, v: Vector3d): AxisAngle4d = set(angle, v.x, v.y, v.z)

    fun setByQuaternion(qx: Double, qy: Double, qz: Double, qw: Double): AxisAngle4d {
        val acos = JomlMath.safeAcos(qw)
        val invSqrt = JomlMath.invsqrt(1.0 - qw * qw)
        if (invSqrt.isInfinite()) {
            x = 0.0
            y = 0.0
            z = 1.0
        } else {
            x = qx * invSqrt
            y = qy * invSqrt
            z = qz * invSqrt
        }
        angle = acos + acos
        return this
    }

    fun set(q: Quaternionf): AxisAngle4d =
        setByQuaternion(q.x.toDouble(), q.y.toDouble(), q.z.toDouble(), q.w.toDouble())

    fun set(q: Quaterniond): AxisAngle4d = setByQuaternion(q.x, q.y, q.z, q.w)

    fun set(
        m00: Float, m01: Float, m02: Float,
        m10: Float, m11: Float, m12: Float,
        m20: Float, m21: Float, m22: Float
    ): AxisAngle4d {
        return set(
            m00.toDouble(), m01.toDouble(), m02.toDouble(),
            m10.toDouble(), m11.toDouble(), m12.toDouble(),
            m20.toDouble(), m21.toDouble(), m22.toDouble()
        )
    }

    fun set(
        m00: Double, m01: Double, m02: Double,
        m10: Double, m11: Double, m12: Double,
        m20: Double, m21: Double, m22: Double
    ): AxisAngle4d {
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
                angle = PI
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

    fun set(m: Matrix3f): AxisAngle4d {
        return set(
            m.m00, m.m01, m.m02,
            m.m10, m.m11, m.m12,
            m.m20, m.m21, m.m22
        )
    }

    fun set(m: Matrix3d): AxisAngle4d {
        return set(
            m.m00, m.m01, m.m02,
            m.m10, m.m11, m.m12,
            m.m20, m.m21, m.m22
        )
    }

    fun set(m: Matrix4f): AxisAngle4d {
        return set(
            m.m00, m.m01, m.m02,
            m.m10, m.m11, m.m12,
            m.m20, m.m21, m.m22
        )
    }

    fun set(m: Matrix4x3f): AxisAngle4d {
        return set(
            m.m00, m.m01, m.m02,
            m.m10, m.m11, m.m12,
            m.m20, m.m21, m.m22
        )
    }

    fun set(m: Matrix4x3d): AxisAngle4d {
        return set(
            m.m00, m.m01, m.m02,
            m.m10, m.m11, m.m12,
            m.m20, m.m21, m.m22
        )
    }

    fun set(m: Matrix4d): AxisAngle4d {
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

    fun get(m: Matrix4d): Matrix4d {
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
        }
    }

    override fun setComp(i: Int, v: Double) {
        when (i) {
            0 -> angle = v
            1 -> x = v
            2 -> y = v
            else -> z = v
        }
    }

    fun normalize(): AxisAngle4d {
        val invLength = JomlMath.invsqrt(x * x + y * y + z * z)
        x *= invLength
        y *= invLength
        z *= invLength
        return this
    }

    fun rotate(ang: Double): AxisAngle4d {
        angle = posMod(angle + ang)
        return this
    }

    @JvmOverloads
    fun transform(v: Vector3d, dst: Vector3d = v): Vector3d {
        return v.rotateAxis(angle, x, y, z, dst)
    }

    @JvmOverloads
    fun transform(v: Vector3f, dst: Vector3f = v): Vector3f {
        return v.rotateAxis(angle.toFloat(), x.toFloat(), y.toFloat(), z.toFloat(), dst)
    }

    @JvmOverloads
    fun transform(v: Vector4d, dst: Vector4d = v): Vector4d {
        return v.rotateAxis(angle, x, y, z, dst)
    }

    override fun toString(): String {
        return "($x,$y,$z <| $angle)"
    }

    private fun posMod(value: Double): Double {
        val tau = PI * 2.0
        val mod = value % tau
        return if (value < 0.0) {
            (tau + mod)
        } else mod
    }

    override fun hashCode(): Int {
        var result = hash(posMod(angle))
        result = 31 * result + hash(x)
        result = 31 * result + hash(y)
        result = 31 * result + hash(z)
        return result
    }

    override fun equals(other: Any?): Boolean {
        return if (this === other) true
        else if (other !is AxisAngle4d) false
        else x == other.x && y == other.y && z == other.z && posMod(angle) == posMod(other.angle)
    }
}