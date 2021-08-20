package me.anno.ecs.components.mesh

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.gpu.shader.Shader

abstract class RendererComponent: Component() {

    open fun defineVertexTransform(shader: Shader, entity: Entity, mesh: Mesh) {
        shader.v1("hasAnimation", false)
    }

}