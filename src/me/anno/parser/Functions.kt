package me.anno.parser

import me.anno.utils.maths.Maths.clamp
import org.kdotjpg.OpenSimplexNoise
import java.lang.StrictMath.cbrt
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.*

object Functions {

    fun UnknownFunction(name: String, paramString: String): Throwable {
        val lcName = name.lowercase(Locale.getDefault())
        val f0 = functions0[name] ?: functions0[lcName]
        val f1 = functions1[name] ?: functions1[lcName]
        val f2 = functions2[name] ?: functions2[lcName]
        val f3 = functions3[name] ?: functions3[lcName]
        val f4 = functions4[name] ?: functions4[lcName]
        val f5 = functions5[name] ?: functions5[lcName]
        return RuntimeException(
            "Unknown function $name($paramString)" + when {
                f0 != null -> ", did you mean $name()?"
                f1 != null -> ", did you mean $name(x)?"
                f2 != null -> ", did you mean $name(x,y)?"
                f3 != null -> ", did you mean $name(x,y,z)?"
                f4 != null -> ", did you mean $name(x,y,z,w)?"
                f5 != null -> ", did you mean $name(a,b,c,d,e)?"
                else -> ""
            }
        )
    }

    fun MutableList<Any>.applyFunc0(): Boolean {
        for (i in 2 until size) {
            if (this[i - 1] != '(') continue
            if (this[i - 0] != ')') continue
            val name = this[i - 2] as? String ?: continue
            val function =
                functions0[name] ?: functions0[name.lowercase(Locale.getDefault())] ?: throw UnknownFunction(name, "x")
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
            val function =
                functions1[name] ?: functions1[name.lowercase(Locale.getDefault())] ?: throw UnknownFunction(name, "x")
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
                functions2[name] ?: functions2[name.lowercase(Locale.getDefault())] ?: throw UnknownFunction(name, "x,y")
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
            val function =
                functions3[name] ?: functions3[name.lowercase(Locale.getDefault())] ?: throw UnknownFunction(name, "x,y,z")
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
            val function =
                functions4[name] ?: functions4[name.lowercase(Locale.getDefault())] ?: throw UnknownFunction(name, "x,y,z,w")
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
     * our simple expression language is meant for simple stuff only anyways
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
            val function =
                functions5[name] ?: functions5[name.lowercase(Locale.getDefault())] ?: throw UnknownFunction(name, "a,b,c,d,e")
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

    val unaryFunctions = functions1
    val binaryFunctions = functions2

    val constants = HashMap<String, Double>()

    init {

        constants["pi"] = Math.PI
        constants["e"] = Math.E
        constants["°"] = Math.toDegrees(1.0)

        functions0["rand"] = { Math.random() }

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
        functions1["cbrt"] = { cbrt(it) }
        functions1["root"] = functions1["sqrt"]!!

        functions2["root"] = { n, number -> StrictMath.pow(number, 1.0 / n) }

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
        functions2["length"] = { a, b -> sqrt(a * a + b * b) }
        functions3["length"] = { a, b, c -> sqrt(a * a + b * b + c * c) }
        functions4["length"] = { a, b, c, d -> sqrt(a * a + b * b + c * c + d * d) }

        functions1["abs"] = { abs(it) }
        functions1["absolute"] = functions1["abs"]!!


        // sin,cos,tan, with degrees
        functions1["sin"] = { sin(Math.toRadians(it)) }
        functions1["sine"] = functions1["sin"]!!
        functions1["asin"] = { Math.toDegrees(asin(it)) }
        functions1["arcsine"] = functions1["asin"]!!
        functions1["sinh"] = { sinh(it) }
        functions1["asinh"] = { asinh(it) }

        functions1["cos"] = { cos(Math.toRadians(it)) }
        functions1["cosine"] = functions1["cos"]!!
        functions1["acos"] = { Math.toDegrees(acos(it)) }
        functions1["arccosine"] = functions1["acos"]!!
        functions1["cosh"] = { cosh(it) }
        functions1["acosh"] = { acosh(it) }

        functions1["tan"] = { tan(Math.toRadians(it)) }
        functions1["tangent"] = functions1["tan"]!!
        functions2["atan"] = { x, y -> atan2(x, y) }
        functions2["arctan"] = functions2["atan"]!!
        functions2["atan2"] = functions2["atan"]!!
        functions1["atanh"] = { atanh(it) }

        functions1["exp"] = { exp(it) }
        functions2["pow"] = { base, exponent -> StrictMath.pow(base, exponent) }
        functions2["power"] = functions2["pow"]!!

        functions1["floor"] = { floor(it) }
        functions1["round"] = { round(it) }
        functions1["ceil"] = { ceil(it) }

        // random
        functions1["rand"] = { seed -> Random(seed.toLong()).nextDouble() }
        functions2["rand"] = { seed, x -> OpenSimplexNoise(seed.toLong()).eval(x, 0.0) }
        functions3["rand"] = { seed, x, y -> OpenSimplexNoise(seed.toLong()).eval(x, y) }
        functions4["rand"] = { seed, x, y, z -> OpenSimplexNoise(seed.toLong()).eval(x, y, z) }
        functions5["rand"] = { seed, x, y, z, w -> OpenSimplexNoise(seed.toLong()).eval(x, y, z, w) }

        // todo function, which takes double and vector...
        /*functions2["harmonics"] = { time, harmonics ->
            val w0 = time * 2.0 * PI
            harmonics as Vector
            harmonics.data.withIndex().sumByDouble { (index, it) -> it * sin((index + 1f) * w0) }
        }*/

    }


}