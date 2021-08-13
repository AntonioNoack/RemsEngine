package me.anno.utils.pooling

import java.nio.BufferUnderflowException

class Stack<V : Any>(
    private val createInstance: () -> V
) {

    private class LocalStack<V : Any>(
        private val createInstance: () -> V
    ) {
        var tmp: Array<Any?> = Array(64) { createInstance() }
        var index = 0
        fun ensure() {
            if (index >= tmp.size) {
                val newSize = tmp.size * 2
                val tmp2 = arrayOfNulls<Any>(newSize)
                System.arraycopy(tmp, 0, tmp2, 0, tmp.size)
                for (i in tmp.size until newSize) {
                    tmp2[i] = createInstance()
                }
                tmp = tmp2
            }
        }

        fun create(): V {
            ensure()
            return tmp[index++] as V
        }

        fun borrow(): V {
            ensure()
            return tmp[index] as V
        }

        fun sub(delta: Int) {
            index -= delta
            if (index < 0) throw BufferUnderflowException()
        }

    }

    private val storage = ThreadLocal.withInitial { LocalStack(createInstance) }

    fun create(): V {
        return storage.get().create()
    }

    fun borrow(): V {
        return storage.get().borrow()
    }

    fun sub(delta: Int) {
        storage.get().sub(delta)
    }

}