package com.bulletphysics.collision.shapes

import com.bulletphysics.linearmath.Transform

/**
 * Compound shape child.
 *
 * @author jezek2
 */
data class CompoundShapeChild(
    val transform: Transform,
    val childShape: CollisionShape
)
