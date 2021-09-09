package me.anno.ecs.components.mesh.terrain

import me.anno.ecs.Component
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.OnEdgeCalculator
import me.anno.ecs.interfaces.CustomEditMode
import me.anno.utils.structures.arrays.ExpandingFloatArray
import org.joml.Matrix3d
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.max

/**
 * a terrain class, that hopefully will be efficient, and which should handle meshes of giant size
 * these meshes then should be editable, and we want to use brushes on them
 * todo expand, when we want to edit a point at the edge
 * todo brushes: thicken, thinnen, make higher, make lower
 * todo quad tree as acceleration structure
 * */
class TriTerrain : Component(), CustomEditMode {

    // could be local, but that would mean a lot more complex calculations
    val positions = ExpandingFloatArray(512)
    // todo support smooth normals on request

    val maxTriangles = 512

    // todo whenever the data is changed, we need to update the meshes
    // todo what about triangles, which cross multiple sections? should not happen...
    val data = TriangleOctTree(this, Vector3f(Float.NEGATIVE_INFINITY), Vector3f(Float.POSITIVE_INFINITY), maxTriangles)

    fun applyBrush(position: Vector3d, lookDir: Matrix3d, brush: TerrainBrush) {
        // todo apply this brush
    }

    // for debugging/initialization purposes
    fun addMesh(mesh: Mesh) {

        val chunk = TriangleOctTree(this, Vector3f(), Vector3f(), max(maxTriangles, mesh.numTriangles))
        val dstIdx = chunk.indices

        val dstPos = positions
        val positions = mesh.positions!!
        val indices = mesh.indices
        val index0 = dstPos.size / 3
        for (element in positions) {
            dstPos.add(element)
        }

        if (indices == null) { // 0 1 2 3 ...
            for (i in 0 until positions.size / 3) {
                dstIdx[i] = index0 + i
            }
            chunk.numTriangles = positions.size / 9
        } else {
            for (i in indices.indices) {
                dstIdx[i] = index0 + indices[i]
            }
            chunk.numTriangles = indices.size / 3
        }

        OnEdgeCalculator.calculateIsOnEdge(mesh, chunk.isOnEdge)

        data.add(chunk)

    }

    override fun clone(): Component {
        TODO("Not yet implemented")
    }

}