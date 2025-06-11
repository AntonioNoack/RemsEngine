package com.bulletphysics.collision.narrowphase

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.MatrixUtil
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.VectorUtil.setInterpolate3
import cz.advel.stack.Stack
import com.bulletphysics.util.setNegate
import com.bulletphysics.util.setSub

/**
 * SubsimplexConvexCast implements Gino van den Bergens' paper
 * "Ray Casting against bteral Convex Objects with Application to Continuous Collision Detection"
 * GJK based Ray Cast, optimized version
 * Objects should not start in overlap, otherwise results are not defined.
 *
 * @author jezek2
 */
class SubSimplexConvexCast(
    private val convexA: ConvexShape,
    private val convexB: ConvexShape,
    private val simplexSolver: SimplexSolverInterface
) : ConvexCast {

    override fun calcTimeOfImpact(
        fromA: Transform, toA: Transform,
        fromB: Transform, toB: Transform,
        result: CastResult
    ): Boolean {
        val tmp = Stack.newVec()

        simplexSolver.reset()

        val linVelA = Stack.newVec()
        val linVelB = Stack.newVec()
        linVelA.setSub(toA.origin, fromA.origin)
        linVelB.setSub(toB.origin, fromB.origin)

        var lambda = 0.0

        val interpolatedTransA = Stack.newTrans(fromA)
        val interpolatedTransB = Stack.newTrans(fromB)

        // take relative motion
        val r = Stack.newVec()
        r.setSub(linVelA, linVelB)

        val v = Stack.newVec()

        r.negate(tmp)
        MatrixUtil.transposeTransform(tmp, tmp, fromA.basis)
        val supVertexA = convexA.localGetSupportingVertex(tmp, Stack.newVec())
        fromA.transform(supVertexA)

        MatrixUtil.transposeTransform(tmp, r, fromB.basis)
        val supVertexB = convexB.localGetSupportingVertex(tmp, Stack.newVec())
        fromB.transform(supVertexB)

        v.setSub(supVertexA, supVertexB)

        var maxIter: Int = MAX_ITERATIONS

        val n = Stack.newVec()
        n.set(0.0, 0.0, 0.0)

        var dist2 = v.lengthSquared()
        val epsilon = 0.0001
        val w = Stack.newVec()
        var VdotR: Double

        while ((dist2 > epsilon) && (maxIter--) != 0) {
            tmp.setNegate(v)
            MatrixUtil.transposeTransform(tmp, tmp, interpolatedTransA.basis)
            convexA.localGetSupportingVertex(tmp, supVertexA)
            interpolatedTransA.transform(supVertexA)

            MatrixUtil.transposeTransform(tmp, v, interpolatedTransB.basis)
            convexB.localGetSupportingVertex(tmp, supVertexB)
            interpolatedTransB.transform(supVertexB)

            w.setSub(supVertexA, supVertexB)

            val VdotW = v.dot(w)

            if (lambda > 1.0) {
                return false
            }

            if (VdotW > 0.0) {
                VdotR = v.dot(r)

                if (VdotR >= -(BulletGlobals.FLT_EPSILON * BulletGlobals.FLT_EPSILON)) {
                    return false
                } else {
                    lambda = lambda - VdotW / VdotR
                    // interpolate to next lambda
                    //	x = s + lambda * r;
                    setInterpolate3(interpolatedTransA.origin, fromA.origin, toA.origin, lambda)
                    setInterpolate3(interpolatedTransB.origin, fromB.origin, toB.origin, lambda)
                    // check next line
                    w.setSub(supVertexA, supVertexB)
                    n.set(v)
                }
            }
            simplexSolver.addVertex(w, supVertexA, supVertexB)
            if (simplexSolver.closest(v)) {
                dist2 = v.lengthSquared()
                // todo: check this normal for validity
                //n.set(v);
            } else {
                dist2 = 0.0
            }
        }

        // don't report a time of impact when moving 'away' from the hitnormal
        result.fraction = lambda
        if (n.lengthSquared() >= BulletGlobals.SIMD_EPSILON * BulletGlobals.SIMD_EPSILON) {
            n.normalize(result.normal)
        } else {
            result.normal.set(0.0, 0.0, 0.0)
        }

        // don't report time of impact for motion away from the contact normal (or causes minor penetration)
        if (result.normal.dot(r) >= -result.allowedPenetration) return false

        val hitA = Stack.newVec()
        val hitB = Stack.newVec()
        simplexSolver.computePoints(hitA, hitB)
        result.hitPoint.set(hitB)
        return true
    }

    companion object {
        // Typically the conservative advancement reaches solution in a few iterations, clip it to 32 for degenerate cases.
        // See discussion about this here http://www.bulletphysics.com/phpBB2/viewtopic.php?t=565
        private const val MAX_ITERATIONS = 32
    }
}
