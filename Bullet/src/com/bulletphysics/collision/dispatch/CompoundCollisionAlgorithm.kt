package com.bulletphysics.collision.dispatch

import com.bulletphysics.collision.broadphase.CollisionAlgorithm
import com.bulletphysics.collision.broadphase.CollisionAlgorithmConstructionInfo
import com.bulletphysics.collision.broadphase.DispatcherInfo
import com.bulletphysics.collision.narrowphase.PersistentManifold
import com.bulletphysics.collision.shapes.CompoundShape
import com.bulletphysics.util.ObjectPool
import cz.advel.stack.Stack

/**
 * CompoundCollisionAlgorithm supports collision between [CompoundShape]s and
 * other collision shapes.
 *
 * @author jezek2
 */
class CompoundCollisionAlgorithm : CollisionAlgorithm() {
    private val childCollisionAlgorithms = ArrayList<CollisionAlgorithm?>()
    private var isSwapped = false

    fun init(
        ci: CollisionAlgorithmConstructionInfo,
        body0: CollisionObject,
        body1: CollisionObject,
        isSwapped: Boolean
    ) {
        super.init(ci)

        this.isSwapped = isSwapped

        val colObj = if (isSwapped) body1 else body0
        val otherObj = if (isSwapped) body0 else body1
        assert(colObj.collisionShape!!.isCompound)

        val compoundShape = colObj.collisionShape as CompoundShape?
        val numChildren = compoundShape!!.numChildShapes

        //childCollisionAlgorithms.resize(numChildren);
        for (i in 0 until numChildren) {
            val tmpShape = colObj.collisionShape
            val childShape = compoundShape.getChildShape(i)
            colObj.internalSetTemporaryCollisionShape(childShape)
            childCollisionAlgorithms.add(ci.dispatcher1!!.findAlgorithm(colObj, otherObj))
            colObj.internalSetTemporaryCollisionShape(tmpShape)
        }
    }

    override fun destroy() {
        val numChildren = childCollisionAlgorithms.size
        for (i in 0 until numChildren) {
            //childCollisionAlgorithms.get(i).destroy();
            dispatcher!!.freeCollisionAlgorithm(childCollisionAlgorithms[i]!!)
        }
        childCollisionAlgorithms.clear()
    }

    override fun processCollision(
        body0: CollisionObject,
        body1: CollisionObject,
        dispatchInfo: DispatcherInfo,
        resultOut: ManifoldResult
    ) {
        val colObj = if (isSwapped) body1 else body0
        val otherObj = if (isSwapped) body0 else body1

        assert(colObj.collisionShape!!.isCompound)
        val compoundShape = colObj.collisionShape as CompoundShape?

        // We will use the OptimizedBVH, AABB tree to cull potential child-overlaps
        // If both proxies are Compound, we will deal with that directly, by performing sequential/parallel tree traversals
        // given Proxy0 and Proxy1, if both have a tree, Tree0 and Tree1, this means:
        // determine overlapping nodes of Proxy1 using Proxy0 AABB against Tree1
        // then use each overlapping node AABB against Tree0
        // and vise versa.
        val tmpTrans = Stack.newTrans()
        val orgTrans = Stack.newTrans()
        val childTrans = Stack.newTrans()
        val orgInterpolationTrans = Stack.newTrans()
        val newChildWorldTrans = Stack.newTrans()

        val numChildren = childCollisionAlgorithms.size
        for (i in 0 until numChildren) {
            // temporarily exchange parent btCollisionShape with childShape, and recurse
            val childShape = compoundShape!!.getChildShape(i)

            // backup
            colObj.getWorldTransform(orgTrans)
            colObj.getInterpolationWorldTransform(orgInterpolationTrans)

            compoundShape.getChildTransform(i, childTrans)
            newChildWorldTrans.mul(orgTrans, childTrans)
            colObj.setWorldTransform(newChildWorldTrans)
            colObj.setInterpolationWorldTransform(newChildWorldTrans)


            // the contactpoint is still projected back using the original inverted worldtrans
            val tmpShape = colObj.collisionShape
            colObj.internalSetTemporaryCollisionShape(childShape)
            childCollisionAlgorithms[i]!!.processCollision(colObj, otherObj, dispatchInfo, resultOut)
            // revert back
            colObj.internalSetTemporaryCollisionShape(tmpShape)
            colObj.setWorldTransform(orgTrans)
            colObj.setInterpolationWorldTransform(orgInterpolationTrans)
        }
    }

    override fun calculateTimeOfImpact(
        body0: CollisionObject,
        body1: CollisionObject,
        dispatchInfo: DispatcherInfo,
        resultOut: ManifoldResult
    ): Double {
        val colObj = if (isSwapped) body1 else body0
        val otherObj = if (isSwapped) body0 else body1

        assert(colObj.collisionShape!!.isCompound)

        val compoundShape = colObj.collisionShape as CompoundShape?

        // We will use the OptimizedBVH, AABB tree to cull potential child-overlaps
        // If both proxies are Compound, we will deal with that directly, by performing sequential/parallel tree traversals
        // given Proxy0 and Proxy1, if both have a tree, Tree0 and Tree1, this means:
        // determine overlapping nodes of Proxy1 using Proxy0 AABB against Tree1
        // then use each overlapping node AABB against Tree0
        // and vise versa.
        val tmpTrans = Stack.newTrans()
        val orgTrans = Stack.newTrans()
        val childTrans = Stack.newTrans()
        var hitFraction = 1.0

        val numChildren = childCollisionAlgorithms.size
        for (i in 0 until numChildren) {
            // temporarily exchange parent btCollisionShape with childShape, and recurse
            val childShape = compoundShape!!.getChildShape(i)

            // backup
            colObj.getWorldTransform(orgTrans)

            compoundShape.getChildTransform(i, childTrans)
            //btTransform	newChildWorldTrans = orgTrans*childTrans ;
            tmpTrans.set(orgTrans)
            tmpTrans.mul(childTrans)
            colObj.setWorldTransform(tmpTrans)

            val tmpShape = colObj.collisionShape
            colObj.internalSetTemporaryCollisionShape(childShape)
            val frac = childCollisionAlgorithms[i]!!
                .calculateTimeOfImpact(colObj, otherObj, dispatchInfo, resultOut)
            if (frac < hitFraction) {
                hitFraction = frac
            }
            // revert back
            colObj.internalSetTemporaryCollisionShape(tmpShape)
            colObj.setWorldTransform(orgTrans)
        }
        return hitFraction
    }

    override fun getAllContactManifolds(manifoldArray: ArrayList<PersistentManifold>) {
        for (i in 0 until childCollisionAlgorithms.size) {
            childCollisionAlgorithms[i]!!.getAllContactManifolds(manifoldArray)
        }
    }

    /**///////////////////////////////////////////////////////////////////////// */
    class CreateFunc : CollisionAlgorithmCreateFunc() {
        private val pool = ObjectPool.Companion.get(CompoundCollisionAlgorithm::class.java)

        override fun createCollisionAlgorithm(
            ci: CollisionAlgorithmConstructionInfo,
            body0: CollisionObject,
            body1: CollisionObject
        ): CollisionAlgorithm {
            val algo = pool.get()
            algo.init(ci, body0, body1, false)
            return algo
        }

        override fun releaseCollisionAlgorithm(algo: CollisionAlgorithm) {
            pool.release(algo as CompoundCollisionAlgorithm)
        }
    }

    class SwappedCreateFunc : CollisionAlgorithmCreateFunc() {
        private val pool = ObjectPool.Companion.get(CompoundCollisionAlgorithm::class.java)

        override fun createCollisionAlgorithm(
            ci: CollisionAlgorithmConstructionInfo,
            body0: CollisionObject,
            body1: CollisionObject
        ): CollisionAlgorithm {
            val algo = pool.get()
            algo.init(ci, body0, body1, true)
            return algo
        }

        override fun releaseCollisionAlgorithm(algo: CollisionAlgorithm) {
            pool.release(algo as CompoundCollisionAlgorithm)
        }
    }
}
