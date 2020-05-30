package me.anno.parser

import me.anno.parser.Functions.applyFunc1
import me.anno.parser.Functions.applyFunc2
import me.anno.parser.Functions.applyFunc3
import me.anno.parser.Functions.applyFunc4
import me.anno.parser.Functions.applyFunc5
import java.lang.Exception
import java.lang.RuntimeException
import java.lang.StrictMath.pow
import kotlin.math.max

/**
 * intended for SMALL calculations
 * doesn't care about helpful errors much or top performance
 * */
object SimpleExpressionParser {

    private fun String.split(): MutableList<Any> {

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
            val number = this[i] as? Double ?: continue
            if(i == 1 || when(this[i-2]){
                    is Double -> false
                    '*', '/', '^' -> true
                    else -> false
                }){
                when(this[i-1]){
                    '+' -> {
                        removeAt(i-1)
                        return true
                    }
                    '-' -> {
                        this[i-1] = -number
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
            val a = this[i-1] as? Double ?: continue
            removeAt(i)
            this[i-1] = if(isAddition) (1.0 + 0.01 * a) else (1.0 - 0.01 * a)
            this[i-2] = '*'
            return true
        }
        return false
    }

    private fun MutableList<Any>.applyMultiplication(): Boolean {
        loop@ for(i in 2 until size){
            val isMultiplication = when(this[i-1]){
                '*' -> true
                '/' -> false
                else -> continue@loop
            }
            val a = this[i-2] as? Double ?: continue
            val b = this[i] as? Double ?: continue
            removeAt(i)
            removeAt(i-1)
            this[i-2] = if(isMultiplication) a * b else a / b
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

    fun parseDouble(expr: String): Double? {

        try {

            val parts = expr.split()
            // println(parts)

            // simplify the expression until empty
            // performance of long strings is improved by CountingList and skipping of steps
            while(parts.size > 1){
                if('%' in parts && parts.applyPercentages()) continue
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
                    println("Couldn't understand $parts")
                    null
                }
            }

        } catch (e: Exception){
            println(e.message)
            e.printStackTrace()
            return null
        }

    }


}