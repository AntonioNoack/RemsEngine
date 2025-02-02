package me.anno.openxr.ecs

import me.anno.ecs.Component
import org.joml.AABBd
import org.joml.Matrix4x3d

class VRTeleportAnchor : Component() {
    override fun fillSpace(globalTransform: Matrix4x3d, dstUnion: AABBd): Boolean {
        val s = 0.5
        AABBd(-s, -s, -s, s, s, s).transformUnion(globalTransform, dstUnion)
        return true
    }
}