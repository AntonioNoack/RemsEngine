package me.anno.fonts.mesh

import me.anno.mesh.Point
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Vectors.findSecondAxis
import me.anno.utils.types.Vectors.minus
import org.joml.*
import org.the3deers.util.EarCut

object Triangulation {

    fun ringToTriangles2f(points: FloatArray): FloatArray {
        val indices = EarCut.earcut(points, 2) ?: return FloatArray(0)
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
        points.forEachIndexed { index, vector2d ->
            joint[index * 2] = vector2d.x
            joint[index * 2 + 1] = vector2d.y
        }
        val indices = EarCut.earcut(joint, 2) ?: return emptyList()
        return indices.map { index -> points[index] }
    }

    fun ringToTrianglesVec2d(points: List<Vector2dc>): List<Vector2dc> {
        val joint = FloatArray(points.size * 2)
        points.forEachIndexed { index, vector2d ->
            joint[index * 2] = vector2d.x().toFloat()
            joint[index * 2 + 1] = vector2d.y().toFloat()
        }
        val indices = EarCut.earcut(joint, 2) ?: return emptyList()
        return indices.map { index -> points[index] }
    }

    fun ringToTrianglesVec3f(points: List<Vector3fc>): List<Vector3fc> {
        if (points.size > 2) {
            val normal = JomlPools.vec3f.create().set(0f)
            for (i in points.indices) {
                val a = points[i]
                val b = points[(i + 1) % points.size]
                val c = points[(i + 2) % points.size]
                normal.add((a - b).cross(b - c))
            }
            normal.normalize()
            if (normal.length() < 0.5f) {
                JomlPools.vec3f.sub(1)
                return emptyList()
            }
            // find 2d coordinate system
            val xAxis = normal.findSecondAxis(JomlPools.vec3f.create())
            val yAxis = normal.cross(xAxis)
            val projected = points.map {
                JomlPools.vec2f.create()
                    .set(it.dot(xAxis), it.dot(yAxis))
            }
            val reverseMap = HashMap<Vector2fc, Vector3fc>()
            points.forEachIndexed { index, vector3f ->
                reverseMap[projected[index]] = vector3f
            }
            val triangles2d = ringToTrianglesVec2f(projected)
            val result = triangles2d.map { reverseMap[it]!! }
            JomlPools.vec2f.sub(projected.size)
            JomlPools.vec3f.sub(2)
            return result
        } else return emptyList()
    }

    fun ringToTrianglesVec3d(points: List<Vector3dc>): List<Vector3dc> {
        if (points.size > 2) {
            val normal = JomlPools.vec3d.create().set(0.0)
            for (i in points.indices) {
                val a = points[i]
                val b = points[(i + 1) % points.size]
                val c = points[(i + 2) % points.size]
                normal.add((a - b).cross(b - c))
            }
            normal.normalize()
            if (normal.length() < 0.5) {
                JomlPools.vec3d.sub(1)
                return emptyList()
            }
            // find 2d coordinate system
            val xAxis = normal.findSecondAxis(JomlPools.vec3d.create())
            val yAxis = normal.cross(xAxis)
            val projected = points.map {
                JomlPools.vec2d.create()
                    .set(it.dot(xAxis), it.dot(yAxis))
            }
            val reverseMap = HashMap<Vector2dc, Vector3dc>()
            points.forEachIndexed { index, vector3d ->
                reverseMap[projected[index]] = vector3d
            }
            val triangles2d = ringToTrianglesVec2d(projected)
            val result = triangles2d.map { reverseMap[it]!! }
            JomlPools.vec2d.sub(projected.size)
            JomlPools.vec3d.sub(2)
            return result
        } else return emptyList()
    }

    fun ringToTrianglesPoint(points: Array<Point>): List<Point> {
        if (points.size > 2) {
            val normal = JomlPools.vec3f.create().set(0f)
            for (i in points.indices) {
                val a = points[i].position
                val b = points[(i + 1) % points.size].position
                val c = points[(i + 2) % points.size].position
                normal.add((a - b).cross(b - c))
            }
            normal.normalize()
            if (normal.length() < 0.5f) {
                JomlPools.vec3f.sub(1)
                return emptyList()
            }
            // find 2d coordinate system
            val xAxis = normal.findSecondAxis(JomlPools.vec3f.create())
            val yAxis = normal.cross(xAxis)
            val projected = points.map {
                JomlPools.vec2f.create()
                    .set(it.position.dot(xAxis), it.position.dot(yAxis))
            }
            val reverseMap = HashMap<Vector2fc, Point>()
            points.forEachIndexed { index, vector3d ->
                reverseMap[projected[index]] = vector3d
            }
            val triangles2f = ringToTrianglesVec2f(projected)
            val result = triangles2f.map { reverseMap[it]!! }
            JomlPools.vec2f.sub(projected.size)
            JomlPools.vec3f.sub(2)
            return result
        } else return emptyList()
    }

    fun ringToTrianglesPoint(points: List<Point>): List<Point> {
        if (points.size > 2) {
            val normal = JomlPools.vec3f.create().set(0f)
            for (i in points.indices) {
                val a = points[i].position
                val b = points[(i + 1) % points.size].position
                val c = points[(i + 2) % points.size].position
                normal.add((a - b).cross(b - c))
            }
            normal.normalize()
            if (normal.length() < 0.5f) {
                JomlPools.vec3f.sub(1)
                return emptyList()
            }
            // find 2d coordinate system
            val xAxis = normal.findSecondAxis(JomlPools.vec3f.create())
            val yAxis = normal.cross(xAxis)
            val projected = points.map {
                JomlPools.vec2f.create()
                    .set(it.position.dot(xAxis), it.position.dot(yAxis))
            }
            val reverseMap = HashMap<Vector2fc, Point>()
            points.forEachIndexed { index, vector3d ->
                reverseMap[projected[index]] = vector3d
            }
            val triangles2f = ringToTrianglesVec2f(projected)
            val result = triangles2f.map { reverseMap[it]!! }
            JomlPools.vec2f.sub(projected.size)
            JomlPools.vec3f.sub(2)
            return result
        } else return emptyList()
    }

}