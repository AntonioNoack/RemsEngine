package me.anno.ecs.components.mesh

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.Attribute.Companion.computeOffsets
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.buffer.IndexBuffer
import me.anno.gpu.buffer.StaticBuffer

object MeshBufferUtils {

    fun replaceBuffer(
        name: String,
        attributes: List<Attribute>,
        vertexCount: Int,
        oldValue: StaticBuffer?,
    ): StaticBuffer {
        if (oldValue != null) {
            // offsets are compared, so they need to be consistent
            computeOffsets(attributes)
            computeOffsets(oldValue.attributes)
            if (oldValue.attributes == attributes && oldValue.vertexCount == vertexCount) {
                oldValue.clear()
                return oldValue
            } else {
                oldValue.destroy()
            }
        }
        return StaticBuffer(name, attributes, vertexCount)
    }

    fun replaceBuffer(base: Buffer, indices: IntArray?, oldValue: IndexBuffer?): IndexBuffer? {
        return if (indices != null) {
            if (oldValue != null) {
                if (base === oldValue.base) {
                    oldValue.indices = indices
                    return oldValue
                } else oldValue.destroy()
            }
            IndexBuffer(base.name, base, indices)
        } else {
            oldValue?.destroy()
            null
        }
    }
}