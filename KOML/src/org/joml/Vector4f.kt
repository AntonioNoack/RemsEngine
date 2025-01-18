package org.joml

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt

@Suppress("unused")
open class Vector4f(
    @JvmField var x: Float,
    @JvmField var y: Float,
    @JvmField var z: Float,
    @JvmField var w: Float
) : Vector {

    constructor() : this(0f, 0f, 0f, 1f)
    constructor(v: Vector4f) : this(v.x, v.y, v.z, v.w)
    constructor(v: Vector4d) : this(v.x.toFloat(), v.y.toFloat(), v.z.toFloat(), v.w.toFloat())
    constructor(v: Vector4i) : this(v.x.toFloat(), v.y.toFloat(), v.z.toFloat(), v.w.toFloat())
    constructor(v: Vector3f, w: Float) : this(v.x, v.y, v.z, w)
    constructor(v: Vector2f, z: Float, w: Float) : this(v.x, v.y, z, w)
    constructor(d: Float) : this(d, d, d, d)
    constructor(v: FloatArray, i: Int = 0) : this(v[i], v[i + 1], v[i + 2], v[i + 3])

    override val numComponents: Int get() = 4
    override fun getComp(i: Int): Double = get(i).toDouble()
    override fun setComp(i: Int, v: Double) {
        setComponent(i, v.toFloat())
    }

    operator fun component1() = x
    operator fun component2() = y
    operator fun component3() = z
    operator fun component4() = w

    fun set(v: Vector4f) = set(v.x, v.y, v.z, v.w)
    fun set(v: Vector4i) = set(v.x.toFloat(), v.y.toFloat(), v.z.toFloat(), v.w.toFloat())
    fun set(v: Vector4d) = set(v.x.toFloat(), v.y.toFloat(), v.z.toFloat(), v.w.toFloat())
    fun set(v: Vector3f, w: Float) = set(v.x, v.y, v.z, w)
    fun set(v: Vector3i, w: Float) = set(v.x.toFloat(), v.y.toFloat(), v.z.toFloat(), w)
    fun set(v: Vector2f, z: Float, w: Float) = set(v.x, v.y, z, w)
    fun set(v: Vector2i, z: Float, w: Float) = set(v.x.toFloat(), v.y.toFloat(), z, w)
    fun set(d: Float) = set(d, d, d, d)
    fun set(x: Float, y: Float, z: Float) = set(x, y, z, this.w)
    fun set(d: Double) = set(d.toFloat())
    fun set(x: Double, y: Double, z: Double, w: Double) = set(x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat())
    fun set(v: FloatArray, i: Int = 0) = set(v[i], v[i + 1], v[i + 2], v[i + 3])
    fun set(x: Float, y: Float, z: Float, w: Float): Vector4f {
        this.x = x
        this.y = y
        this.z = z
        this.w = w
        return this
    }

    operator fun set(component: Int, value: Float) = setComponent(component, value)
    fun setComponent(component: Int, value: Float): Vector4f {
        when (component) {
            0 -> x = value
            1 -> y = value
            2 -> z = value
            else -> w = value
        }
        return this
    }

    fun sub(v: Vector4f, dst: Vector4f = this): Vector4f {
        return sub(v.x, v.y, v.z, v.w, dst)
    }

    @JvmOverloads
    fun sub(x: Float, y: Float, z: Float, w: Float, dst: Vector4f = this): Vector4f {
        dst.x = this.x - x
        dst.y = this.y - y
        dst.z = this.z - z
        dst.w = this.w - w
        return dst
    }

    @JvmOverloads
    fun add(v: Vector4f, dst: Vector4f = this): Vector4f {
        return add(v.x, v.y, v.z, v.w, dst)
    }

    @JvmOverloads
    fun add(x: Float, y: Float, z: Float, w: Float, dst: Vector4f = this): Vector4f {
        dst.x = this.x + x
        dst.y = this.y + y
        dst.z = this.z + z
        dst.w = this.w + w
        return dst
    }

    @JvmOverloads
    fun fma(a: Vector4f, b: Vector4f, dst: Vector4f = this): Vector4f {
        dst.x = a.x * b.x + x
        dst.y = a.y * b.y + y
        dst.z = a.z * b.z + z
        dst.w = a.w * b.w + w
        return dst
    }

    @JvmOverloads
    fun fma(a: Float, b: Vector4f, dst: Vector4f = this): Vector4f {
        dst.x = a * b.x + x
        dst.y = a * b.y + y
        dst.z = a * b.z + z
        dst.w = a * b.w + w
        return dst
    }

    @JvmOverloads
    fun mulAdd3(a: Vector4f, b: Vector4f, dst: Vector4f = this): Vector4f {
        dst.x = x * a.x + b.x
        dst.y = y * a.y + b.y
        dst.z = z * a.z + b.z
        return dst
    }

    @JvmOverloads
    fun mulAdd3(a: Float, b: Vector4f, dst: Vector4f = this): Vector4f {
        dst.x = x * a + b.x
        dst.y = y * a + b.y
        dst.z = z * a + b.z
        return dst
    }

    @JvmOverloads
    fun mulAdd(a: Vector4f, b: Vector4f, dst: Vector4f = this): Vector4f {
        mulAdd3(a, b, dst)
        dst.w = w * a.w + b.w
        return dst
    }

    @JvmOverloads
    fun mulAdd(a: Float, b: Vector4f, dst: Vector4f = this): Vector4f {
        mulAdd3(a, b, dst)
        dst.w = w * a + b.w
        return dst
    }

    @JvmOverloads
    fun mul(v: Vector4f, dst: Vector4f = this): Vector4f {
        return dst.set(x * v.x, y * v.y, z * v.z, w * v.w)
    }

    @JvmOverloads
    fun div(v: Vector4f, dst: Vector4f = this): Vector4f {
        return dst.set(x / v.x, y / v.y, z / v.z, w / v.w)
    }

    @JvmOverloads
    fun mul(mat: Matrix4f, dst: Vector4f = this): Vector4f {
        return if (mat.properties() and 2 != 0) mulAffine(mat, dst) else mulGeneric(mat, dst)
    }

    @JvmOverloads
    fun mulTranspose(mat: Matrix4f, dst: Vector4f = this): Vector4f {
        return if (mat.properties() and 2 != 0) mulAffineTranspose(mat, dst) else mulGenericTranspose(mat, dst)
    }

    @JvmOverloads
    fun mulAffine(mat: Matrix4f, dst: Vector4f = this): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        dst.x = mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w
        dst.y = mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w
        dst.z = mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w
        dst.w = w
        return dst
    }

    private fun mulGeneric(mat: Matrix4f, dst: Vector4f = this): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        dst.x = mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w
        dst.y = mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w
        dst.z = mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w
        dst.w = mat.m03 * x + mat.m13 * y + mat.m23 * z + mat.m33 * w
        return dst
    }

    @JvmOverloads
    fun mulAffineTranspose(mat: Matrix4f, dst: Vector4f = this): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        dst.x = mat.m00 * x + mat.m01 * y + mat.m02 * z
        dst.y = mat.m10 * x + mat.m11 * y + mat.m12 * z
        dst.z = mat.m20 * x + mat.m21 * y + mat.m22 * z
        dst.w = mat.m30 * x + mat.m31 * y + mat.m32 * z + w
        return dst
    }

    private fun mulGenericTranspose(mat: Matrix4f, dst: Vector4f = this): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        dst.x = mat.m00 * x + mat.m01 * y + mat.m02 * z + mat.m03 * w
        dst.y = mat.m10 * x + mat.m11 * y + mat.m12 * z + mat.m13 * w
        dst.z = mat.m20 * x + mat.m21 * y + mat.m22 * z + mat.m23 * w
        dst.w = mat.m30 * x + mat.m31 * y + mat.m32 * z + mat.m33 * w
        return dst
    }

    @JvmOverloads
    fun mul(mat: Matrix4x3f, dst: Vector4f = this): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        dst.x = mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w
        dst.y = mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w
        dst.z = mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w
        dst.w = w
        return dst
    }

    @JvmOverloads
    fun mulProject(mat: Matrix4f, dst: Vector4f = this): Vector4f {
        val x = x
        val y = y
        val z = z
        val w = w
        val invW = 1f / (mat.m03 * x + mat.m13 * y + mat.m23 * z + mat.m33 * w)
        dst.x = (mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w) * invW
        dst.y = (mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w) * invW
        dst.z = (mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w) * invW
        dst.w = 1f
        return dst
    }

    fun mulProject(mat: Matrix4f, dst: Vector3f): Vector3f {
        val x = x
        val y = y
        val z = z
        val w = w
        val invW = 1f / (mat.m03 * x + mat.m13 * y + mat.m23 * z + mat.m33 * w)
        dst.x = (mat.m00 * x + mat.m10 * y + mat.m20 * z + mat.m30 * w) * invW
        dst.y = (mat.m01 * x + mat.m11 * y + mat.m21 * z + mat.m31 * w) * invW
        dst.z = (mat.m02 * x + mat.m12 * y + mat.m22 * z + mat.m32 * w) * invW
        return dst
    }

    @JvmOverloads
    fun mul(scalar: Float, dst: Vector4f = this): Vector4f {
        return mul(scalar, scalar, scalar, scalar, dst)
    }

    @JvmOverloads
    fun mul(vx: Float, vy: Float, vz: Float, vw: Float, dst: Vector4f = this): Vector4f {
        return dst.set(x * vx, y * vy, z * vz, w * vw)
    }

    @JvmOverloads
    fun div(scalar: Float, dst: Vector4f = this) = mul(1f / scalar, dst)

    @JvmOverloads
    fun div(vx: Float, vy: Float, vz: Float, vw: Float, dst: Vector4f = this): Vector4f {
        return dst.set(x / vx, y / vy, z / vz, w / vw)
    }

    @JvmOverloads
    fun rotate(quat: Quaternionf, dst: Vector4f = this): Vector4f {
        val tmp = Vector3f(x, y, z).rotate(quat)
        return dst.set(tmp.x, tmp.y, tmp.z)
    }

    @JvmOverloads
    fun rotateInv(quat: Quaternionf, dst: Vector4f = this): Vector4f {
        val tmp = Vector3f(x, y, z).rotateInv(quat)
        return dst.set(tmp.x, tmp.y, tmp.z)
    }

    /**
     * Warning: ax,ay,az must be normalized!
     * */
    fun rotateAbout(angle: Float, ax: Float, ay: Float, az: Float): Vector4f {
        return rotateAxis(angle, ax, ay, az, this)
    }

    /**
     * Warning: ax,ay,az must be normalized!
     * */
    fun rotateAxis(angle: Float, ax: Float, ay: Float, az: Float, dst: Vector4f = this): Vector4f {
        val sin = sin(angle)
        val cos = cos(angle)
        val vx = x
        val vy = y
        val vz = z
        val dot = ax * vx + ay * vy + az * vz
        val invCos = 1f - cos
        return dst.set(
            vx * cos + sin * (ay * vz - az * vy) + invCos * dot * ax,
            vy * cos + sin * (az * vx - ax * vz) + invCos * dot * ay,
            vz * cos + sin * (ax * vy - ay * vx) + invCos * dot * az, w
        )
    }

    @JvmOverloads
    fun rotateX(angle: Float, dst: Vector4f = this): Vector4f {
        val sin = sin(angle)
        val cos = cos(angle)
        val y = y * cos - z * sin
        val z = this.y * sin + z * cos
        return dst.set(x, y, z, w)
    }

    @JvmOverloads
    fun rotateY(angle: Float, dst: Vector4f = this): Vector4f {
        val sin = sin(angle)
        val cos = cos(angle)
        val x = x * cos + z * sin
        val z = -this.x * sin + z * cos
        return dst.set(x, y, z, w)
    }

    @JvmOverloads
    fun rotateZ(angle: Float, dst: Vector4f = this): Vector4f {
        val sin = sin(angle)
        val cos = cos(angle)
        val x = x * cos - y * sin
        val y = this.x * sin + y * cos
        return dst.set(x, y, z, w)
    }

    fun lengthSquared() = x * x + y * y + z * z + w * w
    fun length() = sqrt(lengthSquared())

    @JvmOverloads
    fun normalize(dst: Vector4f = this) = mul(1f / length(), dst)

    @JvmOverloads
    fun normalize(length: Float, dst: Vector4f = this) = mul(length / length(), dst)

    @JvmOverloads
    fun normalize3(dst: Vector4f = this) = mul(1f / length3(), dst)

    @JvmOverloads
    fun safeNormalize(length: Float = 1f): Vector4f {
        normalize(length)
        if (!isFinite) set(0f)
        return this
    }

    fun length3() = sqrt(x * x + y * y + z * z)

    fun distance(v: Vector4f): Float = distance(v.x, v.y, v.z, v.w)
    fun distance(vx: Float, vy: Float, vz: Float, vw: Float): Float = distance(x, y, z, w, vx, vy, vz, vw)
    fun distanceSquared(v: Vector4f): Float = distanceSquared(v.x, v.y, v.z, v.w)
    fun distanceSquared(vx: Float, vy: Float, vz: Float, vw: Float): Float = distanceSquared(x, y, z, w, vx, vy, vz, vw)

    fun dot(v: Vector4f): Float = dot(v.x, v.y, v.z, v.w)
    fun dot(v: Vector4d): Double = dot(v.x, v.y, v.z, v.w)
    fun dot(vx: Float, vy: Float, vz: Float, vw: Float): Float = x * vx + y * vy + z * vz + w * vw
    fun dot(vx: Double, vy: Double, vz: Double, vw: Double): Double = x * vx + y * vy + z * vz + w * vw

    fun angleCos(v: Vector4f): Float = dot(v) / sqrt(lengthSquared() * v.lengthSquared())
    fun angleCos(vx: Float, vy: Float, vz: Float, vw: Float): Float =
        dot(vx, vy, vz, vw) / sqrt(lengthSquared() * lengthSquared(vx, vy, vz, vw))

    fun angle(v: Vector4f): Float {
        var cos = angleCos(v)
        cos = min(cos, 1f)
        cos = max(cos, -1f)
        return acos(cos)
    }

    fun zero(): Vector4f = set(0f)

    @JvmOverloads
    fun negate(dst: Vector4f = this): Vector4f = dst.set(-x, -y, -z, -w)

    override fun toString(): String {
        return "($x,$y,$z,$w)"
    }

    @JvmOverloads
    fun min(v: Vector4f, dst: Vector4f = this): Vector4f {
        return dst.set(min(x, v.x), min(y, v.y), min(z, v.z), min(w, v.w))
    }

    @JvmOverloads
    fun max(v: Vector4f, dst: Vector4f = this): Vector4f {
        return dst.set(max(x, v.x), max(y, v.y), max(z, v.z), max(w, v.w))
    }

    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + w.toRawBits()
        result = 31 * result + x.toRawBits()
        result = 31 * result + y.toRawBits()
        result = 31 * result + z.toRawBits()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        return other is Vector4f && other.x == x && other.y == y && other.z == z && other.w == w
    }

    fun equals(v: Vector4f, delta: Float): Boolean {
        if (v === this) return true
        return Runtime.equals(x, v.x, delta) && Runtime.equals(y, v.y, delta) &&
                Runtime.equals(z, v.z, delta) && Runtime.equals(w, v.w, delta)
    }

    fun equals(x: Float, y: Float, z: Float, w: Float): Boolean {
        return this.x == x && this.y == y && this.z == z && this.w == w
    }

    @JvmOverloads
    fun smoothStep(v: Vector4f, t: Float, dst: Vector4f = this): Vector4f {
        val t2 = t * t
        val t3 = t2 * t
        return dst.set(
            JomlMath.smoothStep(x, v.x, t, t2, t3),
            JomlMath.smoothStep(y, v.y, t, t2, t3),
            JomlMath.smoothStep(z, v.z, t, t2, t3),
            JomlMath.smoothStep(w, v.w, t, t2, t3),
        )
    }

    @JvmOverloads
    fun hermite(t0: Vector4f, v1: Vector4f, t1: Vector4f, t: Float, dst: Vector4f = this): Vector4f {
        val t2 = t * t
        val t3 = t2 * t
        return dst.set(
            JomlMath.hermite(x, t0.x, v1.x, t1.x, t, t2, t3),
            JomlMath.hermite(y, t0.y, v1.y, t1.y, t, t2, t3),
            JomlMath.hermite(z, t0.z, v1.z, t1.z, t, t2, t3),
            JomlMath.hermite(w, t0.w, v1.w, t1.w, t, t2, t3)
        )
    }

    @JvmOverloads
    fun mix(other: Vector4f, t: Float, dst: Vector4f = this): Vector4f {
        return dst.set(
            JomlMath.mix(x, other.x, t),
            JomlMath.mix(y, other.y, t),
            JomlMath.mix(z, other.z, t),
            JomlMath.mix(w, other.w, t)
        )
    }

    @JvmOverloads
    fun lerp(other: Vector4f, t: Float, dst: Vector4f = this): Vector4f {
        return mix(other, t, dst)
    }

    operator fun get(component: Int): Float {
        return when (component) {
            0 -> x
            1 -> y
            2 -> z
            else -> w
        }
    }

    fun get(dst: Vector4f = this): Vector4f {
        return dst.set(x, y, z, w)
    }

    fun get(dst: Vector4d): Vector4d {
        return dst.set(x, y, z, w)
    }

    fun max(): Float {
        return max(max(x, y), max(z, w))
    }

    fun min(): Float {
        return min(min(x, y), min(z, w))
    }

    fun minComponent(): Int {
        return when (min()) {
            x -> 0
            y -> 1
            z -> 2
            else -> 3
        }
    }

    fun maxComponent(): Int {
        return when (max()) {
            x -> 0
            y -> 1
            z -> 2
            else -> 3
        }
    }

    fun floor(dst: Vector4f = this): Vector4f {
        return dst.set(floor(x), floor(y), floor(z), floor(w))
    }

    fun ceil(dst: Vector4f = this): Vector4f {
        return dst.set(ceil(x), ceil(y), ceil(z), ceil(w))
    }

    fun round(dst: Vector4f = this): Vector4f {
        return dst.set(round(x), round(y), round(z), round(w))
    }

    fun absolute(dst: Vector4f = this): Vector4f {
        return dst.set(abs(x), abs(y), abs(z), abs(w))
    }

    val isFinite: Boolean
        get() = JomlMath.isFinite(x) && JomlMath.isFinite(y) && JomlMath.isFinite(z) && JomlMath.isFinite(w)

    operator fun minus(s: Vector4f) = Vector4f(x - s.x, y - s.y, z - s.z, w - s.w)
    operator fun plus(s: Vector4f) = Vector4f(x + s.x, y + s.y, z + s.z, w + s.w)
    operator fun plus(s: Float) = if (s == 0f) this else Vector4f(x + s, y + s, z + s, w + s)
    operator fun times(s: Float) = Vector4f(x * s, y * s, z * s, w * s)
    operator fun times(s: Vector4f) = Vector4f(x * s.x, y * s.y, z * s.z, w * s.w)
    fun mulAlpha(m: Float, dst: Vector4f = Vector4f()): Vector4f = dst.set(x, y, z, w * m)
    fun is1111() = x == 1f && y == 1f && z == 1f && w == 1f

    companion object {

        @JvmStatic
        fun lengthSquared(x: Float, y: Float, z: Float, w: Float): Float {
            return x * x + y * y + z * z + w * w
        }

        @JvmStatic
        fun length(x: Float, y: Float, z: Float, w: Float): Float {
            return sqrt(lengthSquared(x, y, z, w))
        }

        @JvmStatic
        fun distance(x1: Float, y1: Float, z1: Float, w1: Float, x2: Float, y2: Float, z2: Float, w2: Float): Float {
            return sqrt(distanceSquared(x1, y1, z1, w1, x2, y2, z2, w2))
        }

        @JvmStatic
        fun distanceSquared(
            x1: Float, y1: Float, z1: Float, w1: Float,
            x2: Float, y2: Float, z2: Float, w2: Float
        ): Float {
            val dx = x1 - x2
            val dy = y1 - y2
            val dz = z1 - z2
            val dw = w1 - w2
            return lengthSquared(dx, dy, dz, dw)
        }
    }
}