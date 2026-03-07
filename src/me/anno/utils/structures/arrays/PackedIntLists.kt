package me.anno.utils.structures.arrays

import me.anno.maths.Maths.ceilDiv
import me.anno.utils.InternalAPI
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.callbacks.I2I
import me.anno.utils.types.Booleans.toInt
import kotlin.math.max

/**
 * Represents an Array<List<Int>>, with -1 being an invalid value.
 * Is much more memory friendly than Array<List<Int>>.
 *
 * Be wary that if you underestimate initialCapacityPerValue, this collect gets really slow!
 * The order of the inserted items may change.
 * */
class PackedIntLists(
    var size: Int, initialCapacityPerValue: Int,
    val invalidValue: Int
) {

    @InternalAPI
    var offsets: IntArray = IntArray(size)

    @InternalAPI
    var values: IntArray

    init {
        val initialCapacityPerValue = max(initialCapacityPerValue, 1)
        val totalCapacity = size * initialCapacityPerValue + (initialCapacityPerValue == 1).toInt()
        values = IntArray(totalCapacity)
        clear()
    }

    fun addUnique(index: Int, value: Int) {
        if (value < 0 || contains(index, value)) return
        add(index, value)
    }

    fun add(index: Int, value: Int) {
        var index = index
        var value = value
        while (true) {
            if (index >= offsets.size) println("Illegal index! $index vs $size, ${offsets.size}")
            val pos = offsets[index] + getSize(index)

            // check if next cell is free for end marker
            if (pos + 1 >= values.size) grow()

            val wouldBeOverridden = values[pos + 1]

            // insert value and new end marker
            values[pos] = value
            values[pos + 1] = invalidValue

            index++
            if (index < offsets.size && pos + 1 == offsets[index]) {
                // Need to move suffix forward (shift right until free space)
                offsets[index] = pos + 2
                value = wouldBeOverridden
            } else break
        }
    }

    operator fun get(index: Int, index2: Int): Int {
        var pos = offsets[index]
        var count = 0
        while (values[pos] != invalidValue) {
            if (count == index2) return values[pos]
            pos++
            count++
        }
        throw IndexOutOfBoundsException("row=$index col=$index2")
    }

    inline fun forEach(index: Int, callback: (Int) -> Unit): Int {
        val pos0 = offsets[index]
        var pos = pos0
        while (true) {
            val value = values[pos]
            if (value == invalidValue) return pos - pos0
            callback(value)
            pos++
        }
    }

    fun contains(index: Int, value: Int): Boolean {
        var pos = offsets[index]
        while (true) {
            val valueI = values[pos]
            if (valueI == invalidValue) return false
            if (valueI == value) return true
            pos++
        }
    }

    fun getSize(index: Int): Int {
        var pos = offsets[index]
        var count = 0
        while (values[pos] != invalidValue) {
            count++
            pos++
        }
        return count
    }

    private fun grow() {
        val values = values
        val newValues = values.copyOf(values.size * 2)
        newValues.fill(invalidValue, values.size, newValues.size)
        this.values = newValues
    }

    /**
     * Clears all values for all indices
     * */
    fun clear() {

        // mark values as invalid
        values.fill(invalidValue)

        // distribute blocks evenly
        val factor = values.size.toLong().shl(32) / max(size, 1)
        for (row in 0 until size) {
            offsets[row] = (row * factor).shr(32).toInt()
        }
    }

    /**
     * Clears all values for this index
     * */
    fun clear(index: Int) {
        val size = getSize(index)
        val offset = offsets[index]
        for (i in 0 until size) {
            values[offset + i] = invalidValue
        }
    }

    fun sortBy(index: Int, comparator: I2I) {
        val size = getSize(index)
        val offset = offsets[index]
        for (i in offset + 1 until offset + size) {
            for (j in offset until i) { // j < i
                val vi = values[i]
                val vj = values[j]
                if (comparator.call(vj, vi) > 0) { // swap if vj > vi
                    values[j] = vi
                    values[i] = vj
                }
            }
        }
    }

    fun resizeTo(newSize: Int) {
        val oldSize = size
        val cellsPerSize = ceilDiv(values.size, oldSize)
        assertTrue(cellsPerSize >= 2)

        val oldNumValues = values.size
        val requiredSize = oldNumValues + (newSize - oldSize) * cellsPerSize

        if (requiredSize > oldNumValues) {
            values = values.copyOf(requiredSize)
            values.fill(invalidValue, oldNumValues, requiredSize)
        }

        offsets = offsets.copyOf(newSize)

        // define start offsets for the new cells
        for (i in oldSize until newSize) {
            offsets[i] = oldNumValues + (i - oldSize) * cellsPerSize + 1
        }
        if (oldSize in 1 until newSize) {
            assertEquals(values[offsets[oldSize] - 1], invalidValue)
            // cell before us must be free
        }

        size = newSize
    }
}