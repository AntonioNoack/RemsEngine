package me.anno.ecs.components.mesh.unique

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.CompactAttributeLayout.Companion.bind
import me.anno.gpu.buffer.StaticBuffer

class UMRIndexData<Key, Mesh>(val self: UniqueMeshRenderer<Key, Mesh>) : UMRData<Key, Mesh>(indexAttributes) {
    override fun getRange(key: Mesh): IntRange = self.getIndexRange(key)
    override fun setRange(key: Mesh, value: IntRange) = self.setIndexRange(key, value)
    override fun insertData(from: Int, fromData: Mesh, to: IntRange, toData: StaticBuffer) {
        self.insertIndexData(from, fromData, to, toData)
    }

    companion object {
        private val indexAttributes = bind(Attribute("index", AttributeType.UINT32, 1))
    }
}