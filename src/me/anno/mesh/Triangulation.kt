package me.anno.mesh

import me.anno.utils.pooling.JomlPools
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.the3deers.util.EarCut

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
    @Suppress("unused")
    fun ringToTrianglesVec2f(points: List<Vector2f>): List<Vector2f> {
        val map = HashMap<Vector2d, Vector2f>()
        val tmp = points.map {
            val newVec = JomlPools.vec2d.create().set(it)
            map[newVec] = it
            newVec
        }
        val result = ringToTrianglesVec2d(tmp).map { map[it]!! }
        JomlPools.vec2d.sub(points.size)
        return result
    }

    @JvmStatic
    fun ringToTrianglesVec3f(points: List<Vector3f>): List<Vector3f> {
        val map = HashMap<Vector3d, Vector3f>()
        val points3d = points.map { vec3f ->
            val vec3d = JomlPools.vec3d.create().set(vec3f)
            map[vec3d] = vec3f
            vec3d
        }
        val result = ringToTrianglesVec3d(points3d)
            .map { vec3d -> map[vec3d]!! }
        JomlPools.vec3d.sub(points.size)
        return result
    }

    @JvmStatic
    fun ringToTrianglesVec3d(points: List<Vector3d>): List<Vector3d> {
        if (points.size > 2) {
            val pool2 = JomlPools.vec2d
            val pool3 = JomlPools.vec3d
            val normal = pool3.create().set(0.0)
            val tmp1 = pool3.create()
            val tmp2 = pool3.create()
            for (i in points.indices) {
                val a = points[i]
                val b = points[(i + 1) % points.size]
                val c = points[(i + 2) % points.size]
                tmp1.set(a).sub(b)
                tmp2.set(b).sub(c)
                normal.add(tmp1.cross(tmp2))
            }
            normal.normalize()
            if (normal.length() < 0.5) {
                pool3.sub(3)
                return emptyList()
            }
            // find 2d coordinate system
            val xAxis = normal.findSecondAxis(pool3.create())
            val yAxis = normal.cross(xAxis)
            val projected = points.map {
                pool2.create()
                    .set(it.dot(xAxis), it.dot(yAxis))
            }
            val reverseMap = HashMap<Vector2d, Vector3d>()
            for (index in points.indices) {
                reverseMap[projected[index]] = points[index]
            }
            val triangles2d = ringToTrianglesVec2d(projected)
            val result = triangles2d.map { reverseMap[it]!! }
            pool2.sub(projected.size)
            pool3.sub(4)
            return result
        } else return emptyList()
    }

    @JvmStatic
    fun ringToTrianglesPoint(points: List<Point>): List<Point> {
        if (points.size > 2) {
            val pool2 = JomlPools.vec2d
            val pool3 = JomlPools.vec3d
            val normal = pool3.create().set(0.0)
            val tmp1 = pool3.create()
            val tmp2 = pool3.create()
            for (i in points.indices) {
                val a = points[i].position
                val b = points[(i + 1) % points.size].position
                val c = points[(i + 2) % points.size].position
                tmp1.set(a).sub(b)
                tmp2.set(b).sub(c)
                normal.add(tmp1.cross(tmp2))
            }
            normal.normalize()
            if (normal.length() < 0.5) {
                pool3.sub(3)
                return emptyList()
            }
            // find 2d coordinate system
            val xAxis = normal.findSecondAxis(pool3.create())
            val yAxis = normal.cross(xAxis)
            val projected = points.map {
                pool2.create().set(it.position.dot(xAxis), it.position.dot(yAxis))
            }
            val reverseMap = HashMap<Vector2d, Point>()
            for (index in points.indices) {
                reverseMap[projected[index]] = points[index]
            }
            val triangles2f = ringToTrianglesVec2d(projected)
            val result = triangles2f.map { reverseMap[it]!! }
            pool2.sub(projected.size)
            pool3.sub(4)
            return result
        } else return emptyList()
    }
}