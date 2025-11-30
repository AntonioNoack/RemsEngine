package com.bulletphysics.collision.narrowphase

import com.bulletphysics.collision.narrowphase.DiscreteCollisionDetectorInterface.ClosestPointInput
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.VectorUtil.setInterpolate3
import com.bulletphysics.util.ObjectPool
import cz.advel.stack.Stack
import org.joml.Vector3d

/**
 * GjkConvexCast performs a raycast on a convex object using support mapping.
 *
 * @author jezek2
 */
class GjkConvexCast : ConvexCast {
    val pointInputsPool = ObjectPool.get(ClosestPointInput::class.java)

    private val simplexSolver: SimplexSolverInterface = VoronoiSimplexSolver()
    private lateinit var convexA: ConvexShape
    private lateinit var convexB: ConvexShape

    private val gjk = GjkPairDetector()

    fun init(convexA: ConvexShape, convexB: ConvexShape) {
        this.convexA = convexA
        this.convexB = convexB
    }

    override fun calcTimeOfImpact(
        fromA: Transform, toA: Vector3d,
        fromB: Transform, toB: Vector3d,
        result: CastResult
    ): Boolean {
        simplexSolver.reset()

        // compute linear velocity for this interval, to interpolate
        // assume no rotation/angular velocity, assert here?
        val linVelA = Stack.newVec3d()
        val linVelB = Stack.newVec3d()

        toA.sub(fromA.origin, linVelA)
        toB.sub(fromB.origin, linVelB)

        val radius = 0.001
        var lambda = 0f
        val v = Stack.newVec3d()
        v.set(1.0, 0.0, 0.0)

        val n = Stack.newVec3d()
        n.set(0.0, 0.0, 0.0)
        val hasResult: Boolean
        val c = Stack.newVec3d()
        val r = Stack.newVec3d()
        linVelA.sub(linVelB, r)

        var lastLambda = lambda

        //btScalar epsilon = btScalar(0.001);
        var numIter = 0

        // first solution, using GJK
        val identityTrans = Stack.newTrans()
        identityTrans.setIdentity()

        //result.drawCoordSystem(sphereTr);
        val pointCollector = Stack.newPointCollector()

        gjk.init(convexA, convexB, simplexSolver, null) // penetrationDepthSolver);
        val input = pointInputsPool.get()
        input.init()
        try { // not to catch exceptions, just for cleanup

            // we don't use margins during CCD
            //	gjk.setIgnoreMargin(true);

            input.transformA.set(fromA)
            input.transformB.set(fromB)
            gjk.getClosestPoints(input, pointCollector, null)

            hasResult = pointCollector.hasResult
            c.set(pointCollector.pointInWorld)

            if (hasResult) {
                var dist = pointCollector.distance
                n.set(pointCollector.normalOnBInWorld)

                // not close enough
                while (dist > radius) {
                    numIter++
                    if (numIter > MAX_ITERATIONS) {
                        return false // todo: report a failure
                    }

                    val projectedLinearVelocity = r.dot(n)
                    lambda -= (dist / projectedLinearVelocity).toFloat()

                    if (lambda > 1f) return false
                    if (lambda < 0f) {
                        return false // todo: next check with relative epsilon
                    }

                    if (lambda <= lastLambda) return false
                    lastLambda = lambda

                    // interpolate to next lambda
                    setInterpolate3(input.transformA.origin, fromA.origin, toA, lambda.toDouble())
                    setInterpolate3(input.transformB.origin, fromB.origin, toB, lambda.toDouble())

                    gjk.getClosestPoints(input, pointCollector, null)
                    if (pointCollector.hasResult) {
                        if (pointCollector.distance < 0.0) {
                            result.fraction = lastLambda
                            n.set(pointCollector.normalOnBInWorld)
                            result.normal.set(n)
                            result.hitPoint.set(pointCollector.pointInWorld)
                            return true
                        }
                        c.set(pointCollector.pointInWorld)
                        n.set(pointCollector.normalOnBInWorld)
                        dist = pointCollector.distance
                    } else {
                        // ??
                        return false
                    }
                }

                // is n normalized?
                // don't report time of impact for motion away from the contact normal (or causes minor penetration)
                if (n.dot(r) >= -result.allowedPenetration) {
                    return false
                }
                result.fraction = lambda
                result.normal.set(n)
                result.hitPoint.set(c)
                return true
            }

            return false
        } finally {
            pointInputsPool.release(input)
            Stack.subVec3d(6)
            Stack.subTrans(1)
            Stack.subPointCollector(1)
        }
    }

    companion object {
        private const val MAX_ITERATIONS = 32
    }
}
