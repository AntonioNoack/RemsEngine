package me.anno.recast

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.ui.LineShapes.drawLine
import me.anno.utils.Color.black
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.FloatArrayListUtils.addUnsafe
import me.anno.utils.types.Arrays.resize
import org.joml.Vector3f
import org.recast4j.detour.MeshData
import org.recast4j.detour.Poly
import org.recast4j.detour.PolyDetail

object NavMeshDebug {

    var debugColor = 0xfff973 or black

    private fun decodeIndex(index: Byte): Int {
        return index.toInt().and(0xff)
    }

    private fun countTriangles(data: MeshData, dMeshes: Array<PolyDetail>): Int {
        var numTriangles = 0
        for (i in 0 until data.polyCount) {
            val p = data.polygons[i]
            if (p.type == Poly.DT_POLYTYPE_OFFMESH_CONNECTION) continue
            numTriangles += dMeshes[i].triCount
        }
        return numTriangles
    }

    /**
     * create a mesh from the nav mesh data
     * */
    fun toMesh(data: MeshData?, mesh: Mesh = Mesh()): Mesh? {
        data ?: return null
        val dMeshes = data.detailMeshes ?: return null
        val triCount = countTriangles(data, dMeshes)
        val positions = FloatArrayList(triCount * 3)
        forEachTriangle(data) { a, b, c ->
            positions.addUnsafe(a)
            positions.addUnsafe(b)
            positions.addUnsafe(c)
        }
        mesh.positions = positions.toFloatArray()
        mesh.normals = mesh.normals.resize(positions.size)
        return mesh
    }

    fun drawNavMesh(entity: Entity?, data: MeshData?) {
        val color = debugColor
        forEachTriangle(data) { a, b, c ->
            drawLine(entity, a, b, color)
            drawLine(entity, b, c, color)
            drawLine(entity, c, a, color)
        }
    }

    fun forEachTriangle(data: MeshData?, callback: (Vector3f, Vector3f, Vector3f) -> Unit) {
        val dMeshes = data?.detailMeshes ?: return
        val pVertices = data.vertices
        val dVertices = data.detailVertices
        val a = JomlPools.vec3f.create()
        val b = JomlPools.vec3f.create()
        val c = JomlPools.vec3f.create()
        for (polyIndex in 0 until data.polyCount) {
            val poly = data.polygons[polyIndex]
            if (poly.type == Poly.DT_POLYTYPE_OFFMESH_CONNECTION) continue
            val polyIdx = poly.vertices
            val detailMesh = dMeshes[polyIndex]
            val dIdx = data.detailTriangles
            val pVertexCount = poly.vertCount
            val dOffset = detailMesh.vertBase
            for (triangleIndex in 0 until detailMesh.triCount) {
                val di = (detailMesh.triBase + triangleIndex) * 4
                setVertex(pVertexCount, pVertices, polyIdx, dIdx[di], dOffset, dVertices, a)
                setVertex(pVertexCount, pVertices, polyIdx, dIdx[di + 1], dOffset, dVertices, b)
                setVertex(pVertexCount, pVertices, polyIdx, dIdx[di + 2], dOffset, dVertices, c)
                callback(a, b, c)
            }
        }
        JomlPools.vec3f.sub(3)
    }

    private fun setVertex(
        polyVertCount: Int, polyVertices: FloatArray, polyIndices: IntArray,
        encodedIndex: Byte, detailMeshVertBase: Int, detailVertices: FloatArray, dst: Vector3f
    ) {
        val decodedIndex = decodeIndex(encodedIndex)
        if (decodedIndex < polyVertCount) dst.set(polyVertices, polyIndices[decodedIndex] * 3)
        else dst.set(detailVertices, (detailMeshVertBase + decodedIndex - polyVertCount) * 3)
    }
}