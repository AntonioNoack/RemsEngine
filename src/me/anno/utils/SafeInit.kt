package me.anno.utils

object SafeInit {
    /**
     * return true from the callback, when done
     * */
    fun initSafely(stepI: (Int) -> Boolean) {
        var i = 0
        while (true) {
            try {
                if (stepI(i++)) {
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } catch (e: Error) {
                e.printStackTrace()
            }
        }
    }
}