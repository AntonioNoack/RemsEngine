package me.anno.graph.hdb.allocator

import me.anno.graph.hdb.index.File
import me.anno.utils.InternalAPI
import me.anno.utils.types.size

@InternalAPI
object FileAllocation : AllocationManager<File, ByteArray> {
    override fun allocate(newSize: Int): ByteArray {
        return ByteArray(newSize)
    }

    override fun deallocate(data: ByteArray) {
        // JVM needs to free it
    }

    override fun copyData(from: Int, fromData: ByteArray, to: IntRange, toData: ByteArray) {
        fromData.copyInto(toData, to.first, from, from + to.size)
    }

    override fun setRange(key: File, value: IntRange) {
        key.range = value
    }

    override fun getRange(key: File): IntRange {
        return key.range
    }

    override fun allocationKeepsOldData(): Boolean {
        return true
    }
}