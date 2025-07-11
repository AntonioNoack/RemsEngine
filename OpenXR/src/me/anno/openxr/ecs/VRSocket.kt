package me.anno.openxr.ecs

import me.anno.ecs.Component
import me.anno.ecs.components.FillSpace
import org.joml.AABBd
import org.joml.Matrix4x3

class VRSocket : Component(), FillSpace {

    companion object {
        private val localBounds = AABBd(-0.5, 0.5)
    }

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
        localBounds.transformUnion(globalTransform, dstUnion)
    }
}