package me.anno.ecs.interfaces

import me.anno.ecs.Entity
import me.anno.gpu.pipeline.Pipeline
import org.joml.Vector3d

interface Renderable {
    fun fill(pipeline: Pipeline, entity: Entity, clickId: Int, cameraPosition: Vector3d, worldScale: Double): Int
}