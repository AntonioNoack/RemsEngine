package me.anno.mesh

import me.anno.maths.geometry.Polygons.getPolygonAreaVector3d
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.the3deers.util.EarCut

/**
 * converts any ring of points into a list of triangles of points
 * */
object Triangulation {

    @JvmStatic
    fun findNormalVector(points: List<Vector3d>, dst: Vector3d): Vector3d {
        return getPolygonAreaVector3d(points, dst).safeNormalize(-1.0)
    }

    @JvmStatic
    fun ringToTrianglesVec2d(points: List<Vector2d>, holeStartIndices: IntArray? = null): List<Vector2d> {
        val indices = ringToTrianglesVec2dIndices(points, holeStartIndices) ?: return emptyList()
        return indices.map(points)
    }

    @JvmStatic
    fun ringToTrianglesVec2f(points: List<Vector2f>, holeStartIndices: IntArray? = null): List<Vector2f> {
        val indices = ringToTrianglesVec2fIndices(points, holeStartIndices) ?: return emptyList()
        return indices.map(points)
    }

    @JvmStatic
    fun ringToTrianglesVec3d(points: List<Vector3d>, holeStartIndices: IntArray? = null): List<Vector3d> {
        val indices = ringToTrianglesVec3dIndices(points, holeStartIndices) ?: return emptyList()
        return indices.map(points)
    }

    @JvmStatic
    fun ringToTrianglesVec3f(points: List<Vector3f>, holeStartIndices: IntArray? = null): List<Vector3f> {
        val indices = ringToTrianglesVec3fIndices(points, holeStartIndices) ?: return emptyList()
        return indices.map(points)
    }

    @JvmStatic
    fun ringToTrianglesPoint(points: List<Point>, holeStartIndices: IntArray? = null): List<Point> {
        val indices = ringToTrianglesPointIndices(points, holeStartIndices) ?: return emptyList()
        return indices.map(points)
    }

    @JvmStatic
    fun ringToTrianglesVec2dIndices(points: List<Vector2d>, holeStartIndices: IntArray? = null): IntArrayList? {
        val joint = DoubleArray(points.size * 2)
        for (index in points.indices) {
            val v = points[index]
            joint[index * 2] = v.x
            joint[index * 2 + 1] = v.y
        }
        return EarCut.earcut(joint, holeStartIndices, 2)
    }

    @JvmStatic
    fun ringToTrianglesVec2fIndices(points: List<Vector2f>, holeStartIndices: IntArray? = null): IntArrayList? {
        return ringToTrianglesMapped2dIndices(points, holeStartIndices) { v, dst -> dst.set(v) }
    }

    @JvmStatic
    fun <V : Any> ringToTrianglesMapped2dIndices(
        points: List<V>, holeStartIndices: IntArray? = null,
        mapping: (V, Vector2d) -> Vector2d
    ): IntArrayList? {
        val pool = JomlPools.vec2d
        val tmp = points.map { src -> mapping(src, pool.create()) }
        val result = ringToTrianglesVec2dIndices(tmp, holeStartIndices)
        pool.sub(points.size)
        return result
    }

    @JvmStatic
    fun ringToTrianglesVec3fIndices(points: List<Vector3f>, holeStartIndices: IntArray? = null): IntArrayList? {
        return ringToTrianglesMapped3dIndices(points, holeStartIndices) { v, dst -> dst.set(v) }
    }

    @JvmStatic
    fun ringToTrianglesPointIndices(points: List<Point>, holeStartIndices: IntArray? = null): IntArrayList? {
        return ringToTrianglesMapped3dIndices(points, holeStartIndices) { v, dst -> dst.set(v.position) }
    }

    @JvmStatic
    fun <V> ringToTrianglesMapped3dIndices(
        points: List<V>, holeStartIndices: IntArray?,
        getPoint: (src: V, dst: Vector3d) -> Vector3d
    ): IntArrayList? {
        val pool = JomlPools.vec3d
        val points3d = points.map { src -> getPoint(src, pool.create()) }
        val result = ringToTrianglesVec3dIndices(points3d, holeStartIndices)
        pool.sub(points.size)
        return result
    }

    @JvmStatic
    fun ringToTrianglesVec3dIndices(points: List<Vector3d>, holeStartIndices: IntArray? = null): IntArrayList? {
        if (points.size <= 2) return null
        val pool = JomlPools.vec3d
        // find 2d coordinate system, and project task onto that
        val normal = pool.create().set(0.0)
        if (findNormalVector(points, normal).lengthSquared() < 0.5) {
            pool.sub(1)
            return null
        }
        val xAxis = normal.findSecondAxis(pool.create())
        val yAxis = normal.cross(xAxis).safeNormalize()
        val result = ringToTrianglesMapped2dIndices(points, holeStartIndices) { v, dst ->
            dst.set(xAxis.dot(v), yAxis.dot(v))
        }
        pool.sub(2)
        return result
    }
}