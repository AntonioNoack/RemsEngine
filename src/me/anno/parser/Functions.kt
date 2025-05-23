package me.anno.parser

import me.anno.maths.Maths
import me.anno.maths.Maths.clamp
import me.anno.maths.noise.FullNoise
import me.anno.utils.types.Floats.toDegrees
import me.anno.utils.types.Floats.toRadians
import org.apache.logging.log4j.LogManager
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.acosh
import kotlin.math.asin
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.atanh
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.random.Random

object Functions {

    private val LOGGER = LogManager.getLogger(Functions::class)

    private fun isDefined(functions: Map<String, Any?>, name: String, lcName: String): Boolean {
        return (functions[name] ?: functions[lcName]) != null
    }

    private fun onUnknownFunction(name: String, paramString: String): Boolean {
        val lcName = name.lowercase()
        LOGGER.warn(
            "Unknown function $name($paramString)" + when {
                isDefined(functions0, name, lcName) -> ", did you mean $name()?"
                isDefined(functions1, name, lcName) -> ", did you mean $name(x)?"
                isDefined(functions2, name, lcName) -> ", did you mean $name(x,y)?"
                isDefined(functions3, name, lcName) -> ", did you mean $name(x,y,z)?"
                isDefined(functions4, name, lcName) -> ", did you mean $name(x,y,z,w)?"
                isDefined(functions5, name, lcName) -> ", did you mean $name(a,b,c,d,e)?"
                else -> ""
            }
        )
        return false
    }

    fun MutableList<Any>.applyFunc0(): Boolean {
        for (i in 2 until size) {
            if (this[i - 1] != '(') continue
            if (this[i - 0] != ')') continue
            val name = this[i - 2] as? String ?: continue
            val function = functions0[name]
                ?: functions0[name.lowercase()]
                ?: return onUnknownFunction(name, "x")
            for (j in 0 until 2) removeAt(i - j)
            this[i - 2] = function()
            applyFunc0()
            return true
        }
        return false
    }

    fun MutableList<Any>.applyFunc1(): Boolean {
        for (i in 3 until size) {
            if (this[i - 2] != '(') continue
            if (this[i - 0] != ')') continue
            val name = this[i - 3] as? String ?: continue
            val x = this[i - 1] as? Double ?: continue
            val function = functions1[name]
                ?: functions1[name.lowercase()]
                ?: return onUnknownFunction(name, "x")
            for (j in 0 until 3) removeAt(i - j)
            this[i - 3] = function(x)
            applyFunc1()
            return true
        }
        return false
    }

    fun MutableList<Any>.applyFunc2(): Boolean {
        for (i in 5 until size) {
            if (this[i - 4] != '(') continue
            if (this[i - 2] != ',') continue
            if (this[i - 0] != ')') continue
            val name = this[i - 5] as? String ?: continue
            val x = this[i - 3] as? Double ?: continue
            val y = this[i - 1] as? Double ?: continue
            val function =
                functions2[name]
                    ?: functions2[name.lowercase()]
                    ?: return onUnknownFunction(name, "x,y")
            for (j in 0 until 5) removeAt(i - j)
            this[i - 5] = function(x, y)
            applyFunc2()
            return true
        }
        return false
    }

    fun MutableList<Any>.applyFunc3(): Boolean {
        for (i in 7 until size) {
            if (this[i - 6] != '(') continue
            if (this[i - 4] != ',') continue
            if (this[i - 2] != ',') continue
            if (this[i - 0] != ')') continue
            val name = this[i - 7] as? String ?: continue
            val x = this[i - 5] as? Double ?: continue
            val y = this[i - 3] as? Double ?: continue
            val z = this[i - 1] as? Double ?: continue
            val function = functions3[name]
                ?: functions3[name.lowercase()]
                ?: return onUnknownFunction(name, "x,y,z")
            for (j in 0 until 7) removeAt(i - j)
            this[i - 7] = function(x, y, z)
            applyFunc3()
            return true
        }
        return false
    }

    fun MutableList<Any>.applyFunc4(): Boolean {
        for (i in 9 until size) {
            if (this[i - 8] != '(') continue
            if (this[i - 6] != ',') continue
            if (this[i - 4] != ',') continue
            if (this[i - 2] != ',') continue
            if (this[i - 0] != ')') continue
            val name = this[i - 9] as? String ?: continue
            val x = this[i - 7] as? Double ?: continue
            val y = this[i - 5] as? Double ?: continue
            val z = this[i - 3] as? Double ?: continue
            val w = this[i - 1] as? Double ?: continue
            val function = functions4[name]
                ?: functions4[name.lowercase()]
                ?: return onUnknownFunction(name, "x,y,z,w")
            for (j in 0 until 9) removeAt(i - j)
            this[i - 9] = function(x, y, z, w)
            applyFunc4()
            return true
        }
        return false
    }

    /**
     * apply functions with five arguments
     * longer functions than that could be used, but the longer a function is,
     * the more complex remembering their order gets ;)
     * our simple expression language is meant for simple stuff only anyway
     * */
    fun MutableList<Any>.applyFunc5(): Boolean {
        for (i in 11 until size) {
            if (this[i - 10] != '(') continue
            if (this[i - 8] != ',') continue
            if (this[i - 6] != ',') continue
            if (this[i - 4] != ',') continue
            if (this[i - 2] != ',') continue
            if (this[i - 0] != ')') continue
            val name = this[i - 11] as? String ?: continue
            val a = this[i - 9] as? Double ?: continue
            val b = this[i - 7] as? Double ?: continue
            val c = this[i - 5] as? Double ?: continue
            val d = this[i - 3] as? Double ?: continue
            val e = this[i - 1] as? Double ?: continue
            val function = functions5[name]
                ?: functions5[name.lowercase()]
                ?: return onUnknownFunction(name, "a,b,c,d,e")
            for (j in 0 until 11) removeAt(i - j)
            this[i - 11] = function(a, b, c, d, e)
            applyFunc5()
            return true
        }
        return false
    }

    val functions0 = HashMap<String, () -> Double>()
    val functions1 = HashMap<String, (Double) -> Double>()
    val functions2 = HashMap<String, (Double, Double) -> Double>()
    val functions3 = HashMap<String, (Double, Double, Double) -> Double>()
    val functions4 = HashMap<String, (Double, Double, Double, Double) -> Double>()
    val functions5 = HashMap<String, (Double, Double, Double, Double, Double) -> Double>()

    @Suppress("unused")
    val unaryFunctions = functions1

    @Suppress("unused")
    val binaryFunctions = functions2

    val constants = HashMap<String, Double>()

    init {

        constants["pi"] = PI
        constants["e"] = E
        constants["°"] = 1.0.toRadians()
        constants["inf"] = Double.POSITIVE_INFINITY
        constants["nan"] = Double.NaN

        functions0["rand"] = { Maths.random() }

        // min/max
        functions1["min"] = { it }
        functions1["max"] = { it }
        functions2["min"] = { a, b -> min(a, b) }
        functions2["max"] = { a, b -> max(a, b) }
        functions3["min"] = { a, b, c -> min(a, min(b, c)) }
        functions3["max"] = { a, b, c -> max(a, max(b, c)) }
        functions4["min"] = { a, b, c, d -> min(min(a, d), min(b, c)) }
        functions4["max"] = { a, b, c, d -> max(max(a, d), max(b, c)) }
        functions3["clamp"] = { x, min, max -> clamp(x, min, max) }

        // special powers, and root
        functions1["sq"] = { it * it }
        functions1["square"] = functions1["sq"]!!
        functions2["sq"] = { a, b -> a * a + b * b }
        functions2["square"] = functions2["sq"]!!
        functions3["sq"] = { a, b, c -> a * a + b * b + c * c }
        functions3["square"] = functions3["sq"]!!
        functions4["sq"] = { a, b, c, d -> a * a + b * b + c * c + d * d }
        functions4["square"] = functions4["sq"]!!

        functions1["sqrt"] = { sqrt(it) }
        functions1["cbrt"] = { it.pow(1.0 / 3.0) }
        functions1["root"] = functions1["sqrt"]!!

        functions2["root"] = { n, number -> number.pow(1.0 / n) }

        functions2["hypot"] = { a, b -> hypot(a, b) }

        // logarithm
        // log is not implemented to prevent confusion
        functions1["log2"] = { log2(it) }
        functions1["ld"] = functions1["log2"]!!
        functions1["lb"] = functions1["log2"]!!
        functions1["ln"] = { ln(it) }
        functions1["log10"] = { log10(it) }
        functions2["log"] = { x, base -> log(x, base) }

        functions1["length"] = { abs(it) }
        functions2["length"] = { a, b -> hypot(a, b) }
        functions3["length"] = { a, b, c -> sqrt(a * a + b * b + c * c) }
        functions4["length"] = { a, b, c, d -> sqrt(a * a + b * b + c * c + d * d) }

        functions1["abs"] = { abs(it) }
        functions1["absolute"] = functions1["abs"]!!


        // sin,cos,tan, with degrees
        functions1["sin"] = { sin(it.toRadians()) }
        functions1["sine"] = functions1["sin"]!!
        functions1["asin"] = { asin(it).toDegrees() }
        functions1["arcsine"] = functions1["asin"]!!
        functions1["sinh"] = { sinh(it) }
        functions1["asinh"] = { asinh(it) }

        functions1["cos"] = { cos(it.toRadians()) }
        functions1["cosine"] = functions1["cos"]!!
        functions1["acos"] = { acos(it).toDegrees() }
        functions1["arccosine"] = functions1["acos"]!!
        functions1["cosh"] = { cosh(it) }
        functions1["acosh"] = { acosh(it) }

        functions1["tan"] = { tan(it.toRadians()) }
        functions1["tangent"] = functions1["tan"]!!
        functions1["atan"] = { x -> atan(x).toDegrees() }
        functions2["atan"] = { x, y -> atan2(x, y).toDegrees() }
        functions2["arctan"] = functions2["atan"]!!
        functions2["atan2"] = functions2["atan"]!!
        functions1["atanh"] = { atanh(it) }

        functions1["exp"] = { exp(it) }
        functions2["pow"] = { base, exponent -> base.pow(exponent) }
        functions2["power"] = functions2["pow"]!!

        functions1["floor"] = { floor(it) }
        functions1["round"] = { round(it) }
        functions1["ceil"] = { ceil(it) }

        // random
        functions1["rand"] = { seed -> Random(seed.toLong()).nextDouble() }
        functions2["rand"] = { seed, x ->
            FullNoise(seed.toLong())[x]
        }
        functions3["rand"] = { seed, x, y ->
            FullNoise(seed.toLong())[x, y]
        }
        functions4["rand"] = { seed, x, y, z ->
            FullNoise(seed.toLong())[x, y, z]
        }
        functions5["rand"] = { seed, x, y, z, w ->
            FullNoise(seed.toLong())[x.toFloat(), y.toFloat(), z.toFloat(), w.toFloat()].toDouble()
        }

        // to do function, which takes double and vector...
        /*functions2["harmonics"] = { time, harmonics ->
            val w0 = time * 2.0 * PI
            harmonics as Vector
            harmonics.data.withIndex().sumByDouble { (index, it) -> it * sin((index + 1f) * w0) }
        }*/
    }
}