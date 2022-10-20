package me.anno.tests.gfx

import me.anno.image.Image
import me.anno.image.ImageCPUCache
import me.anno.maths.Maths
import me.anno.maths.Optimization.simplexAlgorithm
import me.anno.utils.OS.desktop
import org.apache.logging.log4j.LogManager
import kotlin.math.cos
import kotlin.math.sin

fun findParams(image: Image, channel: Int, i: Int, n: Int): Pair<Int, Polynomial> {

    val s = 0.025f
    val angle = 2f * cos((i + 0.33f) * Maths.TAUf / n)
    val radius = 32f * s * i / n
    val startX = radius * sin(angle)
    val startY = radius * cos(angle)

    val params = simplexAlgorithm(floatArrayOf(startX, startY), s, 0f, 100) { params ->
        val polynomial = Polynomial(params[0], params[1])
        getError(image, channel, polynomial).toFloat()
    }.second

    val bestPolynomial = Polynomial(params[2], params[3])
    val bestError = getError(image, channel, bestPolynomial)
    return Pair(bestError, bestPolynomial)

}

fun main() {

    val logger = LogManager.getLogger("BarrelProjection")

    // tests... not as good as first one

    val img0 = ImageCPUCache.getImage(desktop.getChild("20220306_091338.jpg"), false)!!
    val img = img0// .createBImage(img0.width/5, img0.height/5)

    val bestPolynomials = Array(3) { Polynomial() }
    /*bestPolynomials[0] = Polynomial(-3.125E-5f * img.width, 8.333333E-5f * img.height)
    bestPolynomials[1] = Polynomial(-3.75E-4f * img.width, 2.0833334E-4f * img.height)
    bestPolynomials[2] = Polynomial(2.5E-4f * img.width, 4.5833335E-4f * img.height)
    apply(img, "rgb1", bestPolynomials)*/

    // red has the largest error (probably), so start with it
    for ((channelIndex, channel) in listOf(2, 1).withIndex()) {

        val p0 = bestPolynomials[channel]
        val image = if (channelIndex == 0) img
        else apply(img, bestPolynomials)

        logger.info("processing channel $channel")

        val error0 = getError(image, channel, p0)
        logger.info("error0: ${error0.toFloat() / (3 * image.width * image.height)}")

        var best = 0
        var bestParams = p0
        val n = 2
        for (i in 0..n) {
            val (errorX, params) = findParams(image, channel, i, n)
            val error = errorX - error0
            if (i == 0 || error < best) {
                best = error
                bestParams = params
                logger.info(
                    "   found better point " +
                            "${(best - error0).toFloat() / (3 * image.width * image.height)}, $params"
                )
            }
        }

        /*val s = 0.025f
        val max = 32
        val dim = 2 * max + 1
        val result = FloatArray(dim * dim)
        searchCube(IntArray(2), max, 0) {
            val p = Polynomial(it[0] * s, it[1] * s)
            val error1 = getError(image, channel, p) - error0
            result[(it[0] + max) * dim + it[1] + max] = error1.toFloat()
            if (error1 < best) {
                best = error1
                bestParams = p
                logger.info("-----------------------------")
                logger.info(
                    "   base method, found better: ${it.joinToString()} " +
                            "${(error1 - error0).toFloat() / (3 * image.width * image.height)}, max: ${p.compute(1f)}"
                )
            }
        }
        val t2 = System.nanoTime()
        ImageWriter.writeImageFloat(dim, dim, "error-$channelIndex-x1.png", true, result)
        logger.info("${(t2 - t1) * 1e-9} vs ${(t1 - t0) * 1e-9}")*/

        logger.info("-------- $channelIndex: $channel --------")
        logger.info("best: ${(best - error0).toFloat() / (3 * image.width * image.height)}, params = $bestParams")

        bestPolynomials[channel] = bestParams
        apply(image, "temp-$channelIndex.png", bestPolynomials)

    }

    for ((index, poly) in bestPolynomials.withIndex()) {
        logger.info("bestPolynomials[$index] = Polynomial(${poly.c3 / img.width}f * img.width, ${poly.c5 / img.height}f * img.height)")
    }

}