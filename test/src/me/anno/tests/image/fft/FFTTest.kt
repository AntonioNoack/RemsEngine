package me.anno.tests.image.fft

import me.anno.Engine
import me.anno.image.Image
import me.anno.image.ImageCache
import me.anno.image.raw.FloatImage
import me.anno.utils.Clock
import me.anno.utils.Color.g
import me.anno.utils.OS.desktop
import org.jtransforms.fft.FloatFFT_2D
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.sqrt

fun nextPowerOfTwo(i: Int): Int {
    return 1 shl (log2(i.toFloat()) + 0.75f).toInt()
}

fun transform(resized: Image, fft: FloatFFT_2D, shift: Int): FloatArray {
    val color = FloatArray(resized.width * resized.height)
    for (i in color.indices) color[i] = srgbMap[resized.getRGB(i).shr(shift).and(255)]
    fft.realForward(color)
    return color
}

fun invTransform(r: FloatArray, fft: FloatFFT_2D) {
    fft.realInverse(r, true)
    for (i in r.indices) r[i] = sqrt(max(r[i], 0f))
}

val srgbMap = FloatArray(256) {
    (it * it).toFloat() / (255 * 255)
}

fun main() {

    Engine.registerForShutdown {
        // when using FFT, this has to be called on shutdown...
        pl.edu.icm.jlargearrays.ConcurrencyUtils.shutdownThreadPoolAndAwaitTermination()
    }

    val clock = Clock("FFTTest")
    val src = ImageCache[desktop.getChild("Moire.jpg")].waitFor()!!
    clock.stop("Load Image")

    val w = nextPowerOfTwo(src.width)
    val h = nextPowerOfTwo(src.height)

    val mask = ImageCache[desktop.getChild("MoireMask.png")].waitFor()!!
        .resized(w, h, true)
    clock.stop("Load Mask")

    // mask.write(desktop.getChild("MaskScaled.png"))

    val resized = src.resized(w, h, true)
    clock.stop("Resize Image")
    // resized.write(desktop.getChild("MoireScaled.jpg"))

    val fft = FloatFFT_2D(w.toLong(), h.toLong())
    val r = transform(resized, fft, 16)
    val g = transform(resized, fft, 8)
    val b = transform(resized, fft, 0)
    clock.stop("FFT")

    val tmp = FloatArray(w * h * 3)
    for (y in 0 until h) {
        var i = y * w
        var j = i * 3
        for (x in 0 until w) {
            tmp[j + 0] = ln(abs(r[i]).toDouble()).toFloat()
            tmp[j + 1] = ln(abs(g[i]).toDouble()).toFloat()
            tmp[j + 2] = ln(abs(b[i]).toDouble()).toFloat()
            i++
            j += 3
        }
    }
    FloatImage(w, h, 3, tmp)
        .normalize01()
        .write(desktop.getChild("FFT.png"))
    clock.stop("Write FFT")

    for (y in 0 until h) {
        val yi = if (y + y < h) y else h - 1 - y // mirror mask on y-axis
        var i = y * w
        for (x in 0 until w) {
            val xi = if (x + x < w) x else w - 1 - x
            val scale0 = mask.getRGB(xi, yi).g() / 255f
            val scale = scale0 * scale0
            r[i] *= scale
            g[i] *= scale
            b[i] *= scale
            i++
        }
    }

    clock.stop("Modulation")

    invTransform(r, fft)
    invTransform(g, fft)
    invTransform(b, fft)
    clock.stop("InvFFT")

    val image = FloatImage(w, h, 3, r + g + b)
    image
        .resized(src.width, src.height, true)
        .write(desktop.getChild("MoireFFT.jpg"))
    clock.stop("Saving")

    Engine.requestShutdown()
}