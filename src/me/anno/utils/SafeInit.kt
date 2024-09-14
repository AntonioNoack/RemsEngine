package me.anno.utils

object SafeInit {
    /**
     * calls the step method until it returns true;
     * any errors get printed, but don't impact whether the elements are that are run
     *
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