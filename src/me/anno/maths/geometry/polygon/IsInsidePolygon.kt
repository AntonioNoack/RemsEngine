package me.anno.maths.geometry.polygon

import me.anno.mesh.Triangulation.findNormalVector
import me.anno.utils.pooling.JomlPools
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * Checking whether a point is inside a polygon.
 * */
object IsInsidePolygon {

    @JvmStatic
    fun Vector2i.isInsidePolygon(
        polygon: List<Vector2i>,
        i0: Int = 0, i1: Int = polygon.size - i0
    ): Boolean = isInsidePolygon(x, y, polygon, i0, i1)

    @JvmStatic
    fun isInsidePolygon(
        x: Int, y: Int, polygon: List<Vector2i>,
        i0: Int = 0, i1: Int = polygon.size - i0
    ): Boolean {
        var isInside = false
        val j = polygon[i1 - 1]
        var jx = j.x
        var jy = j.y
        for (i in i0 until i1) {
            val ix = polygon[i].x
            val iy = polygon[i].y
            if ((iy > y) != (jy > y) && x < (jx - ix).toLong() * (y - iy) / (jy - iy) + ix) {
                isInside = !isInside
            }
            jx = ix
            jy = iy
        }
        return isInside
    }

    @JvmStatic
    fun Vector2i.isInsidePolygon(
        polygon: IntArray,
        i0: Int = 0, i1: Int = polygon.size.shr(1) - i0
    ): Boolean = isInsidePolygon(x, y, polygon, i0, i1)

    @JvmStatic
    fun isInsidePolygon(
        x: Int, y: Int, polygon: IntArray,
        i0: Int = 0, i1: Int = polygon.size.shr(1) - i0
    ): Boolean {
        var isInside = false
        val j = (i1 - 1) * 2
        var jx = polygon[j]
        var jy = polygon[j + 1]
        for (i in i0 until i1) {
            val ix = polygon[i * 2]
            val iy = polygon[i * 2 + 1]
            if ((iy > y) != (jy > y) && x < (jx - ix).toLong() * (y - iy) / (jy - iy) + ix) {
                isInside = !isInside
            }
            jx = ix
            jy = iy
        }
        return isInside
    }

    @JvmStatic
    fun Vector2f.isInsidePolygon(
        polygon: List<Vector2f>,
        i0: Int = 0, i1: Int = polygon.size - i0
    ): Boolean = isInsidePolygon(x, y, polygon, i0, i1)

    @JvmStatic
    fun isInsidePolygon(
        x: Float, z: Float, polygon: List<Vector2f>,
        i0: Int = 0, i1: Int = polygon.size - i0
    ): Boolean {
        var isInside = false
        val j = polygon[i1 - 1]
        var jx = j.x
        var jy = j.y
        for (i in i0 until i1) {
            val ix = polygon[i].x
            val iy = polygon[i].y
            if ((iy > z) != (jy > z) && x < (jx - ix) * (z - iy) / (jy - iy) + ix) {
                isInside = !isInside
            }
            jx = ix
            jy = iy
        }
        return isInside
    }

    @JvmStatic
    fun Vector2f.isInsidePolygon(
        polygon: FloatArray,
        i0: Int = 0, i1: Int = polygon.size.shr(1) - i0
    ): Boolean = isInsidePolygon(x, y, polygon, i0, i1)

    @JvmStatic
    fun isInsidePolygon(
        x: Float, y: Float, polygon: FloatArray,
        i0: Int = 0, i1: Int = polygon.size.shr(1) - i0
    ): Boolean {
        var isInside = false
        val j = (i1 - 1) * 2
        var jx = polygon[j]
        var jy = polygon[j + 1]
        for (i in i0 until i1) {
            val ix = polygon[i * 2]
            val iy = polygon[i * 2 + 1]
            if ((iy > y) != (jy > y) && x < (jx - ix) * (y - iy) / (jy - iy) + ix) {
                isInside = !isInside
            }
            jx = ix
            jy = iy
        }
        return isInside
    }

    @JvmStatic
    fun Vector2d.isInsidePolygon(
        polygon: List<Vector2d>,
        i0: Int = 0, i1: Int = polygon.size - i0
    ): Boolean = isInsidePolygon(x, y, polygon, i0, i1)

    @JvmStatic
    fun isInsidePolygon(
        x: Double, y: Double, polygon: List<Vector2d>,
        i0: Int = 0, i1: Int = polygon.size - i0
    ): Boolean {
        var isInside = false
        val j = polygon[i1 - 1]
        var jx = j.x
        var jy = j.y
        for (i in i0 until i1) {
            val ix = polygon[i].x
            val iy = polygon[i].y
            if ((iy > y) != (jy > y) && x < (jx - ix) * (y - iy) / (jy - iy) + ix) {
                isInside = !isInside
            }
            jx = ix
            jy = iy
        }
        return isInside
    }


    @JvmStatic
    fun Vector2d.isInsidePolygon(
        polygon: DoubleArray,
        i0: Int = 0, i1: Int = polygon.size.shr(1) - i0
    ): Boolean = isInsidePolygon(x, y, polygon, i0, i1)

    @JvmStatic
    fun isInsidePolygon(
        x: Double, y: Double, polygon: DoubleArray,
        i0: Int = 0, i1: Int = polygon.size.shr(1) - i0
    ): Boolean {
        var isInside = false
        val j = (i1 - 1) * 2
        var jx = polygon[j]
        var jy = polygon[j + 1]
        for (i in i0 until i1) {
            val ix = polygon[i * 2]
            val iy = polygon[i * 2 + 1]
            if ((iy > y) != (jy > y) && x < (jx - ix) * (y - iy) / (jy - iy) + ix) {
                isInside = !isInside
            }
            jx = ix
            jy = iy
        }
        return isInside
    }

    @JvmStatic
    fun Vector3f.isInsidePolygon(polygon: List<Vector3f>): Boolean {
        val up = findNormalVector(polygon, JomlPools.vec3f.create())
        val inside = isInsidePolygon(polygon, up)
        JomlPools.vec3f.sub(1)
        return inside
    }

    @JvmStatic
    fun Vector3f.isInsidePolygon(polygon: List<Vector3f>, up: Vector3f): Boolean {
        var isInside = false

        val axisX = JomlPools.vec3f.create()
        val axisZ = JomlPools.vec3f.create()
        up.findSystem(axisX, axisZ, true)

        val posX = dot(axisX)
        val posZ = dot(axisZ)

        val pj = polygon.last()
        var jx = pj.dot(axisX)
        var jz = pj.dot(axisZ)

        for (i in polygon.indices) {
            val pi = polygon[i]
            val ix = pi.dot(axisX)
            val iz = pi.dot(axisZ)

            if ((iz > posZ) != (jz > posZ) &&
                posX < (jx - ix) * (posZ - iz) / (jz - iz) + ix
            ) isInside = !isInside

            jx = ix
            jz = iz
        }

        JomlPools.vec3f.sub(2)
        return isInside
    }

    @JvmStatic
    fun Vector3d.isInsidePolygon(polygon: List<Vector3d>): Boolean {
        val up = findNormalVector(polygon, JomlPools.vec3d.create())
        val inside = isInsidePolygon(polygon, up)
        JomlPools.vec3d.sub(1)
        return inside
    }

    @JvmStatic
    fun Vector3d.isInsidePolygon(polygon: List<Vector3d>, up: Vector3d): Boolean {
        var isInside = false

        val axisX = JomlPools.vec3d.create()
        val axisZ = JomlPools.vec3d.create()
        up.findSystem(axisX, axisZ, true)

        val posX = dot(axisX)
        val posZ = dot(axisZ)

        val pj = polygon.last()
        var jx = pj.dot(axisX)
        var jz = pj.dot(axisZ)

        for (i in polygon.indices) {
            val pi = polygon[i]
            val ix = pi.dot(axisX)
            val iz = pi.dot(axisZ)

            if ((iz > posZ) != (jz > posZ) &&
                posX < (jx - ix) * (posZ - iz) / (jz - iz) + ix
            ) isInside = !isInside

            jx = ix
            jz = iz
        }

        JomlPools.vec3d.sub(2)
        return isInside
    }

}