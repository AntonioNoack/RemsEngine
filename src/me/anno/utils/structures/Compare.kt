package me.anno.utils.structures

object Compare {

    inline fun Int.ifDifferent(func: () -> Int): Int {
        return if (this == 0) func() else this
    }

}