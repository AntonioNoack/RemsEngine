package com.bulletphysics.collision.narrowphase

import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.IDebugDraw
import com.bulletphysics.linearmath.Transform
import org.joml.Vector3d

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
        v: Vector3d, witnessOnA: Vector3d, witnessOnB: Vector3d,
        debugDraw: IDebugDraw?
    ): Boolean
}
