package me.anno.utils.structures.tuples

data class LongTriple(val first: Long, val second: Long, val third: Long) {
    override fun toString() = "($first,$second,$third)"
}