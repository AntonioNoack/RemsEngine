package me.anno.openxr.ecs

import me.anno.ecs.Component
import org.joml.AABBd
import org.joml.Matrix4x3d

class VRHandPickup : Component() {

    var shouldBeLockedInHand: Boolean = false
    var maxPickupDistance = 2.0

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        val s = 0.5
        AABBd(-s, -s, -s, s, s, s).transformUnion(globalTransform, aabb)
        return true
    }
}