package com.bulletphysics.collision.dispatch

import com.bulletphysics.collision.broadphase.*
import com.bulletphysics.collision.narrowphase.PersistentManifold
import com.bulletphysics.util.ObjectPool
import java.util.*

/**
 * CollisionDispatcher supports algorithms that handle ConvexConvex and ConvexConcave collision pairs.
 * Time of Impact, Closest Points and Penetration Depth.
 *
 * @author jezek2
 */
class CollisionDispatcher(collisionConfiguration: CollisionConfiguration) : Dispatcher {

    val manifoldsPool: ObjectPool<PersistentManifold> = ObjectPool.get(PersistentManifold::class.java)

    private val manifoldsPtr = ArrayList<PersistentManifold>()
    private var staticWarningReported = false

    @JvmField
    var nearCallback: NearCallback? = null

    private val doubleDispatch = Array<Array<CollisionAlgorithmCreateFunc?>>(MAX_BROADPHASE_COLLISION_TYPES) {
        arrayOfNulls(MAX_BROADPHASE_COLLISION_TYPES)
    }

    private val tmpCI = CollisionAlgorithmConstructionInfo()

    fun registerCollisionCreateFunc(proxyType0: Int, proxyType1: Int, createFunc: CollisionAlgorithmCreateFunc?) {
        doubleDispatch[proxyType0][proxyType1] = createFunc!!
    }

    override fun findAlgorithm(
        body0: CollisionObject, body1: CollisionObject, sharedManifold: PersistentManifold?
    ): CollisionAlgorithm {
        val ci = tmpCI
        ci.dispatcher1 = this
        ci.manifold = sharedManifold
        val createFunc =
            doubleDispatch[body0.collisionShape!!.shapeType.ordinal][body1.collisionShape!!.shapeType.ordinal]
        val algo = createFunc!!.createCollisionAlgorithm(ci, body0, body1)
        algo.internalSetCreateFunc(createFunc)
        return algo
    }

    override fun freeCollisionAlgorithm(algo: CollisionAlgorithm) {
        val createFunc = algo.internalGetCreateFunc()
        algo.internalSetCreateFunc(null)
        createFunc!!.releaseCollisionAlgorithm(algo)
        algo.destroy()
    }

    override fun getNewManifold(body0: Any, body1: Any): PersistentManifold {
        val body0 = body0 as CollisionObject?
        val body1 = body1 as CollisionObject?

        val manifold = manifoldsPool.get()
        manifold.init(body0, body1)

        manifold.index1a = manifoldsPtr.size
        manifoldsPtr.add(manifold)

        return manifold
    }

    override fun releaseManifold(manifold: PersistentManifold) {
        clearManifold(manifold)

        val findIndex = manifold.index1a
        assert(findIndex < manifoldsPtr.size)
        Collections.swap(manifoldsPtr, findIndex, manifoldsPtr.lastIndex)
        manifoldsPtr[findIndex].index1a = findIndex
        manifoldsPtr.removeLast()

        manifoldsPool.release(manifold)
    }

    override fun clearManifold(manifold: PersistentManifold) {
        manifold.clearManifold()
    }

    override fun needsCollision(body0: CollisionObject, body1: CollisionObject): Boolean {
        checkNotNull(body0)
        checkNotNull(body1)

        var needsCollision = true

        if (!staticWarningReported) {
            // broadphase filtering already deals with this
            if ((body0.isStaticObject || body0.isKinematicObject) &&
                (body1.isStaticObject || body1.isKinematicObject)
            ) {
                staticWarningReported = true
                System.err.println("warning CollisionDispatcher.needsCollision: static-static collision!")
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
        private var dispatchInfo: DispatcherInfo? = null
        private var dispatcher: CollisionDispatcher? = null

        fun init(dispatchInfo: DispatcherInfo, dispatcher: CollisionDispatcher) {
            this.dispatchInfo = dispatchInfo
            this.dispatcher = dispatcher
        }

        override fun processOverlap(pair: BroadphasePair): Boolean {
            val dispatcher = dispatcher!!
            dispatcher.nearCallback!!.handleCollision(pair, dispatcher, dispatchInfo!!)
            return false
        }
    }

    private val collisionPairCallback = CollisionPairCallback()

    init {

        this.nearCallback = DefaultNearCallback()

        for (i in 0 until MAX_BROADPHASE_COLLISION_TYPES) {
            for (j in 0 until MAX_BROADPHASE_COLLISION_TYPES) {
                doubleDispatch[i][j] = collisionConfiguration.getCollisionAlgorithmCreateFunc(
                    BroadphaseNativeType.forValue(i),
                    BroadphaseNativeType.forValue(j)
                )
                checkNotNull(doubleDispatch[i][j])
            }
        }
    }

    override fun dispatchAllCollisionPairs(
        pairCache: OverlappingPairCache, dispatchInfo: DispatcherInfo, dispatcher: Dispatcher
    ) {
        collisionPairCallback.init(dispatchInfo, this)
        pairCache.processAllOverlappingPairs(collisionPairCallback, dispatcher)
    }

    override val numManifolds: Int
        get() = manifoldsPtr.size

    override fun getManifoldByIndexInternal(index: Int): PersistentManifold {
        return manifoldsPtr[index]
    }

    companion object {
        private val MAX_BROADPHASE_COLLISION_TYPES = BroadphaseNativeType.MAX_BROADPHASE_COLLISION_TYPES.ordinal
    }
}
