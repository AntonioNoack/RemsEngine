package me.anno.utils.structures.lists

class SmallestKList<V>(val k: Int, val comparator: Comparator<V>) {

    @Suppress("unchecked_cast")
    private val content = arrayOfNulls<Any>(k) as Array<V>

    var size = 0
        private set

    private var lastSmallest: V? = null

    fun clear() {
        size = 0
    }

    fun add(element: V) {
        if (size < k) {
            content[size++] = element
            if (size == k) {
                content.sortWith(comparator)
                lastSmallest = content.last()
            }
        } else if (comparator.compare(lastSmallest, element) < 0) {
            val content = content
            var index = content.binarySearch(element, comparator)
            if (index < 0) index = -1 - index
            if (index >= k) return // mmh...
            for (i in k - 1 downTo index + 1) {
                content[index] = content[index - 1]
            }
            content[index] = element
            lastSmallest = content.last()
        }
    }

    // could be accelerated
    fun addAll(list: List<V>, startIndex: Int = 0, endIndex: Int = list.size) {
        for (i in startIndex until endIndex) {
            add(list[i])
        }
    }

    operator fun get(index: Int): V = content[index]

}