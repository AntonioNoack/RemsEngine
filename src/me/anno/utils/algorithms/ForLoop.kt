package me.anno.utils.algorithms

object ForLoop {

    /**
     * for loop without allocations
     * */
    @JvmStatic
    inline fun forLoop(start: Int, endExcl: Int, step: Int, process: (Int) -> Unit) {
        var i = start
        while (i < endExcl) {
            process(i)
            i += step
        }
    }
}