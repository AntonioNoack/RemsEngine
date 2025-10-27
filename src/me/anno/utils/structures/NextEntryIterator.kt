package me.anno.utils.structures

/**
 * Iterator for generator functions that return null or something
 * */
abstract class NextEntryIterator<V : Any> : Iterator<V> {

    abstract fun nextEntry(): V?

    private var next: V? = null
    override fun hasNext(): Boolean {
        if (next == null) next = nextEntry()
        return next != null
    }

    override fun next(): V {
        var value = next
        if (value == null) value = nextEntry()
        next = null
        return value ?: throw IllegalStateException("No next element")
    }
}