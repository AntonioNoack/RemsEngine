package me.anno.tests.bench

import me.anno.utils.Clock.Companion.measure
import me.anno.utils.structures.heap.Heap
import me.anno.utils.structures.lists.Lists.buildMinHeap
import me.anno.utils.structures.lists.Lists.extractMin
import org.junit.jupiter.api.Test

// building a heap, and then extracting a few elements is slower than just collecting the smallest elements
// -> use List.extractMin() if you need something like this
fun <X> ArrayList<X>.extractMinFromHeap(k: Int, comparator: Comparator<X>): List<X> {
    val list = ArrayList<X>(k)
    for (i in 0 until k) {
        list.add(Heap.extractMin(this, comparator))
    }
    return list
}

class SmallestK {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SmallestK().execute(10)
        }
    }

    @Test
    fun testCorrectness() {
        execute(1)
    }

    fun execute(tries: Int) {

        val size = 1 shl 20
        val elements = IntArray(size) { it }
        elements.shuffle()

        val elementList = elements.toList()
        val comparator = { x: Int, y: Int -> x.compareTo(y) }

        val k = 32

        fun check(list: List<Int>) {
            if (list.size != k) throw RuntimeException("Size does not match")
            for (i in 0 until k) {
                if (i !in list) throw RuntimeException("$i is missing in list $list")
            }
        }

        // 233ms
        for (i in 0 until tries) {
            measure("sort-sublist") {
                check(elementList.sorted().subList(0, k))
            }
        }

        // 27ms
        for (i in 0 until tries) {
            measure("heap") {
                check(
                    elementList
                        .buildMinHeap(comparator) // O(n)
                        .extractMinFromHeap(k, comparator)
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

        // 6-7ms, because generic, but still 3x faster compared to the heap :)
        for (i in 0 until tries) {
            measure("n*k generic") {
                val topK = elementList.extractMin(k, comparator)
                check(topK)
            }
        }
    }
}