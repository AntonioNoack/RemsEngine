package com.bulletphysics.collision.narrowphase

import com.bulletphysics.BulletGlobals
import com.bulletphysics.collision.dispatch.CollisionObject
import com.bulletphysics.linearmath.Transform
import com.bulletphysics.linearmath.VectorUtil.closestAxis4
import cz.advel.stack.Stack
import com.bulletphysics.util.setScale
import com.bulletphysics.util.setSub

/**
 * PersistentManifold is a contact point cache, it stays persistent as long as objects
 * are overlapping in the broadphase. Those contact points are created by the collision
 * narrow phase.
 *
 *
 *
 *
 * The cache can be empty, or hold 1, 2, 3 or 4 points. Some collision algorithms (GJK)
 * might only add one point at a time, updates/refreshes old contact points, and throw
 * them away if necessary (distance becomes too large).
 *
 *
 *
 *
 * Reduces the cache to 4 points, when more then 4 points are added, using following rules:
 * the contact point with deepest penetration is always kept, and it tries to maximize the
 * area covered by the points.
 *
 *
 *
 *
 * Note that some pairs of objects might have more then one contact manifold.
 *
 * @author jezek2
 */
class PersistentManifold {
    private val pointCache = arrayOf(
        ManifoldPoint(),
        ManifoldPoint(),
        ManifoldPoint(),
        ManifoldPoint()
    )

    /**
     * these two body pointers can point to the physics rigidbody class.
     */
    var body0: CollisionObject? = null
    private var body1: CollisionObject? = null
    var numContacts: Int = 0
        private set

    var index1a: Int = 0

    fun init(body0: CollisionObject?, body1: CollisionObject?) {
        this.body0 = body0
        this.body1 = body1
        this.numContacts = 0
        index1a = 0
    }

    // sort cached points so most isolated points come first
    private fun sortCachedPoints(pt: ManifoldPoint): Int {
        //calculate 4 possible cases areas, and take biggest area
        //also need to keep 'deepest'

        var maxPenetrationIndex = -1
        //#define KEEP_DEEPEST_POINT 1
//#ifdef KEEP_DEEPEST_POINT
        var maxPenetration = pt.distance
        for (i in 0..3) {
            if (pointCache[i].distance < maxPenetration) {
                maxPenetrationIndex = i
                maxPenetration = pointCache[i].distance
            }
        }

        //#endif //KEEP_DEEPEST_POINT
        var res0 = 0.0
        var res1 = 0.0
        var res2 = 0.0
        var res3 = 0.0
        if (maxPenetrationIndex != 0) {
            res0 = getPenetration(pt, 1, 3, 2)
        }

        if (maxPenetrationIndex != 1) {
            res1 = getPenetration(pt, 0, 3, 2)
        }

        if (maxPenetrationIndex != 2) {
            res2 = getPenetration(pt, 0, 3, 1)
        }

        if (maxPenetrationIndex != 3) {
            res3 = getPenetration(pt, 0, 2, 1)
        }

        return closestAxis4(res0, res1, res2, res3)
    }

    private fun getPenetration(pt: ManifoldPoint, i: Int, j: Int, k: Int): Double {
        val a3 = Stack.newVec(pt.localPointA)
        a3.sub(pointCache[i].localPointA)

        val b3 = Stack.newVec(pointCache[j].localPointA)
        b3.sub(pointCache[k].localPointA)

        val cross = Stack.newVec()
        a3.cross(b3, cross)
        val res3 = cross.lengthSquared()
        Stack.subVec(3)
        return res3
    }

    //private int findContactPoint(ManifoldPoint unUsed, int numUnused, ManifoldPoint pt);
    fun getBody0(): Any {
        return body0!!
    }

    fun getBody1(): Any {
        return body1!!
    }

    fun setBodies(body0: CollisionObject, body1: CollisionObject) {
        this.body0 = body0
        this.body1 = body1
    }

    fun clearUserCache(pt: ManifoldPoint) {
        val userPersistentData = pt.userPersistentData
        if (userPersistentData != null) {
            BulletGlobals.contactDestroyedCallback?.contactDestroyed(userPersistentData)
            pt.userPersistentData = null
        }
    }

    fun getContactPoint(index: Int): ManifoldPoint {
        return pointCache[index]
    }

    val contactBreakingThreshold: Double
        // todo: get this margin from the current physics / collision environment
        get() = BulletGlobals.contactBreakingThreshold

    fun getCacheEntry(newPoint: ManifoldPoint): Int {
        var shortestDist = this.contactBreakingThreshold * this.contactBreakingThreshold
        val size = this.numContacts
        var nearestPoint = -1
        val diffA = Stack.newVec()
        for (i in 0 until size) {
            val mp = pointCache[i]

            diffA.setSub(mp.localPointA, newPoint.localPointA)

            val distToManiPoint = diffA.dot(diffA)
            if (distToManiPoint < shortestDist) {
                shortestDist = distToManiPoint
                nearestPoint = i
            }
        }
        return nearestPoint
    }

    fun addManifoldPoint(newPoint: ManifoldPoint): Int {
        assert(validContactDistance(newPoint))

        var insertIndex = this.numContacts
        if (insertIndex == pointCache.size) {
            if (pointCache.size >= 4) {
                // sort cache so best points come first, based on area
                insertIndex = sortCachedPoints(newPoint)
            } else {
                //#else
                insertIndex = 0
            }
            clearUserCache(pointCache[insertIndex])
        } else {
            this.numContacts++
        }
        assert(pointCache[insertIndex].userPersistentData == null)
        pointCache[insertIndex].set(newPoint)
        return insertIndex
    }

    fun removeContactPoint(index: Int) {
        clearUserCache(pointCache[index])

        val lastUsedIndex = this.numContacts - 1
        if (index != lastUsedIndex) {
            // TODO: possible bug
            pointCache[index].set(pointCache[lastUsedIndex])
            //get rid of duplicated userPersistentData pointer
            pointCache[lastUsedIndex].userPersistentData = null
            pointCache[lastUsedIndex].appliedImpulse = 0.0
            pointCache[lastUsedIndex].lateralFrictionInitialized = false
            pointCache[lastUsedIndex].appliedImpulseLateral1 = 0.0
            pointCache[lastUsedIndex].appliedImpulseLateral2 = 0.0
            pointCache[lastUsedIndex].lifeTime = 0
        }

        assert(pointCache[lastUsedIndex].userPersistentData == null)
        this.numContacts--
    }

    fun replaceContactPoint(newPoint: ManifoldPoint, insertIndex: Int) {
        assert(validContactDistance(newPoint))

        val lifeTime = pointCache[insertIndex].lifeTime
        val appliedImpulse = pointCache[insertIndex].appliedImpulse
        val appliedLateralImpulse1 = pointCache[insertIndex].appliedImpulseLateral1
        val appliedLateralImpulse2 = pointCache[insertIndex].appliedImpulseLateral2

        assert(lifeTime >= 0)
        val cache = pointCache[insertIndex].userPersistentData

        pointCache[insertIndex].set(newPoint)

        pointCache[insertIndex].userPersistentData = cache
        pointCache[insertIndex].appliedImpulse = appliedImpulse
        pointCache[insertIndex].appliedImpulseLateral1 = appliedLateralImpulse1
        pointCache[insertIndex].appliedImpulseLateral2 = appliedLateralImpulse2

        pointCache[insertIndex].lifeTime = lifeTime
    }

    private fun validContactDistance(pt: ManifoldPoint): Boolean {
        return pt.distance <= this.contactBreakingThreshold
    }

    // calculated new worldspace coordinates and depth, and reject points that exceed the collision margin
    fun refreshContactPoints(trA: Transform, trB: Transform) {
        val tmp = Stack.newVec()
        var i: Int
        //#ifdef DEBUG_PERSISTENCY
//	printf("refreshContactPoints posA = (%f,%f,%f) posB = (%f,%f,%f)\n",
//		trA.getOrigin().getX(),
//		trA.getOrigin().getY(),
//		trA.getOrigin().getZ(),
//		trB.getOrigin().getX(),
//		trB.getOrigin().getY(),
//		trB.getOrigin().getZ());
//#endif //DEBUG_PERSISTENCY
        // first refresh worldspace positions and distance
        i = this.numContacts - 1
        while (i >= 0) {
            val manifoldPoint = pointCache[i]

            manifoldPoint.positionWorldOnA.set(manifoldPoint.localPointA)
            trA.transform(manifoldPoint.positionWorldOnA)

            manifoldPoint.positionWorldOnB.set(manifoldPoint.localPointB)
            trB.transform(manifoldPoint.positionWorldOnB)

            tmp.set(manifoldPoint.positionWorldOnA)
            tmp.sub(manifoldPoint.positionWorldOnB)
            manifoldPoint.distance = tmp.dot(manifoldPoint.normalWorldOnB)

            manifoldPoint.lifeTime++
            i--
        }

        // then
        val projectedDifference = Stack.newVec()
        val projectedPoint = Stack.newVec()

        i = this.numContacts - 1
        while (i >= 0) {
            val manifoldPoint = pointCache[i]
            // contact becomes invalid when signed distance exceeds margin (projected on contactnormal direction)
            if (!validContactDistance(manifoldPoint)) {
                removeContactPoint(i)
            } else {
                // contact also becomes invalid when relative movement orthogonal to normal exceeds margin
                tmp.setScale(manifoldPoint.distance, manifoldPoint.normalWorldOnB)
                projectedPoint.setSub(manifoldPoint.positionWorldOnA, tmp)
                projectedDifference.setSub(manifoldPoint.positionWorldOnB, projectedPoint)
                val distance2d = projectedDifference.dot(projectedDifference)
                if (distance2d > this.contactBreakingThreshold * this.contactBreakingThreshold) {
                    removeContactPoint(i)
                } else {
                    // contact point processed callback
                    BulletGlobals.contactProcessedCallback
                        ?.contactProcessed(manifoldPoint, body0!!, body1!!)
                }
            }
            i--
        }

        Stack.subVec(3)
    }

    fun clearManifold() {
        for (i in 0 until this.numContacts) {
            clearUserCache(pointCache[i])
        }
        this.numContacts = 0
    }
}
