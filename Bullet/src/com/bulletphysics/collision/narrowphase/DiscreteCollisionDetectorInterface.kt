package com.bulletphysics.collision.narrowphase

import com.bulletphysics.linearmath.IDebugDraw
import com.bulletphysics.linearmath.Transform
import org.joml.Vector3d

/**
 * This interface is made to be used by an iterative approach to do TimeOfImpact calculations.
 *
 *
 *
 * This interface allows to query for closest points and penetration depth between two (convex) objects
 * the closest point is on the second object (B), and the normal points from the surface on B towards A.
 * distance is between the closest points on B and the closest point on A. So you can calculate the closest point on A
 * by taking `closestPointInA = closestPointInB + distance * normalOnSurfaceB`.
 *
 * @author jezek2
 */
interface DiscreteCollisionDetectorInterface {
    interface Result {
        /**setShapeIdentifiers provides experimental support for per-triangle material / custom material combiner */
        fun setShapeIdentifiers(partId0: Int, index0: Int, partId1: Int, index1: Int)

        fun addContactPoint(normalOnBInWorld: Vector3d, pointInWorld: Vector3d, depth: Double)
    }

    class ClosestPointInput {
        @JvmField
		val transformA = Transform()
        @JvmField
		val transformB = Transform()
        @JvmField
		var maximumDistanceSquared = Double.MAX_VALUE

        fun init() {
            maximumDistanceSquared = Double.MAX_VALUE
        }
    }

    /**
     * Give either closest points (distance > 0) or penetration (distance)
     * the normal always points from B towards A.
     */
    fun getClosestPoints(input: ClosestPointInput, output: Result, debugDraw: IDebugDraw?) {
        getClosestPoints(input, output, debugDraw, false)
    }

    /**
     * Give either closest points (distance > 0) or penetration (distance)
     * the normal always points from B towards A.
     */
    fun getClosestPoints(input: ClosestPointInput, output: Result, debugDraw: IDebugDraw?, swapResults: Boolean)
}
