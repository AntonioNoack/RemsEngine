package me.anno.ui.editor.files

class Search(val terms: String) {

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
                '(', '[', '{' -> {
                    expression += '('
                    i++
                }
                ')', ']', '}' -> {
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
                            '|', '&',
                            '(', '[', '{',
                            ')', ']', '}',
                            ' ', '\t' -> {
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
        var length = expression.size
        i = 0
        while (i < length) {
            val element = expression[i]
            if (element == '|' || element == '&') {
                if (expression[i + 1] == element) {
                    expression.removeAt(i + 1)
                    length--
                    continue
                }
            }
            i++
        }
    }

    fun isNotEmpty() = expression.isNotEmpty()
    fun isEmpty() = expression.isEmpty()
    fun matchesAll() = isEmpty()

    fun matches(name: String?): Boolean {
        if (name == null) return false
        if (expression.isEmpty()) return true
        val expr = ArrayList(expression)
        // replace all things
        for (i in expr.indices) {
            val term = expr[i] as? String ?: continue
            val value = name.contains(term, true)
            expr[i] = value
        }
        val result = matches(expr)
        // LOGGER.info("$name x ${this.expr} ? $result")
        return result
    }

    fun matches(expr: ArrayList<Any>): Boolean {
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
        for (i in 0 until expr.size) {
            if (expr[i] is String) throw RuntimeException()
        }
        if (expr.size >= 1) return expr[0] as? Boolean ?: true
        return true
    }

}