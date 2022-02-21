package me.anno.utils.bench

import me.anno.utils.Clock.Companion.measure
import me.anno.utils.structures.lists.Lists.buildMinHeap
import me.anno.utils.structures.lists.Lists.extractMin

fun main() {

    val size = 1 shl 20
    val elements = IntArray(size) { it }
    elements.shuffle()

    val elementList = elements.toList()

    val k = 32

    fun check(list: List<Int>) {
        if (list.size != k) throw RuntimeException("Size does not match")
        for (i in 0 until k) {
            if (i !in list) throw RuntimeException("$i is missing in list $list")
        }
    }

    // 233ms
    val tries = 10
    for (i in 0 until tries) {
        measure("sort-sublist") {
            check(elementList.sorted().subList(0, k))
        }
    }

    // 27ms
    for (i in 0 until tries) {
        measure("heap") {
            val comparator = { x: Int, y: Int -> x.compareTo(y) }
            check(
                elementList
                    .buildMinHeap(comparator) // O(n)
                    .extractMin(k, comparator)
            )
        }
    }

    // 2.7-3.0ms
    for (i in 0 until tries) {
        measure("n*k") {
            val topK = IntArray(k)
            for (j in 0 until k) topK[j] = elementList[j]
            topK.sort()
            var lastBest = topK.last()
            for (j in k until elementList.size) {
                val element = elementList[j]
                if (element < lastBest) {
                    var index = topK.binarySearch(element)
                    if (index < 0) index = -1 - index // insert index
                    for (l in k - 1 downTo index + 1) {
                        topK[l] = topK[l - 1]
                    }
                    topK[index] = element
                    lastBest = topK.last()
                }
            }
            check(topK.toList())
        }
    }

}