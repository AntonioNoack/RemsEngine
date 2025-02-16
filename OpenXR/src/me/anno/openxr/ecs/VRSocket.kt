package me.anno.openxr.ecs

import me.anno.ecs.Component
import org.joml.AABBd
import org.joml.Matrix4x3

class VRSocket : Component() {
    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd): Boolean {
        val s = 0.5
        AABBd(-s, -s, -s, s, s, s).transformUnion(globalTransform, dstUnion)
        return true
    }
}