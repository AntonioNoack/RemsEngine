package com.bulletphysics.collision.broadphase

/**
 * Dbvt implementation by Nathanael Presson
 * @author jezek2
 */
class DbvtProxy(userPtr: Any?, collisionFilter: Int) :
    BroadphaseProxy(userPtr, collisionFilter) {

    val aabb: DbvtAabbMm = DbvtAabbMm()
    var leaf: DbvtNode? = null

    var stage: Int = 0

    var link0: DbvtProxy? = null
    var link1: DbvtProxy? = null
}
