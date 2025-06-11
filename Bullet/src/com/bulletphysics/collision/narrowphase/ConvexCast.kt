package com.bulletphysics.collision.narrowphase

import com.bulletphysics.linearmath.Transform

/**
 * ConvexCast is an interface for casting.
 *
 * @author jezek2
 */
interface ConvexCast {
    /**
     * Cast a convex against another convex object.
     */
    fun calcTimeOfImpact(
        fromA: Transform, toA: Transform,
        fromB: Transform, toB: Transform,
        result: CastResult
    ): Boolean
}
