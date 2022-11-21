package me.anno.tests

import kotlin.math.sqrt

fun main() {
    var x = 294053760
    for (i in 2..sqrt(x.toFloat()).toInt()) {
        while (x % i == 0) {
            print("$i * ")
            x /= i
        }
    }
    println(x)
}

fun countTeiler() {
    val x = 294053760
    var c = 0
    for (i in 1..x) {
        if (x % i == 0) c++
    }
    println(c)
}