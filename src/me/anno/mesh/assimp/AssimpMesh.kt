package me.anno.mesh.assimp

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.IndexedStaticBuffer
import kotlin.math.sqrt


open class AssimpMesh(
    positions: FloatArray,
    uvs: FloatArray,
    normals: FloatArray,
    colors: FloatArray,
    indices: IntArray,
    jointIndices: IntArray?,
    weights: FloatArray?
) {

    companion object {
        const val MAX_WEIGHTS = 4
        val attributes = listOf(
            Attribute("coords", 3),
            Attribute("uvs", 2),
            Attribute("normals", 3),
            Attribute("colors", 4),
            Attribute("weights", MAX_WEIGHTS),
            Attribute("indices", AttributeType.SINT32, MAX_WEIGHTS)
        )
    }

    var animations: List<Animation>? = null

    val buffer: IndexedStaticBuffer

    var vertexCount = 0
    var material: Material? = null
    var boundingRadius = 0f

    private fun calculateBoundingRadius(positions: FloatArray) {
        var radiusSq = 0f
        for (i in positions.indices step 3) {
            val x = positions[i]
            val y = positions[i + 1]
            val z = positions[i + 2]
            val distanceSq = x * x + y * y + z * z
            if (distanceSq > radiusSq) radiusSq = distanceSq
        }
        boundingRadius = sqrt(radiusSq)
    }

    fun destroy() {
        buffer.destroy()
    }

    init {
        calculateBoundingRadius(positions)
        val pointCount = positions.size / 3
        val buffer = IndexedStaticBuffer(attributes, pointCount, indices)
        for (i in 0 until pointCount) {

            // upload all data of one vertex

            buffer.put(positions[i * 3])
            buffer.put(positions[i * 3 + 1])
            buffer.put(positions[i * 3 + 2])

            if (uvs.size > i * 2 + 1) {
                buffer.put(uvs[i * 2])
                buffer.put(uvs[i * 2 + 1])
            } else {
                buffer.put(0f, 0f)
            }

            buffer.put(normals[i * 3])
            buffer.put(normals[i * 3 + 1])
            buffer.put(normals[i * 3 + 2])

            if (colors.size > i * 4 + 3) {
                buffer.put(colors[i * 4])
                buffer.put(colors[i * 4 + 1])
                buffer.put(colors[i * 4 + 2])
                buffer.put(colors[i * 4 + 3])
            } else {
                buffer.put(1f, 1f, 1f, 1f)
            }

            if (weights != null && weights.isNotEmpty()) {
                val w0 = weights[i * 4]
                val w1 = weights[i * 4 + 1]
                val w2 = weights[i * 4 + 2]
                val w3 = weights[i * 4 + 3]
                val normalisation = 1f / (w0 + w1 + w2 + w3)
                buffer.put(w0 * normalisation)
                buffer.put(w1 * normalisation)
                buffer.put(w2 * normalisation)
                buffer.put(w3 * normalisation)
            } else {
                buffer.put(1f, 0f, 0f, 0f)
            }

            if (jointIndices != null && jointIndices.isNotEmpty()) {
                buffer.putInt(jointIndices[i * 4])
                buffer.putInt(jointIndices[i * 4 + 1])
                buffer.putInt(jointIndices[i * 4 + 2])
                buffer.putInt(jointIndices[i * 4 + 3])
            } else {
                buffer.putInt(0)
                buffer.putInt(0)
                buffer.putInt(0)
                buffer.putInt(0)
            }

        }
        this.buffer = buffer
    }

}