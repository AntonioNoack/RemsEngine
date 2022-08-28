package me.anno.utils.structures.lists

class CountingList(capacity: Int = 16): MutableList<Any> {

    constructor(src: CountingList): this(src.size){
        internal.addAll(src.internal)
        for(i in counters.indices){
            counters[i] = src.counters[i]
        }
    }

    private val internal = ArrayList<Any>(capacity)
    private val counters = IntArray(isCounted.size)

    fun update(char: Char, delta: Int) {
        val ci = char.code - minCounted
        if (ci in counters.indices) {
            counters[ci] += delta
        }
    }

    override fun add(element: Any): Boolean {
        if(element is Char) update(element, 1)
        return internal.add(element)
    }

    override fun removeAt(index: Int): Any {
        val oldElement = internal.removeAt(index)
        if(oldElement is Char) update(oldElement, -1)
        return oldElement
    }

    override fun contains(element: Any): Boolean {
        if(element is Char) {
            val ci = element.code - minCounted
            if (ci < isCounted.size && isCounted[ci]) {
                return counters[ci] > 0
            }
        }
        return internal.contains(element)
    }

    override fun get(index: Int): Any = internal[index]
    override val size: Int get() = internal.size
    override fun isEmpty() = internal.isEmpty()
    override fun iterator(): MutableIterator<Any> = internal.iterator()
    override fun lastIndexOf(element: Any) = internal.lastIndexOf(element)
    override fun listIterator() = internal.listIterator()
    override fun listIterator(index: Int) = internal.listIterator(index)

    override fun set(index: Int, element: Any): Any {
        val old = internal.set(index, element)
        if(old != element){
            if(old is Char) update(old, -1)
            if(element is Char) update(element, 1)
        }
        return old
    }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<Any> = internal.subList(fromIndex, toIndex)

    override fun indexOf(element: Any): Int = internal.indexOf(element)

    override fun add(index: Int, element: Any) = throw notSupported
    override fun addAll(elements: Collection<Any>) = throw notSupported
    override fun addAll(index: Int, elements: Collection<Any>) = throw notSupported

    override fun remove(element: Any): Boolean = throw notSupported
    override fun removeAll(elements: Collection<Any>): Boolean = throw notSupported
    override fun retainAll(elements: Collection<Any>): Boolean = throw notSupported

    override fun containsAll(elements: Collection<Any>) = internal.containsAll(elements)

    override fun clear() {
        internal.clear()
        for(i in counters.indices){
            counters[i] = 0
        }
    }

    override fun toString() = internal.toString()

    companion object {
        private val notSupported get() = RuntimeException("Operation not supported, because of laziness ;)")
        private const val countedCharacters = "+-/*^!()[]"
        private val minCounted = countedCharacters.minOrNull()!!.code
        private val maxCountedChar = countedCharacters.maxOrNull()!!
        private val maxCounted = countedCharacters.maxOrNull()!!.code
        private val isCounted = BooleanArray(maxCounted + 1 - minCounted)
        fun Any.isCounted() = this is Char && this.code < maxCountedChar.code
        init {
            countedCharacters.forEach {
                isCounted[it.code - minCounted] = true
            }
        }
    }

}