package com.bulletphysics.collision.narrowphase

import com.bulletphysics.BulletGlobals
import com.bulletphysics.BulletStats
import com.bulletphysics.collision.narrowphase.DiscreteCollisionDetectorInterface.ClosestPointInput
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.IDebugDraw
import cz.advel.stack.Stack
import org.joml.Vector3f
import kotlin.math.sqrt

/**
 * GjkPairDetector uses GJK to implement the [DiscreteCollisionDetectorInterface].
 *
 * @author jezek2
 */
class GjkPairDetector : DiscreteCollisionDetectorInterface {
    private val cachedSeparatingAxis = Vector3f()
    private var penetrationDepthSolver: ConvexPenetrationDepthSolver? = null
    private lateinit var simplexSolver: SimplexSolverInterface
    lateinit var minkowskiA: ConvexShape
    lateinit var minkowskiB: ConvexShape
    private var ignoreMargin = false

    // some debugging to fix degeneracy problems
    var lastUsedMethod: Int = 0
    var curIter: Int = 0
    var degenerateSimplex: Int = 0
    var catchDegeneracies: Int = 0

    fun init(
        objectA: ConvexShape, objectB: ConvexShape,
        simplexSolver: SimplexSolverInterface,
        penetrationDepthSolver: ConvexPenetrationDepthSolver?
    ) {
        this.cachedSeparatingAxis.set(0.0, 0.0, 1.0)
        this.ignoreMargin = false
        this.lastUsedMethod = -1
        this.catchDegeneracies = 1

        this.penetrationDepthSolver = penetrationDepthSolver
        this.simplexSolver = simplexSolver
        this.minkowskiA = objectA
        this.minkowskiB = objectB
    }

    override fun getClosestPoints(
        input: ClosestPointInput,
        output: DiscreteCollisionDetectorInterface.Result,
        debugDraw: IDebugDraw?,
        swapResults: Boolean
    ) {
        val tmp = Stack.newVec3d()

        var distance = 0f
        val normalInB = Stack.newVec3f().set(0f)
        val pointOnA = Stack.newVec3f()
        val pointOnB = Stack.newVec3f()
        val localTransA = Stack.newTrans(input.transformA)
        val localTransB = Stack.newTrans(input.transformB)
        val positionOffset = Stack.newVec3d()
        localTransA.origin.add(localTransB.origin, positionOffset)
        positionOffset.mul(0.5)
        localTransA.origin.sub(positionOffset)
        localTransB.origin.sub(positionOffset)

        var marginA = minkowskiA.margin
        var marginB = minkowskiB.margin

        BulletStats.numGjkChecks++

        // for CCD we don't use margins
        if (ignoreMargin) {
            marginA = 0f
            marginB = 0f
        }

        curIter = 0
        val gGjkMaxIter = 1000 // this is to catch invalid input, perhaps check for #NaN?
        cachedSeparatingAxis.set(0f, 1f, 0f)

        var isValid = false
        var checkSimplex = false
        var checkPenetration = true
        degenerateSimplex = 0

        lastUsedMethod = -1

        var squaredDistance = BulletGlobals.SIMD_INFINITY
        var delta: Double

        val margin = marginA + marginB

        simplexSolver.reset()

        val separatingAxisInA = Stack.newVec3f()
        val separatingAxisInB = Stack.newVec3f()

        val pWorld0 = Stack.newVec3f()
        val qWorld0 = Stack.newVec3f()
        val pWorld = Stack.newVec3d()
        val qWorld = Stack.newVec3d()
        val globalDiff = Stack.newVec3f()

        val tmpPointOnA = Stack.newVec3d()
        val tmpPointOnB = Stack.newVec3d()
        val tmpNormalInB = Stack.newVec3d()

        while (true) {

            // basic plane separating test
            cachedSeparatingAxis.negate(separatingAxisInA)
            input.transformA.basis.transformTranspose(separatingAxisInA, separatingAxisInA)
            input.transformB.basis.transformTranspose(cachedSeparatingAxis, separatingAxisInB)

            minkowskiA.localGetSupportingVertexWithoutMargin(separatingAxisInA, pWorld0)
            minkowskiB.localGetSupportingVertexWithoutMargin(separatingAxisInB, qWorld0)

            localTransA.transformPosition(pWorld.set(pWorld0))
            localTransB.transformPosition(qWorld.set(qWorld0))

            pWorld.sub(qWorld, globalDiff)
            delta = cachedSeparatingAxis.dot(globalDiff).toDouble()

            // potential exit, they don't overlap
            if ((delta > 0.0) && (delta * delta > squaredDistance * input.maximumDistanceSquared)) {
                checkPenetration = false
                break
            }

            // exit 0: the new point is already in the simplex, or we didn't come any closer
            if (simplexSolver.inSimplex(globalDiff)) {
                degenerateSimplex = 1
                checkSimplex = true
                break
            }

            // are we getting any closer ?
            val f0 = squaredDistance - delta
            val f1 = squaredDistance * REL_ERROR2

            if (f0 <= f1) {
                if (f0 <= 0.0) {
                    degenerateSimplex = 2
                }
                checkSimplex = true
                break
            }

            // add current vertex to simplex
            simplexSolver.addVertex(globalDiff, pWorld, qWorld)

            // calculate the closest point to the origin (update vector v)
            if (!simplexSolver.closest(cachedSeparatingAxis)) {
                degenerateSimplex = 3
                checkSimplex = true
                break
            }

            if (cachedSeparatingAxis.lengthSquared() < REL_ERROR2) {
                degenerateSimplex = 6
                checkSimplex = true
                break
            }

            val previousSquaredDistance = squaredDistance
            squaredDistance = cachedSeparatingAxis.lengthSquared()

            // redundant m_simplexSolver->compute_points(pointOnA, pointOnB);

            // are we getting any closer ?
            if (previousSquaredDistance - squaredDistance <= BulletGlobals.FLT_EPSILON * previousSquaredDistance) {
                simplexSolver.backupClosest(cachedSeparatingAxis)
                checkSimplex = true
                break
            }

            // degeneracy, this is typically due to invalid/uninitialized worldTransforms for a CollisionObject
            if (curIter++ > gGjkMaxIter) {
                if (BulletGlobals.DEBUG) {
                    System.err.printf("btGjkPairDetector maxIter exceeded: %d\n", curIter)
                    System.err.printf(
                        "sepAxis=(%f,%f,%f), squaredDistance = %f, shapeTypeA=%s,shapeTypeB=%s\n",
                        cachedSeparatingAxis.x, cachedSeparatingAxis.y, cachedSeparatingAxis.z,
                        squaredDistance, minkowskiA.shapeType, minkowskiB.shapeType
                    )
                }
                break
            }

            val check = !simplexSolver.isSimplex4Full()
            //bool check = (!m_simplexSolver->fullSimplex() && squaredDistance > SIMD_EPSILON * m_simplexSolver->maxVertex());
            if (!check) {
                // do we need this backup_closest here ?
                simplexSolver.backupClosest(cachedSeparatingAxis)
                break
            }
        }

        if (checkSimplex) {

            simplexSolver.computePoints(pointOnA, pointOnB)

            pointOnA.sub(pointOnB, normalInB)
            val lenSqr = cachedSeparatingAxis.lengthSquared()
            // valid normal
            if (lenSqr < 0.0001) {
                degenerateSimplex = 5
            }
            if (lenSqr > BulletGlobals.FLT_EPSILON_SQ) {
                val rlen = 1f / sqrt(lenSqr)
                normalInB.mul(rlen) // normalize
                val s = sqrt(squaredDistance)

                assert(s > 0f)

                cachedSeparatingAxis.mulAdd(-marginA / s, pointOnA, pointOnA)
                cachedSeparatingAxis.mulAdd(+marginB / s, pointOnB, pointOnB)

                distance = 1f / rlen - margin
                isValid = true

                lastUsedMethod = 1
            } else {
                lastUsedMethod = 2
            }
        }

        val penetrationDepthSolver = penetrationDepthSolver
        val catchDegeneratePenetrationCase =
            (catchDegeneracies != 0 && penetrationDepthSolver != null && degenerateSimplex != 0 && ((distance + margin) < 0.01))

        //if (checkPenetration && !isValid)
        if (checkPenetration && (!isValid || catchDegeneratePenetrationCase)) {
            // penetration case

            // if there is no way to handle penetrations, bail out

            if (penetrationDepthSolver != null) {
                // Penetration depth case.
                BulletStats.numDeepPenetrationChecks++

                val isValid2 = penetrationDepthSolver.calculatePenetrationDepth(
                    simplexSolver,
                    minkowskiA, minkowskiB,
                    localTransA, localTransB,
                    cachedSeparatingAxis,
                    tmpPointOnA, tmpPointOnB,
                    debugDraw
                )

                if (isValid2) {
                    tmpPointOnB.sub(tmpPointOnA, tmpNormalInB)

                    val lenSqr = tmpNormalInB.lengthSquared()
                    if (lenSqr > BulletGlobals.FLT_EPSILON_SQ) {
                        tmpNormalInB.mul(1.0 / sqrt(lenSqr))
                        val distance2 = -tmpPointOnA.distance(tmpPointOnB).toFloat()
                        // only replace valid penetrations when the result is deeper (check)
                        if (!isValid || (distance2 < distance)) {
                            distance = distance2
                            pointOnA.set(tmpPointOnA)
                            pointOnB.set(tmpPointOnB)
                            normalInB.set(tmpNormalInB)
                            isValid = true
                            lastUsedMethod = 3
                        }
                    } else {
                        //isValid = false;
                        lastUsedMethod = 4
                    }
                } else {
                    lastUsedMethod = 5
                }
            }
        }

        if (isValid) {
            positionOffset.add(pointOnB, tmp)
            output.addContactPoint(normalInB, tmp, distance)
        }

        Stack.subVec3f(8)
        Stack.subVec3d(7)
        Stack.subTrans(2)
    }

    companion object {
        // must be above the machine epsilon
        private const val REL_ERROR2 = 1.0e-6
    }
}
