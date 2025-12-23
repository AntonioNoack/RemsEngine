package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.callbacks.IntCallback
import speiger.primitivecollections.callbacks.IntPredicate

/**
 * Wrapper around LongHashSet
 * */
class IntHashSet(
    @property:InternalAPI
    val content: LongHashSet
) : PrimitiveCollection {

    constructor(
        minCapacity: Int = DEFAULT_MIN_CAPACITY,
        loadFactor: Float = DEFAULT_LOAD_FACTOR
    ) : this(LongHashSet(minCapacity, loadFactor))

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

    override fun clearAndTrim(size: Int) {
        content.clearAndTrim(size)
    }

    operator fun contains(key: Int): Boolean {
        return content.contains(key.toLong())
    }

    fun forEach(callback: IntCallback) {
        content.forEach { value ->
            callback.call(value.toInt())
        }
    }

    fun first(ifEmpty: Int): Int = content.firstKey(ifEmpty.toLong()).toInt()

    fun removeIf(predicate: IntPredicate) =
        content.removeIf { predicate.test(it.toInt()) }

    override fun clone(): IntHashSet = IntHashSet(content.clone())
}