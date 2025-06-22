package me.anno.experiments.convexdecomposition

import com.bulletphysics.linearmath.convexhull.ConvexHull
import com.bulletphysics.linearmath.convexhull.HullDesc
import com.bulletphysics.linearmath.convexhull.HullLibrary
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangleIndex
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.lists.Lists.createList
import me.anno.utils.types.Floats.toIntOr
import org.joml.AABBf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.max
import kotlin.math.min

class ConvexDecomposition(
    val splitsPerAxis: Int,
    val maxRecursiveDepth: Int,
    val maxVerticesPerHull: Int = 12,
    val axes: List<Vector3f> = defaultAxes
) {

    constructor() : this(6, 5)

    companion object {
        private val defaultAxes = listOf(
            Vector3f(1f, 0f, 0f),
            Vector3f(0f, 1f, 0f),
            Vector3f(0f, 0f, 1f),
        )
    }

    private class Triangle(
        val ai: Int, val bi: Int, val ci: Int,
        val centerX3: Vector3f, val bounds: AABBf
    ) {
        var tmpValue = 0f
    }

    private class Group(val triangles: ArrayList<Triangle>, val bounds: AABBf)

    fun splitMesh(mesh: Mesh): List<ConvexHull> {
        val positions = mesh.positions ?: return emptyList()
        val triangles = meshToTriangles(positions, mesh)
        val result = ArrayList<ConvexHull>()
        fun split(triangles: ArrayList<Triangle>, depth: Int) {
            // to do stop if volume ratio isn't good enough??
            if (depth <= 0 || triangles.size < 16) {
                val hull = createHull(positions, triangles)
                if (hull != null) result.add(hull)
            } else {
                val depth1 = depth - 1
                val (left, right) = findBestSplit(triangles, axes)
                split(left, depth1)
                split(right, depth1)
            }
        }
        split(triangles, maxRecursiveDepth)
        return result
    }

    private fun createHull(positions: FloatArray, triangles: List<Triangle>): ConvexHull? {
        val points = ArrayList<Vector3d>(triangles.size * 3)
        for (i in triangles.indices) {
            val tri = triangles[i]
            points.add(Vector3d(positions, tri.ai * 3))
            points.add(Vector3d(positions, tri.bi * 3))
            points.add(Vector3d(positions, tri.ci * 3))
        }
        return HullLibrary.createConvexHull(HullDesc(points, maxVerticesPerHull))
    }

    private fun meshToTriangles(positions: FloatArray, mesh: Mesh): ArrayList<Triangle> {
        val triangles = ArrayList<Triangle>()
        val a = Vector3f()
        val b = Vector3f()
        val c = Vector3f()
        mesh.forEachTriangleIndex { ai, bi, ci ->
            a.set(positions, ai * 3)
            b.set(positions, bi * 3)
            c.set(positions, ci * 3)
            val sum = Vector3f()
            a.add(b, sum).add(c)
            val bounds = AABBf(a, a).union(b).union(c)
            triangles.add(Triangle(ai, bi, ci, sum, bounds))
            false
        }
        return triangles
    }

    private fun split(triangles: ArrayList<Triangle>, axis: Vector3f): List<Group> {
        var minValue = Float.MAX_VALUE
        var maxValue = Float.MIN_VALUE
        for (i in triangles.indices) {
            val tri = triangles[i]
            val value = axis.dot(tri.centerX3)
            tri.tmpValue = value
            minValue = min(minValue, value)
            maxValue = max(maxValue, value)
        }
        if (minValue == maxValue) return emptyList()
        val result = createList(splitsPerAxis) {
            ArrayList<Triangle>()
        }
        val scale = (splitsPerAxis * (1f - 1e-6f)) / (maxValue - minValue)
        for (i in triangles.indices) {
            val tri = triangles[i]
            val index = ((tri.tmpValue - minValue) * scale).toIntOr()
            result[index].add(tri)
        }
        return result.mapNotNull { triangles -> createGroup(triangles) }
    }

    private fun createGroup(triangles: ArrayList<Triangle>): Group? {
        if (triangles.isEmpty()) return null
        val bounds = AABBf()
        for (i in triangles.indices) {
            bounds.union(triangles[i].bounds)
        }
        return Group(triangles, bounds)
    }

    private fun findBestSplit(triangles: ArrayList<Triangle>, axes: List<Vector3f>):
            Pair<ArrayList<Triangle>, ArrayList<Triangle>> {

        var bestScore = Float.MAX_VALUE
        var bestSplitIndex = -1
        var bestGroups: List<Group> = emptyList()

        for (axisIndex in axes.indices) {
            val axis = axes[axisIndex]
            val groups = split(triangles, axis)
            val numSplitsI = groups.size
            for (splitIndex in 1 until numSplitsI - 1) {
                // todo left side continuously grows -> we can simplify the left volume calculation (2x performance)
                val score = findVolume(groups, 0, splitIndex) +
                        findVolume(groups, splitIndex, numSplitsI)
                if (score < bestScore) {
                    bestScore = score
                    bestGroups = groups
                    bestSplitIndex = splitIndex
                }
            }
        }

        val left = join(bestGroups, 0, bestSplitIndex)
        val right = join(bestGroups, bestSplitIndex, bestGroups.size)
        return left to right
    }

    private fun findVolume(groups: List<Group>, i0: Int, i1: Int): Float {
        // we could also calculate the volume using convex hulls, and that would be better, but also slower
        val tmp = JomlPools.aabbf.borrow()
        tmp.set(groups[i0].bounds)
        for (i in i0 + 1 until i1) {
            tmp.union(groups[i].bounds)
        }
        return tmp.volume
    }

    private fun join(groups: List<Group>, i0: Int, i1: Int): ArrayList<Triangle> {
        var sum = 0
        for (i in i0 until i1) {
            sum += groups[i].triangles.size
        }
        val result = ArrayList<Triangle>(sum)
        for (i in i0 until i1) {
            result.addAll(groups[i].triangles)
        }
        return result
    }
}