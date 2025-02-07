package me.anno.ecs.interfaces

import me.anno.ecs.Transform
import me.anno.gpu.pipeline.Pipeline
import org.joml.AABBd

/**
 * anything that can be rendered in a scene
 * */
interface Renderable {
    fun fill(pipeline: Pipeline, transform: Transform)
    fun findDrawnSubject(searchedId: Int): Any? = null
    fun getGlobalBounds(): AABBd? = null
}