package me.anno.utils.structures.stacks

import me.anno.utils.hpc.ThreadLocal2

/**
 * same as SecureStack, just thread local
 * */
open class ThreadLocalSecureStack<V>(initialValue: V) : List<V> {

    class InternalSecureStack<V>(val self: ThreadLocalSecureStack<V>, initialValue: V) :
        SecureStack<V>(initialValue) {
        override fun onChangeValue(newValue: V, oldValue: V) {
            self.onChangeValue(newValue, oldValue)
        }
    }

    val values = ThreadLocal2 {
        InternalSecureStack(this, initialValue)
    }

    val currentValue: V get() = values.get().currentValue
    val index get() = size - 1

    override val size: Int get() = values.get().size

    val lastV: V get() = values.get().lastV

    open fun onChangeValue(newValue: V, oldValue: V) {
    }

    inline fun <W> use(v: V, func: () -> W): W {
        return values.get().use(v, func)
    }

    fun getParent() = values.get().getParent()

    override fun toString(): String = values.get().toString()
    override fun contains(element: V): Boolean = values.get().contains(element)
    override fun containsAll(elements: Collection<V>): Boolean = values.get().containsAll(elements)
    override fun get(index: Int): V = values.get()[index]
    override fun indexOf(element: V): Int = values.get().indexOf(element)
    override fun isEmpty() = values.get().isEmpty()
    override fun iterator() = values.get().iterator()
    override fun lastIndexOf(element: V): Int = values.get().lastIndexOf(element)
    override fun listIterator(): ListIterator<V> = values.get().listIterator()
    override fun listIterator(index: Int): ListIterator<V> = values.get().listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): List<V> = values.get().subList(fromIndex, toIndex)
}