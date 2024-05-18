package me.anno.ecs.components.mesh.terrain

import org.joml.Vector3f

fun interface TerrainBrush {
    fun apply(point: Vector3f)
}