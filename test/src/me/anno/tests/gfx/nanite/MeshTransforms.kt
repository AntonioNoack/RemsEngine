package me.anno.tests.gfx.nanite

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangleIndex
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.FloatArrayListUtils.add

fun Mesh.createUniqueIndices() {
    val vertices = HashMap<Vertex, Int>()
    val newIndices = IntArray(numPrimitives.toInt() * 3)
    var ii = 0
    val pos = positions!!
    val nor = normals!!
    val uv = uvs!!
    val newPos = FloatArrayList(pos.size)
    val newNor = FloatArrayList(nor.size)
    val newUVs = FloatArrayList(uv.size)
    fun processVertex(i: Int) {
        val x = pos[i * 3]
        val y = pos[i * 3 + 1]
        val z = pos[i * 3 + 2]
        val nx = nor[i * 3]
        val ny = nor[i * 3 + 1]
        val nz = nor[i * 3 + 2]
        val u = uv[i * 2]
        val v = uv[i * 2 + 1]
        newIndices[ii++] = vertices.getOrPut(Vertex(x, y, z, nx, ny, nz, u, v)) {
            newPos.add(x, y, z)
            newNor.add(nx, ny, nz)
            newUVs.add(u, v)
            vertices.size
        }
    }
    forEachTriangleIndex { ai, bi, ci ->
        processVertex(ai)
        processVertex(bi)
        processVertex(ci)
        false
    }
    positions = newPos.toFloatArray()
    normals = newNor.toFloatArray()
    uvs = newUVs.toFloatArray()
    indices = newIndices
    invalidateGeometry()
}
