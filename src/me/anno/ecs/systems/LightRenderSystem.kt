package me.anno.ecs.systems

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.System
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.shaders.Skybox
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

    override fun onEnable(childSystem: System) {
        if (childSystem is LightRenderSystem) {
            lights.addAll(childSystem.lights)
            skyboxes.addAll(childSystem.skyboxes)
        }
    }

    override fun onDisable(childSystem: System) {
        if (childSystem is LightRenderSystem) {
            lights.removeAll(childSystem.lights)
            skyboxes.removeAll(childSystem.skyboxes)
        }
    }

    override fun fill(pipeline: Pipeline, entity: Entity, clickId: Int): Int {
        for (c in lights) {
            val e = c.entity ?: continue
            if (pipeline.frustum.isVisible(e.aabb)) {
                pipeline.addLight(c, e)
            }
        }
        return clickId
    }
}