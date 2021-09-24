package me.anno.utils.structures.stacks

import me.anno.utils.structures.arrays.ExpandingIntArray

open class SecureIntStack(var currentValue: Int) {

    val values = ExpandingIntArray(256)
    var size = 1
    val index get() = size - 1

    init {
        values.add(currentValue)
    }

    var lastV: Int = currentValue

    open fun onChangeValue(newValue: Int, oldValue: Int) {
    }

    fun internalPush(v: Int) {
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

    fun internalSet(v: Int) {
        currentValue = v
        try {
            onChangeValue(v, lastV)
        } finally {
            lastV = v
        }
    }

    inline fun use(v: Int, func: () -> Unit) {
        internalPush(v)
        try {
            internalSet(v)
            func()
        } finally {
            internalPop()
        }
    }

    fun getParent() = values[size - 2]

    override fun toString(): String {
        return super.toString() + ", [${values.toIntArray().joinToString()}]"
    }

}