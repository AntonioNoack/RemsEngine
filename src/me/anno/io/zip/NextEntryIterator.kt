package me.anno.io.zip

abstract class NextEntryIterator<V> : Iterator<V> {

    abstract fun nextEntry(): V?

    var next: V? = null
    override fun hasNext(): Boolean {
        if (next == null) next = nextEntry()
        return next != null
    }

    override fun next(): V {
        val v = next!!
        next = null
        return v
    }
}