package com.bulletphysics.collision.broadphase

import com.bulletphysics.collision.dispatch.CollisionObject
import org.joml.AABBd

/**
 * Dbvt implementation by Nathanael Presson
 * @author jezek2
 */
class DbvtProxy(userPtr: CollisionObject, collisionFilter: Int) :
    BroadphaseProxy(userPtr, collisionFilter) {

    val aabb = AABBd()
    var leaf: DbvtNode? = null

    var stage: Int = 0

    var link0: DbvtProxy? = null
    var link1: DbvtProxy? = null
}
