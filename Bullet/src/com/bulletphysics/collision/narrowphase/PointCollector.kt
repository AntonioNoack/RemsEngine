package com.bulletphysics.collision.narrowphase

import org.joml.Vector3d
import org.joml.Vector3f

/**
 * @author jezek2
 */
class PointCollector : DiscreteCollisionDetectorInterface.Result {

    val normalOnBInWorld = Vector3f()
    val pointInWorld = Vector3d()
    var distance = 1e38f // negative means penetration

    var hasResult: Boolean = false

    fun init() {
        distance = 1e38f
        hasResult = false
    }

    override fun setShapeIdentifiers(partId0: Int, index0: Int, partId1: Int, index1: Int) {
        // ??
    }

    override fun addContactPoint(normalOnBInWorld: Vector3f, pointInWorld: Vector3d, depth: Float) {
        if (depth < distance) {
            hasResult = true
            this.normalOnBInWorld.set(normalOnBInWorld)
            this.pointInWorld.set(pointInWorld)
            // negative means penetration
            distance = depth
        }
    }
}
