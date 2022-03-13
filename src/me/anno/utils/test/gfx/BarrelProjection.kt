package me.anno.utils.test.gfx

import me.anno.image.Image
import me.anno.image.ImageCPUCache
import me.anno.image.ImageWriter
import me.anno.image.raw.IntImage
import me.anno.maths.Maths.length
import me.anno.utils.Color.b
import me.anno.utils.Color.g
import me.anno.utils.Color.r
import me.anno.utils.Color.rgb
import me.anno.utils.OS.desktop
import me.anno.utils.hpc.HeavyProcessing.processBalanced
import kotlin.math.abs
import kotlin.math.roundToInt

data class Polynomial(val c3: Float, val c5: Float) {
    constructor() : this(0f, 0f)

    fun compute(x: Float): Float {
        val xx = x * x
        return c5 * xx + c3
    }

}

fun searchCube(params: IntArray, max: Int, index: Int, calc: (IntArray) -> Unit) {
    if (index < params.size) {
        for (i in -max..max) {
            params[index] = i
            searchCube(params, max, index + 1, calc)
        }
    } else calc(params)
}

fun correct(base: Int, corrected: Int, mask: Int): Int {
    return corrected.and(mask) or base.and(mask.inv())
}

fun getError(color: Int): Int {
    val avg = color.r() + color.g() + color.b()
    val r3 = color.r() * 3
    val g3 = color.g() * 3
    val b3 = color.b() * 3
    return abs(avg - r3) + abs(avg - g3) + abs(avg - b3)
}

fun apply(image: Image, name: String, p0: Polynomial, p1: Polynomial, p2: Polynomial) {
    ImageWriter.writeImageInt(image.width, image.height, false, name, apply(image, p0, p1, p2).data)
}

fun apply(image: Image, name: String, polys: Array<Polynomial>) {
    apply(image, name, polys[0], polys[1], polys[2])
}

fun apply(image: Image, polys: Array<Polynomial>): IntImage {
    return apply(image, polys[0], polys[1], polys[2])
}

fun apply(image: Image, poly0: Polynomial, poly1: Polynomial, poly2: Polynomial): IntImage {
    val w2 = image.width / 2f
    val h2 = image.height / 2f
    val sx = 2f / image.width
    val values = IntArray(image.width * image.height)
    var idx = 0
    for (y in 0 until image.height) {
        for (x in 0 until image.width) {
            val dx = (x - w2) * sx
            val dy = (y - h2) * sx
            val len = length(dx, dy)
            val corr0 = poly0.compute(len)
            val corr1 = poly1.compute(len)
            val corr2 = poly2.compute(len)
            val c0 = image.getValueAt(x + dx * corr0, y + dy * corr0, 0).roundToInt()
            val c1 = image.getValueAt(x + dx * corr1, y + dy * corr1, 8).roundToInt()
            val c2 = image.getValueAt(x + dx * corr2, y + dy * corr2, 16).roundToInt()
            values[idx++] = rgb(c2, c1, c0)
        }
    }
    return IntImage(image.width, image.height, values, false)
}

fun getError(image: Image, channel: Int, polynomial: Polynomial): Int {
    var errorSum = 0
    val w2 = image.width / 2f
    val h2 = image.height / 2f
    val sx = 2f / image.width
    val padding = 16
    val shift = 8 * channel
    val mask = 255 shl shift
    processBalanced(padding, image.height - 2 * padding, 1) { y0, y1 ->
        var error = 0
        val width = image.width
        for (y in y0 until y1) {
            for (x in padding until width - 2 * padding) {
                val base = image.getRGB(x, y)
                val dx = (x - w2) * sx
                val dy = (y - h2) * sx
                val dl = polynomial.compute(length(dx, dy))
                val dxi = dx * dl
                val dyi = dy * dl
                val corrected = image.getValueAt(x + dxi, y + dyi, shift).roundToInt() * 0x10101
                error += getError(correct(base, corrected, mask))
            }
        }
        synchronized(Unit) {
            errorSum += error
        }
    }
    return errorSum
}

fun main() {

    // r = 2, 1 = g, 0 = b

    // best: -1014821, 1, -3, -3, 3, 3

    val s = 0.025f
    val img = ImageCPUCache.getImage(desktop.getChild("20220306_091338.jpg"), false)!!
    // todo scale it down for faster computations?
    // todo find the polynomial of degree 2/4/6 that best fits a black & white image
    // 1 + x^2 + x^4 + x^6

    val p0 = Polynomial()
    val bestPolynomials = Array(3) { p0 }
    bestPolynomials[0] = Polynomial(-3.125E-5f * img.width, 8.333333E-5f * img.height)
    bestPolynomials[1] = Polynomial(-3.75E-4f * img.width, 2.0833334E-4f * img.height)
    bestPolynomials[2] = Polynomial(2.5E-4f * img.width, 4.5833335E-4f * img.height)
    apply(img, "rgb1", bestPolynomials)

    return

    /*val pBest = Polynomial(8f * s * 5f, 11f * s * 5f)
    apply(image, "best", 2, pBest)*/

    // red has the largest error (probably), so start with it
    for ((channelIndex, channel) in listOf(2, 1, 0).withIndex()) {

        val image = if (channelIndex == 0) img
        else apply(img, bestPolynomials)

        println("processing channel $channel")

        // todo we should use the previously best parameters

        val error0 = getError(image, channel, p0)
        println("error0: ${error0.toFloat() / (image.width * image.height)}")

        var best = 0
        var bestParams = p0
        val max = 32
        val dim = 2 * max + 1
        val result = FloatArray(dim * dim)
        searchCube(IntArray(2), max, 0) {
            val p = Polynomial(it[0] * s, it[1] * s)
            val error1 = getError(image, channel, p) - error0
            if (error1 < 0) {
                println("-----------------------------")
                println("${it.joinToString()} $error1, max: ${p.compute(1f)}")
            }
            result[(it[0] + max) * dim + it[1] + max] = error1.toFloat()
            if (error1 < best) {
                best = error1
                bestParams = p
            }
        }

        println("---------------------")
        println("best: $best, params = $bestParams")

        ImageWriter.writeImageFloat(dim, dim, "error-$channel-x1.png", true, result)

        bestPolynomials[channel] = bestParams

    }

    apply(img, "rgb2", bestPolynomials)
    for ((index, poly) in bestPolynomials.withIndex()) {
        println("bestPolynomials[$index] = Polynomial(${poly.c3 / img.width}f * img.width, ${poly.c5 / img.height}f * img.height)")
    }

}