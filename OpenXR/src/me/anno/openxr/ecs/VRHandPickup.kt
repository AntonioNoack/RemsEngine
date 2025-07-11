package me.anno.openxr.ecs

import me.anno.ecs.Component
import me.anno.ecs.components.FillSpace
import org.joml.AABBd
import org.joml.Matrix4x3

class VRHandPickup : Component(), FillSpace {
    companion object {
        private val localAABB = AABBd(-0.5, 0.5)
    }

    var shouldBeLockedInHand: Boolean = false
    var maxPickupDistance = 2.0

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
        localAABB.transformUnion(globalTransform, dstUnion)
    }
}