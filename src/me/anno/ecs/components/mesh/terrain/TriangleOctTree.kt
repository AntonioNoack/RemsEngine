package me.anno.ecs.components.mesh.terrain

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.graph.octtree.OctTree
import me.anno.graph.octtree.SplitResult
import me.anno.utils.types.AABBs.avgX
import me.anno.utils.types.AABBs.avgY
import me.anno.utils.types.AABBs.avgZ
import me.anno.utils.types.AABBs.clear
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector3d
import org.joml.Vector3f

// float coordinates, because other use-cases are probably really rare
// double coordinates, because everything else is double, and #insanity lol
// implement both?
class TriangleOctTree(
    val terrain: TriTerrain,
    parent: TriangleOctTree?,
    min: Vector3f, max: Vector3f,
    val maxTriangles: Int
) : OctTree<Vector3f>(parent, min, max) {

    constructor(terrain: TriTerrain, min: Vector3f, max: Vector3f, maxTriangles: Int) :
            this(terrain, null, min, max, maxTriangles)

    var indices = IntArray(maxTriangles * 3)
    var isOnEdge = BooleanArray(maxTriangles * 3) // A,B,C

    var numTriangles = 0

    var mesh: Entity? = null
    var meshComponent: MeshComponent? = null

    fun createMesh(component: MeshComponent, mesh: Mesh) {
        val srcPos = terrain.positions
        val srcIdx = indices
        var dstPos = mesh.positions
        mesh.indices = null // not required: if we would use indices, the position data would be huge
        if (dstPos == null || dstPos.size != numTriangles * 9) {
            dstPos = FloatArray(numTriangles * 9)
        }
        // define aabb
        // center mesh by that for clickability
        val aabb = mesh.aabb
        aabb.clear()
        for (i in 0 until numTriangles) {
            val a = srcIdx[i * 3] * 3
            val b = srcIdx[i * 3 + 1] * 3
            val c = srcIdx[i * 3 + 2] * 3
            aabb.union(srcPos[a], srcPos[a + 1], srcPos[a + 2])
            aabb.union(srcPos[b], srcPos[b + 1], srcPos[b + 2])
            aabb.union(srcPos[c], srcPos[c + 1], srcPos[c + 2])
        }
        val avg = Vector3f(aabb.avgX(), aabb.avgY(), aabb.avgZ())
        val entity = component.entity!!
        entity.transform.localPosition = Vector3d(avg)
        for (i in 0 until numTriangles * 3) {
            val s3 = srcIdx[i] * 3
            val d3 = i * 3
            dstPos[d3 + 0] = srcPos[s3 + 0] - avg.x
            dstPos[d3 + 1] = srcPos[s3 + 1] - avg.y
            dstPos[d3 + 2] = srcPos[s3 + 2] - avg.z
        }
    }

    override fun getIndex(point: Vector3f): Int {
        val split = splitPoint!!
        val dx = point.x >= split.x
        val dy = point.y >= split.y
        val dz = point.z >= split.z
        return dx.toInt(4) + dy.toInt(2) + dz.toInt(1)
    }

    override fun trySplit(): SplitResult<Vector3f>? {
        // we have enough space -> it's fine :)
        return null
    }

    fun calculateAverage(dst: Vector3f = Vector3f()): Vector3f {
        val avg = dst.set(0f)
        val srcPos = terrain.positions
        val srcIdx = indices
        for (i in 0 until numTriangles * 3) {
            val s3 = srcIdx[i] * 3
            avg.add(srcPos[s3], srcPos[s3 + 1], srcPos[s3 + 2])
        }
        avg.mul(1f / (numTriangles * 3))
        return avg
    }

    override fun split(other: Any): SplitResult<Vector3f> {
        // find the ideal split point: in theory the median, but we will just choose the average as an approximation
        // todo if min/max is infinity, define it
        val avg = calculateAverage()
        TODO("Not yet implemented")
    }

    override fun tryJoin(node: Any): Boolean {
        node as TriangleOctTree
        if (numTriangles + node.numTriangles <= maxTriangles) {

            val tri = indices
            val ioe = isOnEdge

            val tri2 = node.indices
            val ieo2 = node.isOnEdge

            val baseIndex = numTriangles * 3
            for (i in 0 until node.numTriangles * 3) {
                val j = baseIndex + i
                tri[j] = tri2[i]
                ioe[j] = ieo2[i]
            }

            numTriangles += node.numTriangles

            return true
        }
        return false
    }

    override fun setContent(newContent: Any) {
        newContent as TriangleOctTree
        indices = newContent.indices
        isOnEdge = newContent.isOnEdge
        numTriangles = newContent.numTriangles
    }

}