package me.anno.utils.types

object Sets {

    fun <A, B> Set<A>.cross(other: Set<B>): Set<Pair<A, B>> {
        val result = HashSet<Pair<A, B>>(size * other.size)
        for (a in this) {
            for (b in other) {
                result += a to b
            }
        }
        return result
    }

    fun <A, B, C> Set<A>.cross(other: Set<B>, other2: Set<C>): Set<Triple<A, B, C>> {
        val result = HashSet<Triple<A, B, C>>(size * other.size * other2.size)
        for (a in this) {
            for (b in other) {
                for (c in other2) {
                    result += Triple(a, b, c)
                }
            }
        }
        return result
    }

}