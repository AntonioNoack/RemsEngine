package com.bulletphysics.collision.broadphase

import com.bulletphysics.collision.broadphase.Dbvt.ICollide

/**
 * Dbvt implementation by Nathanael Presson
 * @author jezek2
 */
class DbvtTreeCollider(var pbp: DbvtBroadphase) : ICollide() {
    override fun process(na: DbvtNode, nb: DbvtNode) {
        var pa = na.data as DbvtProxy
        var pb = nb.data as DbvtProxy
        if (DbvtAabbMm.intersect(pa.aabb, pb.aabb)) {
            if (pa.hashCode() > pb.hashCode()) {
                val tmp = pa
                pa = pb
                pb = tmp
            }
            pbp.overlappingPairCache.addOverlappingPair(pa, pb)
        }
    }
}
