package com.bulletphysics.collision.narrowphase

import com.bulletphysics.collision.narrowphase.GjkEpaSolver.Results
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.IDebugDraw
import com.bulletphysics.linearmath.Transform
import org.joml.Vector3d

/**
 * GjkEpaPenetrationDepthSolver uses the Expanding Polytope Algorithm to calculate
 * the penetration depth between two convex shapes.
 *
 * @author jezek2
 */
class GjkEpaPenetrationDepthSolver : ConvexPenetrationDepthSolver {
    private val gjkEpaSolver = GjkEpaSolver()

    override fun calculatePenetrationDepth(
        simplexSolver: SimplexSolverInterface,
        convexA: ConvexShape, convexB: ConvexShape,
        transA: Transform, transB: Transform,
        v: Vector3d, witnessOnA: Vector3d, witnessOnB: Vector3d,
        debugDraw: IDebugDraw?
    ): Boolean {
        val radialMargin = 0.0

        // JAVA NOTE: 2.70b1: update when GjkEpaSolver2 is ported
        val results = Results()
        if (gjkEpaSolver.collide(
                convexA, transA,
                convexB, transB,
                radialMargin, results
            )
        ) {
            witnessOnA.set(results.witness0)
            witnessOnB.set(results.witness1)
            return true
        }

        return false
    }
}
