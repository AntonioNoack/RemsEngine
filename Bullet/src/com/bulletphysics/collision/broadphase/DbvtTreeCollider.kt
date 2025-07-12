package com.bulletphysics.collision.broadphase

import com.bulletphysics.collision.broadphase.Dbvt.ICollide

/**
 * Dbvt implementation by Nathanael Presson
 * @author jezek2
 */
class DbvtTreeCollider(var pbp: DbvtBroadphase) : ICollide {
    override fun process(n1: DbvtNode, n2: DbvtNode) {
        var pa = n1.data as DbvtProxy
        var pb = n2.data as DbvtProxy
        if (pa.aabb.testAABB(pb.aabb)) {
            if (pa.hashCode() > pb.hashCode()) {
                val tmp = pa
                pa = pb
                pb = tmp
            }
            pbp.overlappingPairCache.addOverlappingPair(pa, pb)
        }
    }
}
