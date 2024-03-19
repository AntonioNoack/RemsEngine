package me.anno.utils.structures

/**
 * utility functions for collections;
 * currently only used for cross product
 * */
@Suppress("unused")
object Collections {

    @JvmStatic
    fun <A, B, R : MutableCollection<Pair<A, B>>> Collection<A>.cross(
        other: Collection<B>, dst: R
    ): R = crossMap(other, dst) { a, b -> a to b }

    @JvmStatic
    fun <A, B, C, R : MutableCollection<C>> Collection<A>.crossMap(
        other: Collection<B>, dst: R, transform: (A, B) -> C
    ): R {
        if (dst is ArrayList<*>) {
            dst.ensureCapacity(size * other.size)
        }
        for (a in this) {
            for (b in other) {
                    dst += transform(a, b)
            }
        }
        return dst
    }

    @JvmStatic
    fun <A, B, C, R : MutableCollection<Triple<A, B, C>>> Collection<A>.cross(
        other: Collection<B>, other2: Collection<C>, dst: R
    ): R = crossMap(other, other2, dst) { a, b, c -> Triple(a, b, c) }

    @JvmStatic
    fun <A, B, C, D, R : MutableCollection<D>> Collection<A>.crossMap(
        other: Collection<B>, other2: Collection<C>, dst: R, transform: (A, B, C) -> D
    ): R {
        if (dst is ArrayList<*>) {
            dst.ensureCapacity(size * other.size * other2.size)
        }
        for (a in this) {
            for (b in other) {
                for (c in other2) {
                    dst += transform(a, b, c)
                }
            }
        }
        return dst
    }
}