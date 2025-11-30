package com.bulletphysics.collision.narrowphase

import com.bulletphysics.linearmath.Transform
import org.joml.Vector3d

/**
 * ConvexCast is an interface for casting.
 *
 * @author jezek2
 */
interface ConvexCast {
    /**
     * Cast a convex against another convex object.
     * Returns whether a collision has happened.
     */
    fun calcTimeOfImpact(
        fromA: Transform, toA: Vector3d,
        fromB: Transform, toB: Vector3d,
        result: CastResult
    ): Boolean
}
