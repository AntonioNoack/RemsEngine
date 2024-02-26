package me.anno.utils.structures.stacks

/**
 * state-tracking stack with onChangeValue(),
 * that ignores thrown values by rethrowing them
 * */
open class SecureStack<V>(var currentValue: V) : List<V> {

    val values = ArrayList<V>()
    val index get() = size - 1

    override var size = 1

    init {
        values.add(currentValue)
    }

    var lastV: V = currentValue

    open fun onChangeValue(newValue: V, oldValue: V) {
    }

    fun internalPush(v: V) {
        if (values.size < size + 1) {
            values.add(v)
        } else {
            values[size] = v
        }
        size++
    }

    fun internalPop() {
        size--
        if (size > 0) {
            internalSet(values[size - 1])
        }
    }

    fun internalSet(v: V) {
        currentValue = v
        try {
            onChangeValue(v, lastV)
        } finally {
            lastV = v
        }
    }

    inline fun <W> use(v: V, func: () -> W): W {
        internalPush(v)
        try {
            internalSet(v)
            return func()
        } finally {
            internalPop()
        }
    }

    fun getParent() = values[size - 2]

    override fun toString(): String {
        return super.toString() + ", [${values.subList(0, size).joinToString()}]"
    }

    override fun contains(element: V): Boolean {
        for (i in 0 until size) {
            if (element == values[i]) return true
        }
        return false
    }

    override fun containsAll(elements: Collection<V>): Boolean {
        for (e in elements) {
            if (!contains(e)) return false
        }
        return true
    }

    override fun get(index: Int): V {
        return values[index]
    }

    override fun indexOf(element: V): Int {
        for (i in 0 until size) {
            if (element == values[i]) return i
        }
        return -1
    }

    override fun isEmpty() = size == 0

    override fun iterator() = listIterator()

    override fun lastIndexOf(element: V): Int {
        for (i in size - 1 downTo 0) {
            if (element == values[i]) return i
        }
        return -1
    }

    override fun listIterator(): ListIterator<V> {
        return listIterator(0)
    }

    override fun listIterator(index: Int): ListIterator<V> {
        return object : ListIterator<V> {
            private var index1 = index
            override fun hasNext() = index1 < size
            override fun hasPrevious() = index1 > 0
            override fun next(): V = values[index1++]
            override fun nextIndex() = index1
            override fun previous(): V = values[--index1]
            override fun previousIndex() = index1 - 1
        }
    }

    override fun subList(fromIndex: Int, toIndex: Int): List<V> {
        return values.subList(fromIndex, toIndex)
    }

}