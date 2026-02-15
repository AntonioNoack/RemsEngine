package me.anno.ecs.components.mesh.grid

import org.joml.Vector3d

interface DropPositionAdjuster {
    fun adjust(position: Vector3d)
}