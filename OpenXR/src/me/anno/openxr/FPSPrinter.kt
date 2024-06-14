package me.anno.openxr

class FPSPrinter {

    var t0 = System.nanoTime()
    var fps = 0

    fun nextFrame() {
        fps++
        val t1 = System.nanoTime()
        if (t1 - t0 > 1e9) {
            t0 = t1
            println("Running with $fps fps")
            fps = 0
        }
    }
}