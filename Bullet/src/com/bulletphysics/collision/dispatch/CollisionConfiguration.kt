package com.bulletphysics.collision.dispatch

import com.bulletphysics.collision.broadphase.BroadphaseNativeType

/**
 * CollisionConfiguration allows to configure Bullet default collision algorithms.
 *
 * @author jezek2
 */
interface CollisionConfiguration {
    fun getCollisionAlgorithmCreateFunc(
        proxyType0: BroadphaseNativeType,
        proxyType1: BroadphaseNativeType
    ): CollisionAlgorithmCreateFunc
}
