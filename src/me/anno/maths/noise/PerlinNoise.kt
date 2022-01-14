package me.anno.maths.noise

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
        generators.reverse() // now the least significant generators are first
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
        fac = max - min // = delta
        for (i in octaves - 1 downTo 0) {
            factors[i] = fac / sum
            fac *= falloff
        }
        this.factors = factors
    }

    operator fun get(x: Double): Double {
        var sum = 0.0 // add min here? could result in worse precision
        // sum from small to large for improved precision
        val generators = generators!!
        val factors = factors
        for (i in factors.indices) {
            sum += factors[i] * generators[i].eval(x, 0.0)
        }
        return sum + min
    }

    operator fun get(x: Double, y: Double): Double {
        var sum = 0.0 // add min here? could result in worse precision
        // sum from small to large for improved precision
        val generators = generators!!
        val factors = factors
        for (i in factors.indices) {
            sum += factors[i] * generators[i].eval(x, y)
        }
        return sum + min
    }

    operator fun get(x: Double, y: Double, z: Double): Double {
        var sum = 0.0 // add min here? could result in worse precision
        // sum from small to large for improved precision
        val generators = generators!!
        val factors = factors
        for (i in factors.indices) {
            sum += factors[i] * generators[i].eval(x, y, z)
        }
        return sum + min
    }

    operator fun get(x: Double, y: Double, z: Double, w: Double): Double {
        var sum = 0.0 // add min here? could result in worse precision
        // sum from small to large for improved precision
        val generators = generators!!
        val factors = factors
        for (i in factors.indices) {
            sum += factors[i] * generators[i].eval(x, y, z, w)
        }
        return sum + min
    }

}