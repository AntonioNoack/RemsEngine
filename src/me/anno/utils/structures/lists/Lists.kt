package me.anno.utils.structures.lists

import me.anno.graph.knn.Heap
import kotlin.math.max

object Lists {

    fun <V> List<V>.asMutableList(): MutableList<V> {
        return if (this is MutableList<V> && javaClass.name != "java.util.Collections\$SingletonList") this
        else ArrayList(this)
    }

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

    /*fun <V> Iterable<V>.sumByFloat(func: (V) -> Float): Float {
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
    }*/

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

    fun <A, B, C> List<A>.crossMap(other: List<B>, map: (a: A, b: B) -> C): List<C> {
        val result = ArrayList<C>(size * other.size)
        for (a in this) {
            for (b in other) {
                result += map(a, b)
            }
        }
        return result
    }

    fun <A, B, C> List<A>.cross(other: List<B>, other2: List<C>): List<Triple<A, B, C>> {
        val result = ArrayList<Triple<A, B, C>>(size * other.size * other2.size)
        for (a in this) {
            for (b in other) {
                for (c in other2) {
                    result += Triple(a, b, c)
                }
            }
        }
        return result
    }

    fun <V> List<List<V>?>.flatten(): ArrayList<V> {
        val list = ArrayList<V>(sumOf { it?.size ?: 0 })
        for (partialList in this) {
            if (partialList != null) {
                list.addAll(partialList)
            }
        }
        return list
    }

    fun <X, Y : Comparable<Y>> List<X>.smallestKElementsBy(k: Int, getValue: (X) -> Y): List<X> {
        return if (size <= k) {
            this
        } else {
            val comp2 = { a: X, b: X -> getValue(a).compareTo(getValue(b)) }
            this.smallestKElements(k, comp2)
        }
    }

    fun <X> List<X>.smallestKElements(k: Int, comparator: Comparator<X>): List<X> {
        return if (size <= k) {
            this
        } else {
            val topK = ArrayList<X>(k)
            for (j in 0 until k) topK.add(this[j])
            topK.sortWith(comparator)
            var lastBest = topK.last()
            for (j in k until this.size) {
                val element = this[j]
                if (comparator.compare(element, lastBest) < 0) {
                    var index = topK.binarySearch(element, comparator)
                    if (index < 0) index = -1 - index // insert index
                    for (l in k - 1 downTo index + 1) {
                        topK[l] = topK[l - 1]
                    }
                    topK[index] = element
                    lastBest = topK.last()
                }
            }
            topK
        }
    }

    fun <X, Y : Comparable<Y>> List<X>.largestKElementsBy(k: Int, comparator: (X) -> Y): List<X> {
        return if (size <= k) {
            this
        } else {
            val comp2 = { a: X, b: X -> comparator(a).compareTo(comparator(b)) }
            this.largestKElements(k, comp2)
        }
    }

    fun <X> List<X>.largestKElements(k: Int, comparator: Comparator<X>): List<X> {
        return if (size <= k) {
            this
        } else {
            smallestKElements(k) { a, b -> -comparator.compare(a, b) }
        }
    }

    fun <X> List<X>.buildMaxHeap(comparator: Comparator<X>): ArrayList<X> {
        val list = ArrayList(this)
        Heap.buildMaxHeap(list, comparator)
        return list
    }

    fun <X> ArrayList<X>.extractMax(k: Int, comparator: Comparator<X>): List<X> {
        val list = ArrayList<X>(k)
        for (i in 0 until k) {
            list.add(Heap.extractMax(this, comparator))
        }
        return list
    }

    fun <X> List<X>.buildMinHeap(comparator: Comparator<X>): ArrayList<X> {
        val list = ArrayList(this)
        Heap.buildMinHeap(list, comparator)
        return list
    }

    fun <X> ArrayList<X>.extractMin(k: Int, comparator: Comparator<X>): List<X> {
        val list = ArrayList<X>(k)
        for (i in 0 until k) {
            list.add(Heap.extractMin(this, comparator))
        }
        return list
    }

    fun <V> createArrayList(size: Int, createElement: (index: Int) -> V): ArrayList<V> {
        val result = ArrayList<V>(size)
        for (i in 0 until size) result.add(createElement(i))
        return result
    }

    @Suppress("UNCHECKED_CAST")
    fun <V> List<List<V>>.transposed(): List<List<V>> {
        if (isEmpty()) return emptyList()
        val m = size
        val n = maxByOrNull { it.size }!!.size
        return createArrayList(n) { i ->
            createArrayList(m) { j ->
                this[j].getOrNull(i) as V // could be null, V may be nullable,
                // so indeed this could introduce errors, when not all lists are long enough
            }
        }
    }

    fun <V> MutableList<ArrayList<V>>.transpose(): MutableList<ArrayList<V>> {
        // to do function, which is in-place-transpose?
        if (isEmpty()) return this
        val m = size
        val n = maxByOrNull { it.size }!!.size
        // make pseudo square: enough for both formats
        // ensure size
        for (i in m until n) {
            add(ArrayList())
        }
        // in all sub-lists, ensure size
        for (i in 0 until n) {
            val listI = this[i]
            for (j in listI.size until m) {
                @Suppress("UNCHECKED_CAST")
                listI.add(null as V)
            }
        }
        // transpose
        for (i in 1 until max(m, n)) {
            @Suppress("UNCHECKED_CAST")
            for (j in 0 until i) {
                val thisI = this[i]
                val thisJ = this[j]
                // this[j][i] = this[i].set(j, this[j][i]) ^^
                val t = thisJ.getOrNull(i) as V
                if (i < thisJ.size) thisJ[i] = thisI.getOrNull(j) as V
                thisI[j] = t
            }
        }
        // remove all unnecessary lists
        for (i in size - 1 downTo n) {
            removeAt(i)
        }
        // in all sub-lists, remove the unnecessary items
        for (i in 0 until n) {
            val listI = this[i]
            for (j in listI.size - 1 downTo m) {
                listI.removeAt(j)
            }
        }
        return this
    }

    inline fun <reified Type> Iterable<*>.firstInstanceOrNull() =
        firstOrNull { it is Type } as? Type

    inline fun <reified Type> Sequence<*>.firstInstanceOrNull() =
        firstOrNull { it is Type } as? Type

    @JvmStatic
    fun main(args: Array<String>) {
        val l0 = arrayListOf(arrayListOf(1, 2, 3), arrayListOf(9, 8, 7))
        println(l0)
        println(l0.transpose())
        println(l0.transpose())
    }

}