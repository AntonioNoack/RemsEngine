package me.anno.utils.structures

open class SecureStack<V>(var currentValue: V) {

    val values = ArrayList<V>()
    var size = 1

    init {
        values.add(currentValue)
    }

    var lastV: V? = null

    open fun onChangeValue(v: V) {
        lastV = v
    }

    fun internalPush(v: V) {
        if (values.size < size + 1) {
            values.add(v)
        } else {
            values[size] = v
        }
        size++
    }

    fun internalPop(){
        size--
        if (size > 0) {
            currentValue = values[size - 1]
            onChangeValue(currentValue)
        }
    }

    inline fun use(v: V, func: () -> Unit) {
        internalPush(v)
        try {
            currentValue = v
            onChangeValue(v)
            func()
        } finally {
            internalPop()
        }
    }

    fun getParent() = values[size - 2]

    fun clear() {
        size = 0
    }

}