package me.anno.ecs.components

import org.joml.AABBd
import org.joml.Matrix4x3

interface FillSpace {
    /**
     * fills the global transform with its bounds
     * */
    fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd)
}