package me.anno.utils.types

object Lists {

    fun List<Double>.median(default: Double): Double {
        return run {
            if (isEmpty()) default
            else sorted()[size / 2]
        }
    }

    fun Iterable<Double>.median(default: Double): Double {
        return toList().run {
            if (isEmpty()) default
            else sorted()[size / 2]
        }
    }

    fun List<Float>.median(default: Float): Float {
        return run {
            if (isEmpty()) default
            else sorted()[size / 2]
        }
    }

    fun Iterable<Float>.median(default: Float): Float {
        return toList().run {
            if (isEmpty()) default
            else sorted()[size / 2]
        }
    }

    fun <V> Iterable<V>.sumByFloat(func: (V) -> Float): Float {
        var sum = 0f
        for (entry in this) {
            sum += func(entry)
            if (sum.isInfinite() || sum.isNaN()) return sum
        }
        return sum
    }

    fun <V> List<V>.sumByFloat(func: (V) -> Float): Float {
        var sum = 0f
        for (entry in this) {
            sum += func(entry)
            if (sum.isInfinite() || sum.isNaN()) return sum
        }
        return sum
    }

    fun <V> List<V>.sumByLong(func: (V) -> Long): Long {
        var sum = 0L
        for (entry in this) {
            sum += func(entry)
        }
        return sum
    }

    fun <V> MutableList<V>.pop(): V? {
        if (isEmpty()) return null
        val last = last()
        removeAt(lastIndex)
        return last
    }

    fun List<Double>.accumulate(): List<Double> {
        val accumulator = ArrayList<Double>()
        var sum = 0.0
        for (value in this) {
            sum += value
            accumulator += sum
        }
        return accumulator
    }

    val <V> Sequence<V>.size get() = count { true }

    fun <V> List<V>.getOrPrevious(index: Int) = if (index > 0) this[index - 1] else this.getOrNull(0)

    fun <V> List<V>.one(func: (V) -> Boolean): Boolean {
        for (entry in this) {
            if (func(entry)) return true
        }
        return false
    }

    fun <V> Set<V>.one(func: (V) -> Boolean): Boolean {
        for (entry in this) {
            if (func(entry)) return true
        }
        return false
    }

    fun <V> Sequence<V>.one(func: (V) -> Boolean): Boolean {
        for (entry in this) {
            if (func(entry)) return true
        }
        return false
    }

    fun <K, V> Map<K, V>.one(func: (Map.Entry<K, V>) -> Boolean): Boolean {
        for (entry in this) {
            if (func(entry)) return true
        }
        return false
    }

    fun <V> List<V>.indexOf2(v: V, i0: Int, minus1: Boolean): Int {
        for (i in i0 until size) {
            if (this[i] == v) return i
        }
        return if (minus1) -1 else size
    }

    fun <V> List<List<V>>.join(): ArrayList<V> {
        val result = ArrayList<V>(sumOf { it.size })
        for (entries in this) result += entries
        return result
    }

    fun <A, B> List<A>.cross(other: List<B>): List<Pair<A, B>> {
        val result = ArrayList<Pair<A, B>>(size * other.size)
        for (a in this) {
            for (b in other) {
                result += a to b
            }
        }
        return result
    }

    fun <A, B,C> List<A>.cross(other: List<B>, other2: List<C>): List<Triple<A, B, C>> {
        val result = ArrayList<Triple<A, B, C>>(size * other.size * other2.size)
        for (a in this) {
            for (b in other) {
                for(c in other2){
                    result += Triple(a, b, c)
                }
            }
        }
        return result
    }


}