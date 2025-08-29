package me.anno.utils.types

object Ranges {

    fun IntRange.overlaps(other: IntRange): Boolean {
        return last >= other.first && first <= other.last
    }

    val IntRange.size: Int get() = last + 1 - start
}