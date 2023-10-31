package me.anno.graph.hdb.allocator

import me.anno.maths.Maths.max
import me.anno.maths.Maths.min

interface AllocationManager<Key, Data : Any> {

    fun insert(
        elements: Collection<Key>,
        newKey: Key, newData: Data, newDataRange: IntRange,
        available: Int, oldData: Data
    ): Pair<ReplaceType, Data> {

        if (elements.isEmpty()) {
            // easy: just replace
            return ReplaceType.WriteCompletely to newData
        }

        val inOrder = elements.sortedBy { getRange(it).first }

        val newSize = newDataRange.size
        val newDataStart = newDataRange.first

        // check if 0th place is ok
        val r0 = getRange(inOrder.first())
        if (newSize < r0.first) {
            val newRange = 0 until newSize
            copy(newKey, newDataStart, newData, newRange, oldData)
            return ReplaceType.InsertInto to oldData
        }

        // check if we can insert it in-between
        for (i in 1 until inOrder.size) {
            val ri = getRange(inOrder[i - 1])
            val rj = getRange(inOrder[i])
            val start = ri.first + ri.size
            if (start + newSize <= rj.first) {
                val newRange = start until start + newSize
                copy(newKey, newDataStart, newData, newRange, oldData)
                return ReplaceType.InsertInto to oldData
            }
        }

        // check if at the end is ok
        val re = getRange(inOrder.last())
        val start = re.first + re.size
        if (start + newSize < available) {
            val newRange = start until start + newSize
            copy(newKey, newDataStart, newData, newRange, oldData)
            return ReplaceType.InsertInto to oldData
        }

        // we have to append it
        return ReplaceType.Append to append(
            elements,
            newKey, newData, newDataRange,
            newSize, available, oldData
        )
    }

    fun append(
        elements: Collection<Key>,
        newKey: Key, newData: Data, newDataRange: IntRange, newSize: Int,
        available: Int, oldData: Data
    ): Data {
        val start = sumSize(elements)
        val newRange = start until start + newSize
        val newData1 = pack(elements, newKey, available, oldData)
        copy(newKey, newDataRange.first, newData, newRange, newData1)
        return newData1
    }

    fun sumSize(elements: Collection<Key>): Int {
        return elements.sumOf { getRange(it).size }
    }

    fun maximumAcceptableSize(requiredSize: Int): Int {
        return if (requiredSize == 0) 0
        else requiredSize * 3 / 2 + 1024
    }

    fun shouldOptimize(elements: Collection<Key>, available: Int): Boolean {
        val requiredSize = sumSize(elements)
        return available > maximumAcceptableSize(requiredSize)
    }

    fun pack(elements: Collection<Key>, oldData: Data): Data {
        val requiredSize = sumSize(elements)
        val newData = allocate(requiredSize)
        var pos = 0
        for (keys in elements) {
            val oldRange = getRange(keys)
            val newRange = pos until pos + oldRange.size
            copy(keys, oldRange.first, oldData, newRange, newData)
            pos += oldRange.size
        }
        return newData
    }

    fun isCompact(elements: Collection<Key>): Int? {
        if (elements.isEmpty()) return 0
        val first = getRange(elements.first())
        var min = first.first
        var max = first.last
        var size = 0
        for (element in elements) {
            val range = getRange(element)
            min = min(min, range.first)
            max = max(max, range.last)
            size += range.size
        }
        return if (min == 0 && max + 1 == size) size
        else null
    }

    fun pack(elements: Collection<Key>, elementX: Key, available: Int, oldData: Data): Data {
        val compact = isCompact(elements)
        val extraSize = getRange(elementX).size
        if (compact != null) {
            val requiredSize = compact + extraSize
            return if (available >= requiredSize) {
                oldData
            } else {
                val allocSize = roundUpStorage(requiredSize)
                val newData = allocate(allocSize)
                System.arraycopy(oldData, 0, newData, 0, compact)
                newData
            }
        } else {
            val requiredSize = sumSize(elements) + extraSize
            val allocSize = roundUpStorage(requiredSize)
            val newData = allocate(allocSize)
            var pos = 0
            for (keys in elements) {
                val oldRange = getRange(keys)
                val newRange = pos until pos + oldRange.size
                copy(keys, oldRange.first, oldData, newRange, newData)
                pos += oldRange.size
            }
            return newData
        }
    }

    fun roundUpStorage(requiredSize: Int): Int {
        return requiredSize + (requiredSize ushr 2)
    }

    fun getRange(key: Key): IntRange

    fun allocate(newSize: Int): Data

    fun copy(key: Key, from: Int, fromData: Data, to: IntRange, toData: Data)
}