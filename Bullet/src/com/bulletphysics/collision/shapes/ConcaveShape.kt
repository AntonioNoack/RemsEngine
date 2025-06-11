package com.bulletphysics.collision.shapes

import org.joml.Vector3d

/**
 * ConcaveShape class provides an interface for non-moving (static) concave shapes.
 *
 * @author jezek2
 */
abstract class ConcaveShape : CollisionShape() {

    var collisionMargin: Double = 0.0

    override var margin: Double
        get() = collisionMargin
        set(value) {
            collisionMargin = value
        }

    abstract fun processAllTriangles(callback: TriangleCallback, aabbMin: Vector3d, aabbMax: Vector3d)
}
