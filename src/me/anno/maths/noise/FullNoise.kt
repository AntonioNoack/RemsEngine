package me.anno.maths.noise

import me.anno.maths.Maths.smoothStep
import me.anno.maths.Maths.smoothStepUnsafe
import kotlin.math.floor
import kotlin.random.Random

/**
 * noise map;
 * not all functions are implemented; ask for help, if you miss one
 * */
@Suppress("unused", "CanBeParameter")
class FullNoise(val seed: Long) {

    // longs can be used as well, but are slightly slower (long->int, 17->16, 26->13, 26->23, 48->32, ns/sample)
    private val sx: Int
    private val sy: Int
    private val sz: Int
    private val sw: Int

    init {
        val random = Random(seed)
        sx = random.nextInt()
        sy = random.nextInt()
        sz = random.nextInt()
        sw = random.nextInt()
    }

    /**
     * returns a random value in [0,1]
     * */
    operator fun get(x: Int): Float {
        var a = x * sx
        var b = (a shl 16) or (a ushr 16)
        b *= 1911520717
        a = a.xor((b shl 16) or (b ushr 16))
        a *= 2048419325
        return a.shr(16).and(0xffff) * invMax
    }

    operator fun get(x: Int, y: Int): Float {
        var a = x * sx
        var b = y * sy xor ((a shl 16) or (a ushr 16))
        b *= 1911520717
        a = a.xor((b shl 16) or (b ushr 16))
        a *= 2048419325
        return a.shr(16).and(0xffff) * invMax
    }

    operator fun get(x: Int, y: Int, z: Int): Float {
        var a = x * sx
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
        var a = x * sx
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

    operator fun get(x: Double): Double {
        val xi = floor(x)
        val ix = xi.toInt()
        val xf = x - xi
        val v0 = get(ix)
        val v1 = get(ix + 1)
        return v0 * (1.0 - xf) + v1 * xf
    }

    fun getSmooth(x: Float): Float {
        val xi = floor(x)
        val ix = xi.toInt()
        val xf = x - xi
        val v0 = get(ix)
        val v1 = get(ix + 1)
        return smoothStepUnsafe(v0, v1, xf)
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

    operator fun get(x: Double, y: Double): Double {
        val xi = floor(x)
        val yi = floor(y)
        val ix = xi.toInt()
        val iy = yi.toInt()
        val xf = x - xi
        val yf = y - yi
        val v00 = get(ix, iy)
        val v01 = get(ix, iy + 1)
        val v10 = get(ix + 1, iy)
        val v11 = get(ix + 1, iy + 1)
        val yg = 1.0 - yf
        val v0 = v00 * yg + v01 * yf
        val v1 = v10 * yg + v11 * yf
        return v0 * (1.0 - xf) + v1 * xf
    }

    fun getSmooth(x: Float, y: Float): Float {
        val xi = floor(x)
        val yi = floor(y)
        val ix = xi.toInt()
        val iy = yi.toInt()
        val xf = smoothStep(x - xi)
        val yf = smoothStep(y - yi)
        val v00 = get(ix, iy)
        val v01 = get(ix, iy + 1)
        val v10 = get(ix + 1, iy)
        val v11 = get(ix + 1, iy + 1)
        val yg = 1f - yf
        val v0 = v00 * yg + v01 * yf
        val v1 = v10 * yg + v11 * yf
        return v0 * (1f - xf) + v1 * xf
    }

    operator fun get(x: Float, y: Float, z: Float): Float {
        val xi = floor(x)
        val yi = floor(y)
        val zi = floor(z)
        val ix = xi.toInt()
        val iy = yi.toInt()
        val iz = zi.toInt()
        val xf = x - xi
        val yf = y - yi
        val zf = z - zi
        // 17-18ns/run -> 24 ns with actually good noise
        val v000 = get(ix, iy, iz)
        val v001 = get(ix, iy, iz + 1)
        val v010 = get(ix, iy + 1, iz)
        val v011 = get(ix, iy + 1, iz + 1)
        val v100 = get(ix + 1, iy, iz)
        val v101 = get(ix + 1, iy, iz + 1)
        val v110 = get(ix + 1, iy + 1, iz)
        val v111 = get(ix + 1, iy + 1, iz + 1)
        val zg = 1f - zf
        val v00 = v000 * zg + v001 * zf
        val v01 = v010 * zg + v011 * zf
        val v10 = v100 * zg + v101 * zf
        val v11 = v110 * zg + v111 * zf
        val yg = 1f - yf
        val v0 = v00 * yg + v01 * yf
        val v1 = v10 * yg + v11 * yf
        return v0 * (1f - xf) + v1 * xf
    }

    operator fun get(x: Double, y: Double, z: Double): Double {
        val xi = floor(x)
        val yi = floor(y)
        val zi = floor(z)
        val ix = xi.toInt()
        val iy = yi.toInt()
        val iz = zi.toInt()
        val xf = x - xi
        val yf = y - yi
        val zf = z - zi
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

    fun getSmooth(x: Float, y: Float, z: Float): Float {
        val xi = floor(x)
        val yi = floor(y)
        val zi = floor(z)
        val ix = xi.toInt()
        val iy = yi.toInt()
        val iz = zi.toInt()
        val xf = smoothStep(x - xi)
        val yf = smoothStep(y - yi)
        val zf = smoothStep(z - zi)
        val v000 = get(ix, iy, iz)
        val v001 = get(ix, iy, iz + 1)
        val v010 = get(ix, iy + 1, iz)
        val v011 = get(ix, iy + 1, iz + 1)
        val v100 = get(ix + 1, iy, iz)
        val v101 = get(ix + 1, iy, iz + 1)
        val v110 = get(ix + 1, iy + 1, iz)
        val v111 = get(ix + 1, iy + 1, iz + 1)
        val zg = 1f - zf
        val v00 = v000 * zg + v001 * zf
        val v01 = v010 * zg + v011 * zf
        val v10 = v100 * zg + v101 * zf
        val v11 = v110 * zg + v111 * zf
        val yg = 1f - yf
        val v0 = v00 * yg + v01 * yf
        val v1 = v10 * yg + v11 * yf
        return v0 * (1f - xf) + v1 * xf
    }

    operator fun get(x: Float, y: Float, z: Float, w: Float): Float {
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
        // 48 ns/run
        val v0000 = get(ix, iy, iz, iw)
        val v0010 = get(ix, iy, iz + 1, iw)
        val v0100 = get(ix, iy + 1, iz, iw)
        val v0110 = get(ix, iy + 1, iz + 1, iw)
        val v1000 = get(ix + 1, iy, iz, iw)
        val v1010 = get(ix + 1, iy, iz + 1, iw)
        val v1100 = get(ix + 1, iy + 1, iz, iw)
        val v1110 = get(ix + 1, iy + 1, iz + 1, iw)
        val v0001 = get(ix, iy, iz, iw + 1)
        val v0011 = get(ix, iy, iz + 1, iw + 1)
        val v0101 = get(ix, iy + 1, iz, iw + 1)
        val v0111 = get(ix, iy + 1, iz + 1, iw + 1)
        val v1001 = get(ix + 1, iy, iz, iw + 1)
        val v1011 = get(ix + 1, iy, iz + 1, iw + 1)
        val v1101 = get(ix + 1, iy + 1, iz, iw + 1)
        val v1111 = get(ix + 1, iy + 1, iz + 1, iw + 1)
        val wg = 1f - wf
        val v000 = v0000 * wg + v0001 * wf
        val v001 = v0010 * wg + v0011 * wf
        val v010 = v0100 * wg + v0101 * wf
        val v011 = v0110 * wg + v0111 * wf
        val v100 = v1000 * wg + v1001 * wf
        val v101 = v1010 * wg + v1011 * wf
        val v110 = v1100 * wg + v1101 * wf
        val v111 = v1110 * wg + v1111 * wf
        val zg = 1f - zf
        val v00 = v000 * zg + v001 * zf
        val v01 = v010 * zg + v011 * zf
        val v10 = v100 * zg + v101 * zf
        val v11 = v110 * zg + v111 * zf
        val yg = 1f - yf
        val v0 = v00 * yg + v01 * yf
        val v1 = v10 * yg + v11 * yf
        return v0 * (1f - xf) + v1 * xf
    }

    fun getSmooth(x: Float, y: Float, z: Float, w: Float): Float {
        val xi = floor(x)
        val yi = floor(y)
        val zi = floor(z)
        val wi = floor(w)
        val ix = xi.toInt()
        val iy = yi.toInt()
        val iz = zi.toInt()
        val iw = wi.toInt()
        val xf = smoothStep(x - xi)
        val yf = smoothStep(y - yi)
        val zf = smoothStep(z - zi)
        val wf = smoothStep(w - wi)
        val v0000 = get(ix, iy, iz, iw)
        val v0010 = get(ix, iy, iz + 1, iw)
        val v0100 = get(ix, iy + 1, iz, iw)
        val v0110 = get(ix, iy + 1, iz + 1, iw)
        val v1000 = get(ix + 1, iy, iz, iw)
        val v1010 = get(ix + 1, iy, iz + 1, iw)
        val v1100 = get(ix + 1, iy + 1, iz, iw)
        val v1110 = get(ix + 1, iy + 1, iz + 1, iw)
        val v0001 = get(ix, iy, iz, iw + 1)
        val v0011 = get(ix, iy, iz + 1, iw + 1)
        val v0101 = get(ix, iy + 1, iz, iw + 1)
        val v0111 = get(ix, iy + 1, iz + 1, iw + 1)
        val v1001 = get(ix + 1, iy, iz, iw + 1)
        val v1011 = get(ix + 1, iy, iz + 1, iw + 1)
        val v1101 = get(ix + 1, iy + 1, iz, iw + 1)
        val v1111 = get(ix + 1, iy + 1, iz + 1, iw + 1)
        val wg = 1f - wf
        val v000 = v0000 * wg + v0001 * wf
        val v001 = v0010 * wg + v0011 * wf
        val v010 = v0100 * wg + v0101 * wf
        val v011 = v0110 * wg + v0111 * wf
        val v100 = v1000 * wg + v1001 * wf
        val v101 = v1010 * wg + v1011 * wf
        val v110 = v1100 * wg + v1101 * wf
        val v111 = v1110 * wg + v1111 * wf
        val zg = 1f - zf
        val v00 = v000 * zg + v001 * zf
        val v01 = v010 * zg + v011 * zf
        val v10 = v100 * zg + v101 * zf
        val v11 = v110 * zg + v111 * zf
        val yg = 1f - yf
        val v0 = v00 * yg + v01 * yf
        val v1 = v10 * yg + v11 * yf
        return v0 * (1f - xf) + v1 * xf
    }

    companion object {
        private const val invMax = 1f / 0xffff
        private const val invMaxD = 1.0 / 0xffff
    }

}