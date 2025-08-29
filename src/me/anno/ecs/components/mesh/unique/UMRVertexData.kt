package me.anno.ecs.components.mesh.unique

import me.anno.gpu.buffer.OpenGLBuffer
import me.anno.gpu.buffer.StaticBuffer
import me.anno.utils.types.Ranges.overlaps
import me.anno.utils.types.Ranges.size

class UMRVertexData<Key, Mesh>(val self: UniqueMeshRenderer<Key, Mesh>) : UMRData<Key, Mesh>(self.attributes) {
    override fun getRange(key: Mesh): IntRange = self.getVertexRange(key)
    override fun setRange(key: Mesh, value: IntRange) = self.setVertexRange(key, value)
    override fun insertData(from: Int, fromData: Mesh, to: IntRange, toData: StaticBuffer) =
        self.insertVertexData(from, fromData, to, toData)

    private fun collectRangesToMove(fromRange: IntRange): List<IntRange> {
        val toAdjustRanges = ArrayList<IntRange>()
        for ((_, mesh) in entries) {
            val range = self.getVertexRange(mesh)
            if (fromRange.overlaps(range)) {
                toAdjustRanges.add(self.getIndexRange(mesh))
            }
        }
        toAdjustRanges.sortWith { a, b -> a.first.compareTo(b.first) }
        return toAdjustRanges
    }

    private fun adjustIndexValues(indexBuffer: OpenGLBuffer, from: Int, to: IntRange) {
        val valueDelta = to.start - from
        val fromRange = from until (from + to.size)
        object : UMRIterator<IntRange> {
            override fun getRange(entry: IntRange): IntRange = entry
            override fun filter(entry: IntRange): Boolean = true
            override fun push(start: Int, endExcl: Int) {
                IndexCopyShader.copyIndices(
                    start, indexBuffer, start, indexBuffer,
                    endExcl - start, valueDelta
                )
            }
        }.iterateRanges(collectRangesToMove(fromRange))
    }

    override fun moveData(from: Int, fromData: StaticBuffer, to: IntRange, toData: StaticBuffer) {
        super.moveData(from, fromData, to, toData)

        val umrIndexData = self.umrIndexData ?: return
        if (from != to.start && (fromData == buffer0 || fromData == buffer1)) {
            val indexBuffer = umrIndexData.buffer
            adjustIndexValues(indexBuffer, from, to)
        }
    }
}