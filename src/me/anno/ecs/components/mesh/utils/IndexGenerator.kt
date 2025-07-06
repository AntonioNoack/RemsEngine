package me.anno.ecs.components.mesh.utils

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshAttributes.color0
import org.apache.logging.log4j.LogManager
import kotlin.math.min

object IndexGenerator {

    private val LOGGER = LogManager.getLogger(IndexGenerator::class)

    private data class UniquePoint(
        var x: Float, var y: Float, var z: Float,
        var u: Float, var v: Float,
        var color0: Int,
        var materialId: Int,
        var b0: Byte, var b1: Byte, var b2: Byte, var b3: Byte,
        var w0: Float, var w1: Float, var w2: Float, var w3: Float,
    ) {
        constructor(x: Float, y: Float, z: Float) : this(
            x, y, z, 0f, 0f, 0, 0,
            0, 0, 0, 0, 0f, 0f, 0f, 0f
        )
    }

    @JvmStatic
    fun Mesh.generateIndices() {

        val positions = positions!!
        val uvs = uvs
        val color0 = color0
        val materialIds = materialIds
        val boneIndices = boneIndices
        val boneWeights = boneWeights

        // todo we probably should be able to specify a merging radius, so close points get merged
        // for that however, we need other acceleration structures like oct-trees to accelerate the queries

        // what defines a unique point:
        // position, uvs, material index, bone indices and bone weights
        // in the future, we should maybe support all colors...

        // generate all points
        val points = Array(positions.size / 3) {
            val i3 = it * 3
            UniquePoint(positions[i3], positions[i3 + 1], positions[i3 + 2])
        }

        if (uvs != null) {
            for (i in 0 until min(uvs.size shr 1, points.size)) {
                val i2 = i + i
                val p = points[i]
                p.u = uvs[i2]
                p.v = uvs[i2 + 1]
            }
        }

        if (color0 != null) {
            for (i in 0 until min(color0.size, points.size)) {
                points[i].color0 = color0[i]
            }
        }

        if (materialIds != null) {
            for (i in 0 until min(materialIds.size, points.size)) {
                points[i].materialId = materialIds[i]
            }
        }

        if (boneWeights != null && boneIndices != null) {
            for (i in 0 until min(min(boneWeights.size, boneIndices.size) shr 2, points.size)) {
                val i4 = i shl 2
                val p = points[i]
                p.b0 = boneIndices[i4]
                p.b1 = boneIndices[i4 + 1]
                p.b2 = boneIndices[i4 + 2]
                p.b3 = boneIndices[i4 + 3]
                p.w0 = boneWeights[i4]
                p.w1 = boneWeights[i4 + 1]
                p.w2 = boneWeights[i4 + 2]
                p.w3 = boneWeights[i4 + 3]
            }
        }

        // remove
        val builder = MeshBuilder(this)
        val pointToIndex = HashMap<UniquePoint, Int>()
        for (i in points.indices) {
            pointToIndex.getOrPut(points[i]) {
                builder.add(this, i)
                pointToIndex.size
            }
        }

        LOGGER.info("Merged {} into {} points", points.size, pointToIndex.size)

        builder.build(this)

        indices = IntArray(points.size) {
            pointToIndex[points[it]]!!
        }
    }
}