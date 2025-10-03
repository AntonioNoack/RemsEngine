package me.anno.ecs.components.mesh

import me.anno.ecs.components.mesh.HelperMesh.Companion.updateHelperMeshes
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.ecs.components.mesh.MeshAttributes.color1
import me.anno.ecs.components.mesh.MeshAttributes.color2
import me.anno.ecs.components.mesh.MeshAttributes.color3
import me.anno.ecs.components.mesh.utils.VertexAttr
import me.anno.gpu.buffer.AttributeLayout
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.buffer.BufferUsage
import me.anno.gpu.buffer.CompactAttributeLayout
import me.anno.gpu.buffer.IndexBuffer
import me.anno.gpu.buffer.StaticBuffer
import me.anno.gpu.buffer.StridedAttributeLayout
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.lists.Lists.indexOfFirst2
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Ints.isPowerOf2
import org.joml.Vector2i
import kotlin.math.min

object MeshBufferUtils {

    fun replaceBuffer(
        name: String,
        attributes: AttributeLayout,
        vertexCount: Int,
        oldValue: StaticBuffer?,
    ): StaticBuffer {
        if (oldValue != null) {
            if (oldValue.attributes == attributes && oldValue.vertexCount == vertexCount) {
                oldValue.clear()
                return oldValue
            } else {
                oldValue.destroy()
            }
        }
        return StaticBuffer(name, attributes, vertexCount, BufferUsage.STATIC)
    }

    fun replaceBuffer(base: Buffer, indices: IntArray?, oldValue: IndexBuffer?): IndexBuffer? {
        return if (indices != null) {
            if (oldValue != null) {
                if (base === oldValue.base) {
                    oldValue.indices = indices
                    return oldValue
                } else oldValue.destroy()
            }
            IndexBuffer("${base.name}Indices", base, indices)
        } else {
            oldValue?.destroy()
            null
        }
    }

    private fun align(pos: Int, alignment: Int): Int {
        val rem = pos % alignment
        return if (rem != 0) pos - rem + alignment
        else pos
    }

    /**
     * Easier to join two buffers together.
     * */
    private fun Mesh.defineCompactLayout(): Pair<List<VertexAttr>, CompactAttributeLayout> {

        val srcAttributes = vertexAttributes
            .sortedByDescending { it.attribute.alignment }

        val numAttributes = srcAttributes.size
        val vertexAttributes = ArrayList<VertexAttr>(numAttributes)
        val offsets = IntArray(numAttributes)
        val numComponents = ByteArray(numAttributes)
        val names = ArrayList<String>(numAttributes)
        val types = ArrayList<AttributeType>(numAttributes)

        var pos = 0
        val gaps = ArrayList<Vector2i>() // pos, size
        for (i in srcAttributes.indices) {

            val (attr, data) = srcAttributes[i]
            val size = attr.byteSize
            val alignment = attr.alignment
            assertTrue(alignment.isPowerOf2())

            names.add(attr.name)
            types.add(attr.type)
            numComponents[i] = attr.numComponents.toByte()

            val gapIndex = gaps.indexOfFirst2 { it.y >= size } // gaps must be sorted by size
            if (gapIndex >= 0) {
                val gap = gaps.removeAt(gapIndex)

                // insert at end of gap
                val insertPos = gap.x + gap.y - size
                assertEquals(insertPos, align(insertPos, alignment))
                offsets[i] = insertPos

                if (gap.y > size) {
                    gap.y -= size
                    gaps.add(gap)
                    gaps.sortBy { it.y }
                }
            } else {
                pos = align(pos, alignment)
                offsets[i] = pos
                pos += size
            }

            vertexAttributes.add(VertexAttr.map(attr, data))
        }

        val maxAlignment = srcAttributes.first().attribute.alignment
        val stride = align(pos, maxAlignment)

        return vertexAttributes to CompactAttributeLayout(names, types, numComponents, offsets, stride)
    }

    /**
     * Theoretically better cache-locality, if only a subset of properties is used.
     * When using AttributeReadWrite-shader-logic, RenderDoc only shows this properly as a very long table with a single row.
     * */
    @Suppress("unused")
    private fun Mesh.defineStridedLayout(): Pair<List<VertexAttr>, StridedAttributeLayout> {

        val numVertices = numVertices
        val srcAttributes = vertexAttributes
            .sortedByDescending { it.attribute.alignment }

        val numAttributes = srcAttributes.size
        val vertexAttributes = ArrayList<VertexAttr>(numAttributes)
        val offsets = IntArray(numAttributes)
        val numComponents = ByteArray(numAttributes)
        val strides = IntArray(numAttributes)
        val names = ArrayList<String>(numAttributes)
        val types = ArrayList<AttributeType>(numAttributes)

        var pos = 0
        for (i in srcAttributes.indices) {

            val (attr, data) = srcAttributes[i]
            val alignment = attr.alignment
            assertTrue(alignment.isPowerOf2())

            names.add(attr.name)
            types.add(attr.type)
            numComponents[i] = attr.numComponents.toByte()

            // find a good start position
            pos = align(pos, alignment)

            // write current position
            offsets[i] = pos
            strides[i] = alignment

            // advance buffer
            pos += numVertices * alignment

            vertexAttributes.add(VertexAttr.map(attr, data))
        }

        return vertexAttributes to StridedAttributeLayout(
            names, types, numComponents, offsets,
            numVertices, pos, strides
        )
    }

    private fun Mesh.bufferName() = refOrNull?.absolutePath ?: name.ifEmpty { "Mesh" }

    private fun Mesh.createMeshBufferSetup() {

        needsMeshUpdate = false

        // not the safest, but well...
        val positions = positions ?: return
        if (positions.isEmpty()) return

        ensureNorTanUVs()

        val color0 = color0
        val color1 = color1
        val color2 = color2
        val color3 = color3

        val hasBones = hasBones
        hasBonesInBuffer = hasBones

        val hasColor0 = color0 != null && color0.isNotEmpty()
        val hasColor1 = color1 != null && color1.isNotEmpty()
        val hasColor2 = color2 != null && color2.isNotEmpty()
        val hasColor3 = color3 != null && color3.isNotEmpty()
        hasVertexColors = hasColor0.toInt() + hasColor1.toInt(2) + hasColor2.toInt(4) + hasColor3.toInt(8)
    }

    private fun Mesh.createVertexBuffer(attributes: AttributeLayout): StaticBuffer {
        val name = bufferName()
        val buffer = replaceBuffer(name, attributes, numVertices, buffer)
        buffer.drawMode = drawMode
        this.buffer = buffer
        return buffer
    }

    private fun Mesh.createAndFillIndexBuffer(buffer: Buffer) {
        triBuffer = replaceBuffer(buffer, indices, triBuffer)
        triBuffer?.drawMode = drawMode
    }

    private val Mesh.numVertices: Int
        get() {
            val positions = positions ?: return 0
            val normals = normals ?: return 0
            return min(positions.size, normals.size) / 3
        }

    fun Mesh.fillVertexData(
        attributes: List<VertexAttr>,
        layout: AttributeLayout,
        buffer: Buffer
    ) {
        val nio = buffer.getOrCreateNioBuffer()
        val numVertices = numVertices
        if (layout is StridedAttributeLayout) {
            for (i in attributes.indices) {
                val attr = attributes[i]
                val offset = layout.offset(i)
                val stride = layout.stride(i)
                for (vertexIndex in 0 until numVertices) {
                    nio.position(vertexIndex * stride + offset)
                    attr.fill(vertexIndex, nio)
                }
            }
        } else {
            for (vertexIndex in 0 until numVertices) {
                for (i in attributes.indices) {
                    val attr = attributes[i]
                    val offset = layout.offset(i)
                    val stride = layout.stride(i)
                    nio.position(vertexIndex * stride + offset)
                    attr.fill(vertexIndex, nio)
                }
            }
        }
        nio.position(layout.totalSize(numVertices))
        buffer.cpuSideChanged()
    }

    fun Mesh.createMeshBufferImpl() {
        createMeshBufferSetup()
        val (attributes, layout) = defineCompactLayout()
        val buffer = createVertexBuffer(layout)
        fillVertexData(attributes, layout, buffer)
        createAndFillIndexBuffer(buffer)
        updateHelperMeshes()
    }
}