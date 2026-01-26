package me.anno.ecs.components.mesh.shapes

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.shapes.MaxAreaCircle.createCircleIndices
import me.anno.maths.Maths.TAUf
import me.anno.utils.assertions.assertTrue
import me.anno.utils.types.Arrays.resize
import kotlin.math.cos
import kotlin.math.sin

/**
 * Generates circle mesh.
 * */
object CircleModel {

    fun createCircle(numVertices: Int, mesh: Mesh, radius: Float = 1f): Mesh {

        assertTrue(numVertices >= 3)

        val triangleCount = numVertices - 2
        val indexCount = triangleCount * 3

        val positions = mesh.positions.resize(3 * numVertices)
        val normals = mesh.normals.resize(3 * numVertices)
        val uvs = mesh.uvs.resize(2 * numVertices)

        mesh.positions = positions
        mesh.normals = normals
        mesh.uvs = uvs

        // generate vertex data
        normals.fill(0f)
        for (i in 0 until numVertices) {
            val angle = i * TAUf / numVertices
            val cosI = cos(angle)
            val sinI = sin(angle)
            positions[i * 3] = cosI * radius
            positions[i * 3 + 1] = 0f
            positions[i * 3 + 2] = sinI * radius
            uvs[i * 2] = cosI * 0.5f + 0.5f
            uvs[i * 2 + 1] = sinI * 0.5f + 0.5f
            normals[i * 3 + 1] = 1f
        }

        val indices = mesh.indices.resize(indexCount)
        mesh.indices = indices

        // add ring
        // 0 1 2 0 2 3 0 3 4
        createCircleIndices(numVertices, 0, indices, 0, false)
        return mesh
    }
}