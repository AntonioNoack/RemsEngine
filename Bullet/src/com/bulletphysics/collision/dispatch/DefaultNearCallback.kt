package com.bulletphysics.collision.dispatch

import com.bulletphysics.collision.broadphase.BroadphasePair
import com.bulletphysics.collision.broadphase.DispatcherInfo

/**
 * Default implementation of [NearCallback].
 *
 * @author jezek2
 */
class DefaultNearCallback : NearCallback {

    private val contactPointResult = ManifoldResult()

    override fun handleCollision(
        collisionPair: BroadphasePair,
        dispatcher: CollisionDispatcher,
        dispatchInfo: DispatcherInfo
    ) {
        val colObj0 = collisionPair.proxy0.clientObject
        val colObj1 = collisionPair.proxy1.clientObject

        if (dispatcher.needsCollision(colObj0, colObj1)) {
            // dispatcher will keep algorithms persistent in the collision pair
            if (collisionPair.algorithm == null) {
                collisionPair.algorithm = dispatcher.findAlgorithm(colObj0, colObj1)
            }

            val algorithm = collisionPair.algorithm ?: return
            contactPointResult.init(colObj0, colObj1)
            if (dispatchInfo.discreteDispatch) {
                // discrete collision detection query
                algorithm.processCollision(colObj0, colObj1, dispatchInfo, contactPointResult)
            } else {
                // continuous collision detection query, time of impact (toi)
                val toi = algorithm.calculateTimeOfImpact(colObj0, colObj1, dispatchInfo, contactPointResult)
                if (dispatchInfo.timeOfImpact > toi) {
                    dispatchInfo.timeOfImpact = toi
                }
            }
        }
    }
}
