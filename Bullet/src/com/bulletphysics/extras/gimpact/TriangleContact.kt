package com.bulletphysics.extras.gimpact

import com.bulletphysics.BulletGlobals
import com.bulletphysics.util.ArrayPool
import org.joml.Vector3d
import org.joml.Vector4d

/**
 *
 * @author jezek2
 */
class TriangleContact() {

    private val intArrays: ArrayPool<IntArray> = ArrayPool.get(Int::class.javaPrimitiveType!!)

    @JvmField
    var penetrationDepth = 0f

    @JvmField
    var numPoints = 0

    @JvmField
    val separatingNormal = Vector4d()

    @JvmField
    val points: Array<Vector3d> = Array(MAX_TRI_CLIPPING) { Vector3d() }

    @Suppress("unused")
    constructor(other: TriangleContact): this() {
        copyFrom(other)
    }

    fun set(other: TriangleContact) {
        copyFrom(other)
    }

    fun copyFrom(other: TriangleContact) {
        penetrationDepth = other.penetrationDepth
        separatingNormal.set(other.separatingNormal)
        numPoints = other.numPoints
        var i = numPoints
        while ((i--) != 0) {
            points[i].set(other.points[i])
        }
    }

    /**
     * Classify points that are closer.
     */
    fun mergePoints(plane: Vector4d, margin: Double, points: Array<Vector3d>, pointCount1: Int) {
        this.numPoints = 0
        penetrationDepth = -1000f

        val pointIndices = intArrays.getFixed(MAX_TRI_CLIPPING)

        for (k in 0 until pointCount1) {
            val dist = (-ClipPolygon.distancePointPlane(plane, points[k]) + margin).toFloat()
            if (dist >= 0f) {
                if (dist > penetrationDepth) {
                    penetrationDepth = dist
                    pointIndices[0] = k
                    this.numPoints = 1
                } else if ((dist + BulletGlobals.SIMD_EPSILON) >= penetrationDepth) {
                    pointIndices[this.numPoints] = k
                    this.numPoints++
                }
            }
        }

        for (k in 0 until this.numPoints) {
            this.points[k].set(points[pointIndices[k]])
        }

        intArrays.release(pointIndices)
    }

    companion object {
        const val MAX_TRI_CLIPPING: Int = 16
    }
}
