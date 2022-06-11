package me.anno.maths.noise

import me.anno.image.ImageWriter
import me.anno.maths.Maths.smoothStep
import me.anno.maths.Maths.smoothStepUnsafe
import kotlin.math.floor
import kotlin.random.Random

@Suppress("CanBeParameter")
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

    private val invMax = 1f / 0xffff

    /**
     * returns a random value in [0,1]
     * */
    fun getValue(x: Int): Float {
        var a = x * sx
        var b = (a shl 16) or (a ushr 16)
        b *= 1911520717
        a = a.xor((b shl 16) or (b ushr 16))
        a *= 2048419325
        return a.shr(16).and(0xffff) * invMax
    }

    fun getValue(x: Int, y: Int): Float {
        var a = x * sx
        var b = y * sy xor ((a shl 16) or (a ushr 16))
        b *= 1911520717
        a = a.xor((b shl 16) or (b ushr 16))
        a *= 2048419325
        return a.shr(16).and(0xffff) * invMax
    }

    fun getValue(x: Int, y: Int, z: Int): Float {
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

    fun getValue(x: Int, y: Int, z: Int, w: Int): Float {
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

    fun getValue(x: Float): Float {
        val xi = floor(x)
        val ix = xi.toInt()
        val xf = x - xi
        // 4.7-4.8ns/run -> 17 ns with actually good noise
        val v0 = getValue(ix)
        val v1 = getValue(ix + 1)
        return v0 * (1f - xf) + v1 * xf
    }

    @Suppress("unused")
    fun getValueSmooth(x: Float): Float {
        val xi = floor(x)
        val ix = xi.toInt()
        val xf = x - xi
        // 4.7-4.8ns/run -> 17 ns with actually good noise
        val v0 = getValue(ix)
        val v1 = getValue(ix + 1)
        return smoothStepUnsafe(v0, v1, xf)
    }

    fun getValue(x: Float, y: Float): Float {
        val xi = floor(x)
        val yi = floor(y)
        val ix = xi.toInt()
        val iy = yi.toInt()
        val xf = x - xi
        val yf = y - yi
        // 11ns/run -> 26 ns with actually good noise
        val v00 = getValue(ix, iy)
        val v01 = getValue(ix, iy + 1)
        val v10 = getValue(ix + 1, iy)
        val v11 = getValue(ix + 1, iy + 1)
        val yg = 1f - yf
        val v0 = v00 * yg + v01 * yf
        val v1 = v10 * yg + v11 * yf
        return v0 * (1f - xf) + v1 * xf
    }

    @Suppress("unused")
    fun getValueSmooth(x: Float, y: Float): Float {
        val xi = floor(x)
        val yi = floor(y)
        val ix = xi.toInt()
        val iy = yi.toInt()
        val xf = smoothStep(x - xi)
        val yf = smoothStep(y - yi)
        // 11ns/run -> 26 ns with actually good noise
        val v00 = getValue(ix, iy)
        val v01 = getValue(ix, iy + 1)
        val v10 = getValue(ix + 1, iy)
        val v11 = getValue(ix + 1, iy + 1)
        val yg = 1f - yf
        val v0 = v00 * yg + v01 * yf
        val v1 = v10 * yg + v11 * yf
        return v0 * (1f - xf) + v1 * xf
    }

    fun getValue(x: Float, y: Float, z: Float): Float {
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
        val v000 = getValue(ix, iy, iz)
        val v001 = getValue(ix, iy, iz + 1)
        val v010 = getValue(ix, iy + 1, iz)
        val v011 = getValue(ix, iy + 1, iz + 1)
        val v100 = getValue(ix + 1, iy, iz)
        val v101 = getValue(ix + 1, iy, iz + 1)
        val v110 = getValue(ix + 1, iy + 1, iz)
        val v111 = getValue(ix + 1, iy + 1, iz + 1)
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

    @Suppress("unused")
    fun getValueSmooth(x: Float, y: Float, z: Float): Float {
        val xi = floor(x)
        val yi = floor(y)
        val zi = floor(z)
        val ix = xi.toInt()
        val iy = yi.toInt()
        val iz = zi.toInt()
        val xf = smoothStep(x - xi)
        val yf = smoothStep(y - yi)
        val zf = smoothStep(z - zi)
        // 17-18ns/run -> 24 ns with actually good noise
        val v000 = getValue(ix, iy, iz)
        val v001 = getValue(ix, iy, iz + 1)
        val v010 = getValue(ix, iy + 1, iz)
        val v011 = getValue(ix, iy + 1, iz + 1)
        val v100 = getValue(ix + 1, iy, iz)
        val v101 = getValue(ix + 1, iy, iz + 1)
        val v110 = getValue(ix + 1, iy + 1, iz)
        val v111 = getValue(ix + 1, iy + 1, iz + 1)
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

    fun getValue(x: Float, y: Float, z: Float, w: Float): Float {
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
        val v0000 = getValue(ix, iy, iz, iw)
        val v0010 = getValue(ix, iy, iz + 1, iw)
        val v0100 = getValue(ix, iy + 1, iz, iw)
        val v0110 = getValue(ix, iy + 1, iz + 1, iw)
        val v1000 = getValue(ix + 1, iy, iz, iw)
        val v1010 = getValue(ix + 1, iy, iz + 1, iw)
        val v1100 = getValue(ix + 1, iy + 1, iz, iw)
        val v1110 = getValue(ix + 1, iy + 1, iz + 1, iw)
        val v0001 = getValue(ix, iy, iz, iw + 1)
        val v0011 = getValue(ix, iy, iz + 1, iw + 1)
        val v0101 = getValue(ix, iy + 1, iz, iw + 1)
        val v0111 = getValue(ix, iy + 1, iz + 1, iw + 1)
        val v1001 = getValue(ix + 1, iy, iz, iw + 1)
        val v1011 = getValue(ix + 1, iy, iz + 1, iw + 1)
        val v1101 = getValue(ix + 1, iy + 1, iz, iw + 1)
        val v1111 = getValue(ix + 1, iy + 1, iz + 1, iw + 1)
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

    @Suppress("unused")
    fun getValueSmooth(x: Float, y: Float, z: Float, w: Float): Float {
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
        // 48 ns/run
        val v0000 = getValue(ix, iy, iz, iw)
        val v0010 = getValue(ix, iy, iz + 1, iw)
        val v0100 = getValue(ix, iy + 1, iz, iw)
        val v0110 = getValue(ix, iy + 1, iz + 1, iw)
        val v1000 = getValue(ix + 1, iy, iz, iw)
        val v1010 = getValue(ix + 1, iy, iz + 1, iw)
        val v1100 = getValue(ix + 1, iy + 1, iz, iw)
        val v1110 = getValue(ix + 1, iy + 1, iz + 1, iw)
        val v0001 = getValue(ix, iy, iz, iw + 1)
        val v0011 = getValue(ix, iy, iz + 1, iw + 1)
        val v0101 = getValue(ix, iy + 1, iz, iw + 1)
        val v0111 = getValue(ix, iy + 1, iz + 1, iw + 1)
        val v1001 = getValue(ix + 1, iy, iz, iw + 1)
        val v1011 = getValue(ix + 1, iy, iz + 1, iw + 1)
        val v1101 = getValue(ix + 1, iy + 1, iz, iw + 1)
        val v1111 = getValue(ix + 1, iy + 1, iz + 1, iw + 1)
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

        // benchmark vs simplex noise
        // simplex noise: less computation, more unpredictable branches
        @JvmStatic
        fun main(args: Array<String>) {
            noiseQualityTest()
            // test1D() // 17 ns vs 31 ns (1 ns less for OpenSimplexNoise because of less prediction errors, I'd guess)
            // test2D() // 26 ns vs 32 ns
            // test3D() // 23 ns vs 92 ns
            // test4D() // 48 ns vs 3470 ns (no joke, OpenSimplexNoise in 4d is awful)
        }

        private fun noiseQualityTest() {
            val noise = FullNoise(1234L)
            ImageWriter.writeImageFloat(512, 512, "n1d.png", 512, false) { x, y, _ ->
                noise.getValue(x + y * 512)
            }
            ImageWriter.writeImageFloat(512, 512, "n2d.png", 512, false) { x, y, _ ->
                noise.getValue(x, y)
            }
            ImageWriter.writeImageFloat(512, 512, "n3d.png", 512, false) { x, y, _ ->
                noise.getValue(x, x - y, y)
            }
            ImageWriter.writeImageFloat(512, 512, "n4d.png", 512, false) { x, y, _ ->
                noise.getValue(x + y, x - y, x, y)
            }
        }

    }

}