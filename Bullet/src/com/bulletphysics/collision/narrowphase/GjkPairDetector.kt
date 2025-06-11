package com.bulletphysics.collision.narrowphase

import com.bulletphysics.BulletGlobals
import com.bulletphysics.BulletStats
import com.bulletphysics.collision.narrowphase.DiscreteCollisionDetectorInterface.ClosestPointInput
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.IDebugDraw
import com.bulletphysics.linearmath.MatrixUtil
import cz.advel.stack.Stack
import org.joml.Vector3d
import com.bulletphysics.util.setAdd
import com.bulletphysics.util.setNegate
import com.bulletphysics.util.setScale
import com.bulletphysics.util.setSub
import kotlin.math.sqrt

/**
 * GjkPairDetector uses GJK to implement the [DiscreteCollisionDetectorInterface].
 *
 * @author jezek2
 */
class GjkPairDetector : DiscreteCollisionDetectorInterface {
    private val cachedSeparatingAxis = Vector3d()
    private var penetrationDepthSolver: ConvexPenetrationDepthSolver? = null
    private var simplexSolver: SimplexSolverInterface? = null
    var minkowskiA: ConvexShape? = null
    var minkowskiB: ConvexShape? = null
    private var ignoreMargin = false

    // some debugging to fix degeneracy problems
    var lastUsedMethod: Int = 0
    var curIter: Int = 0
    var degenerateSimplex: Int = 0
    var catchDegeneracies: Int = 0

    fun init(
        objectA: ConvexShape?,
        objectB: ConvexShape?,
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
        val tmp = Stack.newVec()

        var distance = 0.0
        val normalInB = Stack.newVec()
        normalInB.set(0.0, 0.0, 0.0)
        val pointOnA = Stack.newVec()
        val pointOnB = Stack.newVec()
        val localTransA = Stack.newTrans(input.transformA)
        val localTransB = Stack.newTrans(input.transformB)
        val positionOffset = Stack.newVec()
        positionOffset.setAdd(localTransA.origin, localTransB.origin)
        positionOffset.mul(0.5)
        localTransA.origin.sub(positionOffset)
        localTransB.origin.sub(positionOffset)

        var marginA = minkowskiA!!.margin
        var marginB = minkowskiB!!.margin

        BulletStats.numGjkChecks++

        // for CCD we don't use margins
        if (ignoreMargin) {
            marginA = 0.0
            marginB = 0.0
        }

        curIter = 0
        val gGjkMaxIter = 1000 // this is to catch invalid input, perhaps check for #NaN?
        cachedSeparatingAxis.set(0.0, 1.0, 0.0)

        var isValid = false
        var checkSimplex = false
        var checkPenetration = true
        degenerateSimplex = 0

        lastUsedMethod = -1

        var squaredDistance = BulletGlobals.SIMD_INFINITY
        var delta: Double

        val margin = marginA + marginB

        simplexSolver!!.reset()

        val separatingAxisInA = Stack.newVec()
        val separatingAxisInB = Stack.newVec()

        val pInA = Stack.newVec()
        val qInB = Stack.newVec()

        val pWorld = Stack.newVec()
        val qWorld = Stack.newVec()
        val w = Stack.newVec()

        val tmpPointOnA = Stack.newVec()
        val tmpPointOnB = Stack.newVec()
        val tmpNormalInB = Stack.newVec()

        while (true) {
            separatingAxisInA.setNegate(cachedSeparatingAxis)
            MatrixUtil.transposeTransform(separatingAxisInA, separatingAxisInA, input.transformA.basis)

            separatingAxisInB.set(cachedSeparatingAxis)
            MatrixUtil.transposeTransform(separatingAxisInB, separatingAxisInB, input.transformB.basis)

            minkowskiA!!.localGetSupportingVertexWithoutMargin(separatingAxisInA, pInA)
            minkowskiB!!.localGetSupportingVertexWithoutMargin(separatingAxisInB, qInB)

            pWorld.set(pInA)
            localTransA.transform(pWorld)

            qWorld.set(qInB)
            localTransB.transform(qWorld)

            w.setSub(pWorld, qWorld)

            delta = cachedSeparatingAxis.dot(w)

            // potential exit, they don't overlap
            if ((delta > 0.0) && (delta * delta > squaredDistance * input.maximumDistanceSquared)) {
                checkPenetration = false
                break
            }

            // exit 0: the new point is already in the simplex, or we didn't come any closer
            if (simplexSolver!!.inSimplex(w)) {
                degenerateSimplex = 1
                checkSimplex = true
                break
            }
            // are we getting any closer ?
            val f0 = squaredDistance - delta
            val f1: Double = squaredDistance * REL_ERROR2

            if (f0 <= f1) {
                if (f0 <= 0.0) {
                    degenerateSimplex = 2
                }
                checkSimplex = true
                break
            }
            // add current vertex to simplex
            simplexSolver!!.addVertex(w, pWorld, qWorld)

            // calculate the closest point to the origin (update vector v)
            if (!simplexSolver!!.closest(cachedSeparatingAxis)) {
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
                simplexSolver!!.backupClosest(cachedSeparatingAxis)
                checkSimplex = true
                break
            }

            // degeneracy, this is typically due to invalid/uninitialized worldtransforms for a CollisionObject
            if (curIter++ > gGjkMaxIter) {
                //#if defined(DEBUG) || defined (_DEBUG)
                if (BulletGlobals.DEBUG) {
                    System.err.printf("btGjkPairDetector maxIter exceeded: %d\n", curIter)
                    System.err.printf(
                        "sepAxis=(%f,%f,%f), squaredDistance = %f, shapeTypeA=%s,shapeTypeB=%s\n",
                        cachedSeparatingAxis.x, cachedSeparatingAxis.y, cachedSeparatingAxis.z,
                        squaredDistance, minkowskiA!!.shapeType, minkowskiB!!.shapeType
                    )
                }
                //#endif
                break
            }

            val check = (!simplexSolver!!.fullSimplex())

            //bool check = (!m_simplexSolver->fullSimplex() && squaredDistance > SIMD_EPSILON * m_simplexSolver->maxVertex());
            if (!check) {
                // do we need this backup_closest here ?
                simplexSolver!!.backupClosest(cachedSeparatingAxis)
                break
            }
        }

        if (checkSimplex) {
            simplexSolver!!.computePoints(pointOnA, pointOnB)
            normalInB.setSub(pointOnA, pointOnB)
            val lenSqr = cachedSeparatingAxis.lengthSquared()
            // valid normal
            if (lenSqr < 0.0001f) {
                degenerateSimplex = 5
            }
            if (lenSqr > BulletGlobals.FLT_EPSILON * BulletGlobals.FLT_EPSILON) {
                val rlen = 1.0 / sqrt(lenSqr)
                normalInB.mul(rlen) // normalize
                val s = sqrt(squaredDistance)

                assert(s > 0.0)

                tmp.setScale((marginA / s), cachedSeparatingAxis)
                pointOnA.sub(tmp)

                tmp.setScale((marginB / s), cachedSeparatingAxis)
                pointOnB.add(tmp)

                distance = ((1.0 / rlen) - margin)
                isValid = true

                lastUsedMethod = 1
            } else {
                lastUsedMethod = 2
            }
        }

        val catchDegeneratePenetrationCase =
            (catchDegeneracies != 0 && penetrationDepthSolver != null && degenerateSimplex != 0 && ((distance + margin) < 0.01))

        //if (checkPenetration && !isValid)
        if (checkPenetration && (!isValid || catchDegeneratePenetrationCase)) {
            // penetration case

            // if there is no way to handle penetrations, bail out

            if (penetrationDepthSolver != null) {
                // Penetration depth case.
                BulletStats.numDeepPenetrationChecks++

                val isValid2 = penetrationDepthSolver!!.calculatePenetrationDepth(
                    simplexSolver!!, minkowskiA!!, minkowskiB!!, localTransA, localTransB,
                    cachedSeparatingAxis, tmpPointOnA, tmpPointOnB, debugDraw /*,input.stackAlloc*/
                )

                if (isValid2) {
                    tmpNormalInB.setSub(tmpPointOnB, tmpPointOnA)

                    val lenSqr = tmpNormalInB.lengthSquared()
                    if (lenSqr > (BulletGlobals.FLT_EPSILON * BulletGlobals.FLT_EPSILON)) {
                        tmpNormalInB.mul(1.0 / sqrt(lenSqr))
                        tmp.setSub(tmpPointOnA, tmpPointOnB)
                        val distance2 = -tmp.length()
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
            tmp.setAdd(pointOnB, positionOffset)
            output.addContactPoint(normalInB, tmp, distance)
        }

        Stack.subVec(15)
        Stack.subTrans(2)
    }

    companion object {
        // must be above the machine epsilon
        private const val REL_ERROR2 = 1.0e-6
    }
}
