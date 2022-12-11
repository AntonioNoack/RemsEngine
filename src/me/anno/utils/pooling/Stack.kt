package me.anno.utils.pooling

import org.apache.logging.log4j.LogManager
import java.lang.reflect.Constructor
import java.nio.BufferUnderflowException

class Stack<V : Any>(private val createInstance: () -> V) {

    companion object {
        @JvmStatic
        private val LOGGER = LogManager.getLogger(Stack::class)
    }

    constructor(clazz: Class<V>) : this(clazz.getConstructor())
    constructor(constructor: Constructor<V>) : this({ constructor.newInstance() })

    private class LocalStack<V : Any>(
        private val createInstance: () -> V
    ) {
        var tmp: Array<Any?> = Array(64) { createInstance() }
        var index = 0
        var localFloor = 0
        val capacity get() = tmp.size
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
            @Suppress("unchecked_cast")
            return tmp[index++] as V
        }

        fun borrow(): V {
            ensure()
            // remove in final build
            if (index < localFloor) throw BufferUnderflowException()
            @Suppress("unchecked_cast")
            return tmp[index] as V
        }

        fun sub(delta: Int) {
            if (index - delta < localFloor) throw BufferUnderflowException()
            index -= delta
        }

    }

    private val storage = ThreadLocal.withInitial { LocalStack(createInstance) }

    fun create(): V {
        return storage.get().create()
    }

    fun reset() {
        val instance = storage.get()
        if (instance.index > 0) LOGGER.warn("Missed to return ${instance.index}x ${instance.tmp[0]!!::class.simpleName}")
        instance.index = 0
    }

    fun borrow(): V {
        return storage.get().borrow()
    }

    fun sub(delta: Int) {
        storage.get().sub(delta)
    }

    fun debugSetLocalFloor() {
        val instance = storage.get()
        instance.localFloor = index
    }

    fun debugResetLocalFloor() {
        val instance = storage.get()
        instance.localFloor = 0
    }

    val capacity get() = storage.get().capacity

    var index: Int
        get() = storage.get().index
        set(value) {
            storage.get().index = value
        }

}