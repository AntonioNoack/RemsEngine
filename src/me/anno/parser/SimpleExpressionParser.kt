package me.anno.parser

import me.anno.parser.Functions.applyFunc1
import me.anno.parser.Functions.applyFunc2
import me.anno.parser.Functions.applyFunc3
import me.anno.parser.Functions.applyFunc4
import me.anno.parser.Functions.applyFunc5
import me.anno.parser.Functions.constants
import java.lang.Exception
import java.lang.RuntimeException
import java.lang.StrictMath.pow
import kotlin.math.max

/**
 * intended for SMALL calculations
 * doesn't care about helpful errors much or top performance
 * no assignments, because it shall be small;
 * no intention for global variables (because it's for artists; it must be good enough via other means)
 * todo array, just in case :D
 * todo ... means repeat last element, whereas otherwise 0?
 * */
object SimpleExpressionParser {

    fun preparse(str: String) = str.splitInternally()

    fun String.splitInternally(): MutableList<Any> {

        val list = CountingList(max(2, length / 3))
        var i0 = 0
        var i = -1

        fun putRemaining(){
            if(i > i0){
                list += substring(i0, i)
                // println("put remaining ${list.last()} at char ${this[i]}")
            }// else println("put nothing at ${this.getOrNull(i)}")
            i0 = i+1
        }

        while(++i < length){

            when(val char = this[i]){
                in '0' .. '9' -> {
                    putRemaining()
                    var j = i
                    searchDigits@ while(++j < length){
                        when(this[j]){
                            in '0' .. '9', '.' -> {}
                            'e', 'E' -> {
                                j++
                                when(this.getOrNull(j)){
                                    '+', '-' -> { j++ }
                                    in '0' .. '9' -> { }
                                    null -> throw RuntimeException("Number without full exponent!")
                                }
                                when(this.getOrNull(j)){
                                    in '0' .. '9' -> { j++ }
                                    null -> throw RuntimeException("Number without full exponent!")
                                }
                                while(j < length){
                                    when(this[j]){
                                        in '0' .. '9' -> { j++ }
                                        else -> break@searchDigits
                                    }
                                }
                            }
                            else -> break@searchDigits
                        }
                    }
                    val number = substring(i, j)
                    list += number.toDoubleOrNull() ?: throw RuntimeException("Invalid number: $number")
                    i = j-1
                    i0 = i+1
                }
                '+', '-', '*', '/', '(', '[', '{', '}', ']', ')', '|', '&', ',', ';', '^', '!', '~', '%' -> {
                    putRemaining()
                    list += char
                }
                ' ' -> putRemaining()
                else -> {}
            }

        }

        putRemaining()

        return list

    }

    private fun MutableList<Any>.joinSigns(): Boolean {
        for(i in 1 until size){
            val number = this[i]
            if(!number.isValue()) continue
            if(i == 1 || when(this[i-2]){
                    is Double, is Vector -> false
                    '*', '/', '^' -> true
                    else -> false
                }){
                when(this[i-1]){
                    '+' -> {
                        removeAt(i-1)
                        return true
                    }
                    '-' -> {
                        this[i-1] = mulAny(-1.0, number)
                        removeAt(i)
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun MutableList<Any>.applyFunctions(): Boolean {
        return applyFunc1() or applyFunc2() or applyFunc3() or applyFunc4() or applyFunc5()
    }

    private fun MutableList<Any>.applyBrackets(): Boolean {
        for(i in 2 until size){
            if(this[i-2] != '(') continue
            if(this[i] != ')') continue
            removeAt(i)
            removeAt(i-2)
            return true
        }
        return false
    }

    private fun MutableList<Any>.applyPower(): Boolean {
        loop@ for(i in 2 until size){
            when(this[i-1]){
                '^' -> {}
                else -> continue@loop
            }
            val a = this[i-2] as? Double ?: continue
            val b = this[i] as? Double ?: continue
            removeAt(i)
            removeAt(i-1)
            this[i-2] = pow(a, b)
            return true
        }
        return false
    }

    private fun MutableList<Any>.applyPercentages(): Boolean {
        loop@ for(i in 2 until size){
            // +/- 30%
            if(this[i] != '%') continue
            val isAddition = when(this[i-2]){
                '+' -> true
                '-' -> false
                else -> continue@loop
            }
            val a = this[i-1]
            if(!a.isValue()) continue
            removeAt(i)
            // 1.0 +/- 0.01 * a
            val temp = mulAny(a, 0.01)
            this[i-1] = if(isAddition) addAny(1.0, temp) else subAny(1.0, temp)
            this[i-2] = '*'
            return true
        }
        return false
    }

    private fun MutableList<Any>.applyMultiplication(): Boolean {
        loop@ for(i in 2 until size){
            val isMultiplication = when(this[i-1]){
                '*', "dot" -> 0
                "x", "cross" -> 1 // mmmh...
                '/', "div" -> 2
                else -> continue@loop
            }
            val a = this[i-2]
            val b = this[i]
            if(!a.isValue() || !b.isValue()) continue
            removeAt(i)
            removeAt(i-1)
            this[i-2] = when(isMultiplication){
                0 -> mulAny(a, b)
                1 -> crossAny(a, b)
                else -> divAny(a, b)
            }
            return true
        }
        return false
    }

    private fun MutableList<Any>.applyAdditions(): Boolean {
        loop@ for(i in 2 until size){
            val isAddition = when(this[i-1]){
                '+' -> true
                '-' -> false
                else -> continue@loop
            }
            val a = this[i-2] as? Double ?: continue
            val b = this[i] as? Double ?: continue
            removeAt(i)
            removeAt(i-1)
            this[i-2] = if(isAddition) a + b else a - b
            return true
        }
        return false
    }

    private fun MutableList<Any>.replaceConstants(constants: Map<String, Any>?) {
        if(constants == null) return
        for(i in indices){
            val name = this[i] as? String ?: continue
            if(getOrNull(i+1) != '('){
                val replacement = constants[name] ?: constants[name.toLowerCase()]
                if(replacement != null){
                    this[i] = replacement
                }
            }
        }
    }

    fun Any?.isValue() = when(this){
        is Vector -> isClosed
        is Double -> true
        else -> false
    }

    // todo multi dimensional arrays?
    // todo pairs? true vectors? (x,y,z)
    private fun MutableList<Any>.findVectors(): Boolean {
        var wasChanged = false
        // searched: [ to open a vector
        loop@ for(i in 0 until size){
            if(this[i] == '['){
                this[i] = Vector()
                wasChanged = true
            }
        }
        // searched: ] to close a vector
        loop@ for(i in 1 until size){
            val vector = this[i-1] as? Vector ?: continue
            if(this[i] == ']' && !vector.isClosed){
                removeAt(i)
                vector.close()
                return true
            }
        }
        // searched: v 5 ,/]
        loop@ for(i in 2 until size){
            val vector = this[i-2] as? Vector ?: continue
            when(val symbol = this[i]){
                ',', ']' -> {
                    val value = this[i-1]
                    if(value.isValue()){
                        vector.data.add(value)
                        if(symbol == ',') removeAt(i)
                        removeAt(i-1)
                        return true
                    }
                }
            }
        }
        // array access
        loop@ for(i in 1 until size){
            val vector = this[i-1] as? Vector ?: continue
            val indices = this[i] as? Vector ?: continue
            if(indices.isClosed && indices.data.size < 2){
                val value = when(val index = indices.data.getOrNull(0) ?: 0.0){
                    is Double -> vector[index]
                    else -> throw RuntimeException("Index type $index not (yet) supported!")
                } ?: 0.0
                removeAt(i)
                this[i-1] = value
            }
        }
        return wasChanged
    }

    fun parseDouble(expr: String) = parseDouble(expr, null)
    fun parseDouble(expr: String, additionalConstants: Map<String, Double>?): Double? {
        return try {
            parseDouble(expr.splitInternally(), additionalConstants)
        } catch (e: Exception){
            println(e.message)
            // e.printStackTrace()
            null
        }
    }


    var knownMessages = HashSet<String>()
    fun parseDouble(parts: MutableList<Any>?) = parseDouble(parts, null)
    fun parseDouble(parts: MutableList<Any>?, additionalConstants: Map<String, Any>?): Double? {

        if(parts == null) return null

        try {

            parts.replaceConstants(additionalConstants)
            parts.replaceConstants(constants)

            // simplify the expression until empty
            // performance of long strings is improved by CountingList and skipping of steps
            while(parts.size > 1){
                if('%' in parts && parts.applyPercentages()) continue
                if(parts.findVectors()) continue
                if('(' in parts){
                    if(parts.applyFunctions()) continue
                    if(parts.applyBrackets()) continue
                }
                if('^' in parts && parts.applyPower()) continue
                if(('*' in parts || '/' in parts) && parts.applyMultiplication()) continue
                if(('+' in parts || '-' in parts)){
                    if(parts.applyAdditions()) continue
                    if(parts.joinSigns()) continue
                }
                break
            }

            return when(parts.size){
                0 -> null
                1 -> parts[0] as? Double
                else -> {
                    println("[WARN] Couldn't understand $parts")
                    null
                }
            }

        } catch (e: Exception){
            val msg = e.message
            if(msg != null && msg !in knownMessages){
                knownMessages.add(msg)
                println(e.message)
            }
            // e.printStackTrace()
            return null
        }

    }


}