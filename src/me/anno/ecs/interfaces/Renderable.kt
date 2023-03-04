package me.anno.ecs.interfaces

import me.anno.ecs.Entity
import me.anno.gpu.pipeline.Pipeline

interface Renderable {
    fun fill(pipeline: Pipeline, entity: Entity, clickId: Int): Int
}