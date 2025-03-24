package me.anno.utils.pooling

import me.anno.utils.Reflections.getBaseConstructor
import me.anno.utils.assertions.assertFalse
import org.apache.logging.log4j.LogManager
import java.lang.ref.WeakReference
import kotlin.reflect.KClass

/**
 * Thread-local stack of reusable-instances to reduce allocations (to reduce GC pressure).
 * Typically used for mathematical objects like vectors or for helpers with a few fields.
 * */
class Stack<V : Any>(private val createInstance: () -> V) {

    companion object {
        @JvmStatic
        private val LOGGER = LogManager.getLogger(Stack::class)
        val stacks = ArrayList<WeakReference<Stack<*>>>()
        fun resetAll() {
            stacks.removeIf { it.get() == null }
            for (i in stacks.indices) {
                (stacks.getOrNull(i) ?: continue).get()?.reset()
            }
        }

        private fun <V : Any> getConstructor(clazz: KClass<V>): () -> V {
            return getBaseConstructor(clazz)
        }
    }

    constructor(clazz: KClass<V>) : this(getConstructor(clazz))

    init {
        stacks.add(WeakReference(this))
    }

    private class LocalStack<V : Any>(
        private val createInstance: () -> V
    ) {
        var tmp: Array<Any?>? = null
        var index = 0
        var localFloor = 0
        val capacity get() = tmp?.size ?: 0
        fun ensure() {
            val tmp = tmp
            if (tmp == null || index >= tmp.size) {
                val newSize = if (tmp == null) 64 else tmp.size * 2
                val tmp2 = arrayOfNulls<Any>(newSize)
                tmp?.copyInto(tmp2)
                for (i in (tmp?.size ?: 0) until newSize) {
                    tmp2[i] = createInstance()
                }
                this.tmp = tmp2
            }
        }

        fun create(): V {
            ensure()
            @Suppress("unchecked_cast")
            return tmp!![index++] as V
        }

        fun borrow(): V {
            ensure()
            // remove in final build
            assertFalse(index < localFloor)
            @Suppress("unchecked_cast")
            return tmp!![index] as V
        }

        fun sub(delta: Int) {
            assertFalse(index - delta < localFloor)
            index -= delta
        }
    }

    private val storage = ThreadLocal.withInitial { LocalStack(createInstance) }

    fun create(): V {
        return storage.get().create()
    }

    fun getInstanceClassName(): String {
        val instance = borrow()
        return instance::class.simpleName ?: "?"
    }

    fun reset() {
        val instance = storage.get()
        if (instance.index > 0) LOGGER.warn("Missed to return ${instance.index}x ${getInstanceClassName()}")
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