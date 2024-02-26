package me.anno.utils.structures

object Compare {

    /**
     * if the result 'r' of a comparison is zero,
     * then adjust the value; else return r
     * */
    @JvmStatic
    inline fun Int.ifSame(func: () -> Int): Int {
        return if (this == 0) func() else this
    }


    /**
     * if the result 'r' of a comparison is zero,
     * then adjust the value; else return r
     * */
    @JvmStatic
    fun Int.ifSame(func: Int): Int {
        return if (this == 0) func else this
    }

}