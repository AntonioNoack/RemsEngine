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
    //default simplex/penetration depth solvers
    var pdSolver: ConvexPenetrationDepthSolver?

    //default CreationFunctions, filling the m_doubleDispatch table
    var convexConvexCreateFunc: CollisionAlgorithmCreateFunc
    var convexConcaveCreateFunc: CollisionAlgorithmCreateFunc
    var swappedConvexConcaveCreateFunc: CollisionAlgorithmCreateFunc
    var compoundCreateFunc: CollisionAlgorithmCreateFunc
    var swappedCompoundCreateFunc: CollisionAlgorithmCreateFunc
    var emptyCreateFunc: CollisionAlgorithmCreateFunc
    var sphereSphereCF: CollisionAlgorithmCreateFunc
    var planeConvexCF: CollisionAlgorithmCreateFunc
    var convexPlaneCF: CollisionAlgorithmCreateFunc

    init {
        pdSolver = GjkEpaPenetrationDepthSolver()

        /*
		//default CreationFunctions, filling the m_doubleDispatch table
		*/
        convexConvexCreateFunc = ConvexConvexAlgorithm.CreateFunc(VoronoiSimplexSolver(), pdSolver)
        convexConcaveCreateFunc = ConvexConcaveCollisionAlgorithm.CreateFunc()
        swappedConvexConcaveCreateFunc = ConvexConcaveCollisionAlgorithm.SwappedCreateFunc()
        compoundCreateFunc = CompoundCollisionAlgorithm.CreateFunc()
        swappedCompoundCreateFunc = CompoundCollisionAlgorithm.SwappedCreateFunc()
        emptyCreateFunc = EmptyAlgorithm.CreateFunc()

        sphereSphereCF = SphereSphereCollisionAlgorithm.CreateFunc()

        // convex versus plane
        convexPlaneCF = ConvexPlaneCollisionAlgorithm.CreateFunc()
        planeConvexCF = ConvexPlaneCollisionAlgorithm.CreateFunc()
        planeConvexCF.swapped = true
    }

    override fun getCollisionAlgorithmCreateFunc(
        proxyType0: BroadphaseNativeType,
        proxyType1: BroadphaseNativeType
    ): CollisionAlgorithmCreateFunc {
        if ((proxyType0 == BroadphaseNativeType.SPHERE_SHAPE_PROXYTYPE) && (proxyType1 == BroadphaseNativeType.SPHERE_SHAPE_PROXYTYPE)) {
            return sphereSphereCF
        }

        if (proxyType0.isConvex && (proxyType1 == BroadphaseNativeType.STATIC_PLANE_PROXYTYPE)) {
            return convexPlaneCF
        }

        if (proxyType1.isConvex && (proxyType0 == BroadphaseNativeType.STATIC_PLANE_PROXYTYPE)) {
            return planeConvexCF
        }

        if (proxyType0.isConvex && proxyType1.isConvex) {
            return convexConvexCreateFunc
        }

        if (proxyType0.isConvex && proxyType1.isConcave) {
            return convexConcaveCreateFunc
        }

        if (proxyType1.isConvex && proxyType0.isConcave) {
            return swappedConvexConcaveCreateFunc
        }

        if (proxyType0.isCompound) {
            return compoundCreateFunc
        } else if (proxyType1.isCompound) {
            return swappedCompoundCreateFunc
        }

        // failed to find an algorithm
        return emptyCreateFunc
    }
}
