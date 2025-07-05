package com.bulletphysics.extras.gimpact

import com.bulletphysics.BulletGlobals
import com.bulletphysics.linearmath.VectorUtil
import com.bulletphysics.util.ArrayPool
import org.joml.Vector3d
import org.joml.Vector4d

/**
 * @author jezek2
 */
internal object ClipPolygon {
    @JvmStatic
    fun distancePointPlane(plane: Vector4d, point: Vector3d): Double {
        return VectorUtil.dot3(point, plane) - plane.w
    }

    /**
     * Distance from a 3D plane.
     */
    fun planeClipPolygonCollect(
        point0: Vector3d, point1: Vector3d, dist0: Double, dist1: Double,
        clipped: Array<Vector3d>, clippedCount: IntArray
    ) {
        val prevClassIf = (dist0 > BulletGlobals.SIMD_EPSILON)
        val classIf = (dist1 > BulletGlobals.SIMD_EPSILON)
        if (classIf != prevClassIf) {
            val f = -dist0 / (dist1 - dist0)
            point0.lerp(point1, f, clipped[clippedCount[0]])
            clippedCount[0]++
        }
        if (!classIf) {
            clipped[clippedCount[0]].set(point1)
            clippedCount[0]++
        }
    }

    /**
     * Clips a polygon by a plane.
     *
     * @return The count of the clipped counts
     */
    @JvmStatic
    fun planeClipPolygon(
        plane: Vector4d, polygonPoints: Array<Vector3d>,
        polygonPointCount: Int, clipped: Array<Vector3d>
    ): Int {
        val intArrays = ArrayPool.get<IntArray>(Int::class.javaPrimitiveType!!)

        val clippedCount = intArrays.getFixed(1)
        clippedCount[0] = 0

        // clip first point
        val firstDist = distancePointPlane(plane, polygonPoints[0])
        if (!(firstDist > BulletGlobals.SIMD_EPSILON)) {
            clipped[clippedCount[0]].set(polygonPoints[0])
            clippedCount[0]++
        }

        var prevDist = firstDist
        for (i in 1 until polygonPointCount) {
            val dist = distancePointPlane(plane, polygonPoints[i])

            planeClipPolygonCollect(
                polygonPoints[i - 1], polygonPoints[i],
                prevDist, dist, clipped, clippedCount
            )


            prevDist = dist
        }

        // RETURN TO FIRST point
        planeClipPolygonCollect(
            polygonPoints[polygonPointCount - 1], polygonPoints[0],
            prevDist, firstDist, clipped, clippedCount
        )

        val ret = clippedCount[0]
        intArrays.release(clippedCount)
        return ret
    }

    /**
     * Clips a polygon by a plane.
     *
     * @param clipped must be an array of 16 points.
     * @return the count of the clipped counts
     */
    @JvmStatic
    fun planeClipTriangle(
        plane: Vector4d,
        point0: Vector3d,
        point1: Vector3d,
        point2: Vector3d,
        clipped: Array<Vector3d>
    ): Int {
        val intArrays = ArrayPool.get<IntArray>(Int::class.javaPrimitiveType!!)

        val clippedCount = intArrays.getFixed(1)
        clippedCount[0] = 0

        // clip first point0
        val firstdist = distancePointPlane(plane, point0)
        if (!(firstdist > BulletGlobals.SIMD_EPSILON)) {
            clipped[clippedCount[0]].set(point0)
            clippedCount[0]++
        }

        // point 1
        var olddist = firstdist
        var dist = distancePointPlane(plane, point1)

        planeClipPolygonCollect(point0, point1, olddist, dist, clipped, clippedCount)

        olddist = dist


        // point 2
        dist = distancePointPlane(plane, point2)

        planeClipPolygonCollect(point1, point2, olddist, dist, clipped, clippedCount)
        olddist = dist


        // RETURN TO FIRST point0
        planeClipPolygonCollect(point2, point0, olddist, firstdist, clipped, clippedCount)

        val ret = clippedCount[0]
        intArrays.release(clippedCount)
        return ret
    }
}
