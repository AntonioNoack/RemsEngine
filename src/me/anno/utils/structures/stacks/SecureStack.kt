package me.anno.utils.structures.stacks

open class SecureStack<V>(var currentValue: V) {

    val values = ArrayList<V>()
    var size = 1
    val index get() = size - 1

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

    inline fun use(v: V, func: () -> Unit) {
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
        return super.toString() + ", [${values.subList(0, size).joinToString()}]"
    }

}