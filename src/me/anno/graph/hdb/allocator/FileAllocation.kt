package me.anno.graph.hdb.allocator

import me.anno.graph.hdb.index.File
import me.anno.utils.InternalAPI
import me.anno.utils.types.Ranges.size

@InternalAPI
class FileAllocation(
    override val instances: ArrayList<File>,
    override var storage: ByteArray?,
    override var storageSize: Int
) : AllocationManager<File, ByteArray, ByteArray> {

    override val holes = ArrayList<IntRange>()

    override fun allocate(newSize: Int): ByteArray {
        return ByteArray(newSize)
    }

    override fun deallocate(data: ByteArray) {
        // JVM needs to free it
    }

    override fun moveData(from: Int, fromData: ByteArray, to: IntRange, toData: ByteArray) {
        fromData.copyInto(toData, to.first, from, from + to.size)
    }

    override fun insertData(from: Int, fromData: ByteArray, to: IntRange, toData: ByteArray) {
        moveData(from, fromData, to, toData)
    }

    override fun setRange(instance: File, value: IntRange) {
        instance.range = value
    }

    override fun getRange(instance: File): IntRange {
        return instance.range
    }
}