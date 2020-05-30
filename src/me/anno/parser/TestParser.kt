package me.anno.parser

import me.anno.parser.SimpleExpressionParser.parseDouble
import java.lang.RuntimeException

fun main(){

    test("1 + 3", 4.0)
    test("sin(90)", 1.0)
    test("sin(90.0e0)", 1.0)
    test("1 + 50%", 1.5)
    test("1 - 20%", 0.8)

}

fun test(expr: String, value: Double){
    assert(parseDouble(expr), value)
}

fun assert(a: Double?, b: Double){
    if(a != b) throw RuntimeException("Expected $b, but got $a")
}