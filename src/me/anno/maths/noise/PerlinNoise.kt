package me.anno.maths.noise

import me.anno.image.ImageWriter
import org.apache.logging.log4j.LogManager
import kotlin.random.Random

@Suppress("MemberVisibilityCanBePrivate")
class PerlinNoise(
    seed: Long,
    octaves: Int,
    falloff: Float,
    min: Float,
    max: Float
) {

    private var generators: Array<FullNoise>? = null
    private var factors = FloatArray(octaves)

    private var offset = min

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

    var min: Float = min
        set(value) {
            if (field != value) {
                field = value
                calculateFactors()
            }
        }

    var max: Float = max
        set(value) {
            if (field != value) {
                field = value
                calculateFactors()
            }
        }

    var falloff: Float = falloff
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
            generators = Array(octaves) { FullNoise(random.nextLong()) }
        } else {
            for (i in 0 until octaves) {
                generators[i] = FullNoise(random.nextLong())
            }
        }
        this.generators = generators
    }

    private fun calculateFactors() {
        val octaves = octaves
        var sum = 0f
        var fac = 1f
        for (i in 0 until octaves) {
            sum += fac
            fac *= falloff
        }
        var factors = factors
        if (factors.size != octaves) {
            factors = FloatArray(octaves)
        }
        fac = max - min
        for (i in 0 until octaves) {
            factors[i] = fac / sum
            fac *= falloff
        }
        offset = min
        this.factors = factors
    }

    operator fun get(x: Float): Float {
        var sum = offset
        val generators = generators!!
        val factors = factors
        var vx = x
        for (i in factors.indices) {
            sum += factors[i] * generators[i].getValue(vx)
            vx *= 2f
        }
        return sum
    }

    operator fun get(x: Float, y: Float): Float {
        var sum = offset
        val generators = generators!!
        val factors = factors
        var vx = x
        var vy = y
        for (i in factors.indices) {
            sum += factors[i] * generators[i].getValue(vx, vy)
            vx *= 2f
            vy *= 2f
        }
        return sum
    }

    operator fun get(x: Float, y: Float, z: Float): Float {
        var sum = offset
        val generators = generators!!
        val factors = factors
        var vx = x
        var vy = y
        var vz = z
        for (i in factors.indices) {
            sum += factors[i] * generators[i].getValue(vx, vy, vz)
            vx *= 2f
            vy *= 2f
            vz *= 2f
        }
        return sum
    }

    operator fun get(x: Float, y: Float, z: Float, w: Float): Float {
        var sum = offset
        val generators = generators!!
        val factors = factors
        var vx = x
        var vy = y
        var vz = z
        var vw = w
        for (i in factors.indices) {
            sum += factors[i] * generators[i].getValue(vx, vy, vz, vw)
            vx *= 2f
            vy *= 2f
            vz *= 2f
            vw *= 2f
        }
        return sum
    }

    fun getSmooth(x: Float): Float {
        var sum = offset
        val generators = generators!!
        val factors = factors
        var vx = x
        for (i in factors.indices) {
            sum += factors[i] * generators[i].getValueSmooth(vx)
            vx *= 2f
        }
        return sum
    }

    fun getSmooth(x: Float, y: Float): Float {
        var sum = offset
        val generators = generators!!
        val factors = factors
        var vx = x
        var vy = y
        for (i in factors.indices) {
            sum += factors[i] * generators[i].getValueSmooth(vx, vy)
            vx *= 2f
            vy *= 2f
        }
        return sum
    }

    fun getSmooth(x: Float, y: Float, z: Float): Float {
        var sum = offset
        val generators = generators!!
        val factors = factors
        var vx = x
        var vy = y
        var vz = z
        for (i in factors.indices) {
            sum += factors[i] * generators[i].getValueSmooth(vx, vy, vz)
            vx *= 2f
            vy *= 2f
            vz *= 2f
        }
        return sum
    }

    fun getSmooth(x: Float, y: Float, z: Float, w: Float): Float {
        var sum = offset
        val generators = generators!!
        val factors = factors
        var vx = x
        var vy = y
        var vz = z
        var vw = w
        for (i in factors.indices) {
            sum += factors[i] * generators[i].getValueSmooth(vx, vy, vz, vw)
            vx *= 2f
            vy *= 2f
            vz *= 2f
            vw *= 2f
        }
        return sum
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val logger = LogManager.getLogger(PerlinNoise::class)
            val gen = PerlinNoise(1234L, 8, 0.5f, 0f, 1f)
            val samples = 10000
            val buckets = IntArray(10)
            for (i in 0 until samples) {
                buckets[(gen[i.toFloat()] * buckets.size).toInt()]++
            }
            logger.info(buckets.joinToString())
            ImageWriter.writeImageInt(256, 256, false, "perlin.png", 16) { x, y, _ ->
                (gen[x / 100f, y / 100f] * 255).toInt() * 0x10101
            }
        }
    }

}