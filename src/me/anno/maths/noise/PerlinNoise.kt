package me.anno.maths.noise

import me.anno.image.ImageWriter
import org.kdotjpg.OpenSimplexNoise
import kotlin.random.Random

@Suppress("MemberVisibilityCanBePrivate")
class PerlinNoise(
    seed: Long,
    octaves: Int,
    falloff: Double,
    min: Double,
    max: Double
) {

    private var generators: Array<OpenSimplexNoise>? = null
    private var factors = DoubleArray(octaves)

    private var offset = (min + max) * 0.5

    var octaves: Int = octaves
        set(value) {
            if (field != value) {
                field = value
                createGenerators()
                calculateFactors()
            }
        }

    var seed: Long = seed
        set(value) {
            if (field != value) {
                field = value
                createGenerators()
            }
        }

    var min: Double = min
        set(value) {
            if (field != value) {
                field = value
                calculateFactors()
            }
        }

    var max: Double = max
        set(value) {
            if (field != value) {
                field = value
                calculateFactors()
            }
        }

    var falloff: Double = falloff
        set(value) {
            if (field != value) {
                field = value
                calculateFactors()
            }
        }

    init {
        createGenerators()
        calculateFactors()
    }

    private fun createGenerators() {
        val random = Random(seed)
        val octaves = octaves
        var generators = generators
        if (generators == null || generators.size != octaves) {
            generators = Array(octaves) { OpenSimplexNoise(random.nextLong()) }
        } else {
            for (i in 0 until octaves) {
                generators[i] = OpenSimplexNoise(random.nextLong())
            }
        }
        this.generators = generators
    }

    private fun calculateFactors() {
        val octaves = octaves
        var sum = 0.0
        var fac = 1.0
        for (i in 0 until octaves) {
            sum += fac
            fac *= falloff
        }
        var factors = factors
        if (factors.size != octaves) {
            factors = DoubleArray(octaves)
        }
        fac = (max - min) * 0.5 // = delta / 2, because OpenSimplexNoise goes from -1 to +1
        for (i in 0 until octaves) {
            factors[i] = fac / sum
            fac *= falloff
        }
        offset = (min + max) * 0.5
        this.factors = factors
    }

    operator fun get(x: Double): Double {
        var sum = offset
        val generators = generators!!
        val factors = factors
        var vx = x
        for (i in factors.indices) {
            sum += factors[i] * generators[i].eval(vx, 0.0)
            vx *= 2.0
        }
        return sum
    }

    operator fun get(x: Double, y: Double): Double {
        var sum = offset
        val generators = generators!!
        val factors = factors
        var vx = x
        var vy = y
        for (i in factors.indices) {
            sum += factors[i] * generators[i].eval(vx, vy)
            vx *= 2.0
            vy *= 2.0
        }
        return sum
    }

    operator fun get(x: Double, y: Double, z: Double): Double {
        var sum = offset
        val generators = generators!!
        val factors = factors
        var vx = x
        var vy = y
        var vz = z
        for (i in factors.indices) {
            sum += factors[i] * generators[i].eval(vx, vy, vz)
            vx *= 2.0
            vy *= 2.0
            vz *= 2.0
        }
        return sum
    }

    operator fun get(x: Double, y: Double, z: Double, w: Double): Double {
        var sum = offset
        val generators = generators!!
        val factors = factors
        var vx = x
        var vy = y
        var vz = z
        var vw = w
        for (i in factors.indices) {
            sum += factors[i] * generators[i].eval(vx, vy, vz, vw)
            vx *= 2.0
            vy *= 2.0
            vz *= 2.0
            vw *= 2.0
        }
        return sum
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val gen = PerlinNoise(1234L, 8, 0.5, 0.0, 1.0)
            var avg = 0.0
            var avg2 = 0.0
            val samples = 10000
            val buckets = IntArray(10)
            for (i in 0 until samples) {
                val g = gen[i.toDouble()]
                if (g !in gen.min..gen.max) {
                    throw RuntimeException("$g at $i")
                }
                avg += g
                avg2 += g * g
                buckets[(g * buckets.size).toInt()]++
            }
            println(buckets.joinToString())
            ImageWriter.writeImageInt(256, 256, false, "perlin.png", 16) { x, y, _ ->
                (gen[x / 100.0, y / 100.0] * 255).toInt() * 0x10101
            }
        }
    }

}