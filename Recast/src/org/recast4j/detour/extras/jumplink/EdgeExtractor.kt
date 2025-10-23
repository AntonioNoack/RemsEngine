package org.recast4j.detour.extras.jumplink

import org.joml.Vector3f
import org.recast4j.recast.PolyMesh
import org.recast4j.recast.RecastConstants

object EdgeExtractor {
    fun extractEdges(mesh: PolyMesh?): List<Edge> {
        mesh ?: return emptyList()
        val edges = ArrayList<Edge>()
        val orig = mesh.bounds.getMin(Vector3f())
        val cs = mesh.cellSize
        val ch = mesh.cellHeight
        val polygons = mesh.polygons
        val vertices = mesh.vertices
        for (i in 0 until mesh.numPolygons) {
            val nvp = mesh.maxVerticesPerPolygon
            val p = i * 2 * nvp
            for (j in 0 until nvp) {
                if (polygons[p + j] == RecastConstants.RC_MESH_NULL_IDX) {
                    break
                }
                // Skip connected edges.
                if (polygons[p + nvp + j] and 0x8000 != 0) {
                    val dir = polygons[p + nvp + j] and 0xf
                    if (dir == 0xf) { // Border
                        if (polygons[p + nvp + j] != RecastConstants.RC_MESH_NULL_IDX) {
                            continue
                        }
                        var nj = j + 1
                        if (nj >= nvp || polygons[p + nj] == RecastConstants.RC_MESH_NULL_IDX) {
                            nj = 0
                        }
                        val e = Edge()
                        val ai = polygons[p + nj] * 3
                        e.a.set(
                            vertices[ai + 0] * cs + orig.x,
                            vertices[ai + 1] * ch + orig.y,
                            vertices[ai + 2] * cs + orig.z
                        )
                        val bi = polygons[p + j] * 3
                        e.b.set(
                            vertices[bi + 0] * cs + orig.x,
                            vertices[bi + 1] * ch + orig.y,
                            vertices[bi + 2] * cs + orig.z
                        )
                        edges.add(e)
                    }
                }
            }
        }
        return edges
    }
}