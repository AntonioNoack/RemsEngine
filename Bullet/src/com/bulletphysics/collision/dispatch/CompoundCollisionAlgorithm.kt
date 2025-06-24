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
    private val childCollisionAlgorithms = ArrayList<CollisionAlgorithm>()
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

        val compoundShape = colObj.collisionShape as CompoundShape
        val children = compoundShape.children

        childCollisionAlgorithms.ensureCapacity(children.size)
        for (i in children.indices) {
            val tmpShape = colObj.collisionShape
            colObj.collisionShape = children[i].shape
            childCollisionAlgorithms.add(ci.dispatcher1.findAlgorithm(colObj, otherObj)!!)
            colObj.collisionShape = tmpShape
        }
    }

    override fun destroy() {
        val dispatcher = dispatcher
        for (i in childCollisionAlgorithms.indices) {
            dispatcher.freeCollisionAlgorithm(childCollisionAlgorithms[i])
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

        val compoundShape = colObj.collisionShape as CompoundShape

        // We will use the OptimizedBVH, AABB tree to cull potential child-overlaps
        // If both proxies are Compound, we will deal with that directly, by performing sequential/parallel tree traversals
        // given Proxy0 and Proxy1, if both have a tree, Tree0 and Tree1, this means:
        // determine overlapping nodes of Proxy1 using Proxy0 AABB against Tree1
        // then use each overlapping node AABB against Tree0
        // and vise versa.
        val orgTrans = Stack.newTrans()
        val orgInterpolationTrans = Stack.newTrans()
        val newChildWorldTrans = Stack.newTrans()

        val numChildren = childCollisionAlgorithms.size
        for (i in 0 until numChildren) {
            val child = compoundShape.children[i]
            // temporarily exchange parent btCollisionShape with childShape, and recurse

            // backup
            colObj.getWorldTransform(orgTrans)
            colObj.getInterpolationWorldTransform(orgInterpolationTrans)

            val childTrans = child.transform
            newChildWorldTrans.setMul(orgTrans, childTrans)
            colObj.setWorldTransform(newChildWorldTrans)
            colObj.setInterpolationWorldTransform(newChildWorldTrans)

            // the contact point is still projected back using the original inverted world transform
            val tmpShape = colObj.collisionShape
            colObj.collisionShape = child.shape
            childCollisionAlgorithms[i].processCollision(colObj, otherObj, dispatchInfo, resultOut)

            // revert back
            colObj.collisionShape = tmpShape
            colObj.setWorldTransform(orgTrans)
            colObj.setInterpolationWorldTransform(orgInterpolationTrans)
        }

        Stack.subTrans(3)
    }

    override fun calculateTimeOfImpact(
        body0: CollisionObject,
        body1: CollisionObject,
        dispatchInfo: DispatcherInfo,
        resultOut: ManifoldResult
    ): Double {
        val colObj = if (isSwapped) body1 else body0
        val otherObj = if (isSwapped) body0 else body1

        val compoundShape = colObj.collisionShape as CompoundShape

        // We will use the OptimizedBVH, AABB tree to cull potential child-overlaps
        // If both proxies are Compound, we will deal with that directly, by performing sequential/parallel tree traversals
        // given Proxy0 and Proxy1, if both have a tree, Tree0 and Tree1, this means:
        // determine overlapping nodes of Proxy1 using Proxy0 AABB against Tree1
        // then use each overlapping node AABB against Tree0
        // and vise versa.
        val childTransform = Stack.newTrans()
        val originalTransform = Stack.newTrans()
        var hitFraction = 1.0

        val numChildren = childCollisionAlgorithms.size
        val originalShape = colObj.collisionShape

        // backup
        colObj.getWorldTransform(originalTransform)
        for (i in 0 until numChildren) {
            // temporarily exchange parent btCollisionShape with childShape, and recurse
            val child = compoundShape.children[i]
            childTransform.setMul(originalTransform, child.transform)
            colObj.setWorldTransform(childTransform)
            colObj.collisionShape = child.shape
            val frac = childCollisionAlgorithms[i]
                .calculateTimeOfImpact(colObj, otherObj, dispatchInfo, resultOut)
            if (frac < hitFraction) {
                hitFraction = frac
            }
        }

        // revert back
        colObj.collisionShape = originalShape
        colObj.setWorldTransform(originalTransform)

        Stack.subTrans(2)

        return hitFraction
    }

    override fun getAllContactManifolds(manifoldArray: ArrayList<PersistentManifold>) {
        for (i in childCollisionAlgorithms.indices) {
            childCollisionAlgorithms[i].getAllContactManifolds(manifoldArray)
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
            algo.init(ci, body0, body1, swapped)
            return algo
        }

        override fun releaseCollisionAlgorithm(algo: CollisionAlgorithm) {
            pool.release(algo as CompoundCollisionAlgorithm)
        }
    }
}
