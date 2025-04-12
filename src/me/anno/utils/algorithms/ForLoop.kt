package me.anno.utils.algorithms

object ForLoop {

    /**
     * for loop without allocations
     * */
    @JvmStatic
    inline fun forLoop(start: Int, endExcl: Int, step: Int, process: (i: Int) -> Unit) {
        var i = start
        while (i < endExcl) {
            process(i)
            i += step
        }
    }

    /**
     * for loop without allocations, and i+step <= size
     * */
    @JvmStatic
    inline fun forLoopSafely(size: Int, step: Int, process: (i: Int) -> Unit) {
        forLoop(0, size + 1 - step, step, process)
    }
}