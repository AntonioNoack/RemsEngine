package com.bulletphysics.collision.broadphase

import com.bulletphysics.collision.dispatch.CollisionAlgorithmCreateFunc
import com.bulletphysics.collision.dispatch.CollisionObject
import com.bulletphysics.collision.dispatch.ManifoldResult
import com.bulletphysics.collision.narrowphase.PersistentManifold

/**
 * Collision algorithm for handling narrowphase or midphase collision detection
 * between two collision object types.
 *
 * @author jezek2
 */
abstract class CollisionAlgorithm {

    private var createFunc: CollisionAlgorithmCreateFunc? = null
    lateinit var dispatcher: Dispatcher

    fun init() {
    }

    open fun init(ci: CollisionAlgorithmConstructionInfo) {
        dispatcher = ci.dispatcher1
    }

    abstract fun destroy()

    abstract fun processCollision(
        body0: CollisionObject,
        body1: CollisionObject,
        dispatchInfo: DispatcherInfo,
        resultOut: ManifoldResult
    )

    abstract fun calculateTimeOfImpact(
        body0: CollisionObject,
        body1: CollisionObject,
        dispatchInfo: DispatcherInfo,
        resultOut: ManifoldResult
    ): Double

    abstract fun getAllContactManifolds(manifoldArray: ArrayList<PersistentManifold>)

    fun internalSetCreateFunc(func: CollisionAlgorithmCreateFunc?) {
        createFunc = func
    }

    fun internalGetCreateFunc(): CollisionAlgorithmCreateFunc? {
        return createFunc
    }
}
