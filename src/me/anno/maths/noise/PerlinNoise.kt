package me.anno.maths.noise

import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
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
    max: Float,
    val scale: Vector4f = Vector4f(1f)
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
        var vx = x * scale.x
        for (i in factors.indices) {
            sum += factors[i] * generators[i][vx]
            vx *= 2f
        }
        return sum
    }

    fun getGradient(x: Float): Float {
        var sum = 0f
        val generators = levels!!
        val factors = factors
        var vx = x * scale.x
        for (i in factors.indices) {
            sum += factors[i] * generators[i].getGradient(vx)
            vx *= 2f
        }
        return sum * scale.x
    }

    operator fun get(x: Float, y: Float): Float {
        var sum = offset
        val generators = levels!!
        val factors = factors
        var vx = x * scale.x
        var vy = y * scale.y
        for (i in factors.indices) {
            sum += factors[i] * generators[i][vx, vy]
            vx *= 2f
            vy *= 2f
        }
        return sum
    }

    fun getGradient(x: Float, y: Float, tmp: Vector2f, dst: Vector2f): Vector2f {
        dst.set(0f)
        val generators = levels!!
        val factors = factors
        var vx = x * scale.x
        var vy = y * scale.y
        for (i in factors.indices) {
            generators[i].getGradient(vx, vy, tmp)
            tmp.mulAdd(factors[i], dst, dst)
            vx *= 2f
            vy *= 2f
        }
        dst.mul(scale.x, scale.y)
        return dst
    }

    operator fun get(x: Float, y: Float, z: Float): Float {
        var sum = offset
        val generators = levels!!
        val factors = factors
        var vx = x * scale.x
        var vy = y * scale.y
        var vz = z * scale.z
        for (i in factors.indices) {
            sum += factors[i] * generators[i][vx, vy, vz]
            vx *= 2f
            vy *= 2f
            vz *= 2f
        }
        return sum
    }

    fun getGradient(x: Float, y: Float, z: Float, tmp: Vector3f, dst: Vector3f): Vector3f {
        dst.set(0f)
        val generators = levels!!
        val factors = factors
        var vx = x * scale.x
        var vy = y * scale.y
        var vz = z * scale.z
        for (i in factors.indices) {
            generators[i].getGradient(vx, vy, vz, tmp)
            tmp.mulAdd(factors[i], dst, dst)
            vx *= 2f
            vy *= 2f
            vz *= 2f
        }
        dst.mul(scale.x, scale.y, scale.z)
        return dst
    }

    operator fun get(x: Float, y: Float, z: Float, w: Float): Float {
        var sum = offset
        val generators = levels!!
        val factors = factors
        var vx = x * scale.x
        var vy = y * scale.y
        var vz = z * scale.z
        var vw = w * scale.w
        for (i in factors.indices) {
            sum += factors[i] * generators[i][vx, vy, vz, vw]
            vx *= 2f
            vy *= 2f
            vz *= 2f
            vw *= 2f
        }
        return sum
    }

    fun getGradient(x: Float, y: Float, z: Float, w: Float, tmp: Vector4f, dst: Vector4f): Vector4f {
        dst.set(0f)
        val generators = levels!!
        val factors = factors
        var vx = x * scale.x
        var vy = y * scale.y
        var vz = z * scale.z
        var vw = w * scale.w
        for (i in factors.indices) {
            generators[i].getGradient(vx, vy, vz, vw, tmp)
            tmp.mulAdd(factors[i], dst, dst)
            vx *= 2f
            vy *= 2f
            vz *= 2f
            vw *= 2f
        }
        dst.mul(scale)
        return dst
    }

    fun getSmooth(x: Float): Float {
        var sum = offset
        val generators = levels!!
        val factors = factors
        var vx = x * scale.x
        for (i in factors.indices) {
            sum += factors[i] * generators[i].getSmooth(vx)
            vx *= 2f
        }
        return sum
    }

    fun getSmoothGradient(x: Float): Float {
        var sum = 0f
        val generators = levels!!
        val factors = factors
        var vx = x * scale.x
        for (i in factors.indices) {
            sum += factors[i] * generators[i].getSmoothGradient(vx)
            vx *= 2f
        }
        return sum * scale.x
    }

    fun getSmooth(x: Float, y: Float): Float {
        var sum = offset
        val generators = levels!!
        val factors = factors
        var vx = x * scale.x
        var vy = y * scale.y
        for (i in factors.indices) {
            sum += factors[i] * generators[i].getSmooth(vx, vy)
            vx *= 2f
            vy *= 2f
        }
        return sum
    }

    fun getSmoothGradient(x: Float, y: Float, tmp: Vector2f, dst: Vector2f): Float {
        var sum = offset
        val generators = levels!!
        val factors = factors
        var vx = x * scale.x
        var vy = y * scale.y
        dst.set(0f)
        for (i in factors.indices) {
            val fac = factors[i]
            sum += fac * generators[i].getSmoothGradient(vx, vy, tmp)
            tmp.mulAdd(fac, dst, dst)
            vx *= 2f
            vy *= 2f
        }
        dst.mul(scale.x, scale.y)
        return sum
    }

    // todo smooth gradients for 3d,4d
    fun getSmooth(x: Float, y: Float, z: Float): Float {
        var sum = offset
        val generators = levels!!
        val factors = factors
        var vx = x * scale.x
        var vy = y * scale.y
        var vz = z * scale.z
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
        var vx = x * scale.x
        var vy = y * scale.y
        var vz = z * scale.z
        var vw = w * scale.z
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