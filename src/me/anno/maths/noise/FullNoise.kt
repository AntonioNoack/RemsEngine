package me.anno.maths.noise

import me.anno.maths.Maths.mix
import me.anno.maths.Maths.smoothStepGradientUnsafe
import me.anno.maths.Maths.smoothStepUnsafe
import me.anno.utils.pooling.JomlPools
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import org.joml.Vector4f
import kotlin.math.floor
import kotlin.random.Random

/**
 * noise map, returns value [0,1];
 * not all functions are implemented; ask for help, if you miss one
 * */
@Suppress("unused")
class FullNoise(val seed: Long) {

    // longs can be used as well, but are slightly slower (long->int, 17->16, 26->13, 26->23, 48->32, ns/sample)
    private val sx: Int
    private val sy: Int
    private val sz: Int
    private val sw: Int
    private val s0: Int

    init {
        val random = Random(seed)
        sx = random.nextInt()
        sy = random.nextInt()
        sz = random.nextInt()
        sw = random.nextInt()
        s0 = random.nextInt()
    }

    /**
     * returns a random value in [0,1]
     * */
    operator fun get(x: Int): Float {
        var a = x * sx + s0
        var b = (a shl 16) or (a ushr 16)
        b *= 1911520717
        a = a.xor((b shl 16) or (b ushr 16))
        a *= 2048419325
        return a.shr(16).and(0xffff) * invMax
    }

    operator fun get(x: Int, y: Int): Float {
        var a = x * sx + s0
        var b = y * sy xor ((a shl 16) or (a ushr 16))
        b *= 1911520717
        a = a.xor((b shl 16) or (b ushr 16))
        a *= 2048419325
        return a.shr(16).and(0xffff) * invMax
    }

    operator fun get(x: Int, y: Int, z: Int): Float {
        var a = x * sx + s0
        var b = y * sy
        val c = z * sz
        b = b.xor((c shl 16) or (c ushr 16))
        b *= 3284157443.toInt()
        b = b.xor((a shl 16) or (a ushr 16))
        b *= 1911520717
        a = a.xor((b shl 16) or (b ushr 16))
        a *= 2048419325
        return a.shr(16).and(0xffff) * invMax
    }

    operator fun get(x: Int, y: Int, z: Int, w: Int): Float {
        var a = x * sx + s0
        var b = y * sy
        val c = z * sz
        val d = w * sw
        b = b.xor((c shl 16) or (c ushr 16))
        b *= 3284157443.toInt()
        a = a.xor((d shl 16) or (d ushr 16))
        a *= 91177017858.toInt()
        b = b.xor((a shl 16) or (a ushr 16))
        b *= 1911520717
        a = a.xor((b shl 16) or (b ushr 16))
        a *= 2048419325
        return a.shr(16).and(0xffff) * invMax
    }

    operator fun get(x: Float): Float {
        val xi = floor(x)
        val ix = xi.toInt()
        val xf = x - xi
        // 4.7-4.8ns/run -> 17 ns with actually good noise
        val v0 = get(ix)
        val v1 = get(ix + 1)
        return v0 * (1f - xf) + v1 * xf
    }

    fun getGradient(x: Float): Float {
        val xi = floor(x)
        val ix = xi.toInt()
        val v0 = get(ix)
        val v1 = get(ix + 1)
        return v1 - v0
    }

    operator fun get(x: Double): Double {
        val xi = floor(x)
        val ix = xi.toInt()
        val xf = x - xi
        val v0 = get(ix)
        val v1 = get(ix + 1)
        return v0 * (1.0 - xf) + v1 * xf
    }

    fun getGradient(x: Double): Double {
        val xi = floor(x)
        val ix = xi.toInt()
        val v0 = get(ix).toDouble()
        val v1 = get(ix + 1).toDouble()
        return v1 - v0
    }

    fun getSmooth(x: Float): Float {
        val xi = floor(x)
        val ix = xi.toInt()
        val xf = x - xi
        val v0 = get(ix)
        val v1 = get(ix + 1)
        return smoothStepUnsafe(v0, v1, xf)
    }

    fun getSmoothGradient(x: Float): Float {
        val xi = floor(x)
        val ix = xi.toInt()
        val xf = x - xi
        val v0 = get(ix)
        val v1 = get(ix + 1)
        return (v1 - v0) * smoothStepGradientUnsafe(xf)
    }

    operator fun get(x: Float, y: Float): Float {
        val xi = floor(x)
        val yi = floor(y)
        val ix = xi.toInt()
        val iy = yi.toInt()
        val xf = x - xi
        val yf = y - yi
        // 11ns/run -> 26 ns with actually good noise
        val v00 = get(ix, iy)
        val v01 = get(ix, iy + 1)
        val v10 = get(ix + 1, iy)
        val v11 = get(ix + 1, iy + 1)
        val yg = 1f - yf
        val v0 = v00 * yg + v01 * yf
        val v1 = v10 * yg + v11 * yf
        return v0 * (1f - xf) + v1 * xf
    }

    fun getGradient(x: Float, y: Float, dst: Vector2f): Float {
        val tmp = JomlPools.vec2d.borrow()
        val v = getGradient(x.toDouble(), y.toDouble(), tmp)
        dst.set(tmp)
        return v.toFloat()
    }

    fun getGradient(x: Double, y: Double, dst: Vector2d?): Double {
        val xi = floor(x)
        val yi = floor(y)
        val ix = xi.toInt()
        val iy = yi.toInt()
        val v00 = get(ix, iy).toDouble()
        val v01 = get(ix, iy + 1).toDouble()
        val v10 = get(ix + 1, iy).toDouble()
        val v11 = get(ix + 1, iy + 1).toDouble()
        val xf = x - xi
        val yf = y - yi
        if (dst != null) {
            val dx = (v10 - v00) * (1.0 - yf) + (v11 - v01) * yf
            val dy = (v01 - v00) * (1.0 - xf) + (v11 - v10) * xf
            dst.set(dx, dy)
        }
        val yg = 1f - yf
        val v0 = v00 * yg + v01 * yf
        val v1 = v10 * yg + v11 * yf
        return v0 * (1f - xf) + v1 * xf
    }

    operator fun get(x: Double, y: Double): Double {
        return getGradient(x, y, null)
    }

    fun getSmooth(x: Float, y: Float): Float {
        return getSmooth(x.toDouble(), y.toDouble()).toFloat()
    }

    fun getSmooth(x: Double, y: Double): Double {
        return getSmoothGradient(x, y, null)
    }

    fun getSmoothGradient(x: Float, y: Float, dst: Vector2f): Float {
        val tmp = JomlPools.vec2d.borrow()
        val v = getSmoothGradient(x.toDouble(), y.toDouble(), tmp)
        dst.set(tmp)
        return v.toFloat()
    }

    fun getSmoothGradient(x: Double, y: Double, dst: Vector2d?): Double {
        val xi = floor(x)
        val yi = floor(y)
        val ix = xi.toInt()
        val iy = yi.toInt()
        val xf = smoothStepUnsafe(x - xi)
        val yf = smoothStepUnsafe(y - yi)
        val v00 = get(ix, iy).toDouble()
        val v01 = get(ix, iy + 1).toDouble()
        val v10 = get(ix + 1, iy).toDouble()
        val v11 = get(ix + 1, iy + 1).toDouble()
        val yg = 1.0 - yf
        val v0x = v00 * yg + v01 * yf
        val v1x = v10 * yg + v11 * yf
        if (dst != null) {
            val xff = smoothStepGradientUnsafe(x - xi)
            val yff = smoothStepGradientUnsafe(y - yi)
            val xg = 1.0 - xf
            val v0y = v00 * xg + v10 * xf
            val v1y = v01 * xg + v11 * xf
            val dx = (v1x - v0x) * xff
            val dy = (v1y - v0y) * yff
            dst.set(dx, dy)
        }
        return v0x * (1.0 - xf) + v1x * xf
    }

    operator fun get(x: Float, y: Float, z: Float): Float {
        return get(x.toDouble(), y.toDouble(), z.toDouble()).toFloat()
    }

    operator fun get(x: Double, y: Double, z: Double): Double {
        return getGradient(x, y, z, null)
    }

    fun getGradient(x: Float, y: Float, z: Float, dst: Vector3f): Float {
        val tmp = JomlPools.vec3d.borrow()
        val v = getGradient(x.toDouble(), y.toDouble(), z.toDouble(), tmp)
        dst.set(tmp)
        return v.toFloat()
    }

    fun getGradient(x: Double, y: Double, z: Double, dst: Vector3d?): Double {
        val xi = floor(x)
        val yi = floor(y)
        val zi = floor(z)
        val ix = xi.toInt()
        val iy = yi.toInt()
        val iz = zi.toInt()
        val xf = x - xi
        val yf = y - yi
        val zf = z - zi
        val v000 = get(ix, iy, iz).toDouble()
        val v001 = get(ix, iy, iz + 1).toDouble()
        val v010 = get(ix, iy + 1, iz).toDouble()
        val v011 = get(ix, iy + 1, iz + 1).toDouble()
        val v100 = get(ix + 1, iy, iz).toDouble()
        val v101 = get(ix + 1, iy, iz + 1).toDouble()
        val v110 = get(ix + 1, iy + 1, iz).toDouble()
        val v111 = get(ix + 1, iy + 1, iz + 1).toDouble()
        if (dst != null) {
            val dx = mix(
                mix(v100 - v000, v110 - v010, yf),
                mix(v101 - v001, v111 - v011, yf),
                zf
            )
            val dy = mix(
                mix(v010 - v000, v110 - v100, xf),
                mix(v011 - v001, v111 - v101, xf),
                zf
            )
            val dz = mix(
                mix(v001 - v000, v011 - v010, yf),
                mix(v101 - v100, v111 - v110, yf),
                xf
            )
            dst.set(dx, dy, dz)
        }
        val zg = 1.0 - zf
        val v00 = v000 * zg + v001 * zf
        val v01 = v010 * zg + v011 * zf
        val v10 = v100 * zg + v101 * zf
        val v11 = v110 * zg + v111 * zf
        val yg = 1.0 - yf
        val v0 = v00 * yg + v01 * yf
        val v1 = v10 * yg + v11 * yf
        return v0 * (1.0 - xf) + v1 * xf
    }

    fun getSmooth(x: Float, y: Float, z: Float): Float {
        return getSmooth(x.toDouble(), y.toDouble(), z.toDouble()).toFloat()
    }

    // todo smooth gradient 3d
    fun getSmooth(x: Double, y: Double, z: Double): Double {
        val xi = floor(x)
        val yi = floor(y)
        val zi = floor(z)
        val ix = xi.toInt()
        val iy = yi.toInt()
        val iz = zi.toInt()
        val xf = smoothStepUnsafe(x - xi)
        val yf = smoothStepUnsafe(y - yi)
        val zf = smoothStepUnsafe(z - zi)
        val v000 = get(ix, iy, iz)
        val v001 = get(ix, iy, iz + 1)
        val v010 = get(ix, iy + 1, iz)
        val v011 = get(ix, iy + 1, iz + 1)
        val v100 = get(ix + 1, iy, iz)
        val v101 = get(ix + 1, iy, iz + 1)
        val v110 = get(ix + 1, iy + 1, iz)
        val v111 = get(ix + 1, iy + 1, iz + 1)
        val zg = 1.0 - zf
        val v00 = v000 * zg + v001 * zf
        val v01 = v010 * zg + v011 * zf
        val v10 = v100 * zg + v101 * zf
        val v11 = v110 * zg + v111 * zf
        val yg = 1.0 - yf
        val v0 = v00 * yg + v01 * yf
        val v1 = v10 * yg + v11 * yf
        return v0 * (1.0 - xf) + v1 * xf
    }

    operator fun get(x: Float, y: Float, z: Float, w: Float): Float {
        return get(x.toDouble(), y.toDouble(), z.toDouble(), w.toDouble()).toFloat()
    }

    operator fun get(x: Double, y: Double, z: Double, w: Double): Double {
        return getGradient(x, y, z, w, null)
    }

    fun getGradient(x: Float, y: Float, z: Float, w: Float, dst: Vector4f): Float {
        val tmp = JomlPools.vec4d.borrow()
        val v = getGradient(x.toDouble(), y.toDouble(), z.toDouble(), w.toDouble(), tmp)
        dst.set(tmp)
        return v.toFloat()
    }

    fun getGradient(x: Double, y: Double, z: Double, w: Double, dst: Vector4d?): Double {
        val xi = floor(x)
        val yi = floor(y)
        val zi = floor(z)
        val wi = floor(w)
        val ix = xi.toInt()
        val iy = yi.toInt()
        val iz = zi.toInt()
        val iw = wi.toInt()
        val xf = x - xi
        val yf = y - yi
        val zf = z - zi
        val wf = w - wi
        val v0000 = get(ix, iy, iz, iw).toDouble()
        val v0010 = get(ix, iy, iz + 1, iw).toDouble()
        val v0100 = get(ix, iy + 1, iz, iw).toDouble()
        val v0110 = get(ix, iy + 1, iz + 1, iw).toDouble()
        val v1000 = get(ix + 1, iy, iz, iw).toDouble()
        val v1010 = get(ix + 1, iy, iz + 1, iw).toDouble()
        val v1100 = get(ix + 1, iy + 1, iz, iw).toDouble()
        val v1110 = get(ix + 1, iy + 1, iz + 1, iw).toDouble()
        val v0001 = get(ix, iy, iz, iw + 1).toDouble()
        val v0011 = get(ix, iy, iz + 1, iw + 1).toDouble()
        val v0101 = get(ix, iy + 1, iz, iw + 1).toDouble()
        val v0111 = get(ix, iy + 1, iz + 1, iw + 1).toDouble()
        val v1001 = get(ix + 1, iy, iz, iw + 1).toDouble()
        val v1011 = get(ix + 1, iy, iz + 1, iw + 1).toDouble()
        val v1101 = get(ix + 1, iy + 1, iz, iw + 1).toDouble()
        val v1111 = get(ix + 1, iy + 1, iz + 1, iw + 1).toDouble()
        if (dst != null) {
            val dx = mix(
                mix(
                    mix(v1000 - v0000, v1100 - v0100, yf),
                    mix(v1010 - v0010, v1110 - v0110, yf), zf
                ),
                mix(
                    mix(v1001 - v0001, v1101 - v0101, yf),
                    mix(v1011 - v0011, v1111 - v0111, yf), zf
                ), wf
            )
            val dy = mix(
                mix(
                    mix(v0100 - v0000, v1100 - v1000, xf),
                    mix(v0110 - v0010, v1110 - v1010, xf), zf
                ),
                mix(
                    mix(v0101 - v0001, v1101 - v1001, xf),
                    mix(v0111 - v0011, v1111 - v1011, xf), zf
                ), wf
            )
            val dz = mix(
                mix(
                    mix(v0010 - v0000, v0110 - v0100, yf),
                    mix(v1010 - v1000, v1110 - v1100, yf), xf
                ),
                mix(
                    mix(v0011 - v0001, v0111 - v0101, yf),
                    mix(v1011 - v1001, v1111 - v1101, yf), xf
                ), wf
            )
            val dw = mix(
                mix(
                    mix(v0001 - v0000, v0101 - v0100, yf),
                    mix(v0011 - v0010, v0111 - v0110, yf), zf
                ),
                mix(
                    mix(v1001 - v1000, v1101 - v1100, yf),
                    mix(v1011 - v1010, v1111 - v1110, yf), zf
                ), xf
            )
            dst.set(dx, dy, dz, dw)
        }
        val wg = 1.0 - wf
        val v000 = v0000 * wg + v0001 * wf
        val v001 = v0010 * wg + v0011 * wf
        val v010 = v0100 * wg + v0101 * wf
        val v011 = v0110 * wg + v0111 * wf
        val v100 = v1000 * wg + v1001 * wf
        val v101 = v1010 * wg + v1011 * wf
        val v110 = v1100 * wg + v1101 * wf
        val v111 = v1110 * wg + v1111 * wf
        val zg = 1.0 - zf
        val v00 = v000 * zg + v001 * zf
        val v01 = v010 * zg + v011 * zf
        val v10 = v100 * zg + v101 * zf
        val v11 = v110 * zg + v111 * zf
        val yg = 1.0 - yf
        val v0 = v00 * yg + v01 * yf
        val v1 = v10 * yg + v11 * yf
        return v0 * (1.0 - xf) + v1 * xf
    }

    fun getSmooth(x: Float, y: Float, z: Float, w: Float): Float {
        return getSmooth(x.toDouble(), y.toDouble(), z.toDouble(), w.toDouble()).toFloat()
    }

    // todo smooth gradient 4d
    fun getSmooth(x: Double, y: Double, z: Double, w: Double): Double {
        val xi = floor(x)
        val yi = floor(y)
        val zi = floor(z)
        val wi = floor(w)
        val ix = xi.toInt()
        val iy = yi.toInt()
        val iz = zi.toInt()
        val iw = wi.toInt()
        val xf = smoothStepUnsafe(x - xi)
        val yf = smoothStepUnsafe(y - yi)
        val zf = smoothStepUnsafe(z - zi)
        val wf = smoothStepUnsafe(w - wi)
        val v0000 = get(ix, iy, iz, iw).toDouble()
        val v0010 = get(ix, iy, iz + 1, iw).toDouble()
        val v0100 = get(ix, iy + 1, iz, iw).toDouble()
        val v0110 = get(ix, iy + 1, iz + 1, iw).toDouble()
        val v1000 = get(ix + 1, iy, iz, iw).toDouble()
        val v1010 = get(ix + 1, iy, iz + 1, iw).toDouble()
        val v1100 = get(ix + 1, iy + 1, iz, iw).toDouble()
        val v1110 = get(ix + 1, iy + 1, iz + 1, iw).toDouble()
        val v0001 = get(ix, iy, iz, iw + 1).toDouble()
        val v0011 = get(ix, iy, iz + 1, iw + 1).toDouble()
        val v0101 = get(ix, iy + 1, iz, iw + 1).toDouble()
        val v0111 = get(ix, iy + 1, iz + 1, iw + 1).toDouble()
        val v1001 = get(ix + 1, iy, iz, iw + 1).toDouble()
        val v1011 = get(ix + 1, iy, iz + 1, iw + 1).toDouble()
        val v1101 = get(ix + 1, iy + 1, iz, iw + 1).toDouble()
        val v1111 = get(ix + 1, iy + 1, iz + 1, iw + 1).toDouble()
        val wg = 1.0 - wf
        val v000 = v0000 * wg + v0001 * wf
        val v001 = v0010 * wg + v0011 * wf
        val v010 = v0100 * wg + v0101 * wf
        val v011 = v0110 * wg + v0111 * wf
        val v100 = v1000 * wg + v1001 * wf
        val v101 = v1010 * wg + v1011 * wf
        val v110 = v1100 * wg + v1101 * wf
        val v111 = v1110 * wg + v1111 * wf
        val zg = 1.0 - zf
        val v00 = v000 * zg + v001 * zf
        val v01 = v010 * zg + v011 * zf
        val v10 = v100 * zg + v101 * zf
        val v11 = v110 * zg + v111 * zf
        val yg = 1.0 - yf
        val v0 = v00 * yg + v01 * yf
        val v1 = v10 * yg + v11 * yf
        return v0 * (1.0 - xf) + v1 * xf
    }

    companion object {
        private const val invMax = 1f / 0xffff
        private const val invMaxD = 1.0 / 0xffff
    }
}