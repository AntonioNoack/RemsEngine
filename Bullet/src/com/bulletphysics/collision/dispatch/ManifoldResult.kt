package com.bulletphysics.collision.dispatch

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.narrowphase.DiscreteCollisionDetectorInterface
import com.bulletphysics.collision.narrowphase.ManifoldPoint
import com.bulletphysics.collision.narrowphase.PersistentManifold
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.util.ObjectPool
import cz.advel.stack.Stack
import org.joml.Vector3d
import com.bulletphysics.util.setScaleAdd

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

    private var body0: CollisionObject? = null
    private var body1: CollisionObject? = null

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

    override fun addContactPoint(normalOnBInWorld: Vector3d, pointInWorld: Vector3d, depth: Double) {
        val manifold = checkNotNull(manifold)

        //order in manifold needs to match
        if (depth > manifold.contactBreakingThreshold) {
            return
        }

        val isSwapped = manifold.body0 !== body0

        val pointA = Stack.newVec()
        pointA.setScaleAdd(depth, normalOnBInWorld, pointInWorld)

        val localA = Stack.newVec()
        val localB = Stack.newVec()

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

        newPt.combinedFriction = calculateCombinedFriction(body0!!, body1!!)
        newPt.combinedRestitution = calculateCombinedRestitution(body0!!, body1!!)

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

        val body0 = body0!!
        val body1 = body1!!
        // User can override friction and/or restitution
        if (  // and if either of the two bodies requires custom material
            ((body0.collisionFlags and CollisionFlags.CUSTOM_MATERIAL_CALLBACK) != 0 ||
                    (body1.collisionFlags and CollisionFlags.CUSTOM_MATERIAL_CALLBACK) != 0)
        ) {
            //experimental feature info, for per-triangle material etc.
            val obj0 = if (isSwapped) body1 else body0
            val obj1 = if (isSwapped) body0 else body1
            BulletGlobals.contactAddedCallback
                ?.contactAdded(manifold.getContactPoint(insertIndex), obj0, partId0, index0, obj1, partId1, index1)
        }

        pointsPool.release(newPt)
    }

    fun refreshContactPoints() {
        val manifold = checkNotNull(manifold)
        if (manifold.numContacts == 0) {
            return
        }

        val isSwapped = manifold.body0 !== body0

        if (isSwapped) {
            manifold.refreshContactPoints(rootTransB, rootTransA)
        } else {
            manifold.refreshContactPoints(rootTransA, rootTransB)
        }
    }

    companion object {
        // User can override this material combiner by implementing gContactAddedCallback and setting body0->m_collisionFlags |= btCollisionObject::customMaterialCallback;
        private fun calculateCombinedFriction(body0: CollisionObject, body1: CollisionObject): Double {
            var friction = body0.friction * body1.friction

            val MAX_FRICTION = 10.0
            if (friction < -MAX_FRICTION) {
                friction = -MAX_FRICTION
            }
            if (friction > MAX_FRICTION) {
                friction = MAX_FRICTION
            }
            return friction
        }

        private fun calculateCombinedRestitution(body0: CollisionObject, body1: CollisionObject): Double {
            return body0.restitution * body1.restitution
        }
    }
}
