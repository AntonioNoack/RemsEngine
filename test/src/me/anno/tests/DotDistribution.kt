package me.anno.tests

import me.anno.image.raw.FloatImage
import me.anno.maths.Maths.mix
import me.anno.utils.OS.desktop
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

fun calcRelativeDensity(rand: Float, rayX: Float): Float {
    // intersect sphere at (0,0) from (-1,0) to (rayX-1,rayY)
    // adjusted from https://mathworld.wolfram.com/Circle-LineIntersection.html
    // and simplified a lot
    val r2 = rand * rand
    return max(0f, 1f + (abs(rayX) * rayX - 1f) / r2)
}

fun main() {

    // todo there still is a small systematic difference... where is it coming from?

    // create a nice image from it

    val numBuckets = 255
    val ySamples = 255

    val image = FloatImage(numBuckets, ySamples * 2, 1)
    val buckets = IntArray(numBuckets)

    for (y in 0 until ySamples) {

        val numTries = 500000

        if (y > 0) buckets.fill(0)

        val r = mix(0.000001f, 0.999999f, y / (ySamples - 1f))
        val a = 1f / r

        val random = Random()

        // val minValue = cos(asin(randomness))
        val minValue = -1f // sqrt(max(0f, 1f - r * r))
        val maxValue = +1f
        val invDeltaValue = numBuckets * 0.9999f / max(1e-7f, maxValue - minValue)

        for (i in 0 until numTries) {
            var rx: Float
            var ry: Float
            var rz: Float
            var r2xy: Float
            do {
                rx = random.nextFloat() * 2f - 1f
                ry = random.nextFloat() * 2f - 1f
                rz = random.nextFloat() * 2f - 1f
                r2xy = rx * rx + ry * ry
            } while (r2xy + rz * rz > 1f)
            val vz = rz + a
            val value = vz / sqrt(r2xy + vz * vz)
            val bucket = (value - minValue) * invDeltaValue
            buckets[bucket.toInt()]++
        }

        // me.anno.tests.normalize line and write it
        val maxFillHeight = buckets.maxOrNull()!!
        val invFillHeight = 1f / maxFillHeight
        val analyticNorm = 1f
        for (x in 0 until numBuckets) {
            val simulatedV = buckets[x] * invFillHeight
            image.setValue(x, y, 0, simulatedV)
            // calculate analytical result
            var analytic = 0f
            val analyticSamples = 10
            for (xi in 0 until analyticSamples) {
                val v = mix(minValue, maxValue, (x + xi / 9f) / (numBuckets))
                analytic += calcRelativeDensity(r, v)
            }
            // println("$r,$v -> $ana*$anaNorm = ${ana * anaNorm}")
            val analyticV = analytic * analyticNorm / analyticSamples
            image.setValue(
                x, y + ySamples, 0,
                simulatedV - analyticV
            )
        }

    }

    image.write(desktop.getChild("dist.png"))


}