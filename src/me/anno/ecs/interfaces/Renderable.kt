package me.anno.ecs.interfaces

import me.anno.ecs.Entity
import me.anno.gpu.pipeline.Pipeline

/**
 * anything that can be rendered in a scene
 * */
interface Renderable {
    fun fill(pipeline: Pipeline, entity: Entity, clickId: Int): Int
    fun findDrawnSubject(searchedId: Int): Any? = null
}