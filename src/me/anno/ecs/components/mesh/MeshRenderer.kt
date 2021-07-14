package me.anno.ecs.components.mesh

import me.anno.ecs.Entity
import me.anno.gpu.shader.Shader

// todo animated mesh renderer as well...
// todo which then uses bones :)

class MeshRenderer : RendererComponent() {

    val meshes = ArrayList<MeshComponent>()

    var mesh: MeshComponent?
        get() = meshes.getOrNull(0)
        set(value) {
            meshes.clear()
            if (value != null) meshes.add(value)
        }

    override fun defineVertexTransform(shader: Shader, entity: Entity, mesh: Mesh) {
        shader.v1("hasAnimation", 0f)
    }

    override val className get() = "MeshRenderer"

}