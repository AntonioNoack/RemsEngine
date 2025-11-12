package com.bulletphysics.collision.narrowphase

import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.IDebugDraw
import com.bulletphysics.linearmath.Transform
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * ConvexPenetrationDepthSolver provides an interface for penetration depth calculation.
 *
 * @author jezek2
 */
interface ConvexPenetrationDepthSolver {
    fun calculatePenetrationDepth(
        simplexSolver: SimplexSolverInterface,
        convexA: ConvexShape, convexB: ConvexShape,
        transA: Transform, transB: Transform,
        axisOrDirection: Vector3f,
        witnessOnA: Vector3d, witnessOnB: Vector3d,
        debugDraw: IDebugDraw?
    ): Boolean
}
