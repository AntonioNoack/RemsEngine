package com.bulletphysics.collision.dispatch

import com.bulletphysics.collision.broadphase.BroadphaseNativeType
import com.bulletphysics.collision.broadphase.BroadphasePair
import com.bulletphysics.collision.broadphase.CollisionAlgorithm
import com.bulletphysics.collision.broadphase.CollisionAlgorithmConstructionInfo
import com.bulletphysics.collision.broadphase.Dispatcher
import com.bulletphysics.collision.broadphase.DispatcherInfo
import com.bulletphysics.collision.broadphase.OverlapCallback
import com.bulletphysics.collision.broadphase.OverlappingPairCache
import com.bulletphysics.collision.narrowphase.PersistentManifold
import com.bulletphysics.util.ObjectPool
import org.apache.logging.log4j.LogManager
import java.util.Collections

/**
 * CollisionDispatcher supports algorithms that handle ConvexConvex and ConvexConcave collision pairs.
 * Time of Impact, Closest Points and Penetration Depth.
 *
 * If you have completely custom collision types, use this class to register them (see GImpact and SoftBody).
 *
 * @author jezek2
 */
class CollisionDispatcher(collisionConfiguration: CollisionConfiguration) : Dispatcher {

    val manifoldsPool: ObjectPool<PersistentManifold> = ObjectPool.get(PersistentManifold::class.java)
    val manifoldsList = ArrayList<PersistentManifold>()
    private var staticWarningReported = false

    @JvmField
    var nearCallback: NearCallback = DefaultNearCallback()

    private val collisionPairCallback = CollisionPairCallback()
    private val doubleDispatch = Array(NUM_COLLISION_TYPES * NUM_COLLISION_TYPES) { idx ->
        val j = idx % NUM_COLLISION_TYPES
        val i = idx / NUM_COLLISION_TYPES
        collisionConfiguration.getCollisionAlgorithmCreateFunc(
            BroadphaseNativeType.entries[i],
            BroadphaseNativeType.entries[j]
        )
    }

    private fun getCollisionCreateFunc(
        type0: BroadphaseNativeType, type1: BroadphaseNativeType
    ): CollisionAlgorithmCreateFunc {
        return doubleDispatch[type0.ordinal * NUM_COLLISION_TYPES + type1.ordinal]
    }

    fun registerCollisionCreateFunc(proxyType0: Int, proxyType1: Int, createFunc: CollisionAlgorithmCreateFunc) {
        doubleDispatch[proxyType0 * NUM_COLLISION_TYPES + proxyType1] = createFunc
    }

    fun registerCollisionCreateFunc(
        proxyType0: BroadphaseNativeType,
        proxyType1: BroadphaseNativeType,
        createFunc: CollisionAlgorithmCreateFunc
    ) = registerCollisionCreateFunc(
        proxyType0.ordinal,
        proxyType1.ordinal,
        createFunc
    )

    private val tmpCI = CollisionAlgorithmConstructionInfo()
    override fun findAlgorithm(
        body0: CollisionObject, body1: CollisionObject, sharedManifold: PersistentManifold?
    ): CollisionAlgorithm {
        val ci = tmpCI
        ci.dispatcher1 = this
        ci.manifold = sharedManifold
        val type0 = body0.collisionShape!!.shapeType
        val type1 = body1.collisionShape!!.shapeType
        val createFunc = getCollisionCreateFunc(type0, type1)
        val algo = createFunc.createCollisionAlgorithm(ci, body0, body1)
        algo.internalSetCreateFunc(createFunc)
        return algo
    }

    override fun freeCollisionAlgorithm(algo: CollisionAlgorithm) {
        val createFunc = algo.internalGetCreateFunc()
        algo.internalSetCreateFunc(null)
        createFunc!!.releaseCollisionAlgorithm(algo)
        algo.destroy()
    }

    override fun getNewManifold(body0: CollisionObject, body1: CollisionObject): PersistentManifold {
        val manifold = manifoldsPool.get()
        manifold.init(body0, body1)

        manifold.index1a = manifoldsList.size
        manifoldsList.add(manifold)

        return manifold
    }

    override fun releaseManifold(manifold: PersistentManifold) {
        clearManifold(manifold)

        val findIndex = manifold.index1a
        assert(findIndex < manifoldsList.size)
        Collections.swap(manifoldsList, findIndex, manifoldsList.lastIndex)
        manifoldsList[findIndex].index1a = findIndex
        manifoldsList.removeLast()

        manifoldsPool.release(manifold)
    }

    override fun clearManifold(manifold: PersistentManifold) {
        manifold.clearManifold()
    }

    override fun needsCollision(body0: CollisionObject, body1: CollisionObject): Boolean {
        var needsCollision = true

        if (!staticWarningReported) {
            // broadphase filtering already deals with this
            if (body0.isStaticOrKinematicObject && body1.isStaticOrKinematicObject) {
                staticWarningReported = true
                LOGGER.warn("needsCollision(): Static-static collision!?! Masks are probably misconfigured!")
            }
        }

        if (!body0.isActive && !body1.isActive) {
            needsCollision = false
        } else if (!body0.checkCollideWith(body1)) {
            needsCollision = false
        }

        return needsCollision
    }

    override fun needsResponse(body0: CollisionObject, body1: CollisionObject): Boolean {
        //here you can do filtering
        var hasResponse = (body0.hasContactResponse() && body1.hasContactResponse())
        //no response between two static/kinematic bodies:
        hasResponse = hasResponse && ((!body0.isStaticOrKinematicObject) || (!body1.isStaticOrKinematicObject))
        return hasResponse
    }

    private class CollisionPairCallback : OverlapCallback {
        private lateinit var dispatchInfo: DispatcherInfo
        private lateinit var dispatcher: CollisionDispatcher

        fun init(dispatchInfo: DispatcherInfo, dispatcher: CollisionDispatcher) {
            this.dispatchInfo = dispatchInfo
            this.dispatcher = dispatcher
        }

        override fun processOverlap(pair: BroadphasePair): Boolean {
            dispatcher.nearCallback.handleCollision(pair, dispatcher, dispatchInfo)
            return false
        }
    }

    override fun dispatchAllCollisionPairs(
        pairCache: OverlappingPairCache, dispatchInfo: DispatcherInfo, dispatcher: Dispatcher
    ) {
        collisionPairCallback.init(dispatchInfo, this)
        pairCache.processAllOverlappingPairs(collisionPairCallback, dispatcher)
    }

    override val numManifolds: Int
        get() = manifoldsList.size

    override fun getManifold(index: Int): PersistentManifold {
        return manifoldsList[index]
    }

    companion object {
        private val NUM_COLLISION_TYPES = BroadphaseNativeType.entries.size
        private val LOGGER = LogManager.getLogger(CollisionDispatcher::class)
    }
}
