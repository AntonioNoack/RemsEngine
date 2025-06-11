package com.bulletphysics.collision.dispatch

import com.bulletphysics.collision.broadphase.BroadphasePair
import com.bulletphysics.collision.broadphase.BroadphaseProxy
import com.bulletphysics.collision.broadphase.Dispatcher
import com.bulletphysics.collision.broadphase.OverlappingPairCallback

/**
 * GhostPairCallback interfaces and forwards adding and removal of overlapping
 * pairs from the [BroadphaseInterface] to [GhostObject].
 *
 * @author tomrbryn
 */
class GhostPairCallback : OverlappingPairCallback {
    override fun addOverlappingPair(proxy0: BroadphaseProxy, proxy1: BroadphaseProxy): BroadphasePair? {
        val colObj0 = proxy0.clientObject as CollisionObject?
        val colObj1 = proxy1.clientObject as CollisionObject?
        val ghost0 = GhostObject.upcast(colObj0)
        val ghost1 = GhostObject.upcast(colObj1)

        ghost0?.addOverlappingObjectInternal(proxy1, proxy0)
        ghost1?.addOverlappingObjectInternal(proxy0, proxy1)
        return null
    }

    override fun removeOverlappingPair(
        proxy0: BroadphaseProxy, proxy1: BroadphaseProxy, dispatcher: Dispatcher
    ): Any? {
        val colObj0 = proxy0.clientObject as CollisionObject?
        val colObj1 = proxy1.clientObject as CollisionObject?
        val ghost0 = GhostObject.upcast(colObj0)
        val ghost1 = GhostObject.upcast(colObj1)

        ghost0?.removeOverlappingObjectInternal(proxy1, dispatcher, proxy0)
        ghost1?.removeOverlappingObjectInternal(proxy0, dispatcher, proxy1)
        return null
    }

    override fun removeOverlappingPairsContainingProxy(proxy0: BroadphaseProxy, dispatcher: Dispatcher) {
        assert(false)

        // need to keep track of all ghost objects and call them here
        // hashPairCache.removeOverlappingPairsContainingProxy(proxy0, dispatcher);
    }
}
