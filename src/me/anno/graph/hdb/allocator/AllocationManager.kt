package me.anno.graph.hdb.allocator

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.Lists.binarySearch
import me.anno.utils.types.Ranges.size
import org.apache.logging.log4j.LogManager

interface AllocationManager<Key, StoredData, InsertData> {

    companion object {
        private val LOGGER = LogManager.getLogger(AllocationManager::class)
    }

    fun remove(element: Key, sortedElements: ArrayList<Key>, sortedRanges: ArrayList<IntRange>): Boolean {
        val searchedRange = getRange(element)
        val searchedStart = searchedRange.first
        val ei = sortedElements.binarySearch {
            getRange(it).first.compareTo(searchedStart)
        }
        if (ei < 0) return false
        sortedElements.removeAt(ei)
        removeFromSortedRanges(searchedRange, sortedRanges)
        return true
    }

    private fun removeFromSortedRanges(searchedRange: IntRange, sortedRanges: ArrayList<IntRange>) {
        val searchedStart = searchedRange.first
        val ri = sortedRanges.binarySearch {
            it.first.compareTo(searchedStart)
        }
        assertTrue(ri != -1) // if it was -1, it would be less than sortedRanges[0]
        if (ri < 0) {
            // no range starts with us, so we must be in-between or at the end
            val idx = (-ri - 1) - 1
            val foundRange = sortedRanges[idx]
            assertTrue(foundRange.last >= searchedRange.last) // else we're in an impossible place
            sortedRanges[idx] = foundRange.first until searchedStart // the first part always stays
            if (foundRange.last > searchedRange.last) {
                // we're in-between -> split this range -> add the end segment
                sortedRanges.add(idx + 1, searchedRange.last + 1..foundRange.last)
            }
        } else {
            val foundRange = sortedRanges[ri]
            // there is a range with that start -> shrink or remove it
            if (foundRange == searchedRange) {
                sortedRanges.removeAt(ri)
            } else {
                // shrink it
                sortedRanges[ri] = searchedRange.last + 1..foundRange.last
            }
        }
    }

    fun calculateSortedRanges(sortedElements: List<Key>, dst: ArrayList<IntRange>): ArrayList<IntRange> {
        var position = 0
        var size = 0
        var lastRange: IntRange? = null
        for (i in sortedElements.indices) {
            val range = getRange(sortedElements[i])
            if (range.first == position + size) { // grow current range
                size += range.size
                lastRange = null
            } else { // new range needs to be created
                if (size > 0) {
                    val lastRange1 = lastRange ?: (position until position + size)
                    dst.add(lastRange1)
                } // else the first range might not start at zero
                position = range.first
                size = range.size
                lastRange = range
            }
        }
        if (size > 0) {
            dst.add(lastRange ?: (position until position + size))
        }
        return dst
    }

    fun insert(
        sortedElements: ArrayList<Key>,
        sortedRanges: ArrayList<IntRange>,
        newKey: Key, newData: InsertData,
        available: Int, dstData: StoredData
    ): Pair<ReplaceType, StoredData> {

        val newDataRange = getRange(newKey)
        val newSize = newDataRange.size
        if (sortedRanges.isEmpty()) {
            return ReplaceType.Append to append(
                sortedElements, sortedRanges,
                newKey, newData, newDataRange,
                newSize, available, dstData
            )
        }

        val newDataStart = newDataRange.first

        // check if 0th place is ok
        val r0 = sortedRanges.first()
        if (newSize <= r0.first) {
            val newRange = 0 until newSize

            copyKey(newKey, newDataStart, newData, newRange, dstData)

            sortedElements.add(0, newKey)
            if (newSize == r0.first) {
                // compact with first, if fills in gap perfectly
                sortedRanges[0] = 0..r0.last
            } else {
                sortedRanges.add(0, newRange)
            }
            return ReplaceType.InsertInto to dstData
        }

        // check if we can insert it in-between
        intermediate@ for (i in 1 until sortedRanges.size) {
            val ri = sortedRanges[i - 1]
            val rj = sortedRanges[i]
            val start = ri.last + 1
            if (start + newSize <= rj.first) {

                val newRange = start until start + newSize
                copyKey(newKey, newDataStart, newData, newRange, dstData)

                val idx = sortedElements.binarySearch { // find the element, which belongs to ri by end-position
                    (getRange(it).last + 1).compareTo(start)
                }
                if (idx !in sortedElements.indices) {
                    LOGGER.error("BinarySearch went wrong or data is corrupted")
                    continue@intermediate
                }
                sortedElements.add(idx + 1, newKey)
                if (start + newSize == rj.first) {
                    // fill in gap completely
                    sortedRanges[i - 1] = ri.first..rj.last
                    sortedRanges.removeAt(i)
                } else {
                    // append onto 'ri'
                    sortedRanges[i - 1] = ri.first until start + newSize
                }
                return ReplaceType.InsertInto to dstData
            }
        }

        // check if at the end is ok
        val re = sortedRanges.last()
        val start = re.first + re.size
        if (start + newSize < available) {
            val newRange = start until start + newSize
            copyKey(newKey, newDataStart, newData, newRange, dstData)
            sortedElements.add(newKey)
            // extend last range
            sortedRanges[sortedRanges.lastIndex] = re.first..newRange.last
            return ReplaceType.InsertInto to dstData
        }

        // we have to append it
        return ReplaceType.Append to append(
            sortedElements, sortedRanges,
            newKey, newData, newDataRange,
            newSize, available, dstData
        )
    }

    /**
     * appends element at the end; compacts if necessary;
     * */
    private fun append(
        sortedElements: ArrayList<Key>, sortedRanges: ArrayList<IntRange>,
        newKey: Key, newData: InsertData, newDataRange: IntRange, newSize: Int,
        available: Int, oldData: StoredData
    ): StoredData {
        val newData1 = pack(sortedElements, sortedRanges, newKey, available, oldData)
        val start = sortedRanges.lastOrNull()?.run { last + 1 } ?: 0
        val newRange = start until start + newSize
        sortedElements.add(newKey)
        if (sortedRanges.isNotEmpty()) {
            // merge with last range
            val last = sortedRanges.last()
            sortedRanges[sortedRanges.lastIndex] = last.first..newRange.last
        } else {
            // append it
            sortedRanges.add(newRange)
        }
        copyKey(newKey, newDataRange.first, newData, newRange, newData1)
        return newData1
    }

    private fun sumSize(elements: Collection<Key>): Int {
        return elements.sumOf { getRange(it).size }
    }

    private fun sumSize1(ranges: Collection<IntRange>): Int {
        return ranges.sumOf { it.size }
    }

    private fun maximumAcceptableSize(requiredSize: Int): Int {
        return if (requiredSize == 0) 0
        else requiredSize * 3 / 2 + 1024
    }

    fun shouldOptimize(elements: List<Key>, available: Int): Boolean {
        val requiredSize = sumSize(elements)
        return available > maximumAcceptableSize(requiredSize)
    }

    fun pack(keys: Collection<Key>, oldData: StoredData): StoredData {
        return pack(keys, oldData, sumSize(keys)).first
    }

    fun pack(keys: Collection<Key>, oldData: StoredData, requiredSize: Int): Pair<StoredData, Int> {
        val newData = allocate(requiredSize)
        var pos = 0
        for (key in keys) {
            val oldRange = getRange(key)
            val newRange = pos until pos + oldRange.size
            moveData(oldRange.first, oldData, newRange, newData)
            setRange(key, newRange)
            pos += oldRange.size
        }
        deallocate(oldData)
        return newData to pos
    }

    /**
     * returns whether the data is compact, and if so, how many elements it contains
     * */
    private fun getCompactSizeIfCompact(ranges: List<IntRange>): Int {
        if (ranges.isEmpty()) return 0
        val first = ranges.first()
        if (first.first != 0) return -1
        val last = ranges.last()
        val size = ranges.sumOf { it.size }
        return if (size == last.last + 1) size
        else -1
    }

    /**
     * pack elements, so we have enough space for elementX, too;
     * adjust ranges as needed, but don't add X yet.
     * */
    fun pack(
        elements: Collection<Key>, ranges: ArrayList<IntRange>,
        elementX: Key, available: Int, oldData: StoredData
    ): StoredData {
        val extraSize = getRange(elementX).size
        val compactSize = getCompactSizeIfCompact(ranges)
        if (compactSize > 0) {
            val requiredSize = compactSize + extraSize
            if (available >= requiredSize) {
                return oldData
            } else if (allocationKeepsOldData()) {
                // it is compact, but we need extra storage...
                val allocSize = roundUpStorage(requiredSize)
                return resizeTo(oldData, compactSize, allocSize)
            }
        }

        val requiredSize = sumSize1(ranges) + extraSize
        val allocSize = roundUpStorage(requiredSize)
        val (newData, pos) = pack(elements, oldData, allocSize)
        ranges.clear()
        ranges.add(0 until pos)
        return newData
    }

    fun resizeTo(oldData: StoredData, compactSize: Int, allocSize: Int): StoredData {
        val newData = allocate(allocSize)
        moveData(0, oldData, 0 until compactSize, newData)
        deallocate(oldData)
        return newData
    }

    fun roundUpStorage(requiredSize: Int): Int {
        return requiredSize + (requiredSize ushr 1)
    }

    fun setRange(key: Key, value: IntRange)
    fun getRange(key: Key): IntRange

    fun allocate(newSize: Int): StoredData
    fun deallocate(data: StoredData)

    /**
     * return false, if any allocate()-call clears all data and needs all data reinserted
     * */
    fun allocationKeepsOldData(): Boolean

    fun moveData(from: Int, fromData: StoredData, to: IntRange, toData: StoredData)
    fun insertData(from: Int, fromData: InsertData, to: IntRange, toData: StoredData)

    fun copyKey(key: Key, from: Int, fromData: InsertData, to: IntRange, toData: StoredData) {
        assertEquals(getRange(key).size, to.size)
        insertData(from, fromData, to, toData)
        setRange(key, to)
    }
}