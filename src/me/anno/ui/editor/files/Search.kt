package me.anno.ui.editor.files

import me.anno.maths.Maths.min
import me.anno.utils.structures.lists.Lists.any2
import kotlin.math.max

class Search(val terms: String) {

    override fun equals(other: Any?): Boolean {
        return other is Search && other.terms == terms
    }

    override fun hashCode(): Int {
        return terms.hashCode()
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
        if (terms == other.terms) return true
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
        while (i < terms.length) {
            when (val char = terms[i]) {
                '"', '\'' -> {
                    i++
                    var str = ""
                    string@ while (i < terms.length) {
                        when (val char2 = terms[i]) {
                            char -> break@string
                            '\\' -> {
                                i++
                                if (i < terms.length) {
                                    str += when (val char3 = terms[i]) {
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
                '|', '&', '!' -> {
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
                    string@ while (i < terms.length) {
                        when (val char2 = terms[i]) {
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

    private fun CharSequence.containsPieces(part: CharSequence, ignoreCase: Boolean): Boolean {
        @Suppress("SpellCheckingInspection")
        // search: sdfelioid, target: sdf ellipsoid, -> return true
        var index = 0
        for (partIndex in part.indices) {
            val letter = part[partIndex]
            val nextIndex = indexOf(letter, index, ignoreCase)
            if (nextIndex < 0) return false
            index = max(index + 1, nextIndex)
        }
        return true
    }

    fun matches(name: CharSequence?): Boolean {
        if (name == null) return false
        if (expression.isEmpty()) return true
        val expr = ArrayList(expression)
        // replace all things
        for (i in expr.indices) {
            val term = expr[i] as? String ?: continue
            val value = name.containsPieces(term, true)
            expr[i] = value
        }
        return matches(expr)
    }

    fun matches(expr: ArrayList<Any>): Boolean {
        if (expr.any2 { it is String }) throw IllegalArgumentException()
        for (i in 0 until expr.size - 2) {
            if (expr[i] == '(' && expr[i + 2] == ')') {
                expr.removeAt(i + 2)
                expr.remove(i)
                return matches(expr)
            }
        }
        for (i in 0 until expr.size - 1) {
            val b = expr[i + 1]
            if (expr[i] == '!' && b is Boolean) {
                expr[i] = !b
                expr.removeAt(i + 1)
                return matches(expr)
            }
        }
        for (i in 0 until expr.size - 1) {
            val a = expr[i]
            val b = expr[i + 1]
            if (a is Boolean && b is Boolean) {
                expr[i] = a && b
                expr.removeAt(i + 1)
                return matches(expr)
            }
        }
        for (i in 0 until expr.size - 2) {
            if (expr[i + 1] == '|') {
                val a = expr[i] as? Boolean ?: continue
                val b = expr[i + 2] as? Boolean ?: continue
                expr[i] = a || b
                expr.removeAt(i + 2)
                expr.removeAt(i + 1)
                return matches(expr)
            }
        }
        if (expr.size >= 1) return expr[0] as? Boolean ?: true
        return true
    }
}