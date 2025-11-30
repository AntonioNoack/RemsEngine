package com.bulletphysics.collision.dispatch

import com.bulletphysics.collision.broadphase.BroadphaseProxy
import com.bulletphysics.collision.broadphase.Dispatcher
import com.bulletphysics.collision.dispatch.CollisionWorld.ConvexResultCallback
import com.bulletphysics.collision.dispatch.CollisionWorld.RayResultCallback
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.Transform
import cz.advel.stack.Stack
import me.anno.utils.InternalAPI
import me.anno.utils.structures.lists.Lists.swapRemove
import me.anno.utils.structures.lists.Lists.swapRemoveAt
import org.joml.Vector3d

/**
 * GhostObject can keep track of all objects that are overlapping. By default, this
 * overlap is based on the AABB. This is useful for creating a character controller,
 * collision sensors/triggers, explosions etc.
 *
 * @author tomrbryn
 */
open class GhostObject : CollisionObject() {

    val overlappingPairs = ArrayList<CollisionObject>()

    @InternalAPI
    open fun addOverlappingObjectInternal(otherProxy: BroadphaseProxy, thisProxy: BroadphaseProxy?) {
        val otherObject = otherProxy.clientObject
        // if this linearSearch becomes too slow (too many overlapping objects) we should add a more appropriate data structure
        val index = overlappingPairs.indexOf(otherObject)
        if (index == -1) {
            // not found
            overlappingPairs.add(otherObject)
        }
    }

    @InternalAPI
    open fun removeOverlappingObjectInternal(
        otherProxy: BroadphaseProxy,
        dispatcher: Dispatcher,
        thisProxy: BroadphaseProxy?
    ) {
        val otherObject = otherProxy.clientObject
        overlappingPairs.swapRemove(otherObject)
    }

    fun convexSweepTest(
        selfShape: ConvexShape,
        convexFromWorld: Transform,
        convexToWorld: Transform,
        resultCallback: ConvexResultCallback,
        allowedCcdPenetration: Float
    ) {
        CollisionWorld.convexSweepTest(
            selfShape, convexFromWorld, convexToWorld,
            resultCallback, allowedCcdPenetration,
            overlappingPairs
        )
    }

    @Suppress("unused")
    fun rayTest(rayFromWorld: Vector3d, rayToWorld: Vector3d, resultCallback: RayResultCallback) {
        val rayFromTrans = Stack.newTrans()
        rayFromTrans.setIdentity()
        rayFromTrans.setTranslation(rayFromWorld)

        for (i in overlappingPairs.indices) {
            val collisionObject = overlappingPairs[i]

            // only perform raycast if filterMask matches
            if (resultCallback.needsCollision(collisionObject.broadphaseHandle!!)) {
                CollisionWorld.rayTestSingle(
                    rayFromTrans, rayToWorld, collisionObject,
                    collisionObject.collisionShape!!,
                    collisionObject.worldTransform,
                    resultCallback
                )
            }
        }

        Stack.subTrans(1)
    }
}
