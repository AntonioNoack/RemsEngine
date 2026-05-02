package me.anno.graph.hdb.allocator

import me.anno.utils.structures.lists.Lists.indexOfFirst2
import me.anno.utils.types.Ranges.size
import org.apache.logging.log4j.LogManager

interface AllocationManager<Instance, Array, ArrayDelta> {

    companion object {
        private val LOGGER = LogManager.getLogger(AllocationManager::class)
        val emptyRange = IntRange(0, -1)
    }

    val instances: ArrayList<Instance>
    val holes: ArrayList<IntRange>
    var storage: Array?
    var storageSize: Int

    fun validate(): Boolean {
        instances.sortBy { instance -> getRange(instance).first }
        synchronized(this) {
            holes.clear()
            var start = 0
            for (i in instances.indices) {
                val instance = instances[i]
                val range = getRange(instance)
                if (start > range.first) {
                    LOGGER.warn("Overlapping ranges[$i] ${instances.map { getRange(it) }}")
                    return false
                }
                addHole(start, range.first)
                start = range.last + 1
            }
            addHole(start, storageSize)
        }
        return true
    }

    private fun addHole(start: Int, endExcl: Int) {
        val space = endExcl - start
        check(space >= 0) { "Expected space to be non-negative" }
        if (space > 0) holes.add(IntRange(start, endExcl - 1))
    }

    fun addData(addedInstance: Instance, addedData: ArrayDelta): Insertion? {
        synchronized(this) {
            // find hole with enough space
            val insertRange = getRange(addedInstance)
            val requiredSpace = insertRange.size
            if (requiredSpace <= 0) return null

            val holeIndex = holes.indexOfFirst2 { hole -> hole.size >= requiredSpace }
            if (holeIndex >= 0) {
                val hole = holes[holeIndex]
                val remainder = hole.size - requiredSpace
                check(remainder >= 0)

                // copy data
                val toRange = IntRange(hole.first, hole.first + requiredSpace - 1)
                insertData(insertRange.first, addedData, toRange, storage!!)
                setRange(addedInstance, toRange)

                // updating instances-list
                var insertIndex = instances.binarySearch { other -> getRange(other).first.compareTo(hole.first) }
                check(insertIndex < 0) { "Tried inserting into $hole, but found instance ${getRange(instances[insertIndex])} at the same place" }
                insertIndex = -insertIndex - 1
                instances.add(insertIndex, addedInstance)

                // updating holes
                if (remainder > 0) {
                    holes[holeIndex] = IntRange(hole.first + requiredSpace, hole.last)
                } else {
                    holes.removeAt(holeIndex)
                }
                return Insertion(ReplaceType.InsertInto, hole.first)
            } else {
                val insertPos = if (!canMoveData() || !shouldOptimize(instances, storageSize)) {
                    // old data is kept -> no copy of all elements necessary
                    resizeAndAppend(addedInstance, addedData, insertRange)
                } else {
                    packAndAppend(addedInstance, addedData, insertRange)
                }
                val type = if (canMoveData()) ReplaceType.WriteCompletely else ReplaceType.Append
                return Insertion(type, insertPos)
            }
        }
    }

    private fun resizeAndAppend(addedInstance: Instance, addedData: ArrayDelta, insertRange: IntRange): Int {
        val requiredSpace = insertRange.size
        val newSize = roundUpStorage(storageSize + requiredSpace)
        val newData = allocate(newSize)
        val canMoveData = canMoveData()
        val storage = storage
        return if (newData === storage || !canMoveData) {

            val lastHole = holes.lastOrNull()
            val newStart = if (lastHole != null && lastHole.last + 1 == storageSize) {
                // use the last hole, too
                holes.removeLast()
                lastHole.first
            } else storageSize

            if (storage != null && newData !== storage && instances.isNotEmpty() && newStart > 0) {
                val i0 = getRange(instances.first()).first
                moveData(i0, storage, IntRange(i0, newStart - 1), newData)
            }

            insertLast(newStart, insertRange, newData, addedInstance, addedData)
            finishResize(newData, newStart + requiredSpace, newSize)
            newStart
        } else {
            packAndAppend(addedInstance, addedData, insertRange, newData, newSize)
        }
    }

    private fun packAndAppend(addedInstance: Instance, addedData: ArrayDelta, insertRange: IntRange): Int {
        val requiredSpace = insertRange.size
        val totalSize = instances.sumOf { instance -> getRange(instance).size }
        val newSize = roundUpStorage(totalSize + requiredSpace)
        val newData = allocate(newSize)
        return packAndAppend(addedInstance, addedData, insertRange, newData, newSize)
    }

    private fun packAndAppend(
        addedInstance: Instance, addedData: ArrayDelta, insertRange: IntRange,
        newData: Array, newSize: Int,
    ): Int {
        val oldData = storage
        val newStart = packImpl(newData)
        insertLast(newStart, insertRange, newData, addedInstance, addedData)
        finishResize(newData, newStart + insertRange.size, newSize)
        if (oldData !== newData && oldData != null) deallocate(oldData)
        return newStart
    }

    fun pack(newSize: Int) {
        val newData = allocate(newSize)
        val newStart = packImpl(newData)
        finishResize(newData, newStart, newSize)
    }

    private fun packImpl(newData: Array): Int {
        if (instances.isEmpty()) return 0
        var newStart = 0
        val oldData = storage!!
        for (i in instances.indices) {
            val instance = instances[i]
            val oldRange = getRange(instance)
            val newRange = IntRange(newStart, newStart + oldRange.size - 1)

            // todo if chunks are consecutive, we need much fewer copies
            moveData(oldRange.first, oldData, newRange, newData)

            setRange(instance, newRange)
            newStart += oldRange.size
        }
        return newStart
    }

    private fun insertLast(
        newStart: Int, insertRange: IntRange, newData: Array,
        addedInstance: Instance, addedData: ArrayDelta
    ) {
        val requiredSpace = insertRange.size
        val newRange = IntRange(newStart, newStart + requiredSpace - 1)
        insertData(insertRange.first, addedData, newRange, newData)
        setRange(addedInstance, newRange)
        instances.add(addedInstance)
    }

    private fun finishResize(newData: Array, newStart: Int, newSize: Int) {
        storage = newData
        storageSize = newSize
        holes.clear()
        addHole(newStart, newSize)
    }

    fun removeData(element: Instance): Boolean {
        synchronized(this) {
            val searchedRange = getRange(element)
            if (searchedRange.isEmpty()) return false

            val searchedStart = searchedRange.first
            val index = instances.binarySearch { other -> getRange(other).first.compareTo(searchedStart) }
            if (index < 0) return false

            val removed = instances.removeAt(index)
            val removedRange = getRange(removed)
            if (removedRange != searchedRange) {
                LOGGER.warn("Tried removing $element@$searchedRange, but removed $removed@$removedRange instead")
            }

            addHole(removedRange)
            setRange(element, emptyRange) // just in case
            return true
        }
    }

    private fun addHole(range: IntRange) {
        var insertIndex = holes.binarySearch { other -> other.first.compareTo(range.first) }
        check(insertIndex < 0) { "Duplicate hole start" }
        insertIndex = -insertIndex - 1

        holes.add(insertIndex, range)
        tryJoinHoles(insertIndex)
        tryJoinHoles(insertIndex - 1)
    }

    private fun tryJoinHoles(i: Int) {
        if (i < 0 || i + 1 >= holes.size) return
        val h0 = holes[i]
        val h1 = holes[i + 1]
        val delta = h1.first - (h0.last + 1)
        check(delta >= 0) { "Overlapping holes" }
        if (delta == 0) {
            holes.removeAt(i + 1)
            holes[i] = IntRange(h0.first, h1.last)
        }
    }

    fun clear() {
        synchronized(this) {
            instances.clear()
            holes.clear()
            holes.add(IntRange(0, storageSize - 1))
        }
    }

    private fun maximumAcceptableSize(requiredSize: Int): Int {
        return if (requiredSize == 0) 0
        else requiredSize * 3 / 2 + 1024
    }

    fun shouldOptimize(elements: List<Instance>, available: Int): Boolean {
        val requiredSize = elements.sumOf { getRange(it).size }
        return available > maximumAcceptableSize(requiredSize)
    }

    fun roundUpStorage(requiredSize: Int): Int {
        return requiredSize + (requiredSize ushr 1)
    }

    fun setRange(instance: Instance, value: IntRange)
    fun getRange(instance: Instance): IntRange

    fun allocate(newSize: Int): Array
    fun deallocate(data: Array)

    fun moveData(from: Int, fromData: Array, to: IntRange, toData: Array)
    fun insertData(from: Int, fromData: ArrayDelta, to: IntRange, toData: Array)

    /**
     * return false, if data must not move, e.g. because you store indices of it;
     * a first move (insertData) must be supported though
     * */
    fun canMoveData(): Boolean = true
}