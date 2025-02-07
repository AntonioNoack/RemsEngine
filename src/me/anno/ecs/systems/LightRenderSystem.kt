package me.anno.ecs.systems

import me.anno.ecs.Component
import me.anno.ecs.System
import me.anno.ecs.Transform
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.interfaces.Renderable
import me.anno.gpu.pipeline.Pipeline
import me.anno.utils.structures.Collections.setContains

// todo spatial acceleration systems for these rendering systems:
//  - we don't want a deeply nested tree, because slow
//  - we don't want a flat list, because non-accelerate-able for millions of items
//  -> shallow trees, with 16-32 items each (?)
class LightRenderSystem() : System(), Renderable {

    val lights = HashSet<LightComponent>(512)
    val skyboxes = HashSet<Skybox>(16)

    override fun setContains(component: Component, contains: Boolean) {
        when (component) {
            is LightComponent -> lights.setContains(component, contains)
            is Skybox -> skyboxes.setContains(component, contains)
        }
    }

    override fun fill(pipeline: Pipeline, transform: Transform) {
        for (c in lights) {
            val e = c.entity ?: continue
            if (pipeline.frustum.isVisible(e.getGlobalBounds())) {
                pipeline.addLight(c, e)
            }
        }
    }

    override fun clear() {
        lights.clear()
        skyboxes.clear()
    }
}