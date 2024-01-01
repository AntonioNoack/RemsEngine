package me.anno.graph.hdb.allocator

import org.apache.logging.log4j.LogManager

interface AllocationManager<Key, Data : Any> {

    companion object {
        private val LOGGER = LogManager.getLogger(AllocationManager::class)
    }

    // todo metadata to remember dense areas...

    fun compact(sortedElements: List<Key>): ArrayList<IntRange> {
        val result = ArrayList<IntRange>()
        var position = 0
        var size = 0
        var lastRange: IntRange? = null
        for (i in sortedElements.indices) {
            val range = getRange(sortedElements[i])
            if (range.first == position + size) {
                // good :), extend
                size += range.size
                lastRange = null
            } else {
                val lastRange1 = lastRange ?: (position until position + size)
                result.add(lastRange1)
                position = range.first
                size = range.size
                lastRange = range
            }
        }
        if (size > 0) {
            val lastRange1 = lastRange ?: (position until position + size)
            result.add(lastRange1)
        }
        return result
    }

    fun insert(
        sortedElements: ArrayList<Key>,
        sortedRanges: ArrayList<IntRange>,
        newKey: Key, newData: Data, newDataRange: IntRange,
        available: Int, oldData: Data,
        allowReturningNewData: Boolean
    ): Pair<ReplaceType, Data> {

        val newSize = newDataRange.size
        if (sortedRanges.isEmpty()) {
            return if (allowReturningNewData) {
                // easy: just replace
                sortedElements.add(newKey)
                sortedRanges.add(newDataRange)
                ReplaceType.WriteCompletely to newData
            } else {
                ReplaceType.Append to append(
                    sortedElements, sortedRanges,
                    newKey, newData, newDataRange,
                    newSize, available, oldData
                )
            }
        }

        val newDataStart = newDataRange.first

        // check if 0th place is ok
        val r0 = sortedRanges.first()
        if (newSize <= r0.first) {
            val newRange = 0 until newSize
            copy(newKey, newDataStart, newData, newRange, oldData)
            sortedElements.add(0, newKey)
            if (newSize == r0.first) {
                // compact with first, if fills in gap perfectly
                sortedRanges[0] = 0..r0.last
            } else {
                sortedRanges.add(0, newRange)
            }
            return ReplaceType.InsertInto to oldData
        }

        // check if we can insert it in-between
        for (i in 1 until sortedRanges.size) {
            val ri = sortedRanges[i - 1]
            val rj = sortedRanges[i]
            val start = ri.first + ri.size
            if (start + newSize <= rj.first) {
                val newRange = start until start + newSize
                copy(newKey, newDataStart, newData, newRange, oldData)
                val idx = sortedElements.binarySearch {
                    (getRange(it).last + 1).compareTo(start)
                }
                if (idx !in sortedElements.indices) {
                    // this really shouldn't happen, but it did for me...
                    // so maybe a bug?, maybe something indeed corrupted?
                    LOGGER.warn("BinarySearch went wrong or data is corrupted, " +
                            "${sortedElements.map { getRange(it) }} vs $newRange vs $idx in ${sortedElements.indices}"
                    )
                    // maybe sorting fixes it
                    sortedElements.sortBy { getRange(it).first }
                    // let's save ourselves by getting out of here
                    return ReplaceType.Append to append(
                        sortedElements, sortedRanges,
                        newKey, newData, newDataRange,
                        newSize, available, oldData
                    )
                }
                sortedElements.add(idx + 1, newKey) // correct???
                if (start + newSize == rj.first) {
                    // fill in gap completely
                    sortedRanges[i - 1] = ri.first..rj.last
                    sortedRanges.removeAt(i)
                } else {
                    // append onto 'ri'
                    sortedRanges[i - 1] = ri.first until start + newSize
                }
                return ReplaceType.InsertInto to oldData
            }
        }

        // check if at the end is ok
        val re = sortedRanges.last()
        val start = re.first + re.size
        if (start + newSize < available) {
            val newRange = start until start + newSize
            copy(newKey, newDataStart, newData, newRange, oldData)
            sortedElements.add(newKey)
            // extend last range
            sortedRanges[sortedRanges.lastIndex] = re.first..newRange.last
            return ReplaceType.InsertInto to oldData
        }

        // we have to append it
        return ReplaceType.Append to append(
            sortedElements, sortedRanges,
            newKey, newData, newDataRange,
            newSize, available, oldData
        )
    }

    /**
     * appends element at the end; compacts if necessary;
     * */
    fun append(
        elements: ArrayList<Key>, ranges: ArrayList<IntRange>,
        newKey: Key, newData: Data, newDataRange: IntRange, newSize: Int,
        available: Int, oldData: Data
    ): Data {
        val newData1 = pack(elements, ranges, newKey, available, oldData)
        val start = ranges.lastOrNull()?.run { last + 1 } ?: 0
        val newRange = start until start + newSize
        elements.add(newKey)
        if (ranges.isNotEmpty()) {
            // merge with last range
            val last = ranges.last()
            ranges[ranges.lastIndex] = last.first..newRange.last
        } else {
            // append it
            ranges.add(newRange)
        }
        copy(newKey, newDataRange.first, newData, newRange, newData1)
        return newData1
    }

    fun sumSize(elements: Collection<Key>): Int {
        return elements.sumOf { getRange(it).size }
    }

    fun sumSize1(ranges: Collection<IntRange>): Int {
        return ranges.sumOf { it.size }
    }

    fun maximumAcceptableSize(requiredSize: Int): Int {
        return if (requiredSize == 0) 0
        else requiredSize * 3 / 2 + 1024
    }

    fun shouldOptimize(elements: List<Key>, available: Int): Boolean {
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
        deallocate(oldData)
        return newData
    }

    /**
     * returns whether the data is compact, and if so, how many elements it contains
     * */
    fun getCompactSizeIfCompact(ranges: List<IntRange>): Int? {
        if (ranges.isEmpty()) return 0
        val first = ranges.first()
        if (first.first != 0) return null
        val last = ranges.last()
        val size = ranges.sumOf { it.size }
        return if (size == last.last + 1) size
        else null
    }

    /**
     * pack elements, so we have enough space for elementX, too;
     * adjust ranges as needed, but don't add X yet.
     * */
    fun pack(
        elements: Collection<Key>, ranges: ArrayList<IntRange>,
        elementX: Key, available: Int, oldData: Data
    ): Data {
        val extraSize = getRange(elementX).size
        val compactSize = getCompactSizeIfCompact(ranges)
        if (compactSize != null) {
            val requiredSize = compactSize + extraSize
            if (available >= requiredSize) {
                return oldData
            } else if (allocationKeepsOldData()) {
                // it is compact, but we need extra storage...
                val allocSize = roundUpStorage(requiredSize)
                val newData = allocate(allocSize)
                copy(0, oldData, 0 until compactSize, newData)
                deallocate(oldData)
                return newData
            }
        }

        val requiredSize = sumSize1(ranges) + extraSize
        val allocSize = roundUpStorage(requiredSize)
        val newData = allocate(allocSize)
        var pos = 0
        for (keys in elements) {
            val oldRange = getRange(keys)
            val newRange = pos until pos + oldRange.size
            copy(keys, oldRange.first, oldData, newRange, newData)
            pos += oldRange.size
        }
        deallocate(oldData)
        ranges.clear()
        ranges.add(0 until pos)
        return newData
    }

    fun roundUpStorage(requiredSize: Int): Int {
        return requiredSize + (requiredSize ushr 2)
    }

    fun getRange(key: Key): IntRange

    fun allocate(newSize: Int): Data
    fun deallocate(data: Data) {}
    fun allocationKeepsOldData(): Boolean = true

    fun copy(key: Key, from: Int, fromData: Data, to: IntRange, toData: Data)
    fun copy(from: Int, fromData: Data, to: IntRange, toData: Data)
}