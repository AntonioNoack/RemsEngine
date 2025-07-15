package com.bulletphysics.collision.narrowphase

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.MatrixUtil
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.VectorUtil.setInterpolate3
import cz.advel.stack.Stack

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
        toA.origin.sub(fromA.origin, linVelA)
        toB.origin.sub(fromB.origin, linVelB)

        var lambda = 0.0

        val interpolatedTransA = Stack.newTrans(fromA)
        val interpolatedTransB = Stack.newTrans(fromB)

        val relVelocity = Stack.newVec()
        linVelA.sub(linVelB, relVelocity)

        val v = Stack.newVec()

        relVelocity.negate(tmp)
        fromA.basis.transformTranspose(tmp)
        val supVertexA = convexA.localGetSupportingVertex(tmp, Stack.newVec())
        fromA.transform(supVertexA)

        fromB.basis.transformTranspose(relVelocity, tmp)
        val supVertexB = convexB.localGetSupportingVertex(tmp, Stack.newVec())
        fromB.transform(supVertexB)

        supVertexA.sub(supVertexB, v)

        var maxIter: Int = MAX_ITERATIONS

        val n = Stack.newVec()
        n.set(0.0, 0.0, 0.0)

        var dist2 = v.lengthSquared()
        val epsilon = 0.0001
        val w = Stack.newVec()

        while ((dist2 > epsilon) && (maxIter--) != 0) {
            // basic plane separation algorithm
            v.negate(tmp)
            MatrixUtil.transposeTransform(tmp, tmp, interpolatedTransA.basis)
            convexA.localGetSupportingVertex(tmp, supVertexA)
            interpolatedTransA.transform(supVertexA)

            MatrixUtil.transposeTransform(tmp, v, interpolatedTransB.basis)
            convexB.localGetSupportingVertex(tmp, supVertexB)
            interpolatedTransB.transform(supVertexB)

            supVertexA.sub(supVertexB, w)

            if (lambda > 1.0) {
                Stack.subVec(8)
                Stack.subTrans(2)
                return false
            }

            val depth = v.dot(w)
            if (depth > 0.0) {
                val VdotR = v.dot(relVelocity)
                if (VdotR >= -BulletGlobals.FLT_EPSILON_SQ) {
                    Stack.subVec(8)
                    Stack.subTrans(2)
                    return false
                } else {
                    lambda = lambda - depth / VdotR
                    // interpolate to next lambda
                    //	x = s + lambda * r;
                    setInterpolate3(interpolatedTransA.origin, fromA.origin, toA.origin, lambda)
                    setInterpolate3(interpolatedTransB.origin, fromB.origin, toB.origin, lambda)
                    // check next line
                    supVertexA.sub(supVertexB, w)
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

        // don't report a time of impact when moving 'away' from the hitNormal
        result.fraction = lambda
        if (n.lengthSquared() >= BulletGlobals.SIMD_EPSILON * BulletGlobals.SIMD_EPSILON) {
            n.normalize(result.normal)
        } else {
            result.normal.set(0.0, 0.0, 0.0)
        }

        // don't report time of impact for motion away from the contact normal (or causes minor penetration)
        if (result.normal.dot(relVelocity) >= -result.allowedPenetration) {
            Stack.subVec(8)
            Stack.subTrans(2)
            return false
        }

        val hitA = Stack.newVec()
        val hitB = Stack.newVec()
        simplexSolver.computePoints(hitA, hitB)
        result.hitPoint.set(hitB)
        
        Stack.subVec(10)
        Stack.subTrans(2)
        return true
    }

    companion object {
        // Typically the conservative advancement reaches solution in a few iterations, clip it to 32 for degenerate cases.
        // See discussion about this here http://www.bulletphysics.com/phpBB2/viewtopic.php?t=565
        private const val MAX_ITERATIONS = 32
    }
}
