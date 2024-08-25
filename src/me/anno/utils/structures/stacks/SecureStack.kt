package me.anno.utils.structures.stacks

import me.anno.utils.structures.lists.SimpleList

/**
 * state-tracking stack with onChangeValue(),
 * that ignores thrown values by rethrowing them
 * */
open class SecureStack<V>(initialValue: V) : SimpleList<V>() {

    val values = ArrayList<V>()
    val index get() = size - 1

    var currentValue: V = initialValue
        private set

    final override var size = 1
        private set

    init {
        values.add(currentValue)
    }

    var lastV: V = currentValue
        private set

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

    override fun get(index: Int): V = values[index]
}