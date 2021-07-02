package me.anno.ecs.components.render

import me.anno.ecs.Component
import me.anno.ecs.components.mesh.Mesh

// todo animated mesh renderer as well...
// todo which then uses bones :)

class MeshRenderer : Component() {

    val meshes = ArrayList<Mesh>()

    var mesh: Mesh?
        get() = meshes.getOrNull(0)
        set(value) {
            meshes.clear()
            if (value != null) meshes.add(value)
        }

    override fun getClassName(): String = "MeshRenderer"

}