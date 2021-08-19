package me.anno.utils.test

import kotlin.concurrent.thread

fun main() {

    val t0 = System.nanoTime()

    var ctr = 0

    for (i in 0 until 2000) {
        Array(2) {
            thread(name = "CtrTest[$it]") {
                ctr++
            }
        }.forEach {
            it.join()
        }
    }

    val t1 = System.nanoTime()

    println("${(t1 - t0) * 1e-9} $ctr")

}
