package org.recast4j.detour.extras.jumplink

import org.recast4j.Vectors
import org.recast4j.recast.PolyMesh
import org.recast4j.recast.RecastConstants

object EdgeExtractor {
    fun extractEdges(mesh: PolyMesh?): List<Edge> {
        mesh ?: return emptyList()
        val edges = ArrayList<Edge>()
        val orig = mesh.bmin
        val cs = mesh.cellSize
        val ch = mesh.cellHeight
        for (i in 0 until mesh.numPolygons) {
            val nvp = mesh.maxVerticesPerPolygon
            val p = i * 2 * nvp
            for (j in 0 until nvp) {
                if (mesh.polygons[p + j] == RecastConstants.RC_MESH_NULL_IDX) {
                    break
                }
                // Skip connected edges.
                if (mesh.polygons[p + nvp + j] and 0x8000 != 0) {
                    val dir = mesh.polygons[p + nvp + j] and 0xf
                    if (dir == 0xf) { // Border
                        if (mesh.polygons[p + nvp + j] != RecastConstants.RC_MESH_NULL_IDX) {
                            continue
                        }
                        var nj = j + 1
                        if (nj >= nvp || mesh.polygons[p + nj] == RecastConstants.RC_MESH_NULL_IDX) {
                            nj = 0
                        }
                        val e = Edge()
                        Vectors.copy(e.a, mesh.vertices, mesh.polygons[p + nj] * 3)
                        e.a.mul(cs, ch, cs).add(orig)
                        Vectors.copy(e.b, mesh.vertices, mesh.polygons[p + j] * 3)
                        e.b.mul(cs, ch, cs).add(orig)
                        edges.add(e)
                    }
                }
            }
        }
        return edges
    }
}