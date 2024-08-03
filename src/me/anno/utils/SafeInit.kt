package me.anno.utils

object SafeInit {
    /**
     * return true from the callback, when done
     * */
    inline fun initSafely(stepI: (Int) -> Boolean) {
        var i = 0
        while (true) {
            try {
                if (stepI(i++)) {
                    return
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }
}