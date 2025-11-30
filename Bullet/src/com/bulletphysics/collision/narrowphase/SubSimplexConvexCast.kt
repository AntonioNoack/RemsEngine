package com.bulletphysics.collision.narrowphase

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.VectorUtil.setInterpolate3
import cz.advel.stack.Stack
import org.joml.Vector3d

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
        fromA: Transform, toA: Vector3d,
        fromB: Transform, toB: Vector3d,
        result: CastResult
    ): Boolean = calcTimeOfImpactImpl(
        convexA, convexB, simplexSolver,
        fromA, toA,
        fromB, toB,
        result
    )

    companion object {
        // Typically the conservative advancement reaches solution in a few iterations, clip it to 32 for degenerate cases.
        // See discussion about this here http://www.bulletphysics.com/phpBB2/viewtopic.php?t=565
        private const val MAX_ITERATIONS = 32

        fun calcTimeOfImpactImpl(
            convexA: ConvexShape,
            convexB: ConvexShape,
            simplexSolver: SimplexSolverInterface,

            fromA: Transform, toA: Vector3d,
            fromB: Transform, toB: Vector3d,
            result: CastResult
        ): Boolean {

            simplexSolver.reset()

            val linVelA = Stack.newVec3f()
            val linVelB = Stack.newVec3f()
            toA.sub(fromA.origin, linVelA)
            toB.sub(fromB.origin, linVelB)

            var lambda = 0f

            val interpolatedTransA = Stack.newTrans(fromA)
            val interpolatedTransB = Stack.newTrans(fromB)

            val relVelocity = Stack.newVec3f()
            linVelA.sub(linVelB, relVelocity)

            val supportDir = Stack.newVec3f()
            val tmp = Stack.newVec3f()

            relVelocity.negate(tmp)
            fromA.basis.transformTranspose(tmp)
            val supVertexA = convexA.localGetSupportingVertex(tmp, Stack.newVec3f())
            fromA.transformPosition(supVertexA)

            fromB.basis.transformTranspose(relVelocity, tmp)
            val supVertexB = convexB.localGetSupportingVertex(tmp, Stack.newVec3f())
            fromB.transformPosition(supVertexB)

            supVertexA.sub(supVertexB, supportDir)

            var maxIter: Int = MAX_ITERATIONS

            val n = Stack.newVec3f().set(0f)

            var dist2 = supportDir.lengthSquared()
            val epsilon = 0.0001f
            val diff = Stack.newVec3f()

            while ((dist2 > epsilon) && (maxIter--) != 0) {
                // basic plane separation algorithm
                supportDir.negate(tmp)
                interpolatedTransA.basis.transformTranspose(tmp, tmp)
                convexA.localGetSupportingVertex(tmp, supVertexA)
                interpolatedTransA.transformPosition(supVertexA)

                interpolatedTransB.basis.transformTranspose(supportDir, tmp)
                convexB.localGetSupportingVertex(tmp, supVertexB)
                interpolatedTransB.transformPosition(supVertexB)

                supVertexA.sub(supVertexB, diff)

                if (lambda > 1.0) {
                    Stack.subVec3f(9)
                    Stack.subTrans(2)
                    return false
                }

                val depth = supportDir.dot(diff)
                if (depth > 0.0) {
                    val alignment = supportDir.dot(relVelocity)
                    if (alignment >= -BulletGlobals.FLT_EPSILON_SQ) {
                        Stack.subVec3f(9)
                        Stack.subTrans(2)
                        return false
                    } else {
                        lambda -= depth / alignment
                        // interpolate to next lambda
                        //	x = s + lambda * r;
                        setInterpolate3(interpolatedTransA.origin, fromA.origin, toA, lambda.toDouble())
                        setInterpolate3(interpolatedTransB.origin, fromB.origin, toB, lambda.toDouble())
                        // check next line
                        supVertexA.sub(supVertexB, diff)
                        n.set(supportDir)
                    }
                }

                simplexSolver.addVertex(diff, supVertexA, supVertexB)
                dist2 = if (simplexSolver.closest(supportDir)) {
                    supportDir.lengthSquared()
                    // todo: check this normal for validity
                    //n.set(v);
                } else {
                    0f
                }
            }

            // don't report a time of impact when moving 'away' from the hitNormal
            result.fraction = lambda
            if (n.lengthSquared() >= BulletGlobals.SIMD_EPSILON * BulletGlobals.SIMD_EPSILON) {
                n.normalize(result.normal)
            } else {
                result.normal.set(0f)
            }

            // don't report time of impact for motion away from the contact normal (or causes minor penetration)
            if (result.normal.dot(relVelocity) >= -result.allowedPenetration) {
                Stack.subVec3f(9)
                Stack.subTrans(2)
                return false
            }

            val hitA = Stack.newVec3f()
            val hitB = Stack.newVec3f()
            simplexSolver.computePoints(hitA, hitB)
            result.hitPoint.set(hitB)

            Stack.subVec3f(11)
            Stack.subTrans(2)
            return true
        }
    }
}
