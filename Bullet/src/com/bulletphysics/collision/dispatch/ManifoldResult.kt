package com.bulletphysics.collision.dispatch

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.narrowphase.DiscreteCollisionDetectorInterface
import com.bulletphysics.collision.narrowphase.ManifoldPoint
import com.bulletphysics.collision.narrowphase.PersistentManifold
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.util.ObjectPool
import cz.advel.stack.Stack
import me.anno.maths.Maths.clamp
import me.anno.utils.types.Booleans.hasFlag
import org.joml.Vector3d
import org.joml.Vector3f

/**
 * ManifoldResult is helper class to manage contact results.
 *
 * @author jezek2
 */
class ManifoldResult : DiscreteCollisionDetectorInterface.Result {

    val pointsPool: ObjectPool<ManifoldPoint> = ObjectPool.get(ManifoldPoint::class.java)

    private var manifold: PersistentManifold? = null

    // we need this for compounds
    private val rootTransA = Transform()
    private val rootTransB = Transform()

    private lateinit var body0: CollisionObject
    private lateinit var body1: CollisionObject

    private var partId0 = 0
    private var partId1 = 0

    private var index0 = 0
    private var index1 = 0

    constructor()

    constructor(body0: CollisionObject, body1: CollisionObject) {
        init(body0, body1)
    }

    fun init(body0: CollisionObject, body1: CollisionObject) {
        this.body0 = body0
        this.body1 = body1
        body0.getWorldTransform(this.rootTransA)
        body1.getWorldTransform(this.rootTransB)
    }

    @get:Suppress("unused")
    var persistentManifold: PersistentManifold
        get() = manifold!!
        set(manifoldPtr) {
            this.manifold = manifoldPtr
        }

    override fun setShapeIdentifiers(partId0: Int, index0: Int, partId1: Int, index1: Int) {
        this.partId0 = partId0
        this.partId1 = partId1
        this.index0 = index0
        this.index1 = index1
    }

    override fun addContactPoint(normalOnBInWorld: Vector3f, pointInWorld: Vector3d, depth: Float) {
        val manifold = manifold
        // order in manifold needs to match
        if (manifold == null || depth > manifold.contactBreakingThreshold) {
            return
        }

        val isSwapped = manifold.body0 !== body0
        val pointA = Stack.newVec3d()
        normalOnBInWorld.mulAdd(depth, pointInWorld, pointA)

        val localA = Stack.newVec3d()
        val localB = Stack.newVec3d()

        if (isSwapped) {
            rootTransB.invXform(pointA, localA)
            rootTransA.invXform(pointInWorld, localB)
        } else {
            rootTransA.invXform(pointA, localA)
            rootTransB.invXform(pointInWorld, localB)
        }

        val newPt = pointsPool.get()
        newPt.init(localA, localB, normalOnBInWorld, depth)

        newPt.positionWorldOnA.set(pointA)
        newPt.positionWorldOnB.set(pointInWorld)

        var insertIndex = manifold.getCacheEntry(newPt)

        val body0 = body0
        val body1 = body1
        newPt.combinedFriction = calculateCombinedFriction(body0, body1)
        newPt.combinedRestitution = calculateCombinedRestitution(body0, body1)

        // BP mod, store contact triangles.
        newPt.partId0 = partId0
        newPt.partId1 = partId1
        newPt.index0 = index0
        newPt.index1 = index1

        // todo, check this for any side effects
        if (insertIndex >= 0) {
            //const btManifoldPoint& oldPoint = m_manifoldPtr->getContactPoint(insertIndex);
            manifold.replaceContactPoint(newPt, insertIndex)
        } else {
            insertIndex = manifold.addManifoldPoint(newPt)
        }

        // User can override friction and/or restitution
        // and if either of the two bodies requires custom material
        if ((body0.collisionFlags or body1.collisionFlags).hasFlag(CollisionFlags.CUSTOM_MATERIAL_CALLBACK)) {
            //experimental feature info, for per-triangle material etc.
            val obj0 = if (isSwapped) body1 else body0
            val obj1 = if (isSwapped) body0 else body1
            BulletGlobals.contactAddedCallback?.contactAdded(
                manifold.getContactPoint(insertIndex),
                obj0, partId0, index0,
                obj1, partId1, index1
            )
        }

        Stack.subVec3d(3)
        pointsPool.release(newPt)
    }

    fun refreshContactPoints() {
        val manifold = checkNotNull(manifold)
        if (manifold.numContacts == 0) return

        val swapped = manifold.body0 !== body0
        if (swapped) {
            manifold.refreshContactPoints(rootTransB, rootTransA)
        } else {
            manifold.refreshContactPoints(rootTransA, rootTransB)
        }
    }

    companion object {
        private const val MAX_FRICTION = 10f

        // User can override this material combiner by implementing contactAddedCallback and setting body0->m_collisionFlags |= btCollisionObject::customMaterialCallback;
        private fun calculateCombinedFriction(body0: CollisionObject, body1: CollisionObject): Float {
            val combined = body0.friction * body1.friction
            return clamp(combined, -MAX_FRICTION, MAX_FRICTION)
        }

        private fun calculateCombinedRestitution(body0: CollisionObject, body1: CollisionObject): Float {
            return body0.restitution * body1.restitution
        }
    }
}
