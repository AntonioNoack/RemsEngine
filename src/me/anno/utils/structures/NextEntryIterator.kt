package me.anno.utils.structures

abstract class NextEntryIterator<V: Any> : Iterator<V> {

    abstract fun nextEntry(): V?

    private var next: V? = null
    override fun hasNext(): Boolean {
        if (next == null) next = nextEntry()
        return next != null
    }

    override fun next(): V {
        var v = next
        if (v == null) v = nextEntry()
        next = null
        return v!!
    }
}