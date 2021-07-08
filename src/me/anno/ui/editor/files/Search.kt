package me.anno.ui.editor.files

import java.lang.RuntimeException

class Search(val term: String) {

    val expr = ArrayList<Any>()

    init {
        parse()
    }

    fun parse() {
        // parse expression
        var i = 0
        while (i < term.length) {
            when (val char = term[i]) {
                '"', '\'' -> {
                    i++
                    var str = ""
                    string@ while (i < term.length) {
                        when (val char2 = term[i]) {
                            char -> break@string
                            '\\' -> {
                                i++
                                if(i < term.length){
                                    str += when (val char3 = term[i]) {
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
                    expr += str
                }
                '|', '&', '!' -> {
                    expr += char
                    i++
                }
                '(', '[', '{' -> {
                    expr += '('
                    i++
                }
                ')', ']', '}' -> {
                    expr += ')'
                    i++
                }
                ' ', '\t' -> { i++ }
                else -> {
                    // read string without escapes
                    var str = ""
                    string@while(i < term.length){
                        when(val char2 = term[i]){
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
                    expr += str
                }
            }
        }

        compress()

    }

    fun compress(){
        for(i in 0 until expr.size-1){
            if(expr[i] == '|' && expr[i+1] == '|'){
                expr.removeAt(i+1)
                return compress()
            }
            if(expr[i] == '&' && expr[i+1] == '&'){
                expr.removeAt(i+1)
                return compress()
            }
            if(expr[i] == '|' && expr[i+1] == '&'){
                expr.removeAt(i+1)
                return compress()
            }
            if(expr[i] == '&' && expr[i+1] == '|'){
                expr.removeAt(i+1)
                return compress()
            }
        }
    }

    fun isNotEmpty() = expr.isNotEmpty()
    fun isEmpty() = expr.isEmpty()
    fun matchesAll() = isEmpty()

    fun matches(name: String): Boolean {
        if (expr.isEmpty()) return true
        val expr = ArrayList(expr)
        // replace all things
        for (i in expr.indices) {
            val term = expr[i] as? String ?: continue
            val value = name.contains(term, true)
            expr[i] = value
        }
        val result = matches(expr)
        // println("$name x ${this.expr} ? $result")
        return result
    }

    fun matches(expr: ArrayList<Any>): Boolean {
        for(i in 0 until expr.size-2){
            if(expr[i] == '(' && expr[i+2] == ')'){
                expr.removeAt(i+2)
                expr.remove(i)
                return matches(expr)
            }
        }
        for(i in 0 until expr.size-1){
            val b = expr[i+1]
            if(expr[i] == '!' && b is Boolean){
                expr[i] = !b
                expr.removeAt(i+1)
                return matches(expr)
            }
        }
        for(i in 0 until expr.size-1){
            val a = expr[i]
            val b = expr[i+1]
            if(a is Boolean && b is Boolean){
                expr[i] = a && b
                expr.removeAt(i+1)
                return matches(expr)
            }
        }
        for(i in 0 until expr.size-2){
            if(expr[i+1] == '|'){
                val a = expr[i] as? Boolean ?: continue
                val b = expr[i+2] as? Boolean ?: continue
                expr[i] = a || b
                expr.removeAt(i+2)
                expr.removeAt(i+1)
                return matches(expr)
            }
        }
        for(i in 0 until expr.size){
            if(expr[i] is String) throw RuntimeException()
        }
        if(expr.size >= 1) return expr[0] as? Boolean ?: true
        return true
    }

}