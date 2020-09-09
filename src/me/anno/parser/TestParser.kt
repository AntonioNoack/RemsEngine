package me.anno.parser

import me.anno.parser.SimpleExpressionParser.parseDouble

fun main(){

    /**
     - used symbols: +*-/^
     - behaviour is very similar for + only (just 3x-4x faster)
     - numbers for +*-/^ :
          1372 chars/s for length 33
         69907 chars/s for length 65
         74420 chars/s for length 129
         81920 chars/s for length 257
        110263 chars/s for length 513
        110051 chars/s for length 1025
        193358 chars/s for length 2049
        107290 chars/s for length 4097
        114492 chars/s for length 8193
         62834 chars/s for length 16385
         32616 chars/s for length 32769
         14698 chars/s for length 65537
          6798 chars/s for length 131073
      - performance could be further improved with a cached linked list, or a hybrid
     * */

    /*for(logLength in 4 until 20){
        val length = 1 shl logLength
        val symbols = "+*-/^"
        val long = StringBuilder(length + 2)
        long.append("5")
        for(i in 0 until length){
            long.append(symbols.random())
            long.append('1')
        }
        val symbolCount = length * 2 + 1
        val t0 = System.nanoTime()
        parseDouble(long.toString())
        val t1 = System.nanoTime()
        val dt = t1 - t0
        val charsPerSecond = symbolCount * 1e9f / dt
        println("$charsPerSecond chars/s for length $symbolCount")
    }*/

    test("(17.15)", 17.15)
    test("1 + 3", 4.0)
    test("sin(90)", 1.0)
    test("sin(90.0e0)", 1.0)
    test("1 + 50%", 1.5)
    test("1 - 20%", 0.8)
    test("1 + 3 * 5", 16.0)
    test("3 * 5 + 1", 16.0)
    test("3 / 5 + 1", 1.6)
    test("3 / 5 * 5 + 1", 4.0)
    test("[1,2,3][0]", 1.0)
    test("[1,2,3][0.5]", 1.5)

}

fun test(expr: String, value: Double){
    assert(parseDouble(expr), value)
}

fun assert(a: Double?, b: Double){
    if(a != b) throw RuntimeException("Expected $b, but got $a")
}