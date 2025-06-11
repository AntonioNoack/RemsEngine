package com.bulletphysics.collision.narrowphase

import org.joml.Vector3d

/**
 * @author jezek2
 */
class PointCollector : DiscreteCollisionDetectorInterface.Result {

    val normalOnBInWorld: Vector3d = Vector3d()
    val pointInWorld: Vector3d = Vector3d()
    var distance: Double = 1e308 // negative means penetration

    var hasResult: Boolean = false

    fun init() {
        distance = 1e308
        hasResult = false
    }

    override fun setShapeIdentifiers(partId0: Int, index0: Int, partId1: Int, index1: Int) {
        // ??
    }

    override fun addContactPoint(normalOnBInWorld: Vector3d, pointInWorld: Vector3d, depth: Double) {
        if (depth < distance) {
            hasResult = true
            this.normalOnBInWorld.set(normalOnBInWorld)
            this.pointInWorld.set(pointInWorld)
            // negative means penetration
            distance = depth
        }
    }
}
