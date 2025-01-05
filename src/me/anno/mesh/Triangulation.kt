package me.anno.mesh

import me.anno.maths.geometry.Polygons.getPolygonAreaVector3d
import me.anno.utils.pooling.JomlPools
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.the3deers.util.EarCut
import kotlin.math.ceil

/**
 * converts any ring of points into a list of triangles of points
 * */
object Triangulation {

    @JvmStatic
    fun ringToTrianglesVec2d(points: List<Vector2d>): List<Vector2d> {
        val joint = DoubleArray(points.size * 2)
        for (index in points.indices) {
            val v = points[index]
            joint[index * 2] = v.x
            joint[index * 2 + 1] = v.y
        }
        val indices = EarCut.earcut(joint, 2)
            ?: return emptyList()
        return indices.map { index -> points[index] }
    }

    @JvmStatic
    fun ringToTrianglesVec2f(points: List<Vector2f>): List<Vector2f> {
        return ringToTrianglesMapped2d(points) { v, dst -> dst.set(v) }
    }

    @JvmStatic
    fun <V : Any> ringToTrianglesMapped2d(points: List<V>, mapping: (V, Vector2d) -> Vector2d): List<V> {
        val map = HashMap<Vector2d, V>()
        val pool = JomlPools.vec2d
        val tmp = points.map { src ->
            val mapped = mapping(src, pool.create())
            map[mapped] = src
            mapped
        }
        val result = ringToTrianglesVec2d(tmp).map { map[it]!! }
        pool.sub(points.size)
        return result
    }

    @JvmStatic
    fun findNormalVector(points: List<Vector3d>, dst: Vector3d): Vector3d {
        return getPolygonAreaVector3d(points, dst).safeNormalize(-1.0)
    }

    @JvmStatic
    fun ringToTrianglesVec3d(points: List<Vector3d>): List<Vector3d> {
        if (points.size <= 2) return emptyList()
        val pool3 = JomlPools.vec3d
        // find 2d coordinate system, and project task onto that
        val normal = pool3.create().set(0.0)
        if (findNormalVector(points, normal).lengthSquared() < 0.5) {
            pool3.sub(1)
            return emptyList()
        }
        val xAxis = normal.findSecondAxis(pool3.create())
        val yAxis = normal.cross(xAxis).safeNormalize()
        val result = ringToTrianglesMapped2d(points) { v, dst ->
            dst.set(xAxis.dot(v), yAxis.dot(v))
        }
        pool3.sub(2)
        return result
    }

    @JvmStatic
    fun ringToTrianglesVec3f(points: List<Vector3f>): List<Vector3f> {
        return ringToTrianglesMapped3d(points) { v, dst -> dst.set(v) }
    }

    @JvmStatic
    fun ringToTrianglesPoint(points: List<Point>): List<Point> {
        return ringToTrianglesMapped3d(points) { v, dst -> dst.set(v.position) }
    }

    @JvmStatic
    fun <V> ringToTrianglesMapped3d(points: List<V>, getPoint: (src: V, dst: Vector3d) -> Vector3d): List<V> {
        val loadFactor = 0.75f
        val capacity = ceil(points.size / loadFactor).toInt() // good enough??
        val dstToSrc = HashMap<Vector3d, V>(capacity, loadFactor)
        val points3d = points.map { src ->
            val mapped = getPoint(src, JomlPools.vec3d.create())
            dstToSrc[mapped] = src
            mapped
        }
        val result = ringToTrianglesVec3d(points3d)
            .map { vec3d -> dstToSrc[vec3d]!! }
        JomlPools.vec3d.sub(points.size)
        return result
    }
}