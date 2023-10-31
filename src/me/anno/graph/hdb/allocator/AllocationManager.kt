package me.anno.graph.hdb.allocator

interface AllocationManager<Key, Data> {

    fun insert(
        elements: Collection<Key>,
        newKey: Key, newData: Data,
        newSize: Int, available: Int,
        oldData: Data
    ): Pair<ReplaceType, Data> {

        if (elements.isEmpty()) return ReplaceType.WriteCompletely to newData // easy: just replace
        val inOrder = elements.sortedBy { getRange(it).first }

        // check if 0th place is ok
        val r0 = getRange(inOrder.first())
        if (newSize < r0.first) {
            val newRange = 0 until newSize
            copy(newKey, 0, newData, newRange, oldData)
            return ReplaceType.InsertInto to oldData
        }

        // check if we can insert it in-between
        for (i in 1 until inOrder.size) {
            val ri = getRange(inOrder[i - 1])
            val rj = getRange(inOrder[i])
            val start = ri.first + ri.size
            if (start + newSize <= rj.first) {
                val newRange = start until start + newSize
                copy(newKey, 0, newData, newRange, oldData)
                return ReplaceType.InsertInto to oldData
            }
        }

        // check if at the end is ok
        val re = getRange(inOrder.last())
        val start = re.first + re.size
        if (start + newSize < available) {
            val newRange = start until start + newSize
            copy(newKey, 0, newData, newRange, oldData)
            return ReplaceType.InsertInto to oldData
        }

        // we have to append it
        // todo round up size, so we can potentially append more
        return ReplaceType.Append to append(elements, newKey, newData, newSize, oldData)
    }

    fun append(elements: Collection<Key>, newKey: Key, newData: Data, newSize: Int, oldData: Data): Data {
        val start = sumSize(elements)
        val newRange = start until start + newSize
        val newData1 = pack(elements + newKey, oldData)
        copy(newKey, 0, newData, newRange, newData1)
        return newData1
    }

    fun sumSize(elements: Collection<Key>): Int {
        return elements.sumOf { getRange(it).size }
    }

    fun maximumAcceptableSize(requiredSize: Int): Int {
        return if (requiredSize == 0) 0
        else requiredSize * 3 / 2 + 65536
    }

    fun shouldOptimize(elements: Collection<Key>, available: Int): Boolean {
        val requiredSize = sumSize(elements)
        return available > maximumAcceptableSize(requiredSize)
    }

    fun pack(elements: Collection<Key>, oldData: Data): Data {
        val requiredSize = sumSize(elements)
        // optimization is worth it
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

    fun getRange(key: Key): IntRange

    fun allocate(newSize: Int): Data

    fun copy(key: Key, from: Int, fromData: Data, to: IntRange, toData: Data)

}