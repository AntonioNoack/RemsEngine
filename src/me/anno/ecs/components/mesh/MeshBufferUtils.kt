package me.anno.ecs.components.mesh

import me.anno.ecs.components.mesh.HelperMesh.Companion.updateHelperMeshes
import me.anno.ecs.components.mesh.Mesh.Companion.MAX_WEIGHTS
import me.anno.ecs.components.mesh.MeshAttributes.color0
import me.anno.ecs.components.mesh.MeshAttributes.color1
import me.anno.ecs.components.mesh.MeshAttributes.color2
import me.anno.ecs.components.mesh.MeshAttributes.color3
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeLayout
import me.anno.gpu.buffer.AttributeLayout.Companion.bind
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.Buffer
import me.anno.gpu.buffer.BufferUsage
import me.anno.gpu.buffer.IndexBuffer
import me.anno.gpu.buffer.StaticBuffer
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Floats.roundToIntOr
import kotlin.math.max
import kotlin.math.min

object MeshBufferUtils {

    fun replaceBuffer(
        name: String,
        attributes: List<Attribute>,
        vertexCount: Int,
        oldValue: StaticBuffer?,
    ): StaticBuffer {
        if (oldValue != null) {
            if (attributesAreEqual(oldValue.attributes, attributes) && oldValue.vertexCount == vertexCount) {
                oldValue.clear()
                return oldValue
            } else {
                oldValue.destroy()
            }
        }
        return StaticBuffer(name, bind(attributes), vertexCount, BufferUsage.STATIC)
    }

    private fun attributesAreEqual(a: AttributeLayout, b: List<Attribute>): Boolean {
        if (a.size != b.size) return false
        for (i in b.indices) {
            if (!a.equals(i, b[i])) {
                return false
            }
        }
        return true
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

    fun addColorAttributes(
        attributes: ArrayList<Attribute>,
        hasColor0: Boolean, hasColor1: Boolean, hasColor2: Boolean, hasColor3: Boolean
    ) {
        if (hasColor0) attributes += Attribute("colors0", AttributeType.UINT8_NORM, 4)
        if (hasColor1) attributes += Attribute("colors1", AttributeType.UINT8_NORM, 4)
        if (hasColor2) attributes += Attribute("colors2", AttributeType.UINT8_NORM, 4)
        if (hasColor3) attributes += Attribute("colors3", AttributeType.UINT8_NORM, 4)
    }

    fun addUVAttributes(attributes: ArrayList<Attribute>) {
        attributes += Attribute("uvs", 2)
        attributes += Attribute("tangents", AttributeType.SINT8_NORM, 4)
    }

    fun addBoneAttributes(attributes: ArrayList<Attribute>) {
        attributes += Attribute("boneWeights", AttributeType.UINT8_NORM, MAX_WEIGHTS)
        attributes += Attribute("boneIndices", AttributeType.UINT8, MAX_WEIGHTS)
    }

    fun addNormalAttribute(attributes: ArrayList<Attribute>, hasHighPrecisionNormals: Boolean) {
        attributes += if (hasHighPrecisionNormals) {
            Attribute("normals", AttributeType.FLOAT, 3)
        } else {
            // todo normals could be oct-encoded
            Attribute("normals", AttributeType.SINT8_NORM, 4)
        }
    }

    fun Mesh.createMeshBufferImpl() {

        needsMeshUpdate = false

        // not the safest, but well...
        val positions = positions ?: return
        if (positions.isEmpty()) return

        ensureNorTanUVs()

        val normals = normals!!
        val tangents = tangents

        val uvs = uvs
        val hasUVs = hasUVs

        val color0 = color0
        val color1 = color1
        val color2 = color2
        val color3 = color3
        val boneWeights = boneWeights
        val boneIndices = boneIndices

        val vertexCount = min(positions.size, normals.size) / 3
        val indices = indices

        val hasBones = hasBones
        hasBonesInBuffer = hasBones

        val hasColor0 = color0 != null && color0.isNotEmpty()
        val hasColor1 = color1 != null && color1.isNotEmpty()
        val hasColor2 = color2 != null && color2.isNotEmpty()
        val hasColor3 = color3 != null && color3.isNotEmpty()
        hasVertexColors = hasColor0.toInt() + hasColor1.toInt(2) + hasColor2.toInt(4) + hasColor3.toInt(8)

        val hasHighPrecisionNormals = hasHighPrecisionNormals

        val attributes = ArrayList<Attribute>()
        attributes += Attribute("positions", 3)
        addNormalAttribute(attributes, hasHighPrecisionNormals)
        if (hasUVs) addUVAttributes(attributes)
        addColorAttributes(attributes, hasColor0, hasColor1, hasColor2, hasColor3)
        if (hasBones) addBoneAttributes(attributes)

        val name = refOrNull?.absolutePath ?: name.ifEmpty { "Mesh" }
        val buffer = replaceBuffer(name, attributes, vertexCount, buffer)
        buffer.drawMode = drawMode
        this.buffer = buffer

        triBuffer = replaceBuffer(buffer, indices, triBuffer)
        triBuffer?.drawMode = drawMode

        for (i in 0 until vertexCount) {

            // upload all data of one vertex

            val i3 = i * 3
            val i4 = i * 4

            putPosition(buffer, positions, i3)
            putNormal(buffer, normals, i3, hasHighPrecisionNormals)

            if (hasUVs) {
                putUVs(buffer, uvs, i * 2)
                putTangent(buffer, tangents, i4)
            }

            if (hasColor0) putColor(buffer, color0, i)
            if (hasColor1) putColor(buffer, color1, i)
            if (hasColor2) putColor(buffer, color2, i)
            if (hasColor3) putColor(buffer, color3, i)

            // only works if MAX_WEIGHTS is four
            if (hasBones) {
                putBoneWeights(buffer, boneWeights, i4)
                putBoneIndices(buffer, boneIndices, i4)
            }
        }

        updateHelperMeshes()
    }

    fun putPosition(buffer: StaticBuffer, positions: FloatArray, i3: Int) {
        buffer.put(positions[i3])
        buffer.put(positions[i3 + 1])
        buffer.put(positions[i3 + 2])
    }

    fun putNormal(buffer: StaticBuffer, normals: FloatArray, i3: Int, hasHighPrecisionNormals: Boolean) {
        if (hasHighPrecisionNormals) {
            buffer.put(normals[i3])
            buffer.put(normals[i3 + 1])
            buffer.put(normals[i3 + 2])
        } else {
            buffer.putByte(normals[i3])
            buffer.putByte(normals[i3 + 1])
            buffer.putByte(normals[i3 + 2])
            buffer.putByte(0) // alignment
        }
    }

    fun putTangent(buffer: StaticBuffer, tangents: FloatArray?, i4: Int) {
        if (tangents != null && i4 + 3 < tangents.size) {
            buffer.putByte(tangents[i4])
            buffer.putByte(tangents[i4 + 1])
            buffer.putByte(tangents[i4 + 2])
            buffer.putByte(tangents[i4 + 3])
        } else {
            buffer.putByte(0)
            buffer.putByte(0)
            buffer.putByte(0)
            buffer.putByte(127) // positive ^^
        }
    }

    fun putUVs(buffer: StaticBuffer, uvs: FloatArray?, i2: Int) {
        if (uvs != null && i2 + 1 < uvs.size) {
            buffer.put(uvs[i2])
            buffer.put(uvs[i2 + 1])
        } else buffer.put(0f, 0f)
    }

    fun putColor(buffer: StaticBuffer, colors: IntArray?, i: Int) {
        if (i < colors!!.size) {
            buffer.putRGBA(colors[i])
        } else buffer.putInt(-1)
    }

    fun putBoneWeights(buffer: StaticBuffer, boneWeights: FloatArray?, i4: Int) {
        if (boneWeights != null && i4 + 3 < boneWeights.size) {
            val w0 = max(boneWeights[i4], 1e-5f)
            val w1 = boneWeights[i4 + 1]
            val w2 = boneWeights[i4 + 2]
            val w3 = boneWeights[i4 + 3]
            val normalisation = 255f / (w0 + w1 + w2 + w3)
            val w1b = (w1 * normalisation).roundToIntOr()
            val w2b = (w2 * normalisation).roundToIntOr()
            val w3b = (w3 * normalisation).roundToIntOr()
            val w0b = max(255 - (w1b + w2b + w3b), 0)
            buffer.putByte(w0b.toByte())
            buffer.putByte(w1b.toByte())
            buffer.putByte(w2b.toByte())
            buffer.putByte(w3b.toByte())
        } else {
            buffer.putByte(-1)
            buffer.putByte(0)
            buffer.putByte(0)
            buffer.putByte(0)
        }
    }

    fun putBoneIndices(buffer: StaticBuffer, boneIndices: ByteArray?, i4: Int) {
        if (boneIndices != null && i4 + 3 < boneIndices.size) {
            buffer.putByte(boneIndices[i4])
            buffer.putByte(boneIndices[i4 + 1])
            buffer.putByte(boneIndices[i4 + 2])
            buffer.putByte(boneIndices[i4 + 3])
        } else {
            buffer.putInt(0)
        }
    }
}