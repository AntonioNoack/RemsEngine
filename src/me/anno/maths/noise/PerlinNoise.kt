package me.anno.maths.noise

import me.anno.image.ImageWriter
import org.apache.logging.log4j.LogManager
import kotlin.random.Random

@Suppress("MemberVisibilityCanBePrivate", "unused")
/**
 * class to create cloud-like noise;
 * @param seed seed for random values
 * @param octaves levels of detail
 * @param falloff how much each detail level is weaker; [0,1)
 * @param min minimum output value
 * @param max maximum output value
 * */
class PerlinNoise(
    seed: Long,
    octaves: Int, // 0 .. Int.Max
    falloff: Float, // 0 .. 1
    min: Float,
    max: Float
) {

    private var levels: Array<FullNoise>? = null
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
        var generators = levels
        if (generators == null || generators.size != octaves) {
            generators = Array(octaves) { FullNoise(random.nextLong()) }
        } else {
            for (i in 0 until octaves) {
                generators[i] = FullNoise(random.nextLong())
            }
        }
        this.levels = generators
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
        val generators = levels!!
        val factors = factors
        var vx = x
        for (i in factors.indices) {
            sum += factors[i] * generators[i].get(vx)
            vx *= 2f
        }
        return sum
    }

    operator fun get(x: Float, y: Float): Float {
        var sum = offset
        val generators = levels!!
        val factors = factors
        var vx = x
        var vy = y
        for (i in factors.indices) {
            sum += factors[i] * generators[i].get(vx, vy)
            vx *= 2f
            vy *= 2f
        }
        return sum
    }

    operator fun get(x: Float, y: Float, z: Float): Float {
        var sum = offset
        val generators = levels!!
        val factors = factors
        var vx = x
        var vy = y
        var vz = z
        for (i in factors.indices) {
            sum += factors[i] * generators[i].get(vx, vy, vz)
            vx *= 2f
            vy *= 2f
            vz *= 2f
        }
        return sum
    }

    operator fun get(x: Float, y: Float, z: Float, w: Float): Float {
        var sum = offset
        val generators = levels!!
        val factors = factors
        var vx = x
        var vy = y
        var vz = z
        var vw = w
        for (i in factors.indices) {
            sum += factors[i] * generators[i].get(vx, vy, vz, vw)
            vx *= 2f
            vy *= 2f
            vz *= 2f
            vw *= 2f
        }
        return sum
    }

    fun getSmooth(x: Float): Float {
        var sum = offset
        val generators = levels!!
        val factors = factors
        var vx = x
        for (i in factors.indices) {
            sum += factors[i] * generators[i].getSmooth(vx)
            vx *= 2f
        }
        return sum
    }

    fun getSmooth(x: Float, y: Float): Float {
        var sum = offset
        val generators = levels!!
        val factors = factors
        var vx = x
        var vy = y
        for (i in factors.indices) {
            sum += factors[i] * generators[i].getSmooth(vx, vy)
            vx *= 2f
            vy *= 2f
        }
        return sum
    }

    fun getSmooth(x: Float, y: Float, z: Float): Float {
        var sum = offset
        val generators = levels!!
        val factors = factors
        var vx = x
        var vy = y
        var vz = z
        for (i in factors.indices) {
            sum += factors[i] * generators[i].getSmooth(vx, vy, vz)
            vx *= 2f
            vy *= 2f
            vz *= 2f
        }
        return sum
    }

    fun getSmooth(x: Float, y: Float, z: Float, w: Float): Float {
        var sum = offset
        val generators = levels!!
        val factors = factors
        var vx = x
        var vy = y
        var vz = z
        var vw = w
        for (i in factors.indices) {
            sum += factors[i] * generators[i].getSmooth(vx, vy, vz, vw)
            vx *= 2f
            vy *= 2f
            vz *= 2f
            vw *= 2f
        }
        return sum
    }

}