package com.bulletphysics.collision.shapes

import org.joml.Vector3d

/**
 * ConcaveShape class provides an interface for non-moving (static) concave shapes.
 *
 * @author jezek2
 */
abstract class ConcaveShape : CollisionShape() {

    var collisionMargin = 0f

    override var margin: Float
        get() = collisionMargin
        set(value) {
            collisionMargin = value
        }

    abstract fun processAllTriangles(callback: TriangleCallback, aabbMin: Vector3d, aabbMax: Vector3d)
}
