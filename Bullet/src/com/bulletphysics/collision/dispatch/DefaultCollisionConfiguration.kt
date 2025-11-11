package com.bulletphysics.collision.dispatch

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.collision.narrowphase.ConvexPenetrationDepthSolver
import com.bulletphysics.collision.narrowphase.GjkEpaPenetrationDepthSolver
import com.bulletphysics.collision.narrowphase.VoronoiSimplexSolver

/**
 * Default implementation of [CollisionConfiguration]. Provides all core
 * collision algorithms. Some extra algorithms (like [GImpact][GImpactCollisionAlgorithm])
 * must be registered manually by calling appropriate register method.
 *
 * @author jezek2
 */
class DefaultCollisionConfiguration : CollisionConfiguration {

    // default simplex/penetration depth solvers
    var convexPenetrationSolver: ConvexPenetrationDepthSolver = GjkEpaPenetrationDepthSolver()

    var convexConvexCreateFunc: CollisionAlgorithmCreateFunc =
        ConvexConvexAlgorithm.CreateFunc(VoronoiSimplexSolver(), convexPenetrationSolver)
    var convexConcaveCreateFunc: CollisionAlgorithmCreateFunc =
        ConvexConcaveCollisionAlgorithm.CreateFunc()
    var swappedConvexConcaveCreateFunc: CollisionAlgorithmCreateFunc =
        ConvexConcaveCollisionAlgorithm.CreateFunc()
    var compoundCreateFunc: CollisionAlgorithmCreateFunc =
        CompoundCollisionAlgorithm.CreateFunc()
    var swappedCompoundCreateFunc: CollisionAlgorithmCreateFunc =
        CompoundCollisionAlgorithm.CreateFunc()
    var emptyCreateFunc: CollisionAlgorithmCreateFunc =
        EmptyAlgorithm.CreateFunc()
    var sphereSphereCreateFunc: CollisionAlgorithmCreateFunc =
        SphereSphereCollisionAlgorithm.CreateFunc()
    var convexPlaneCreateFunc: CollisionAlgorithmCreateFunc =
        ConvexPlaneCollisionAlgorithm.CreateFunc()
    var swappedConvexPlaneCreateFunc: CollisionAlgorithmCreateFunc =
        ConvexPlaneCollisionAlgorithm.CreateFunc()

    init {
        swappedConvexPlaneCreateFunc.swapped = true
        swappedConvexConcaveCreateFunc.swapped = true
        swappedCompoundCreateFunc.swapped = true
    }

    override fun getCollisionAlgorithmCreateFunc(
        proxyType0: BroadphaseNativeType,
        proxyType1: BroadphaseNativeType
    ): CollisionAlgorithmCreateFunc {
        return when {
            proxyType0 == BroadphaseNativeType.SPHERE &&
                    proxyType1 == BroadphaseNativeType.SPHERE -> sphereSphereCreateFunc
            proxyType0.isConvex && proxyType1 == BroadphaseNativeType.STATIC_PLANE -> convexPlaneCreateFunc
            proxyType1.isConvex && proxyType0 == BroadphaseNativeType.STATIC_PLANE -> swappedConvexPlaneCreateFunc
            proxyType0.isConvex && proxyType1.isConvex -> convexConvexCreateFunc
            proxyType0.isConvex && proxyType1.isConcave -> convexConcaveCreateFunc
            proxyType1.isConvex && proxyType0.isConcave -> swappedConvexConcaveCreateFunc
            proxyType0.isCompound -> compoundCreateFunc
            proxyType1.isCompound -> swappedCompoundCreateFunc

            // failed to find an algorithm
            else -> emptyCreateFunc
        }
    }
}
