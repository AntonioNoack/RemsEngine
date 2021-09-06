package me.anno.fonts.mesh

import me.anno.mesh.Point
import me.anno.objects.Transform.Companion.xAxis
import me.anno.objects.Transform.Companion.yAxis
import me.anno.utils.types.Vectors.minus
import org.joml.*
import org.the3deers.util.EarCut
import kotlin.math.abs

object Triangulation {

    fun ringToTriangles2f(points: FloatArray): FloatArray {
        val indices = EarCut.earcut(points, intArrayOf(), 2)
        val result = FloatArray(indices.size * 2)
        for (i in indices.indices) {
            val index = indices[i] * 2
            result[i * 2] = points[index]
            result[i * 2 + 1] = points[index + 1]
        }
        return result
    }

    fun ringToTrianglesVec2f(points: List<Vector2f>): List<Vector2f> {
        val joint = FloatArray(points.size * 2)
        points.forEachIndexed { index, vector2d -> joint[index * 2] = vector2d.x; joint[index * 2 + 1] = vector2d.y }
        val indices = EarCut.earcut(joint, intArrayOf(), 2)
        return indices.map { index -> points[index] }
    }

    fun ringToTrianglesVec2d(points: List<Vector2dc>): List<Vector2dc> {
        val joint = FloatArray(points.size * 2)
        points.forEachIndexed { index, vector2d ->
            joint[index * 2] = vector2d.x().toFloat(); joint[index * 2 + 1] = vector2d.y().toFloat()
        }
        val indices = EarCut.earcut(joint, intArrayOf(), 2)
        return indices.map { index -> points[index] }
    }

    fun ringToTrianglesVec3d(points: List<Vector3dc>): List<Vector3dc> {
        if (points.size > 2) {
            val normal = Vector3d()
            for (i in points.indices) {
                val a = points[i]
                val b = points[(i + 1) % points.size]
                val c = points[(i + 2) % points.size]
                normal.add((a - b).cross(b - c))
            }
            normal.normalize()
            if (normal.length() < 0.5f) return emptyList()
            // find 2d coordinate system
            val xAxis = findSecondAxis(normal)
            val yAxis = normal.cross(xAxis)
            val projected = points.map {
                Vector2d(it.dot(xAxis), it.dot(yAxis))
            }
            val reverseMap = HashMap<Vector2dc, Vector3dc>()
            points.forEachIndexed { index, vector3d ->
                reverseMap[projected[index]] = vector3d
            }
            val triangles2d = ringToTrianglesVec2d(projected)
            return triangles2d.map { reverseMap[it]!! }
        } else return emptyList()
    }

    fun ringToTrianglesPoint(points: Array<Point>): List<Point> {
        if (points.size > 2) {
            val normal = Vector3f()
            for (i in points.indices) {
                val a = points[i].position
                val b = points[(i + 1) % points.size].position
                val c = points[(i + 2) % points.size].position
                normal.add((a - b).cross(b - c))
            }
            normal.normalize()
            if (normal.length() < 0.5f) return emptyList()
            // find 2d coordinate system
            val xAxis = findSecondAxis(normal)
            val yAxis = normal.cross(xAxis)
            val projected = points.map {
                Vector2f(it.position.dot(xAxis), it.position.dot(yAxis))
            }
            val reverseMap = HashMap<Vector2fc, Point>()
            points.forEachIndexed { index, vector3d ->
                reverseMap[projected[index]] = vector3d
            }
            val triangles2f = ringToTrianglesVec2f(projected)
            return triangles2f.map { reverseMap[it]!! }
        } else return emptyList()
    }

    fun ringToTrianglesPoint(points: List<Point>): List<Point> {
        if (points.size > 2) {
            val normal = Vector3f()
            for (i in points.indices) {
                val a = points[i].position
                val b = points[(i + 1) % points.size].position
                val c = points[(i + 2) % points.size].position
                normal.add((a - b).cross(b - c))
            }
            normal.normalize()
            if (normal.length() < 0.5f) return emptyList()
            // find 2d coordinate system
            val xAxis = findSecondAxis(normal)
            val yAxis = normal.cross(xAxis)
            val projected = points.map {
                Vector2f(it.position.dot(xAxis), it.position.dot(yAxis))
            }
            val reverseMap = HashMap<Vector2fc, Point>()
            points.forEachIndexed { index, vector3d ->
                reverseMap[projected[index]] = vector3d
            }
            val triangles2f = ringToTrianglesVec2f(projected)
            return triangles2f.map { reverseMap[it]!! }
        } else return emptyList()
    }

    private fun findSecondAxis(axis: Vector3f): Vector3f {
        val ax = abs(axis.x)
        val ay = abs(axis.y)
        val try0 = if (ax > ay) yAxis else xAxis
        val rect = axis.cross(try0, Vector3f())
        return rect.normalize()
    }

    private fun findSecondAxis(axis: Vector3d): Vector3d {
        val ax = abs(axis.x)
        val ay = abs(axis.y)
        val try0 = if (ax > ay) Vector3d(0.0, 1.0, 0.0) else Vector3d(1.0, 0.0, 0.0)
        val rect = axis.cross(try0, Vector3d())
        return rect.normalize()
    }

}