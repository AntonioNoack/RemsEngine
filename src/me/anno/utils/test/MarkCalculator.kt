package me.anno.utils.test

import me.anno.mesh.fbx.f2xI
import me.anno.mesh.fbx.p

// Jasmin was my ex-crush
fun markCalculatorForJasmin() {

    val marks = 10..22
    val markCount = 13..15

    fun line() {
        for (mark in markCount) {
            print("------------------------")
        }
        println("----------")
    }

    print("    |  ")
    for (mark in markCount) {
        print("          $mark            ")
    }
    println()
    line()
    fun format(ones: Int, twos: Int, threes: Int) {
        // val durchschnitt = (einsen + zweien * 2 + dreien * 3) * 1f / (einsen + zweien + dreien)
        //  (${durchschnitt.f2()})
        print("${ones.f2xI(1)}${ones.p()} ${twos.f2xI(2)}${threes.p()} ${threes.f2xI(3)} | ")
    }
    for (mark in marks) {
        if (mark % 10 < 2) line()
        if (mark % 10 == 0) {
            print("${mark / 10},0 | ")
            for (markC in markCount) {
                when (mark) {
                    10 -> format(markC, 0, 0)
                    20 -> format(0, markC, 0)
                    30 -> format(0, 0, markC)
                }
            }
            println()
        } else {
            val mark2 = mark * 0.1
            print("${mark / 10},${mark % 10} | ")
            for (markC in markCount) {
                val rest = mark2 + 0.0499 - (mark / 10)
                val twos = (rest * markC).toInt()
                val ones = markC - twos
                if (mark > 20) {
                    format(0, ones, twos)
                } else {
                    format(ones, twos, 0)
                }
            }
            println()
        }
    }
}

fun main(){
    markCalculatorForJasmin()
}