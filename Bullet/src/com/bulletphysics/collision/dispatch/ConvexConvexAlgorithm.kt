package com.bulletphysics.collision.dispatch

import com.bulletphysics.collision.broadphase.CollisionAlgorithm
import com.bulletphysics.collision.broadphase.CollisionAlgorithmConstructionInfo
import com.bulletphysics.collision.broadphase.DispatcherInfo
import com.bulletphysics.collision.narrowphase.ConvexPenetrationDepthSolver
import com.bulletphysics.collision.narrowphase.DiscreteCollisionDetectorInterface.ClosestPointInput
import com.bulletphysics.collision.narrowphase.GjkPairDetector
import com.bulletphysics.collision.narrowphase.PersistentManifold
import com.bulletphysics.collision.narrowphase.SimplexSolverInterface
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.util.ObjectPool
import cz.advel.stack.Stack
import com.bulletphysics.util.setSub

/**
 * ConvexConvexAlgorithm collision algorithm implements time of impact, convex
 * closest points and penetration depth calculations.
 *
 * @author jezek2
 */
class ConvexConvexAlgorithm : CollisionAlgorithm() {
    val pointInputsPool =
        ObjectPool.Companion.get(ClosestPointInput::class.java)

    private val gjkPairDetector = GjkPairDetector()

    var ownManifold: Boolean = false
    var manifold: PersistentManifold? = null
    var lowLevelOfDetail: Boolean = false

    fun init(
        mf: PersistentManifold?,
        ci: CollisionAlgorithmConstructionInfo,
        simplexSolver: SimplexSolverInterface,
        pdSolver: ConvexPenetrationDepthSolver?
    ) {
        super.init(ci)
        gjkPairDetector.init(null, null, simplexSolver, pdSolver)
        this.manifold = mf
        this.ownManifold = false
        this.lowLevelOfDetail = false
    }

    override fun destroy() {
        if (ownManifold) {
            if (this.manifold != null) {
                dispatcher!!.releaseManifold(this.manifold!!)
            }
            this.manifold = null
        }
    }

    /**
     * Convex-Convex collision algorithm.
     */
    override fun processCollision(
        body0: CollisionObject,
        body1: CollisionObject,
        dispatchInfo: DispatcherInfo,
        resultOut: ManifoldResult
    ) {
        if (this.manifold == null) {
            // swapped?
            this.manifold = dispatcher!!.getNewManifold(body0, body1)
            ownManifold = true
        }
        resultOut.persistentManifold = this.manifold!!

        val min0 = body0.collisionShape as ConvexShape?
        val min1 = body1.collisionShape as ConvexShape?

        val input = pointInputsPool.get()
        input.init()

        gjkPairDetector.minkowskiA = min0
        gjkPairDetector.minkowskiB = min1
        input.maximumDistanceSquared = min0!!.margin + min1!!.margin + manifold!!.contactBreakingThreshold
        input.maximumDistanceSquared *= input.maximumDistanceSquared

        body0.getWorldTransform(input.transformA)
        body1.getWorldTransform(input.transformB)

        gjkPairDetector.getClosestPoints(input, resultOut, dispatchInfo.debugDraw)

        pointInputsPool.release(input)

        //	#endif
        if (ownManifold) {
            resultOut.refreshContactPoints()
        }
    }

    override fun calculateTimeOfImpact(
        col0: CollisionObject,
        col1: CollisionObject,
        dispatchInfo: DispatcherInfo,
        resultOut: ManifoldResult
    ): Double {
        val tmp = Stack.newVec()

        val tmpTrans1 = Stack.newTrans()
        val tmpTrans2 = Stack.newTrans()

        // Rather then checking ALL pairs, only calculate TOI when motion exceeds threshold

        // Linear motion for one of objects needs to exceed m_ccdSquareMotionThreshold
        // col0->m_worldTransform,
        var resultFraction = 1.0

        tmp.setSub(col0.getInterpolationWorldTransform(tmpTrans1).origin, col0.getWorldTransform(tmpTrans2).origin)
        val squareMot0 = tmp.lengthSquared()

        tmp.setSub(col1.getInterpolationWorldTransform(tmpTrans1).origin, col1.getWorldTransform(tmpTrans2).origin)
        val squareMot1 = tmp.lengthSquared()

        if (squareMot0 < col0.ccdSquareMotionThreshold &&
            squareMot1 < col1.ccdSquareMotionThreshold
        ) {
            return resultFraction
        }

        val tmpTrans3 = Stack.newTrans()
        val tmpTrans4 = Stack.newTrans()

        // An adhoc way of testing the Continuous Collision Detection algorithms
        // One object is approximated as a sphere, to simplify things
        // Starting in penetration should report no time of impact
        // For proper CCD, better accuracy and handling of 'allowed' penetration should be added
        // also the mainloop of the physics should have a kind of toi queue (something like Brian Mirtich's application of Timewarp for Rigidbodies)

        // Convex0 against sphere for Convex1
        run {
            val convex0 = col0.collisionShape as ConvexShape?
            // todo: allow non-zero sphere sizes, for better approximation
            val sphere1 = SphereShape(col1.ccdSweptSphereRadius)
            val result = Stack.newCastResult()
            //SubsimplexConvexCast ccd0(&sphere,min0,&voronoiSimplex);
            /**Simplification, one object is simplified as a sphere */
            val ccd1 = Stack.newGjkCC(convex0, sphere1)
            //ContinuousConvexCollision ccd(min0,min1,&voronoiSimplex,0);
            if (ccd1.calcTimeOfImpact(
                    col0.getWorldTransform(tmpTrans1), col0.getInterpolationWorldTransform(tmpTrans2),
                    col1.getWorldTransform(tmpTrans3), col1.getInterpolationWorldTransform(tmpTrans4), result
                )
            ) {
                // store result.m_fraction in both bodies

                if (col0.hitFraction > result.fraction) {
                    col0.hitFraction = result.fraction
                }

                if (col1.hitFraction > result.fraction) {
                    col1.hitFraction = result.fraction
                }

                if (resultFraction > result.fraction) {
                    resultFraction = result.fraction
                }
            }
            Stack.subCastResult(1)
            Stack.subGjkCC(1)
        }

        // Sphere (for convex0) against Convex1
        run {
            val convex1 = col1.collisionShape as ConvexShape?
            val sphere0 =
                SphereShape(col0.ccdSweptSphereRadius) // todo: allow non-zero sphere sizes, for better approximation
            val result = Stack.newCastResult()
            //SubsimplexConvexCast ccd0(&sphere,min0,&voronoiSimplex);
            /**Simplification, one object is simplified as a sphere */
            val ccd1 = Stack.newGjkCC(sphere0, convex1)
            //ContinuousConvexCollision ccd(min0,min1,&voronoiSimplex,0);
            if (ccd1.calcTimeOfImpact(
                    col0.getWorldTransform(tmpTrans1), col0.getInterpolationWorldTransform(tmpTrans2),
                    col1.getWorldTransform(tmpTrans3), col1.getInterpolationWorldTransform(tmpTrans4), result
                )
            ) {
                //store result.m_fraction in both bodies

                if (col0.hitFraction > result.fraction) {
                    col0.hitFraction = result.fraction
                }

                if (col1.hitFraction > result.fraction) {
                    col1.hitFraction = result.fraction
                }

                if (resultFraction > result.fraction) {
                    resultFraction = result.fraction
                }
            }
            Stack.subCastResult(1)
            Stack.subGjkCC(1)
        }

        return resultFraction
    }

    override fun getAllContactManifolds(manifoldArray: ArrayList<PersistentManifold>) {
        // should we use ownManifold to avoid adding duplicates?
        val manifold = manifold
        if (manifold != null && ownManifold) {
            manifoldArray.add(manifold)
        }
    }

    /** ///////////////////////////////////////////////////////////////////////// */
    class CreateFunc(var simplexSolver: SimplexSolverInterface, var pdSolver: ConvexPenetrationDepthSolver?) :
        CollisionAlgorithmCreateFunc() {
        private val pool = ObjectPool.Companion.get(ConvexConvexAlgorithm::class.java)

        override fun createCollisionAlgorithm(
            ci: CollisionAlgorithmConstructionInfo,
            body0: CollisionObject, body1: CollisionObject
        ): CollisionAlgorithm {
            val algo = pool.get()
            algo.init(ci.manifold, ci, simplexSolver, pdSolver)
            return algo
        }

        override fun releaseCollisionAlgorithm(algo: CollisionAlgorithm) {
            pool.release(algo as ConvexConvexAlgorithm)
        }
    }
}
