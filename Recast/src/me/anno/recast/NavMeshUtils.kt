package me.anno.recast

import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugShapes
import me.anno.maths.Maths.posMod
import me.anno.utils.pooling.JomlPools
import org.joml.Vector3d
import org.recast4j.LongArrayList
import org.recast4j.detour.MeshData
import org.recast4j.detour.NavMesh
import org.recast4j.detour.Poly
import org.recast4j.detour.crowd.PathQueryResult

object NavMeshUtils {
    fun drawPath(navMesh: NavMesh, meshData: MeshData, pathQueryResult: PathQueryResult?, color: Int) {
        val pathNodes = pathQueryResult?.path ?: return
        drawPath(navMesh, meshData, pathNodes, color)
    }

    fun drawPath(navMesh: NavMesh, meshData: MeshData, pathNodes: LongArrayList, color: Int) {
        for (i in 0 until pathNodes.size) {
            val ref = pathNodes[i]
            val tile = navMesh.getTileByRef(ref)!!
            val poly = navMesh.getPolyByRef(ref, tile)
            drawPoly(meshData, poly ?: continue, color)
        }
        val centers = (0 until pathNodes.size).mapNotNull {
            val ref = pathNodes[it]
            val tile = navMesh.getTileByRef(ref)!!
            val poly = navMesh.getPolyByRef(ref, tile)
            if (poly != null) getPolyCenter(meshData, poly, Vector3d())
            else null
        }
        for (i in 1 until centers.size) {
            val from = centers[i - 1]
            val to = centers[i]
            DebugShapes.debugArrows.add(DebugLine(from, to, color, 0f))
        }
    }

    fun drawPoly(meshData: MeshData, poly: Poly, color: Int) {
        val pos = meshData.vertices
        val vertices = poly.vertices.map {
            Vector3d(pos, it * 3)
        }
        for (vi in vertices.indices) {
            val va = vertices[vi]
            val vb = vertices[posMod(vi + 1, vertices.size)]
            DebugShapes.debugLines.add(DebugLine(va, vb, color, 0f))
        }
    }

    fun getPolyCenter(meshData: MeshData, poly: Poly, dst: Vector3d): Vector3d {
        val meshVertices = meshData.vertices
        val polyVertices = poly.vertices
        val tmp = JomlPools.vec3f.borrow()
        dst.set(0.0)
        for (vi in polyVertices.indices) {
            val va = polyVertices[vi]
            tmp.set(meshVertices, va * 3)
            dst.add(tmp)
        }
        return dst.div(polyVertices.size.toDouble())
    }
}