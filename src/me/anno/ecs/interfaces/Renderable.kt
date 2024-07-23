package me.anno.ecs.interfaces

import me.anno.ecs.Transform
import me.anno.gpu.pipeline.Pipeline

/**
 * anything that can be rendered in a scene
 * */
interface Renderable {
    fun fill(pipeline: Pipeline, transform: Transform, clickId: Int): Int
    fun findDrawnSubject(searchedId: Int): Any? = null
}