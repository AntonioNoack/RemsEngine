package me.anno.ecs.components.mesh

import me.anno.ecs.Component

// todo animated mesh renderer as well...
// todo which then uses bones :)

class MeshRenderer : Component() {

    val meshes = ArrayList<MeshComponent>()

    var mesh: MeshComponent?
        get() = meshes.getOrNull(0)
        set(value) {
            meshes.clear()
            if (value != null) meshes.add(value)
        }

    override val className get() = "MeshRenderer"

}