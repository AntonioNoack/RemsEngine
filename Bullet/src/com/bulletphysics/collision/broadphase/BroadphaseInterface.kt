package com.bulletphysics.collision.broadphase

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
        userPtr: Any?,
        collisionFilter: Int,
        dispatcher: Dispatcher,
        multiSapProxy: Any?
    ): BroadphaseProxy

    abstract fun destroyProxy(proxy: BroadphaseProxy, dispatcher: Dispatcher)

    abstract fun setAabb(proxy: BroadphaseProxy, aabbMin: Vector3d, aabbMax: Vector3d, dispatcher: Dispatcher)

    // calculateOverlappingPairs is optional: incremental algorithms (sweep and prune) might do it during the set aabb
    abstract fun calculateOverlappingPairs(dispatcher: Dispatcher)

    abstract val overlappingPairCache: OverlappingPairCache

    /**
     * returns the axis aligned bounding box in the 'global' coordinate frame
     * will add some transform later
     */
    @Suppress("unused")
    abstract fun getBroadphaseAabb(aabbMin: Vector3d, aabbMax: Vector3d)
}
