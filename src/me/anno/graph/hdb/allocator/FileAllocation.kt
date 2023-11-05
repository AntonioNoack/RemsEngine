package me.anno.graph.hdb.allocator

import me.anno.graph.hdb.index.File

object FileAllocation : AllocationManager<File, ByteArray> {
    override fun allocate(newSize: Int): ByteArray {
        return ByteArray(newSize)
    }

    override fun copy(key: File, from: Int, fromData: ByteArray, to: IntRange, toData: ByteArray) {
        System.arraycopy(fromData, from, toData, to.first, to.size)
        key.range = to
    }

    override fun copy(from: Int, fromData: ByteArray, to: IntRange, toData: ByteArray) {
        System.arraycopy(fromData, from, toData, to.first, to.size)
    }

    override fun getRange(key: File): IntRange {
        return key.range
    }
}