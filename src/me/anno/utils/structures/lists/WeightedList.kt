package me.anno.utils.structures.lists

import me.anno.maths.Maths.min
import me.anno.utils.assertions.assertTrue
import me.anno.utils.search.BinarySearch
import me.anno.utils.structures.arrays.DoubleArrayList
import kotlin.math.max

class WeightedList<V>(initialCapacity: Int = 16) {

    val elements = ArrayList<V>(initialCapacity)
    val accumulator = DoubleArrayList(initialCapacity)

    fun weightSum(): Double {
        return if (accumulator.size == 0) 0.0
        else accumulator[accumulator.size - 1]
    }

    fun add(v: V, weight: Double) {
        assertTrue(weight >= 0.0)
        elements.add(v)
        accumulator.add(weightSum() + weight)
    }

    fun clear() {
        elements.clear()
        accumulator.clear()
    }

    fun getOrNull(relativeIndex: Double): V? {
        if (elements.isEmpty()) return null
        return getInterpolated(relativeIndex) { a, b, w -> if (w < 1.0) a else b }
    }

    fun getInterpolated(relativeIndex: Double, interpolate: (V, V, Double) -> V): V? {
        if (elements.isEmpty()) return null
        if (elements.size == 1) return elements[0]
        val absoluteIndex = relativeIndex * weightSum()
        var index = BinarySearch.binarySearch(elements.size) { idx ->
            accumulator[idx].compareTo(absoluteIndex)
        }
        if (index < 0) index = -1 - index
        index = min(index, elements.size - 2)
        val w0 = if (index > 0) accumulator[index - 1] else 0.0
        val w1 = accumulator[index]
        val t = (absoluteIndex - w0) / max(w1 - w0, 1e-308)
        return interpolate(elements[index], elements[index + 1], t)
    }

    val size: Int get() = elements.size
}