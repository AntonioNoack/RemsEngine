package me.anno.utils.structures.stacks

import me.anno.utils.structures.arrays.ExpandingByteArray

open class SecureBoolStack(var currentValue: Boolean) {

    val values = ExpandingByteArray(256)
    var size = 1
    val index get() = size - 1

    init {
        values.add(if (currentValue) 1 else 0)
    }

    var lastV: Boolean = currentValue

    open fun onChangeValue(newValue: Boolean, oldValue: Boolean) {
    }

    fun internalPush(v: Boolean) {
        val vb: Byte = if (v) 1 else 0
        if (values.size < size + 1) {
            values.add(vb)
        } else {
            values[size] = vb
        }
        size++
    }

    fun internalPop() {
        size--
        if (size > 0) {
            internalSet(values[size - 1] > 0)
        }
    }

    fun internalSet(v: Boolean) {
        currentValue = v
        try {
            onChangeValue(v, lastV)
        } finally {
            lastV = v
        }
    }

    inline fun use(v: Boolean, func: () -> Unit) {
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
        return super.toString() + ", [${IntArray(size).joinToString { values[it].toString() }}]"
    }

}