package me.anno.mesh

import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * Helps to create a triangulated Mesh with holes.
 * V must be one of: Vector2f, Vector3f, Vector2d, Vector3d, Point.
 * */
class TriangulationBuilder<V : Vector>(numVertexCapacity: Int = 16) {

    /**
     * All outer vertices and ring vertices packed into one list.
     * Outer vertices first. Then rings.
     * */
    val vertices = ArrayList<V>(numVertexCapacity)

    /**
     * All outer vertices and ring vertices are packed into "vertices".
     * Outer vertices must appear first. Then rings follow.
     * Rings are separated by this list: each ring starting, adds the index of its first vertex inside "vertices".
     * */
    val holesStartIndices = IntArrayList()

    fun addVertex(v: V) {
        assertTrue(holesStartIndices.isEmpty(), "Ring must be defined before holes")
        vertices.add(v)
    }

    fun addVertices(vs: List<V>) {
        assertTrue(holesStartIndices.isEmpty(), "Ring must be defined before holes")
        vertices.addAll(vs)
    }

    fun addHole(vs: List<V>) {
        beginHole()
        vertices.addAll(vs)
    }

    fun beginHole() {
        holesStartIndices.add(vertices.size)
    }

    fun addHoleVertex(v: V) {
        assertTrue(holesStartIndices.isNotEmpty(), "beginHole must be called before adding hole vertices")
        vertices.add(v)
    }

    fun triangulate(): List<V> {
        val indices = triangulateToIndices() ?: return emptyList()
        return indices.map(vertices)
    }

    fun triangulateToIndices(): IntArrayList? {
        val vs = vertices
        val sample = vs.firstOrNull() ?: return null
        val holes = if (holesStartIndices.isNotEmpty()) holesStartIndices.toIntArray() else null

        @Suppress("UNCHECKED_CAST")
        return when (sample) {
            is Vector2f -> Triangulation.ringToTrianglesVec2fIndices(vs as List<Vector2f>, holes)
            is Vector3f -> Triangulation.ringToTrianglesVec3fIndices(vs as List<Vector3f>, holes)
            is Vector2d -> Triangulation.ringToTrianglesVec2dIndices(vs as List<Vector2d>, holes)
            is Vector3d -> Triangulation.ringToTrianglesVec3dIndices(vs as List<Vector3d>, holes)
            else -> throw NotImplementedError("${sample.javaClass} isn't supported by TriangulationBuilder")
        }
    }

    fun clear() {
        vertices.clear()
        holesStartIndices.clear()
    }
}