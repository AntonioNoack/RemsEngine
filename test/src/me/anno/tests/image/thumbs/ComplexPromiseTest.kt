package me.anno.tests.image.thumbs

import me.anno.utils.async.firstPromise

fun main() {
    // todo test:
    //  two queues,
    firstPromise(listOf(0, 1, 2, 3)) { v, cb ->
        when (v) {
            0, 1, 2 -> cb.err(null)
            17 -> cb.ok(1)
            else -> {
                firstPromise(listOf(1, 2, 3, 4)) { v1, cb1 ->
                    if (v1 > 2) cb1.ok(v1)
                    else cb1.err(null)
                }.then(cb)
            }
        }
    }.then {
        println("success")
    }.catch {
        println("failure")
    }
}