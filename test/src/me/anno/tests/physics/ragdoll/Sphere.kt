package me.anno.tests.physics.ragdoll

import me.anno.maths.Maths.sq
import org.joml.Vector3d

class Sphere(val position: Vector3d, var radius: Double) {
    fun overlaps(other: Sphere): Boolean {
        val distanceSq = position.distanceSquared(other.position)
        val radiusSq = sq(radius + other.radius)
        return distanceSq <= radiusSq
    }
}