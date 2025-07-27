package speiger.primitivecollections

import me.anno.utils.InternalAPI
import speiger.primitivecollections.HashUtil.DEFAULT_LOAD_FACTOR
import speiger.primitivecollections.HashUtil.DEFAULT_MIN_CAPACITY
import speiger.primitivecollections.HashUtil.getMaxFill
import speiger.primitivecollections.callbacks.SlotPredicate
import kotlin.math.max

abstract class BaseHashMap<AK, AV>(
    minCapacity: Int = DEFAULT_MIN_CAPACITY,
    loadFactor: Float = DEFAULT_LOAD_FACTOR
) : PrimitiveCollection {

    /**
     * 0 is used as an empty-cell-marker and may be a key in this set
     * */
    var containsNull = false

    /**
     * Where the key 0L is stored.
     * It is stored at the very last index.
     * For size 16, an array of size 17 is created, and nullIndex = 16.
     * */
    var nullIndex: Int

    /**
     * first size when being created and always >= 16
     * */
    var minCapacity: Int

    val loadFactor: Float

    final override var maxFill = 0
    final override var size = 0

    @InternalAPI
    var mask = 0

    val minFill get() = maxFill ushr 2

    init {
        val loadFactor = if (loadFactor in 0.1f..1f) loadFactor else DEFAULT_LOAD_FACTOR
        this.loadFactor = loadFactor
        nullIndex = HashUtil.arraySize(minCapacity, loadFactor)
        this.minCapacity = max(nullIndex, 16)
        mask = nullIndex - 1
        maxFill = getMaxFill(nullIndex, loadFactor)
    }

    @InternalAPI
    var keys = createKeys(nullIndex + 1)

    @InternalAPI
    var values = createValues(nullIndex + 1)

    abstract fun hasKey(slot: Int): Boolean

    /**
     * Needs to be called after a value has been remove at startPos:
     * As long as other values follow, we must close their gap.
     * */
    abstract fun shiftKeys(removedSlot: Int)
    abstract fun rehash(newSize: Int)
    abstract fun setEntryNull(slot: Int)

    abstract fun createKeys(size: Int): AK
    abstract fun createValues(size: Int): AV

    abstract fun fillNullKeys(keys: AK)
    abstract fun fillNullValues(values: AV)

    abstract fun copyOver(
        dstValues: AV, dstIndex: Int,
        srcValues: AV, srcIndex: Int
    )

    abstract fun setNull(dstValues: AV, dstIndex: Int)

    override fun clear() {
        if (size != 0) {
            size = 0
            containsNull = false
            fillNullKeys(keys)
            fillNullValues(values)
        }
    }

    fun shrinkMaybe() {
        if (nullIndex > minCapacity && size < minFill && nullIndex > 16) {
            rehash(nullIndex shr 1)
        }
    }

    fun growMaybe() {
        if (size >= maxFill) {
            rehash(HashUtil.arraySize(size + 1, loadFactor))
        }
    }

    @InternalAPI
    fun removeNullIndex() {
        containsNull = false
        setEntryNull(nullIndex)
        --size
        shrinkMaybe()
    }

    @InternalAPI
    fun removeIndex(slot: Int): Boolean {
        if (slot == nullIndex) {
            if (!containsNull) return false
            removeNullIndex()
        } else {
            setEntryNull(slot)
            --size
            shiftKeys(slot)
            shrinkMaybe()
        }
        return true
    }

    fun removeIfImpl(predicate: SlotPredicate): Int {
        val originalSize = size

        // Handle key == 0L (null key)
        if (containsNull && predicate.test(nullIndex)) {
            removeNullIndex()
        }

        // Traverse regular keys
        for (i in 0 until nullIndex) {
            if (hasKey(i) && predicate.test(i)) {
                size--
                shiftKeys(i)
                // Must recheck the same index since shiftKeys moves another entry into it
                while (hasKey(i) && predicate.test(i)) {
                    size--
                    shiftKeys(i)
                }
            }
        }

        val numRemoved = originalSize - size
        if (numRemoved > 0) {
            var minFill = minFill
            var nullIndex = nullIndex
            while (nullIndex > minCapacity && size < minFill) {
                nullIndex = nullIndex shr 1
                minFill = getMaxFill(nullIndex, loadFactor) ushr 2
            }
            if (nullIndex < this.nullIndex) {
                rehash(nullIndex)
            }
        }
        return numRemoved
    }

    fun copyBasePropertiesInto(dst: BaseHashMap<*, *>) {
        dst.mask = mask
        dst.containsNull = containsNull
        dst.minCapacity = minCapacity
        dst.nullIndex = nullIndex
        dst.maxFill = maxFill
        dst.mask = mask
        dst.size = size
    }
}