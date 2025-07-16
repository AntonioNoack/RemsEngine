package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.callbacks.IntCallback

/**
 * Wrapper around LongHashSet
 * */
class IntHashSet(minCapacity: Int = 16, loadFactor: Float = DEFAULT_LOAD_FACTOR) :
    PrimitiveCollection {

    @InternalAPI
    val content = LongHashSet(minCapacity, loadFactor)

    override val size: Int get() = content.size
    override val maxFill: Int get() = content.maxFill

    fun add(key: Int): Boolean {
        return content.add(key.toLong())
    }

    fun remove(key: Int): Boolean {
        return content.remove(key.toLong())
    }

    override fun clear() {
        content.clear()
    }

    operator fun contains(key: Int): Boolean {
        return content.contains(key.toLong())
    }

    fun forEach(callback: IntCallback) {
        content.forEach { value ->
            callback.callback(value.toInt())
        }
    }
}