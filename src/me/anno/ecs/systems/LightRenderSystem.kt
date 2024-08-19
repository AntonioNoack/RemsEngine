package me.anno.ecs.systems

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.System
import me.anno.ecs.Transform
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.interfaces.Renderable
import me.anno.gpu.pipeline.Pipeline

// todo spatial acceleration systems for these rendering systems:
//  - we don't want a deeply nested tree, because slow
//  - we don't want a flat list, because non-accelerate-able for millions of items
//  -> shallow trees, with 16-32 items each (?)
class LightRenderSystem() : System(), Renderable {

    val lights = HashSet<LightComponent>(512)
    val skyboxes = HashSet<Skybox>(16)

    override fun onEnable(component: Component) {
        when (component) {
            is LightComponent -> lights.add(component)
            is Skybox -> skyboxes.add(component)
        }
    }

    override fun fill(pipeline: Pipeline, transform: Transform, clickId: Int): Int {
        for (c in lights) {
            val e = c.entity ?: continue
            if (pipeline.frustum.isVisible(e.getBounds())) {
                pipeline.addLight(c, e)
            }
        }
        return clickId
    }

    override fun clear() {
        lights.clear()
        skyboxes.clear()
    }
}