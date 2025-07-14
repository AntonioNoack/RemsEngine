package com.bulletphysics.collision.broadphase

import com.bulletphysics.collision.dispatch.CollisionObject
import org.joml.Vector3d

/**
 * BroadphaseInterface for AABB overlapping object pairs.
 *
 * @author jezek2
 */
abstract class BroadphaseInterface {

    abstract fun createProxy(
        aabbMin: Vector3d,
        aabbMax: Vector3d,
        shapeType: BroadphaseNativeType,
        userPtr: CollisionObject,
        collisionFilter: Int,
        dispatcher: Dispatcher
    ): BroadphaseProxy

    abstract fun destroyProxy(proxy: BroadphaseProxy, dispatcher: Dispatcher)

    abstract fun setAabb(proxy: BroadphaseProxy, aabbMin: Vector3d, aabbMax: Vector3d, dispatcher: Dispatcher)

    // calculateOverlappingPairs is optional: incremental algorithms (sweep and prune) might do it during the set aabb
    abstract fun calculateOverlappingPairs(dispatcher: Dispatcher)

    abstract val overlappingPairCache: OverlappingPairCache

}
