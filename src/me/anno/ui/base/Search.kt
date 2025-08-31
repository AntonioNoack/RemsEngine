package me.anno.ui.base

import me.anno.maths.Maths.min
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.Lists.any2
import me.anno.utils.structures.lists.Lists.firstInstanceOrNull2
import me.anno.utils.structures.lists.Lists.none2
import kotlin.math.max

class Search(val userInput: String) {

    override fun equals(other: Any?): Boolean {
        return other is Search && other.userInput == userInput
    }

    override fun hashCode(): Int {
        return userInput.hashCode()
    }

    override fun toString(): String {
        return expression.joinToString {
            when (it) {
                is Char -> "'$it'"
                is String -> "\"$it\""
                else -> it.toString()
            }
        }
    }

    fun containsAllResultsOf(other: Search): Boolean {
        if (userInput == other.userInput) return true
        if (expression == other.expression) return true
        if ('!' in expression || '!' in other.expression) {
            // idk how to handle this case; negations would be valued negative or in reverse
            return false
        }
        if (expression.size != other.expression.size) {
            // idk how to handle that
            return false
        }
        var isEasierInAllParts = true
        for (i in 0 until min(expression.size, other.expression.size)) {
            val self = expression[i]
            val oth = other.expression[i]
            if (self == oth) {
                // very good
            } else if (self is String && oth is String) {
                if (oth.startsWith(self, true) || oth.endsWith(self, true)) {
                    // good
                } else {
                    isEasierInAllParts = false
                    break // in our simple algorithm, we can break here
                }
            } else return false // idk how to handle that
        }
        return isEasierInAllParts
    }

    private val expression = ArrayList<Any>()

    init {
        /**
         * parse expression
         * */
        var i = 0
        while (i < userInput.length) {
            when (val char = userInput[i]) {
                '"', '\'' -> {
                    i++
                    var str = ""
                    string@ while (i < userInput.length) {
                        when (val char2 = userInput[i]) {
                            char -> break@string
                            '\\' -> {
                                i++
                                if (i < userInput.length) {
                                    str += when (val char3 = userInput[i]) {
                                        '\\' -> '\\'
                                        else -> char3
                                    }
                                }
                            }
                            else -> {
                                i++
                                str += char2
                            }
                        }
                    }
                    expression += str
                }
                '&' -> i++ // ands are implicit
                '|', '!' -> {
                    expression += char
                    i++
                }
                '(' -> {
                    expression += '('
                    i++
                }
                ')' -> {
                    expression += ')'
                    i++
                }
                ' ', '\t' -> {
                    i++
                }
                else -> {
                    // read string without escapes
                    var str = ""
                    string@ while (i < userInput.length) {
                        when (val char2 = userInput[i]) {
                            '|', '&', '(', ')', ' ', '\t' -> {
                                break@string
                            }
                            else -> {
                                i++
                                str += char2
                            }
                        }
                    }
                    expression += str
                }
            }
        }

        /**
         * remove || and &&, replace them with | and &
         * */
        var length = expression.size - 1
        var j = 0
        while (j < length) {
            val element = expression[j]
            if (element == '|' || element == '&') {
                if (expression[j + 1] == element) {
                    expression.removeAt(j + 1)
                    length--
                    continue
                }
            }
            j++
        }
    }

    fun matchesEverything(): Boolean = expression.isEmpty()

    fun matches(name: CharSequence?): Boolean {
        if (name == null) return false
        if (expression.isEmpty()) return true
        return matches(listOf(name))
    }

    fun matches(names: List<CharSequence>): Boolean {
        if (names.isEmpty()) return false
        if (expression.isEmpty()) return true
        val expr = ArrayList(expression)
        // replace all things
        for (i in expr.indices) {
            val term = expr[i] as? String ?: continue
            val value = names.any2 { name -> containsPieces(name, term) }
            expr[i] = value
        }
        return eval(expr)
    }

    private fun eval(expr: ArrayList<Any>): Boolean {
        assertTrue(expr.none2 { it is String })
        replace@ while (true) {
            val originalLength = expr.size
            // brackets
            var i = 2
            while (i < expr.size) {
                if (expr[i - 2] == '(' && expr[i] == ')') {
                    expr.removeAt(i)
                    expr.removeAt(i - 2)
                    i = max(i - 1, 2)
                } else i++
            }
            // negations
            i = 1
            while (i < expr.size) {
                val b = expr[i]
                if (expr[i - 1] == '!' && b is Boolean) {
                    expr[i - 1] = !b
                    expr.removeAt(i)
                } else i++
            }
            // implicit ands
            val andStartLength = expr.size
            i = 1
            while (i < expr.size) {
                val a = expr[i - 1]
                val b = expr[i]
                if (a is Boolean && b is Boolean) {
                    expr[i - 1] = a and b
                    expr.removeAt(i)
                } else i++
            }
            // ands need a restart, all of them need to be handled before or
            if (expr.size < andStartLength) continue@replace
            // explicit or
            i = 2
            while (i < expr.size) {
                val a = expr[i - 2]
                val b = expr[i]
                if (a is Boolean && b is Boolean && expr[i - 1] == '|') {
                    expr[i - 2] = a or b
                    expr.removeAt(i)
                    expr.removeAt(i - 1)
                    i = max(i - 1, 2)
                } else i++
            }
            // check result
            if (expr.size == 1) return expr[0] as? Boolean ?: true
            if (expr.size < originalLength) continue@replace
            return expr.firstInstanceOrNull2(Boolean::class) ?: true
        }
    }

    companion object {
        fun containsPieces(name: CharSequence, userInput: CharSequence): Boolean {
            @Suppress("SpellCheckingInspection")
            // search: sdfoid, target: sdf ellipsoid, -> return true
            var index = 0
            for (partIndex in userInput.indices) {
                val letter = userInput[partIndex]
                var nextIndex = name.indexOf(letter, index, true)
                if (nextIndex < 0) return false

                // only allowed to skip, if the distance is 0/1, or the next letter is upper-case
                val allowSkip = index == 0 || nextIndex < index + 2 || name[nextIndex].isUpperCase()
                if (!allowSkip) {
                    val nextIndex2 = name.indexOf(letter.uppercase(), nextIndex + 1, false)
                    if (nextIndex2 < 0) {
                        // todo this is a bad solution... we'll probably find samples, where this logic isn't good
                        // this had to be introduced, because "depth test" -> "test" would not match with "Depth Test"
                        return name.contains(userInput,true)
                    }
                    nextIndex = nextIndex2
                }
                index = max(index + 1, nextIndex)
            }
            return true
        }
    }
}